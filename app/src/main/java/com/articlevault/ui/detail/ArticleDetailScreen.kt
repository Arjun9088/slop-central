package com.articlevault.ui.detail

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val article by viewModel.article(articleId).collectAsStateWithLifecycle(null)

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
            }
        },
        update = { webView ->
            article?.let { art ->
                if (art.htmlContent.isNotBlank()) {
                    webView.loadDataWithBaseURL(
                        art.url,
                        art.htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
