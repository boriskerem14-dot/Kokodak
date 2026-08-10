package com.app.kokodak

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.LinkedList
import java.util.Queue

data class PageResult(
    val url: String,
    val title: String,
    val extractedData: String
)

class KokodakSpider(
    private val minDelayMs: Long = 1500, // Минимална пауза (1.5 сек)
    private val maxDelayMs: Long = 3500, // Максимална пауза (3.5 сек)
    private val maxDepth: Int = 2        // Максимална дълбочина на обхождане
) {

    // Клас за следене на адресите и тяхната дълбочина
    private data class CrawlItem(val url: String, val depth: Int)

    private val visitedUrls = mutableSetOf<String>()
    private val urlQueue: Queue<CrawlItem> = LinkedList()

    /**
     * Основна функция за стартиране на паяка.
     * Изпълнява се в IO нишка, за да не блокира потребителския интерфейс.
     */
    suspend fun startCrawling(
        seedUrl: String,
        onPageProcessed: (PageResult) -> Unit
    ) = withContext(Dispatchers.IO) {

        // Добавяме началния адрес с дълбочина 0
        urlQueue.add(CrawlItem(seedUrl, 0))

        while (urlQueue.isNotEmpty()) {
            val currentItem = urlQueue.poll() ?: break
            val (url, depth) = currentItem

            // Пропускаме, ако вече сме го посетили или е твърде надълбоко
            if (url in visitedUrls || depth > maxDepth) continue
            visitedUrls.add(url)

            try {
                // 1. Изтегляме и парсваме страницата
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) KokodakBot/1.0")
                    .timeout(10000)
                    .get()

                val title = doc.title()
                // Тук извличаш данните, които ти трябват за Kokodak
                val extractedText = doc.select("p").firstOrNull()?.text() ?: "Няма текст"

                // Изпращаме резултата
                val result = PageResult(url, title, extractedText)
                withContext(Dispatchers.Main) {
                    onPageProcessed(result)
                }

                // 2. Намираме нови вътрешни връзки
                if (depth < maxDepth) {
                    val links = doc.select("a[href]")
                    for (link in links) {
                        val nextUrl = link.attr("abs:href")

                        // Филтрираме само валидни HTTP/HTTPS линкове
                        if (nextUrl.startsWith("http") && nextUrl !in visitedUrls) {
                            urlQueue.add(CrawlItem(nextUrl, depth + 1))
                        }
                    }
                }

            } catch (e: Exception) {
                // Обработка на грешка при мрежов проблем или грешен URL
                println("Грешка при сваляне на $url: ${e.localizedMessage}")
            }

            // 3. НАМАЛЯВАНЕ НА ТЕМПОТО (Рандом пауза)
            val randomDelay = (minDelayMs..maxDelayMs).random()
            delay(randomDelay)
        }
    }
}