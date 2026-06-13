package com.articlevault.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.ml.TagClassifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TagWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository,
    private val tagClassifier: TagClassifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(ArticleWorkerKeys.ARTICLE_ID)!!
        val articleTitle = inputData.getString(ArticleWorkerKeys.ARTICLE_TITLE) ?: "Article"

        // Read extracted text from SQLite
        val article = repository.getArticle(articleId)
        val articleText = article?.extractedText ?: ""

        if (articleText.isNotBlank()) {
            try {
                val tags = tagClassifier.classify(articleText)
                repository.insertTags(articleId, tags)
            } catch (_: Exception) {
            }
        }

        showNotification(articleTitle)
        return Result.success()
    }

    private fun showNotification(title: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, "article_saved")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Article saved")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
