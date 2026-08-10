package com.app.kokodak

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object RobotsParser {

    private val crawlDelayCache = ConcurrentHashMap<String, Long>()

    /**
     * Връща Crawl-delay в милисекунди от robots.txt на сайта (ако е посочен)
     */
    suspend fun getCrawlDelay(urlString: String): Long = withContext(Dispatchers.IO) {
        val host = try {
            URI(urlString).host ?: return@withContext 2000L
        } catch (e: Exception) {
            return@withContext 2000L
        }

        // Ако вече имаме кеширано забавяне за този сайт, го връщаме директно
        val cachedDelay = crawlDelayCache[host]
        if (cachedDelay != null) {
            return@withContext cachedDelay
        }

        val robotsUrl = "https://$host/robots.txt"
        var delayMs = 2000L // По подразбиране 2 секунди

        try {
            val doc = Jsoup.connect(robotsUrl)
                .userAgent("KokodakBot/1.0")
                .timeout(3000)
                .ignoreContentType(true)
                .get()

            val lines = doc.text().split("\n")
            for (line in lines) {
                val trimmed = line.trim().lowercase()
                if (trimmed.startsWith("crawl-delay:")) {
                    val delaySeconds = trimmed.substringAfter("crawl-delay:").trim().toLongOrNull()
                    if (delaySeconds != null) {
                        delayMs = delaySeconds * 1000L
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // При отсъствие на robots.txt или грешка, оставяме стойността по подразбиране
        }

        crawlDelayCache[host] = delayMs
        return@withContext delayMs
    }
}