package com.example.nori_tura.data

import com.example.nori_tura.data.dto.ConsentFormCreateRequest
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.ConsentSignRequest
import kotlinx.serialization.SerialName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ConsentRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun createConsentForm(request: ConsentFormCreateRequest): Result<ConsentFormResponse> = safeApiCall {
        client.post("/consent/forms") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getConsentForm(id: String): Result<ConsentFormDto> = safeApiCall {
        client.get("/consent/forms/$id").body()
    }

    suspend fun signConsentForm(id: String, request: ConsentSignRequest): Result<ConsentFormDto> = safeApiCall {
        client.post("/consent/forms/$id/sign") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

@kotlinx.serialization.Serializable
data class ConsentFormResponse(
    @SerialName("consent_form") val consentForm: ConsentFormDto,
    @SerialName("pdf_url") val pdfUrl: String? = null
) {
    companion object
}
