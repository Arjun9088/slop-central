package com.articlevault.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.articlevault.data.repository.ArticleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ExtractWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(ArticleWorkerKeys.ARTICLE_ID)!!
        val articleTitle = inputData.getString(ArticleWorkerKeys.ARTICLE_TITLE) ?: ""

        return Result.success(workDataOf(
            ArticleWorkerKeys.ARTICLE_ID to articleId,
            ArticleWorkerKeys.ARTICLE_TITLE to articleTitle
        ))
    }
}
