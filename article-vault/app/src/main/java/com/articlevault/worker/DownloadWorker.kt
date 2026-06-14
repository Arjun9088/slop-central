package com.articlevault.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.articlevault.data.AppStorage
import com.articlevault.data.db.entity.Article
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.ml.DomainClassifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository,
    private val appStorage: AppStorage
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DownloadWorker"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val urlRegex = Regex("https?://[^\\s]+")
    private val genericTitles = setOf(
        "untitled", "home", "page not found", "404", "not found",
        "index", "default", "new tab", "untitled document",
        "loading...", "redirecting..."
    )

    private fun extractTitle(doc: org.jsoup.nodes.Document, fallbackUrl: String): String {
        // 1. Try <title> tag
        val titleTag = doc.title()?.trim() ?: ""
        if (isValidTitle(titleTag)) return titleTag

        // 2. Try Open Graph title
        val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
        if (isValidTitle(ogTitle)) return ogTitle

        // 3. Try Twitter card title
        val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").trim()
        if (isValidTitle(twitterTitle)) return twitterTitle

        // 4. Try first <h1> text
        val h1 = doc.select("h1").first()?.text()?.trim() ?: ""
        if (isValidTitle(h1)) return h1

        // 5. Try <h2> as fallback
        val h2 = doc.select("h2").first()?.text()?.trim() ?: ""
        if (isValidTitle(h2)) return h2

        // 6. Parse URL path into readable title
        return titleFromUrl(fallbackUrl)
    }

    private fun isValidTitle(title: String): Boolean {
        if (title.isBlank()) return false
        if (title.length < 3) return false
        if (title.length > 500) return false
        if (title.contains('<') || title.contains('>')) return false
        if (urlRegex.matches(title)) return false
        if (genericTitles.contains(title.lowercase().trim())) return false
        return true
    }

    private fun titleFromUrl(url: String): String {
        return try {
            val path = java.net.URI(url).path.trim('/')
            if (path.isBlank()) {
                DomainClassifier.extractDomain(url).ifBlank { url }
            } else {
                path.split('/')
                    .last()
                    .replace('-', ' ')
                    .replace('_', ' ')
                    .replace(Regex("[.+?]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    .ifBlank { DomainClassifier.extractDomain(url).ifBlank { url } }
            }
        } catch (_: Exception) {
            DomainClassifier.extractDomain(url).ifBlank { url }
        }
    }

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(ArticleWorkerKeys.ARTICLE_ID)!!
        val articleUrl = inputData.getString(ArticleWorkerKeys.ARTICLE_URL)!!

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(articleUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} for $articleUrl")
                    return@withContext Result.failure()
                }

                val html = response.body?.string() ?: ""
                if (html.isBlank()) {
                    Log.e(TAG, "Empty response for $articleUrl")
                    return@withContext Result.failure()
                }

                // Save HTML to file (avoids SQLite CursorWindow 2MB limit)
                val articlesDir = appStorage.articlesDir
                articlesDir.mkdirs()
                val htmlFile = File(articlesDir, "$articleId.html")
                htmlFile.writeText(html)

                val doc = Jsoup.parse(html)
                val title = extractTitle(doc, articleUrl)
                doc.select("script, style, nav, header, footer, iframe, noscript, svg, .cookie-banner, .ad, .advertisement").remove()
                val bodyText = doc.body()?.text()?.replace(Regex("\\s+"), " ")?.trim() ?: ""
                val excerpt = if (bodyText.length > 280) bodyText.take(280) + "..." else bodyText
                val wordCount = bodyText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val domain = DomainClassifier.extractDomain(articleUrl)

                repository.insertArticle(Article(
                    id = articleId,
                    url = articleUrl,
                    title = title,
                    excerpt = excerpt,
                    extractedText = bodyText,
                    htmlPath = htmlFile.absolutePath,
                    wordCount = wordCount,
                    domain = domain
                ))

                Log.d(TAG, "Saved: $title ($wordCount words, domain=$domain)")

                Result.success(workDataOf(
                    ArticleWorkerKeys.ARTICLE_ID to articleId,
                    ArticleWorkerKeys.ARTICLE_URL to articleUrl,
                    ArticleWorkerKeys.ARTICLE_TITLE to title
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for $articleUrl: ${e.message}", e)
                Result.failure()
            }
        }
    }
}
