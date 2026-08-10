package com.app.kokodak

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Модел за данните, които изпращаме към сървъра
data class LinkSyncRequest(
    val url: String,
    val domain: String,
    val title: String?
)

interface ApiService {

    /**
     * Изпраща открит линк към сървъра Sony E6853
     */
    @POST("/api/links/sync")
    suspend fun syncLinkToServer(@Body linkData: LinkSyncRequest): Response<Unit>
}