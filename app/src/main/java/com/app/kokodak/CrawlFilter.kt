package com.app.kokodak

import java.net.URI

class CrawlFilter(
    private val maxDepth: Int = 3,
    private val maxUrlLength: Int = 2048
) {
    // Използваме HashSet за мигновена проверка (O(1) време)
    private val visitedUrls = HashSet<String>()

    /**
     * Главен метод: Проверява дали даден URL е безопасен и готов за сканиране
     */
    fun shouldCrawl(url: String, currentDepth: Int): Boolean {
        // 1. Проверка за дълбочина
        if (currentDepth > maxDepth) return false

        // 2. Валидация на дължина и празни стойности
        if (url.isBlank() || url.length > maxUrlLength) return false

        // 3. Нормализация (премахваме #хашове и параметри за проследяване)
        val normalizedUrl = normalizeUrl(url) ?: return false

        // 4. Проверка за ненужни файлове (изображения, PDF, архиви)
        if (isStaticResource(normalizedUrl)) return false

        // 5. Проверка за капани (повторение на едни и същи директории)
        if (hasDirectoryLoop(normalizedUrl)) return false

        // 6. Проверка дали вече сме го посетили (Thread-safe съхранение)
        synchronized(visitedUrls) {
            if (visitedUrls.contains(normalizedUrl)) {
                return false // Вече е сканиран!
            }
            visitedUrls.add(normalizedUrl)
        }

        return true
    }

    /**
     * Нормализира URL-а: превръща го в малки букви, маха #section2 и излишни `?utm_source=...`
     */
    private fun normalizeUrl(rawUrl: String): String? {
        return try {
            val uri = URI(rawUrl).normalize()
            val scheme = uri.scheme?.lowercase() ?: return null

            // Работим само с HTTP/HTTPS
            if (scheme != "http" && scheme != "https") return null

            val host = uri.host?.lowercase() ?: return null
            val path = uri.path ?: ""

            // Сглобяваме чисто URL без котва (#)
            "$scheme://$host$path"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Пропуска ресурси, които не съдържат текстов индекс
     */
    private fun isStaticResource(url: String): Boolean {
        val lowercase = url.lowercase()
        val ignoredExtensions = listOf(
            ".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp",
            ".pdf", ".zip", ".rar", ".exe", ".apk", ".mp3", ".mp4"
        )
        return ignoredExtensions.any { lowercase.endsWith(it) }
    }

    /**
     * Засича спирали в пътя от рода на: /catalog/products/catalog/products/
     */
    private fun hasDirectoryLoop(url: String): Boolean {
        val segments = URI(url).path.split("/").filter { it.isNotEmpty() }
        val uniqueSegments = segments.toSet()

        // Ако има твърде много дублиращи се сегменти в пътя -> това е капан
        return segments.size - uniqueSegments.size > 2
    }
}