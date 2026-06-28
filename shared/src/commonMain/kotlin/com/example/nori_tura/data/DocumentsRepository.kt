package com.example.nori_tura.data

import com.example.nori_tura.data.dto.DocumentCreateRequest
import com.example.nori_tura.data.dto.DocumentDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class DocumentsRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun getPatientDocuments(patientId: String): Result<List<DocumentDto>> = safeApiCall {
        client.get("/documents/patients/$patientId").body()
    }

    suspend fun createDocument(request: DocumentCreateRequest): Result<DocumentDto> = safeApiCall {
        client.post("/documents") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
