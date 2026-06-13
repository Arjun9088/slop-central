package com.articlevault.receiver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.work.*
import com.articlevault.worker.ArticleWorkerKeys
import com.articlevault.worker.DownloadWorker
import com.articlevault.worker.ExtractWorker
import com.articlevault.worker.SummarizeWorker
import com.articlevault.worker.TagWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiver : ComponentActivity() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) {
            finish()
            return
        }

        val raw = intent.getStringExtra(Intent.EXTRA_TEXT)
        val url = extractUrl(raw)
        if (url == null) {
            Toast.makeText(this, "No URL found in shared content", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        enqueueSave(url)
        Toast.makeText(this, "Saving: $url", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private val URL_REGEX = Regex("https?://[^\\s<>\"']+")

        fun extractUrl(text: String?): String? {
            if (text.isNullOrBlank()) return null
            val match = URL_REGEX.find(text) ?: return null
            return match.value.trimEnd('.', ',', ')', ']', '}', '!', '?', ':', ';')
        }

        fun enqueueSave(workManager: WorkManager, url: String) {
            val articleId = UUID.randomUUID().toString()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    ArticleWorkerKeys.ARTICLE_ID to articleId,
                    ArticleWorkerKeys.ARTICLE_URL to url
                ))
                .addTag("article-$articleId")
                .build()

            val extractRequest = OneTimeWorkRequestBuilder<ExtractWorker>()
                .setInputData(workDataOf(ArticleWorkerKeys.ARTICLE_ID to articleId))
                .addTag("article-$articleId")
                .build()

            val tagRequest = OneTimeWorkRequestBuilder<TagWorker>()
                .setInputData(workDataOf(
                    ArticleWorkerKeys.ARTICLE_ID to articleId,
                    ArticleWorkerKeys.ARTICLE_TITLE to url
                ))
                .addTag("article-$articleId")
                .build()

            val summarizeRequest = OneTimeWorkRequestBuilder<SummarizeWorker>()
                .setInputData(workDataOf(ArticleWorkerKeys.ARTICLE_ID to articleId))
                .addTag("article-$articleId")
                .build()

            workManager
                .beginUniqueWork(
                    "save-article-$articleId",
                    ExistingWorkPolicy.KEEP,
                    downloadRequest
                )
                .then(extractRequest)
                .then(tagRequest)
                .then(summarizeRequest)
                .enqueue()
        }
    }

    private fun enqueueSave(url: String) {
        enqueueSave(workManager, url)
    }
}
