package com.app.kokodak

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val content: String,
    val pageRank: Double = 1.0,
    val lastIndexed: Long = System.currentTimeMillis()
)