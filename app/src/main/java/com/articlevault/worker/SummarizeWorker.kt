package com.articlevault.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.ml.LlmSummarizer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SummarizeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository,
    private val summarizer: LlmSummarizer
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SummarizeWorker"
    }

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(ArticleWorkerKeys.ARTICLE_ID) ?: return Result.failure()

        val canSummarize = summarizer.isApiConfigured()
        if (!canSummarize) {
            Log.d(TAG, "No API configured, skipping auto-summarize")
            return Result.success()
        }

        val article = repository.getArticle(articleId) ?: return Result.failure()
        if (article.extractedText.isBlank()) {
            Log.d(TAG, "Article has no text content, skipping")
            return Result.success()
        }
        if (article.summary.isNotBlank()) {
            Log.d(TAG, "Article already has a summary, skipping")
            return Result.success()
        }

        return try {
            val summary = summarizer.summarize(article.extractedText)
            repository.updateSummary(articleId, summary)
            Log.d(TAG, "Auto-summary generated for $articleId (${summary.length} chars)")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Auto-summary failed: ${e.message}")
            Result.success()
        }
    }
}
