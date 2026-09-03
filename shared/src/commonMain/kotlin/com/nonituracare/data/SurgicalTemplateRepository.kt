package com.nonituracare.data

import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateCreateRequest
import com.nonituracare.data.dto.SurgicalTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateUpdateRequest
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

    suspend fun updateTemplate(
        id: String,
        request: SurgicalTemplateUpdateRequest
    ): Result<SurgicalTemplateDto> = safeApiCall {
        client.patch("/surgical-templates/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteTemplate(id: String): Result<Unit> = safeApiCall {
        client.delete("/surgical-templates/$id")
    }

    /** Admin-curated, global procedure templates — usable as a starting point for a
     * new personal template, or directly when generating a consent form. */
    suspend fun getContentTemplates(): Result<List<ContentTemplateDto>> = safeApiCall {
        client.get("/surgical-templates/content-templates").body()
    }
}
