package com.app.kokodak

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "page_links")
data class LinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceUrl: String,
    val targetUrl: String
)