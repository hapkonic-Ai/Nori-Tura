package com.example.nori_tura.data

import com.example.nori_tura.data.dto.AlertsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class AlertsRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun getAlerts(): Result<AlertsResponseDto> = safeApiCall {
        client.get("/alerts").body()
    }
}
