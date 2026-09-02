package com.nonituracare.data

import com.nonituracare.data.dto.ConsentFormCreateRequest
import com.nonituracare.data.dto.ConsentFormDto
import com.nonituracare.data.dto.ConsentOtpRequestResponse
import com.nonituracare.data.dto.ConsentOtpVerifyRequest
import com.nonituracare.data.dto.ConsentWitnessOtpRequest
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

    suspend fun requestConsentOtp(id: String): Result<ConsentOtpRequestResponse> = safeApiCall {
        client.post("/consent/forms/$id/request-otp").body()
    }

    suspend fun requestWitnessOtp(id: String, request: ConsentWitnessOtpRequest): Result<ConsentOtpRequestResponse> = safeApiCall {
        client.post("/consent/forms/$id/request-witness-otp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun verifyConsentOtp(id: String, request: ConsentOtpVerifyRequest): Result<ConsentFormDto> = safeApiCall {
        client.post("/consent/forms/$id/verify-otp") {
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
