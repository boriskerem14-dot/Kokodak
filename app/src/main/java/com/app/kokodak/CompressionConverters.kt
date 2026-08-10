package com.app.kokodak

import androidx.room.TypeConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class CompressionConverters {

    /**
     * Конвертира обикновен String в компресиран ByteArray (GZIP) за запис в Room.
     */
    @TypeConverter
    fun compressString(input: String?): ByteArray? {
        if (input.isNullOrEmpty()) return null
        return try {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(input) }
            bos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Декомпресира GZIP ByteArray от Room обратно в оригинален String.
     */
    @TypeConverter
    fun decompressString(input: ByteArray?): String? {
        if (input == null || input.isEmpty()) return null
        return try {
            val bis = ByteArrayInputStream(input)
            GZIPInputStream(bis).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}