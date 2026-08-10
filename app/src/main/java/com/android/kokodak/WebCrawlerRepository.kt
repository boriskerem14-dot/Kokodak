package com.app.kokodak

import java.net.URLEncoder
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedList
import java.util.Queue

class WebCrawlerRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val pageDao = db.pageDao()

    // Опашка за адреси и сет за вече посетени адреси
    private val urlQueue: Queue<String> = LinkedList()
    private val visitedUrls = mutableSetOf<String>()

    /**
     * Стартира обхождането от начален URL (seed, напр. "https://bg.wikipedia.org/wiki/Main_Page")
     * maxPages определя колко страници максимално да обходи при едно пускане.
     */
    suspend fun startCrawling(seedUrl: String, maxPages: Int = 50, xperiaServerIp: String? = null) {
        withContext(Dispatchers.IO) {
            urlQueue.clear()
            urlQueue.add(seedUrl)

            var pagesCrawled = 0

            while (urlQueue.isNotEmpty() && pagesCrawled < maxPages) {
                val currentUrl = urlQueue.poll() ?: break

                // Пропускаме, ако вече сме я посетили
                if (visitedUrls.contains(currentUrl)) continue
                visitedUrls.add(currentUrl)

                try {
                    // 1. Свързване и сваляне на съдържанието с Jsoup
                    val doc = Jsoup.connect(currentUrl)
                        .userAgent("KokodakBot/1.0 (+https://kokodak.search)")
                        .timeout(5000)
                        .get()

                    val title = doc.title()
                    // Вземаме само видимия текст (без HTML тагове и скриптове)
                    val content = doc.body().text()

                    // 2. Запазване на страницата в локалната Room база данни
                    val pageEntity = PageEntity(
                        url = currentUrl,
                        title = title,
                        content = content,
                        pageRank = 1.0,
                        lastIndexed = System.currentTimeMillis()
                    )
                    pageDao.insertPage(pageEntity)

                    // 3. (Опционално) Изпращане на данните към централния сървър (напр. Xperia Z5 Premium)
                    if (!xperiaServerIp.isNullOrEmpty()) {
                        sendDataToXperiaServer(xperiaServerIp, currentUrl, title, content)
                    }

                    // 4. Извличане на хипервръзки за бъдещо обхождане
                    val links = doc.select("a[href]")
                    for (link in links) {
                        val absUrl = link.attr("abs:href")

                        // Филтър: Вземаме само валидни HTTP/HTTPS линкове
                        if (absUrl.startsWith("http://") || absUrl.startsWith("https://")) {

                            // За запис на връзката в таблицата page_links
                            val linkEntity = LinkEntity(
                                sourceUrl = currentUrl,
                                targetUrl = absUrl
                            )
                            pageDao.insertLink(linkEntity)

                            // Добавяме в опашката, ако не е посетена
                            if (!visitedUrls.contains(absUrl) && !urlQueue.contains(absUrl)) {
                                urlQueue.add(absUrl)
                            }
                        }
                    }

                    pagesCrawled++

                } catch (e: Exception) {
                    e.printStackTrace()
                    // При грешка (напр. липсващ интернет или 404) просто продължаваме със следващия URL
                }
            }
        }
    }

    /**
     * Помощен метод за изпращане на индексирана страница към Xperia Z5 Premium (UrlServer)
     */
    private fun sendDataToXperiaServer(serverIp: String, pageUrl: String, title: String, content: String) {
        try {
            val url = URL("http://$serverIp:8080/add-page")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = "url=${URLEncoder.encode(pageUrl, "UTF-8")}&title=${URLEncoder.encode(title, "UTF-8")}&content=${URLEncoder.encode(content, "UTF-8")}"

            connection.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            connection.responseCode // Изпълнява заявката
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}