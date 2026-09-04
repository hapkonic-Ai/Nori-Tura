package com.nonituracare.data

import com.nonituracare.data.dto.OtNoteTemplateCreateRequest
import com.nonituracare.data.dto.OtNoteTemplateDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * OT note templates: the global corpus-seeded library (visible to every
 * doctor) plus the calling doctor's own saved templates — mirrors
 * [SurgicalTemplateRepository] exactly.
 */
class OtNoteTemplateRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun listTemplates(procedure: String? = null): Result<List<OtNoteTemplateDto>> = safeApiCall {
        client.get("/ot-note-templates") {
            procedure?.let { parameter("procedure", it) }
        }.body()
    }

    suspend fun createTemplate(request: OtNoteTemplateCreateRequest): Result<OtNoteTemplateDto> = safeApiCall {
        client.post("/ot-note-templates") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateTemplate(id: String, request: OtNoteTemplateCreateRequest): Result<OtNoteTemplateDto> = safeApiCall {
        client.patch("/ot-note-templates/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteTemplate(id: String): Result<Unit> = safeApiCall {
        client.delete("/ot-note-templates/$id")
    }
}
