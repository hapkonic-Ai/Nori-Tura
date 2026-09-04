package com.nonituracare.data

import com.nonituracare.data.dto.ConsentFormCreateRequest
import com.nonituracare.data.dto.ConsentFormDto
import kotlinx.serialization.SerialName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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

    /**
     * Fetches the download URL/response for a consent PDF, optionally in a
     * different language than it was originally generated in. When the
     * server already has a cached PDF URL for the requested language, this
     * returns JSON `{pdf_url}`; otherwise it streams the regenerated PDF
     * bytes (see [ConsentDownloadResult]).
     */
    suspend fun getConsentDownloadUrl(id: String, language: String? = null): Result<ConsentDownloadUrlResponse> = safeApiCall {
        client.get("/consent/forms/$id/download") {
            language?.let { parameter("language", it) }
        }.body()
    }
}

@kotlinx.serialization.Serializable
data class ConsentDownloadUrlResponse(
    @SerialName("pdf_url") val pdfUrl: String? = null
)

@kotlinx.serialization.Serializable
data class ConsentFormResponse(
    @SerialName("consent_form") val consentForm: ConsentFormDto,
    @SerialName("pdf_url") val pdfUrl: String? = null
) {
    companion object
}
