package com.app.kokodak

import kotlinx.coroutines.delay
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object RateLimiter {

    // Пазим времето на последната заявка за всеки отделен хост/домейн (напр. example.com)
    private val lastRequestTimes = ConcurrentHashMap<String, Long>()

    // Минимално време между две заявки към един и същ домейн (в милисекунди)
    // 2000 ms = 2 секунди пауза (стандартно за добронамерени паяци)
    private const val DEFAULT_DELAY_MS = 2000L

    /**
     * Изчаква необходимото време преди да позволи следващата заявка към конкретния URL.
     */
    suspend fun acquirePermission(url: String, customDelayMs: Long = DEFAULT_DELAY_MS) {
        val host = getHostFromUrl(url) ?: return
        val currentTime = System.currentTimeMillis()

        val lastTime = lastRequestTimes[host] ?: 0L
        val timePassed = currentTime - lastTime

        if (timePassed < customDelayMs) {
            val waitTime = customDelayMs - timePassed
            delay(waitTime) // Забавяме корутината, без да блокираме нишката
        }

        // Обновяваме последното време на заявката за този хост
        lastRequestTimes[host] = System.currentTimeMillis()
    }

    private fun getHostFromUrl(urlString: String): String? {
        return try {
            val uri = URI(urlString)
            uri.host
        } catch (e: Exception) {
            null
        }
    }
}