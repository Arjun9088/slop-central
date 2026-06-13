# Agent Guide: ExpenseTracker

This file contains context for AI coding agents working on the ExpenseTracker Android app.

## Project Background

ExpenseTracker is a personal finance Android application that tracks expenses through three input methods:

1. **Manual entry** via a Compose form
2. **SMS auto-capture** from Indian bank/transaction SMS (UPI, cards, wallets)
3. **Receipt scanning** via camera + ML Kit OCR

Data is stored locally in a Room database and can be synced bi-directionally with Google Sheets.

## Tech Stack

- **Language**: Kotlin 2.1.20
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Dependency Injection**: Dagger Hilt 2.56.2
- **Database**: Room 2.6.1
- **Background Work**: WorkManager 2.10.0
- **Cloud Sync**: Google Sheets API v4, Google Drive API v3, Play Services Auth
- **OCR**: ML Kit Text Recognition 16.0.1
- **Build**: Gradle 8.11.1, Android Gradle Plugin 8.7.3

## Build Instructions

```bash
# Assemble debug APK
./gradlew :app:assembleDebug

# Assemble release APK (uses signing config in app/build.gradle.kts)
./gradlew :app:assembleRelease

# Install debug build on connected device
./gradlew :app:installDebug

# Install release build on connected device
./gradlew :app:installRelease
```

The release build is minified with R8/ProGuard.

## Project Structure

```
app/src/main/java/com/expensetracker/
├── ExpenseTrackerApp.kt          # Application class; configures Hilt WorkManager
├── di/AppModule.kt               # Hilt providers (Room DB, WorkManager)
├── data/
│   ├── db/                       # Room entities and DAOs
│   ├── repository/               # Repository wrappers
│   └── sync/                     # Google Sheets service, sync worker, preferences
├── sms/                          # SMS receiver and transaction parsing
└── ui/                           # Compose screens and ViewModels
```

## Architecture Conventions

- All ViewModels are Hilt-injected (`@HiltViewModel`) and expose UI state via `StateFlow`
- Screens use `collectAsStateWithLifecycle()` to collect flows
- Database operations are `suspend` functions; ViewModels launch them in `viewModelScope`
- The repository layer is thin; most code uses `ExpenseDao` directly through Hilt
- Background sync is performed by `SyncWorker`, a `@HiltWorker` scheduled periodically and on demand

## Key Entities

### Expense (`data/db/entity/Expense.kt`)

```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                          // ISO_LOCAL_DATE (yyyy-MM-dd)
    val description: String,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val syncedToSheet: Boolean = false,
    val sheetRowId: Long? = null,              // row number in linked Google Sheet
    val source: String = "manual",             // "manual", "sms", or "receipt"
    val dedupHash: String? = null              // for SMS deduplication
)
```

Default categories and payment methods are also defined in this file.

## Database Migrations

The database is at version 3. Migrations live in `di/AppModule.kt`:

- `1 → 2`: Added `modifiedAt INTEGER NOT NULL DEFAULT 0`
- `2 → 3`: Added `source TEXT NOT NULL DEFAULT 'manual'`, `dedupHash TEXT`

When changing the schema, increment the version in `AppDatabase.kt` and add a migration.

## Google Sheets Sync

Sync logic is in `data/sync/GoogleSheetsService.kt`.

- Sheets are expected to have headers: Date, Description, Category, Amount, Payment Method, Modified At
- Data is written to columns B:G (column A is intentionally left blank to allow formula columns)
- Sync uses `modifiedAt` timestamps for conflict resolution
- The sync worker runs every 15 minutes when network is available

OAuth setup requires the app's SHA-1 fingerprint to be registered in the Google Cloud Console.

## SMS Auto-Capture

The `SmsReceiver` is registered in `AndroidManifest.xml` with `android.permission.BROADCAST_SMS`. It parses transaction SMS in India using regex patterns for:

- Amount (Rs/INR/₹ formats)
- Merchant names (after "at", "to", "from", "@")
- Dates (dd/MM/yyyy, dd-MM-yyyy, dd MMM yyyy)
- Payment methods (UPI apps, card keywords, wallet keywords)

Classification is keyword-based in `CategoryClassifier.kt`.

## Notification Capture

`NotificationExpenseListener` is a `NotificationListenerService` fallback for devices where the default SMS app (e.g. Truecaller, Google Messages) does not propagate the standard `SMS_RECEIVED` broadcast. It listens for notifications from known SMS, banking, and UPI apps, extracts the notification text, and runs it through the same `TransactionParser` used by the SMS receiver.

Users must enable notification access for ExpenseTracker in system settings. The toggle is exposed in `SettingsScreen`. Known packages are maintained in `NotificationExpenseListener.knownPackages`.

## Receipt Scanning

The receipt flow:

1. User takes a photo via `ActivityResultContracts.TakePicture()`
2. Image URI is stored via `FileProvider`
3. ML Kit Text Recognition extracts text
4. `parseReceiptText()` finds the largest amount and a merchant name
5. User can edit fields before saving

## Adding a New Screen

1. Create a new package under `ui/`
2. Add a `*Screen.kt` Composable and a `*ViewModel.kt` Hilt ViewModel
3. Add the destination string and `composable` route in `MainActivity.kt`
4. Trigger navigation from an existing screen callback

## Testing

There are currently no automated tests in the project. If adding tests:

- Unit tests: `src/test/java/`
- Instrumentation tests: `src/androidTest/java/`
- Use Hilt's testing support for ViewModel/Repository tests

## Release / Deployment Notes

- Release builds are signed with `app/expense-tracker.keystore`
- The release keystore password and alias are hardcoded in `app/build.gradle.kts` for local builds
- Do not commit production keystore credentials to version control
- ProGuard rules are in `app/proguard-rules.pro`

## Common Pitfalls

- `SettingsViewModel.loadSettings()` currently stores the spreadsheet ID as the spreadsheet name. Update it if displaying the actual spreadsheet name.
- Always enqueue `SyncWorker.enqueueOneTime()` after insert/update/delete to keep the sheet in sync.
- Google Sheets API requires network and valid OAuth consent; handle `SheetsResult.ConsentRequired` in UI.
- The `SmsReceiver` runs in a broadcast context; use `goAsync()` + a coroutine, as currently implemented.
- Third-party SMS apps (Truecaller, Google Messages) may not propagate `SMS_RECEIVED`; rely on `NotificationExpenseListener` as the primary capture mechanism for those users.

## Code Style

- Use Compose Material 3 components
- Prefer `StateFlow` + `collectAsStateWithLifecycle()` over `remember` for view model state
- Format currency with `NumberFormat.getCurrencyInstance(Locale("en", "IN"))`
- Keep UI logic in ViewModels; keep screens focused on rendering
