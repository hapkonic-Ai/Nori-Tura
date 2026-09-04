package com.nonituracare.data

import com.nonituracare.data.dto.OpdRecordDto
import com.nonituracare.data.dto.SendMessageRequest
import com.nonituracare.data.dto.SendMessageResponse
import com.nonituracare.data.dto.WhatsAppPreviewDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FollowUpRepository(
    private val client: HttpClient = ApiClient.client
) {

    /** Every not-yet-attended follow-up for the current doctor, past and
     * future — there's no date filter any more, the caller (the follow-ups
     * screen) splits the list into overdue vs. upcoming itself. */
    suspend fun listFollowUps(): Result<List<OpdRecordDto>> = safeApiCall {
        client.get("/opd/follow-ups").body()
    }

    suspend fun markAttended(recordId: String): Result<OpdRecordDto> = safeApiCall {
        client.post("/opd/follow-ups/$recordId/attendance").body()
    }

    suspend fun getPreview(recordId: String): Result<WhatsAppPreviewDto> = safeApiCall {
        client.get("/opd/follow-ups/$recordId/preview").body()
    }

    suspend fun sendMessage(
        recordId: String,
        channel: String,
        message: String? = null
    ): Result<SendMessageResponse> = safeApiCall {
        client.post("/opd/follow-ups/$recordId/send") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(channel = channel, message = message))
        }.body()
    }
}
