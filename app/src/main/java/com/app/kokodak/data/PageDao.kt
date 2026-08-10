package com.app.kokodak

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PageDao {

    // ==========================================
    // 1. МЕТОДИ ЗА СТРАНИЦИТЕ (INDEX / SEARCH / PAGERANK)
    // ==========================================

    /**
     * Запазва или обновява индексирана страница в базата данни.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    /**
     * Търси страници по зададена ключова дума в съдържанието,
     * подредени по PageRank в намаляващ ред.
     */
    @Query("SELECT * FROM pages WHERE content LIKE '%' || :query || '%' ORDER BY pageRank DESC")
    suspend fun searchPages(query: String): List<PageEntity>

    /**
     * Връща всички индексирани страници (за пресмятане на PageRank).
     */
    @Query("SELECT * FROM pages")
    suspend fun getAllPages(): List<PageEntity>

    /**
     * Обновява PageRank стойността на определен URL адрес.
     */
    @Query("UPDATE pages SET pageRank = :rank WHERE url = :url")
    suspend fun updatePageRank(url: String, rank: Double)


    // ==========================================
    // 2. МЕТОДИ ЗА ОПАШКАТА (CRAWL QUEUE & RESILIENCE)
    // ==========================================

    /**
     * Вмъква единичен LinkEntity (използва се от WebCrawlerRepository).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: LinkEntity)

    /**
     * Вмъква една задача за опашката.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(queueItem: CrawlQueueEntity)

    /**
     * Добавя списък от нови задачи в опашката. Игнорира дублираните URL адреси.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQueueItems(items: List<CrawlQueueEntity>)

    /**
     * Взема следващия чакащ URL с най-малка дълбочина.
     */
    @Query("SELECT * FROM crawl_queue WHERE status = 'PENDING' ORDER BY depth ASC LIMIT 1")
    suspend fun getNextPendingUrl(): CrawlQueueEntity?

    /**
     * Обновява статуса на задача от опашката ('IN_PROGRESS', 'COMPLETED', 'FAILED').
     */
    @Query("UPDATE crawl_queue SET status = :status WHERE url = :url")
    suspend fun updateQueueStatus(url: String, status: String)

    /**
     * ЗАЩИТЕН МЕХАНИЗЪМ: Възстановява задачи, които са останали в 'IN_PROGRESS'
     * поради внезапен срив, прегряване на Xperia Z5 или рестарт на приложението.
     */
    @Query("UPDATE crawl_queue SET status = 'PENDING' WHERE status = 'IN_PROGRESS'")
    suspend fun resetInterruptedTasks()
}