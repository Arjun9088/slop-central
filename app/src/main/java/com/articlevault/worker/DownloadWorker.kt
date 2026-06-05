package com.articlevault.worker

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.articlevault.data.db.entity.Article
import com.articlevault.data.repository.ArticleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository
) : CoroutineWorker(context, params) {

    companion object {
        private fun unescapeJsonString(json: String?): String {
            if (json.isNullOrEmpty() || json == "null") return ""
            return try {
                org.json.JSONTokener(json).nextValue() as String
            } catch (e: Exception) {
                json
            }
        }
    }

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(ArticleWorkerKeys.ARTICLE_ID)!!
        val articleUrl = inputData.getString(ArticleWorkerKeys.ARTICLE_URL)!!

        // Load page and extract HTML on main thread
        val result = withContext(Dispatchers.Main) {
            loadPageAndExtractHtml(articleId, articleUrl)
        }

        if (result == null) return Result.failure()

        val (title, html) = result

        // Save to SQLite on IO thread
        withContext(Dispatchers.IO) {
            val doc = org.jsoup.Jsoup.parse(html)
            doc.select("script, style, nav, header, footer, iframe, noscript").remove()
            val bodyText = doc.body()?.text()?.replace(Regex("\\s+"), " ")?.trim() ?: ""
            val excerpt = if (bodyText.length > 280) bodyText.take(280) + "..." else bodyText

            repository.insertArticle(Article(
                id = articleId,
                url = articleUrl,
                title = title,
                excerpt = excerpt,
                extractedText = bodyText,
                htmlContent = html
            ))
        }

        return Result.success(workDataOf(
            ArticleWorkerKeys.ARTICLE_ID to articleId,
            ArticleWorkerKeys.ARTICLE_URL to articleUrl,
            ArticleWorkerKeys.ARTICLE_TITLE to title
        ))
    }

    private suspend fun loadPageAndExtractHtml(
        articleId: String,
        articleUrl: String
    ): Pair<String, String>? = suspendCancellableCoroutine { cont ->
        val webView = WebView(applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
        }

        var isDone = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (isDone) return
                val title = view.title ?: articleUrl

                val inlineCssScript = """
                    (function(){
                        try {
                            var sheets = document.styleSheets;
                            var css = '';
                            for (var i = 0; i < sheets.length; i++) {
                                try {
                                    var rules = sheets[i].cssRules;
                                    for (var j = 0; j < rules.length; j++) {
                                        css += rules[j].cssText + '\n';
                                    }
                                } catch(e) {}
                            }
                            if (css) {
                                var style = document.createElement('style');
                                style.textContent = css;
                                document.head.appendChild(style);
                            }
                            var links = document.querySelectorAll('link[rel="stylesheet"]');
                            for (var i = 0; i < links.length; i++) links[i].remove();
                        } catch(e) {}
                        return document.documentElement.outerHTML;
                    })()
                """.trimIndent()

                view.evaluateJavascript(inlineCssScript) { rawHtml ->
                    if (!isDone) {
                        isDone = true
                        val html = unescapeJsonString(rawHtml)
                        view.destroy()
                        cont.resume(title to html)
                    }
                }
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Log.e("DownloadWorker", "WebView error $errorCode: $description")
                if (!isDone) {
                    isDone = true
                    view.destroy()
                    cont.resume(null)
                }
            }
        }

        webView.loadUrl(articleUrl)

        cont.invokeOnCancellation {
            if (!isDone) {
                isDone = true
                webView.destroy()
            }
        }
    }
}
