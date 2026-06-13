package com.example.nori_tura.data

import com.example.nori_tura.data.dto.DoctorDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AdminRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun listDoctors(): Result<List<DoctorDto>> = safeApiCall {
        client.get("/admin/doctors").body()
    }

    suspend fun listPendingDoctors(): Result<List<DoctorDto>> = safeApiCall {
        client.get("/admin/doctors/pending").body()
    }

    suspend fun updateDoctorStatus(id: String, isActive: Boolean): Result<DoctorDto> = safeApiCall {
        client.patch("/admin/doctors/$id/status") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("is_active" to isActive))
        }.body()
    }
}
