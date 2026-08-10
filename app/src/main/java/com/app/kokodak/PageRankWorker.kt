package com.app.kokodak

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PageRankWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = AppDatabase.getDatabase(context)
    private val pageDao = db.pageDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pages = pageDao.getAllPages()
            if (pages.isEmpty()) return@withContext Result.success()

            val totalPagesCount = pages.size
            val initialRank = 1.0 / totalPagesCount

            pages.forEach { page ->
                pageDao.updatePageRank(page.url, initialRank)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}