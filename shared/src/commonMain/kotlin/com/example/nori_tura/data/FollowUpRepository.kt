package com.example.nori_tura.data

import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.data.dto.SendMessageRequest
import com.example.nori_tura.data.dto.SendMessageResponse
import com.example.nori_tura.data.dto.WhatsAppPreviewDto
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

    suspend fun listFollowUps(date: String? = null): Result<List<OpdRecordDto>> = safeApiCall {
        val path = if (date.isNullOrBlank()) {
            "/opd/follow-ups"
        } else {
            "/opd/follow-ups?follow_up_date=$date"
        }
        client.get(path).body()
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
