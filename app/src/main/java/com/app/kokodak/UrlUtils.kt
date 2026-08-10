package com.app.kokodak

/**
 * Извлича главния домейн от пълен URL адрес.
 * Пример: "https://www.example.com/page/1" -> "example.com"
 */
fun extractDomain(url: String): String? {
    return try {
        val uri = java.net.URI(url)
        val domain = uri.host ?: return null
        if (domain.startsWith("www.")) domain.substring(4) else domain
    } catch (e: Exception) {
        null
    }
}