package com.app.kokodak

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crawl_queue")
data class CrawlQueueEntity(
    @PrimaryKey val url: String,
    val depth: Int,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, FAILED
    val timestamp: Long = System.currentTimeMillis()
)