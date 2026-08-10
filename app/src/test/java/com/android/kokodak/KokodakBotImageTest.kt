package com.android.kokodak

import org.junit.Test
import org.junit.Assert.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class KokodakBotImageTest {

    // --- ЕКСПЕРИМЕНТ 1: Обработка на картинки (Локален HTML) ---
    @Test
    fun testImageTagExtraction() {
        val sampleHtml = """
            <div>
                <p>Здрасти! Виж това пиле:</p>
                <img src="https://example.com/kokodak_bot_avatar.jpg" alt="Kokodak" />
            </div>
        """.trimIndent()

        val document = Jsoup.parse(sampleHtml)
        val imgElement = document.select("img").first()
        val extractedUrl = imgElement?.attr("src")

        assertEquals("https://example.com/kokodak_bot_avatar.jpg", extractedUrl)

        println("=== ЕКСПЕРИМЕНТ 1: УСПЕХ ===")
        println("Намерен линк към изображение: $extractedUrl\n")
    }

    // --- ЕКСПЕРИМЕНТ 2: Тежест на думите (Алгоритъм за класиране) ---
    @Test
    fun testWordWeighting() {
        val pageContent = "кокодак бот кокодак пиле кокодак храна"

        val words = pageContent.lowercase().split("\\s+".toRegex())
        val totalWordsCount = words.size

        val wordCounts = mutableMapOf<String, Int>()
        for (word in words) {
            wordCounts[word] = wordCounts.getOrDefault(word, 0) + 1
        }

        val wordWeights = mutableMapOf<String, Double>()
        for ((word, count) in wordCounts) {
            val weight = count.toDouble() / totalWordsCount
            wordWeights[word] = weight
        }

        println("=== ЕКСПЕРИМЕНТ 2: УСПЕХ ===")
        println("Тежест на думите в страницата:")
        wordWeights.forEach { (word, weight) ->
            println("- Дума: '$word' -> Тежест: ${String.format("%.2f", weight * 100)}%")
        }
        println()
    }

    // --- ЕКСПЕРИМЕНТ 3: Оптимизиран паяк (Спестяване на трафик) ---
    @Test
    fun testSmartNetworkCrawler() {
        println("=== ЕКСПЕРИМЕНТ 3: СТАРТИРАНЕ НА ОПТИМИЗИРАН ПАЯК ===")

        // За теста подаваме примерен истински сайт.
        // Можеш да пробваш с "https://wikipedia.org" за реален тест, ако емулаторът ти има интернет!
        val testUrl = "https://example.com"

        crawlSmartAndSaveTraffic(testUrl)
    }

    // Основната оптимизирана функция на паяка на Kokodak
    private fun crawlSmartAndSaveTraffic(url: String) {
        try {
            // 1. Бърза HEAD заявка за проверка на типа (без да сваляме съдържанието)
            val response = Jsoup.connect(url)
                .method(org.jsoup.Connection.Method.HEAD)
                .timeout(5000)
                .execute()

            val contentType = response.contentType()

            // Проверяваме дали файлът изобщо е HTML текст
            if (contentType == null || !contentType.startsWith("text/html")) {
                println("⏩ Прескочен линк (Спестен трафик! Не е HTML, а е: $contentType): $url")
                return
            }

            // 2. Сваляме само текстовия HTML код на уебсайта
            val document: Document = Jsoup.connect(url).get()

            // 3. Извличаме текстовите линкове (за следващи стъпки на паяка)
            val links = document.select("a[href]")
            println("🌐 [Паяк] Успешно обходен текстов HTML сайт: $url")
            println("🔗 Намерени следващи адреси (линкове):")
            for (link in links) {
                val nextUrl = link.absUrl("href")
                if (nextUrl.isNotEmpty()) {
                    println("   -> Линк: $nextUrl")
                }
            }

            // 4. Извличаме само текстовите URL-и на картинките, без да ги теглим в паметта!
            val images = document.select("img[src]")
            println("🖼️ Намерени адреси на изображения (само като текст):")
            for (img in images) {
                val imageUrl = img.absUrl("src")
                println("   -> Картинка: $imageUrl")
            }

        } catch (e: IOException) {
            println("❌ [Паяк] Проблем при свързване с $url (Няма интернет или сайтът е блокиран): ${e.message}")
        }
    }
}