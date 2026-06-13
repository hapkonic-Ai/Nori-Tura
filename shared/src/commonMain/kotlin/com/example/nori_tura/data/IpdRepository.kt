package com.example.nori_tura.data

import com.example.nori_tura.data.dto.AdmissionCreateRequest
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.DischargeSummaryCreateRequest
import com.example.nori_tura.data.dto.DischargeSummaryDto
import com.example.nori_tura.data.dto.IntraOpNoteCreateRequest
import com.example.nori_tura.data.dto.IntraOpNoteDto
import com.example.nori_tura.data.dto.PostOpNoteCreateRequest
import com.example.nori_tura.data.dto.PostOpNoteDto
import com.example.nori_tura.data.dto.PreOpNoteCreateRequest
import com.example.nori_tura.data.dto.PreOpNoteDto
import com.example.nori_tura.data.dto.WardRoundNoteCreateRequest
import com.example.nori_tura.data.dto.WardRoundNoteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class IpdRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun listAdmissions(): Result<List<AdmissionDto>> = safeApiCall {
        client.get("/ipd/admissions").body()
    }

    suspend fun getAdmission(id: String): Result<AdmissionDto> = safeApiCall {
        client.get("/ipd/admissions/$id").body()
    }

    suspend fun createAdmission(request: AdmissionCreateRequest): Result<AdmissionDto> = safeApiCall {
        client.post("/ipd/admissions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun createPreOpNote(admissionId: String, request: PreOpNoteCreateRequest): Result<PreOpNoteDto> = safeApiCall {
        client.post("/ipd/admissions/$admissionId/pre-op") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun createIntraOpNote(admissionId: String, request: IntraOpNoteCreateRequest): Result<IntraOpNoteDto> = safeApiCall {
        client.post("/ipd/admissions/$admissionId/intra-op") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun createPostOpNote(admissionId: String, request: PostOpNoteCreateRequest): Result<PostOpNoteDto> = safeApiCall {
        client.post("/ipd/admissions/$admissionId/post-op") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun createWardRoundNote(admissionId: String, request: WardRoundNoteCreateRequest): Result<WardRoundNoteDto> = safeApiCall {
        client.post("/ipd/admissions/$admissionId/ward-round") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun createDischargeSummary(admissionId: String, request: DischargeSummaryCreateRequest): Result<DischargeSummaryDto> = safeApiCall {
        client.post("/ipd/admissions/$admissionId/discharge") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
