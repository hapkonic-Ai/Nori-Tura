package com.example.nori_tura.data

import com.example.nori_tura.data.dto.SurgicalTemplateCreateRequest
import com.example.nori_tura.data.dto.SurgicalTemplateDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SurgicalTemplateRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun listTemplates(): Result<List<SurgicalTemplateDto>> = safeApiCall {
        client.get("/surgical-templates").body()
    }

    suspend fun createTemplate(request: SurgicalTemplateCreateRequest): Result<SurgicalTemplateDto> = safeApiCall {
        client.post("/surgical-templates") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteTemplate(id: String): Result<Unit> = safeApiCall {
        client.delete("/surgical-templates/$id")
    }
}
