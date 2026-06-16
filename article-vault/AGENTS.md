# ArticleVault — Agent Guide

## Build & Run

JAVA_HOME must point to Android Studio's bundled JBR:
```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installRelease   # build release + install to device (USE THIS for device installs)
.\gradlew.bat assembleDebug    # compile check only (fast, no R8)
```
ADB lives at `C:\Users\Arjun\platform-tools\adb.exe` (not on PATH by default).

**Always use `installRelease` for device installs.** Debug builds lack R8 optimization and make the app noticeably laggy. `assembleDebug` is fine for quick compile checks.

**Release signing** uses the debug keystore (`~/.android/debug.keystore`, alias `androiddebugkey`, password `android`) configured in `app/build.gradle.kts`. No separate release keystore exists — we reuse the debug key for local device testing.

## Architecture

Single-module Android app. All source under `app/src/main/java/com/articlevault/`.

| Layer | Key files |
|---|---|
| Data | `data/db/entity/Article.kt`, `data/db/dao/ArticleDao.kt`, `data/repository/ArticleRepository.kt` |
| Prefs | `data/ThemePreferences.kt`, `data/ReadingPreferences.kt`, `data/NotificationPreferences.kt` |
| DI | `di/AppModule.kt` — provides Room DB, WorkManager, DAOs |
| Workers | `worker/DownloadWorker.kt`, `ExtractWorker.kt`, `TagWorker.kt`, `SummarizeWorker.kt`, `DailyStatsWorker.kt` |
| Scheduling | `worker/DailyStatsScheduler.kt` — periodic work for daily notifications |
| ML | `ml/LlmSummarizer.kt`, `ml/TagClassifier.kt`, `ml/DomainClassifier.kt` |
| Models | `data/model/ModelRepository.kt`, `ui/models/ModelSelectionScreen.kt` |
| UI | `ui/MainActivity.kt` (nav host), `ui/list/`, `ui/detail/`, `ui/search/`, `ui/stats/` |

## Critical Gotchas

### Room Database
- **DB is on public external storage** (`/sdcard/ArticleVault/`), not internal app storage. Survives uninstalls.
- **Version must be bumped** in `AppDatabase.kt` when schema changes. Add migration in `AppModule.kt`.
- Current version: **7**. Migrations: 2→3 (add htmlContent), 3→4 (add summary), 4→5 (read flag + folders), 5→6 (htmlPath), 6→7 (wordCount, domain, readingProgress, readAt, lastOpenedAt).
- `Article.extractedText` maps to DB column `content` via `@ColumnInfo(name = "content")` — required for FTS4 `contentEntity` to match.
- FTS4 uses `@Fts4(contentEntity = Article::class)` — Room auto-syncs via triggers. No manual FTS insert needed.
- **Storage permission** (`MANAGE_EXTERNAL_STORAGE` on Android 11+) is required on first launch. The app gates on this and offers migration from old location.

### WorkManager
- All workers **must** have `@HiltWorker` annotation or they silently fail.
- Default WorkManager initializer is **disabled** in manifest (we use `Configuration.Provider` in `ArticleVaultApp`).
- Worker chain: `Download → Extract → Tag → Summarize`. No IndexWorker (DownloadWorker saves directly to DB).
- WorkManager `Data` has a **~10KB limit**. Large text must be saved to DB/files first, not passed through the chain.
- Periodic workers use `ExistingPeriodicWorkPolicy` (not `ExistingWorkPolicy`). For rescheduling on time change, cancel + re-enqueue with `REPLACE`.

### Daily Stats Notification
- `DailyStatsWorker` is a periodic `@HiltWorker` (24h interval, computed initial delay to hit user's chosen time).
- `DailyStatsScheduler` manages scheduling. Called on app startup (`ArticleVaultApp.onCreate()`) and when user changes prefs.
- `NotificationPreferences` stores: `enabled` (default `false`), `hour` (default `18` = 6 PM), `minute` (default `0`).
- Notification channel: `"daily_stats"` with `IMPORTANCE_DEFAULT` (separate from `"article_saved"` channel).
- Worker checks `NotificationManager.areNotificationsEnabled()` before posting — silently skips if notifications disabled.
- No new DAO queries today-scoped and injected into the worker directly via repository. No schema changes needed.
- Settings UI lives in `ModelSelectionScreen.kt` under "Notifications" section (toggle + `TimePicker` dialog via Material3).

### DownloadWorker
- WebView **must** run on `Dispatchers.Main`. Uses `withContext(Dispatchers.Main)` + `suspendCancellableCoroutine`.
- `return` is prohibited inside non-inline lambdas (callbacks). Use `if (!isDone) { ... }` instead.
- HTML is extracted via `evaluateJavascript` which returns JSON-encoded strings. Must unescape with `JSONTokener`.
- CSS is inlined into the HTML for offline viewing before saving.

### MediaPipe LlmInference
- Requires `.task` format (multi-prefill-seq). `.tflite` and `.litertlm` do **not** work.
- `Backend` enum is at `LlmInference.Backend`, not `LlmInferenceOptions.Backend`.
- Must set `setPreferredBackend(LlmInference.Backend.CPU)` or it crashes.
- LiteRT-LM (faster engine) requires Kotlin 2.3.0 — blocked by KSP compatibility. Do not attempt upgrade until KSP supports it.
- Models >500MB will OOM on devices with <2GB free RAM. Catch `OutOfMemoryError`.

### Hilt
- Workers use `@AssistedInject` + `@Assisted`. Do **not** put `@ApplicationContext` on `@Assisted` params — Dagger rejects it.
- `WorkManager` must be provided via `@Provides` in `AppModule` (not auto-injectable).

### UI
- Compose with Material3 dark theme. No light theme.
- Bottom nav bar hidden on `article/{id}` and `models` routes.
- Long-press on article cards → context menu (Summarize / Delete).
- Summaries stored in `Article.summary` column. Can be regenerated.
- `Icons.Default.Bookmark` does not exist — use `Icons.Default.Star` or `Icons.AutoMirrored.Filled.List`.
- No `material-icons-extended` dependency. Only default Material Icons are available. `Schedule`, `AccessTime`, `Notifications` etc. are NOT available — stick to the core set.

### ML Kit Entity Extraction
- Dependency: `com.google.mlkit:entity-extraction:16.0.0-beta6` (not stable).
- `com.google.mlkit:text-classification` **does not exist** as a standalone library.
- Keyword-based category classifier in `TagClassifier.kt` is the fallback.

## Key Dependency Versions

| Dep | Version | Notes |
|---|---|---|
| Kotlin | 2.1.20 | LiteRT-LM needs 2.3.0 (blocked) |
| KSP | 2.1.20-1.0.32 | Must match Kotlin |
| Hilt | 2.56.2 | |
| Room | 2.6.1 | |
| MediaPipe tasks-genai | 0.10.22 | LlmInference API |
| Compose BOM | 2024.12.01 | |
| minSdk | 26 | All devices support adaptive icons |

## Adding a New Room Migration

1. Bump `version` in `AppDatabase.kt`
2. Add `Migration(N-1, N)` object in `AppModule.provideDatabase()`
3. Add it to `.addMigrations(...)` chain
4. Do **not** use `fallbackToDestructiveMigration()` — it wipes user data

## File Locations on Device

| Path | Content |
|---|---|
| `/sdcard/ArticleVault/article_vault.db` | SQLite database (public, survives uninstalls) |
| `/sdcard/ArticleVault/articles/*.html` | Saved article HTML files (public, survives uninstalls) |
| `/data/data/com.articlevault/shared_prefs/model_prefs.json` | Selected model + HF token |
