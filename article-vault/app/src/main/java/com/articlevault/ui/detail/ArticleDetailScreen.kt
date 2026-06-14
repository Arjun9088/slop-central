package com.articlevault.ui.detail

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private fun loadReaderModeScript(context: Context): String {
    val readabilityJs = context.assets.open("vendor/mozilla/readability/0.6.0/Readability.min.js")
        .bufferedReader().use { it.readText() }
    val readerModeJs = context.assets.open("reader_mode.js")
        .bufferedReader().use { it.readText() }
    return readabilityJs + "\n" + readerModeJs
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val article by viewModel.article(articleId).collectAsStateWithLifecycle(null)
    val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()
    val showSearch by viewModel.showSearch.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val scrollStore = viewModel.scrollStore
    val readingPrefs = viewModel.readingPrefs
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadedArticleId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var fontSize by remember { mutableIntStateOf(readingPrefs.fontSizePercent) }
    var lastNotifiedProgress by remember { mutableFloatStateOf(0f) }
    var topBarVisible by remember { mutableStateOf(true) }
    var zapMode by remember { mutableStateOf(false) }
    var readerModeActive by remember { mutableStateOf(false) }
    var readerModeJsLoaded by remember { mutableStateOf(false) }
    val readerModeScript by remember { mutableStateOf(loadReaderModeScript(context)) }

    fun injectZapper(wv: WebView) {
        wv.evaluateJavascript("""
            (function(){
                window.__zapperActive = true;
                var s = document.createElement('style');
                s.id = '__zap';
                s.textContent = '*{cursor:crosshair!important}*:active{outline:3px solid rgba(255,60,60,0.9)!important;outline-offset:-2px!important}';
                document.head.appendChild(s);
                document.addEventListener('touchstart', function(e){
                    if(!window.__zapperActive) return;
                    var t = e.target;
                    if(!t||t===document.body||t===document.documentElement) return;
                    e.preventDefault();
                    e.stopPropagation();
                    t.style.setProperty('display','none','important');
                }, {passive:false, capture:true});
            })();
        """.trimIndent(), null)
    }

    fun deactivateZapper(wv: WebView) {
        wv.evaluateJavascript("""
            window.__zapperActive = false;
            var s = document.getElementById('__zap');
            if(s) s.remove();
        """.trimIndent(), null)
    }

    fun Color.toHexString(): String {
        val argb = toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }

    fun buildThemeJson(): String {
        val surface = colorScheme.surface
        val onSurface = colorScheme.onSurface
        val onSurfaceVariant = colorScheme.onSurfaceVariant
        val primary = colorScheme.primary
        val outlineVariant = colorScheme.outlineVariant
        val surfaceVariant = colorScheme.surfaceVariant
        val luminance = 0.2126f * surface.red + 0.7152f * surface.green + 0.0722f * surface.blue
        val isDark = luminance < 0.5f
        return """{
            isLight: ${!isDark},
            backgroundColor: "${surface.toHexString()}",
            textColor: "${onSurface.toHexString()}",
            headingColor: "${onSurface.toHexString()}",
            secondaryTextColor: "${onSurfaceVariant.toHexString()}",
            linkColor: "${primary.toHexString()}",
            dividerColor: "${outlineVariant.toHexString()}",
            codeBackgroundColor: "${surfaceVariant.toHexString()}"
        }"""
    }

    fun enableReaderMode(wv: WebView) {
        wv.settings.javaScriptEnabled = true
        val themeJson = buildThemeJson()
        val js = """
            $readerModeScript
            ArticleVaultReaderMode.setTheme($themeJson);
            ArticleVaultReaderMode.enable();
        """.trimIndent()
        wv.evaluateJavascript(js) { result ->
            val cleaned = result?.trim('"') ?: ""
            if (cleaned == "enabled") {
                readerModeActive = true
                readerModeJsLoaded = true
            } else {
                readerModeActive = false
            }
        }
    }

    fun disableReaderMode(wv: WebView) {
        wv.evaluateJavascript("ArticleVaultReaderMode.disable();") { result ->
            readerModeActive = false
        }
    }

    LaunchedEffect(article) {
        article?.let {
            viewModel.loadHtmlContent(it.htmlPath)
            viewModel.updateLastOpenedAt(articleId)
            readerModeActive = false
            readerModeJsLoaded = false
        }
    }

    DisposableEffect(articleId) {
        onDispose {
            webViewRef?.let { wv ->
                scrollStore.save(articleId, wv.scrollY)
                val contentHeight = wv.contentHeight * wv.resources.displayMetrics.density
                val progress = if (contentHeight > wv.height) {
                    (wv.scrollY.toFloat() / (contentHeight - wv.height)).coerceIn(0f, 1f)
                } else 1f
                if (progress > lastNotifiedProgress + 0.05f || progress > 0.95f) {
                    viewModel.updateReadingProgress(articleId, progress)
                }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            webViewRef?.findAllAsync(searchQuery)
        }
    }

    LaunchedEffect(zapMode) {
        webViewRef?.let { wv ->
            if (zapMode) {
                wv.settings.javaScriptEnabled = true
                injectZapper(wv)
            } else {
                deactivateZapper(wv)
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Reading settings") },
            text = {
                Column {
                    Text("Font size: ${fontSize}%")
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { fontSize = it.roundToInt() },
                        onValueChangeFinished = {
                            readingPrefs.fontSizePercent = fontSize
                            webViewRef?.settings?.textZoom = fontSize
                        },
                        valueRange = 50f..200f,
                        steps = 14
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("A", style = MaterialTheme.typography.labelSmall)
                        Text("A", style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // WebView - full screen
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.textZoom = fontSize
                    setBackgroundColor(0)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            if (url == "about:blank") return
                            val savedY = scrollStore.get(articleId)
                            if (savedY > 0) {
                                view.postDelayed({ view.scrollTo(0, savedY) }, 300)
                            }
                        }
                    }

                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        val contentH = contentHeight * resources.displayMetrics.density
                        val progress = if (contentH > height) {
                            (scrollY.toFloat() / (contentH - height)).coerceIn(0f, 1f)
                        } else 1f
                        if (progress > lastNotifiedProgress + 0.05f || progress > 0.95f) {
                            lastNotifiedProgress = progress
                            viewModel.updateReadingProgress(articleId, progress)
                        }
                    }
                }
            },
            update = { webView ->
                val html = htmlContent
                val art = article
                if (art != null && html != null && loadedArticleId != art.id) {
                    loadedArticleId = art.id
                    webView.loadDataWithBaseURL(
                        art.url,
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar overlay
        AnimatedVisibility(
            visible = topBarVisible,
            enter = slideInVertically(),
            exit = slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        if (showSearch) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Find in article...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = article?.title ?: "",
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            IconButton(onClick = { topBarVisible = false }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Hide toolbar")
                            }
                            IconButton(onClick = {
                                webViewRef?.let { wv ->
                                    if (readerModeActive) {
                                        disableReaderMode(wv)
                                    } else {
                                        enableReaderMode(wv)
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = "Reader mode",
                                    tint = if (readerModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { zapMode = !zapMode }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Zap element",
                                    tint = if (zapMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = "Find in article")
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Font size")
                            }
                        } else {
                            IconButton(onClick = { webViewRef?.findNext(true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                            }
                            IconButton(onClick = { webViewRef?.findNext(false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                            }
                            IconButton(onClick = {
                                viewModel.toggleSearch()
                                topBarVisible = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Seamless mode: small floating back button at top-left + restore button at top-center
        if (!topBarVisible) {
            // Semi-transparent back button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(40.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f),
                tonalElevation = 0.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // Restore toolbar button at top-center
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clickable { topBarVisible = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.35f),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Toolbar",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }


        }
    }
}
