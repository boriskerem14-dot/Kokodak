package com.app.kokodak

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileOutputStream

/**
 * Вграден HTTP сървър за таблета (SM-T211), който приема намерените URL адреси
 * и ги записва във файл върху SD картата.
 */
class UrlServer(
    port: Int,
    private val sdCardDir: File
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        // Приемаме само POST заявки с нови URL-и
        if (session.method == Method.POST) {
            val files = HashMap<String, String>()
            try {
                // Парсваме тялото на изпратената HTTP заявка
                session.parseBody(files)
                val postData = files["postData"] // Съдържанието на пратения URL/текст

                if (!postData.isNullOrEmpty()) {
                    saveUrlToSdCard(postData)
                    Log.i("UrlServer", "Успешно записан URL: $postData")
                    return newFixedLengthResponse(
                        Response.Status.OK,
                        "text/plain",
                        "URL е приет и записан!"
                    )
                }
            } catch (e: Exception) {
                Log.e("UrlServer", "Грешка при обработка на заявката", e)
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "text/plain",
                    "Грешка при запис на сървъра"
                )
            }
        }

        return newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "text/plain",
            "Моля, изпращайте POST заявки с URL адрес."
        )
    }

    /**
     * Записва получения URL адрес във файл `indexed_urls.txt` на SD картата
     */
    private fun saveUrlToSdCard(urlData: String) {
        if (!sdCardDir.exists()) {
            sdCardDir.mkdirs()
        }

        val targetFile = File(sdCardDir, "indexed_urls.txt")

        // Отваряме файла в режим append (добавяне на нов ред)
        FileOutputStream(targetFile, true).use { outputStream ->
            val dataToAppend = "$urlData\n"
            outputStream.write(dataToAppend.toByteArray())
        }
    }
}