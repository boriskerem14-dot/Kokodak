package com.app.kokodak

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File

class CrawlWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = AppDatabase.getDatabase(context)
    private val pageDao = db.pageDao()
    private val thermalGuard = ThermalGuard(context)

    companion object {
        private const val MAX_DEPTH = 3
        private const val TAG = "CrawlWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Възстановяване на изоставени задачи от минал срив...")

        // 1. ЗАЩИТА ПРИ СРИВ: Връщаме прекъснатите задачи (IN_PROGRESS) обратно в PENDING
        pageDao.resetInterruptedTasks()

        var processedCount = 0
        val maxPagesPerRun = 50 // Лимит за един цикъл на Worker-а с цел спестяване на памет

        while (processedCount < maxPagesPerRun) {

            // 2. ЗАЩИТА ОТ ПРЕГРЯВАНЕ: Проверка на сензора за температура на Xperia Z5
            if (thermalGuard.isOverheating()) {
                val temp = thermalGuard.getBatteryTemperature()
                Log.w(TAG, "Xperia Z5 прегрява ($temp°C)! Пауза за 30 секунди...")
                delay(30_000) // Изчакваме 30 секунди за охлаждане

                if (thermalGuard.isOverheating()) {
                    Log.e(TAG, "Устройството все още е горещо. Прекратяване на този работен цикъл за безопасност.")
                    return@withContext Result.retry() // Изчакване за следващия цикъл на WorkManager
                }
            }

            // 3. ВЗЕМАНЕ НА СЛЕДВАЩАТА ЗАДАЧА ОТ БАЗАТА ДАННИ
            val nextTask = pageDao.getNextPendingUrl() ?: break // Ако няма чакащи URL адреси, спираме
            val currentUrl = nextTask.url
            val currentDepth = nextTask.depth

            pageDao.updateQueueStatus(currentUrl, "IN_PROGRESS")

            try {
                // 4. ИЗТЕГЛЯНЕ И ПАРСВАНЕ НА УЕБ СТРАНИЦАТА
                val doc = Jsoup.connect(currentUrl)
                    .userAgent("KokodakBot/1.0 (Android Xperia Z5 Premium)")
                    .timeout(8000)
                    .get()

                val title = doc.title()
                val textContent = doc.body().text()

                // Запазване на страницата в SQLite/Room
                pageDao.insertPage(
                    PageEntity(
                        url = currentUrl,
                        title = title,
                        content = textContent,
                        pageRank = 1.0
                    )
                )

                // Запазване на страницата като текстови файл във папката за MEGA
                saveToMegaFolder(currentUrl, title, textContent)

                // 5. ДЪЛБОЧИННО ОБХОЖДАНЕ (Екстракция на нови линкове)
                if (currentDepth < MAX_DEPTH) {
                    val extractedLinks = doc.select("a[href^=http]")
                        .map { it.attr("abs:href") }
                        .distinct()
                        .take(15) // Вземаме до 15 връзки от страница

                    val newQueueItems = extractedLinks.map { link ->
                        CrawlQueueEntity(
                            url = link,
                            depth = currentDepth + 1,
                            status = "PENDING"
                        )
                    }

                    pageDao.insertQueueItems(newQueueItems)
                }

                // Отбелязваме задачата като завършена
                pageDao.updateQueueStatus(currentUrl, "COMPLETED")
                processedCount++

                // Пауза от половин секунда за баланс между скорост и термично натоварване
                delay(500)

            } catch (e: Exception) {
                Log.e(TAG, "Грешка при изтегляне на $currentUrl: ${e.message}")
                pageDao.updateQueueStatus(currentUrl, "FAILED")
            }
        }

        Result.success()
    }

    private fun saveToMegaFolder(url: String, title: String, content: String) {
        try {
            val megaDir = File(context.getExternalFilesDir(null), "MegaSearchPages")
            if (!megaDir.exists()) megaDir.mkdirs()

            val fileName = "page_${System.currentTimeMillis()}.txt"
            val file = File(megaDir, fileName)
            file.writeText("URL: $url\nTITLE: $title\n\nCONTENT:\n$content")
        } catch (e: Exception) {
            Log.e(TAG, "Грешка при запазване за MEGA: ${e.message}")
        }
    }
}