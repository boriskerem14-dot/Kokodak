package com.app.kokodak

data class CrawlTask(
    val url: String,
    val currentDepth: Int
)