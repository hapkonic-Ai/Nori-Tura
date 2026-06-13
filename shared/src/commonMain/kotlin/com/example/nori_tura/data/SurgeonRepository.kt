package com.example.nori_tura.data

import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.AiDiagnosisRequest
import com.example.nori_tura.data.dto.AiDiagnosisResponse
import com.example.nori_tura.data.dto.AppointmentDto
import com.example.nori_tura.data.dto.OpdRecordCreateRequest
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.data.dto.PatientDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class SurgeonRepository(
    private val client: HttpClient = ApiClient.client
) {

    suspend fun getPatients(token: String): Result<List<PatientDto>> = safeApiCall {
        client.get("/patients") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getPatientDetail(token: String, patientId: String): Result<PatientDto> = safeApiCall {
        client.get("/patients/$patientId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getOpdRecords(token: String, patientId: String): Result<List<OpdRecordDto>> = safeApiCall {
        client.get("/opd/patients/$patientId/records") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getAppointments(token: String): Result<List<AppointmentDto>> = safeApiCall {
        client.get("/appointments") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getAdmissions(token: String): Result<List<AdmissionDto>> = safeApiCall {
        client.get("/ipd/admissions") {
            bearerAuth(token)
        }.body()
    }

    suspend fun createOpdRecord(
        token: String,
        patientId: String,
        request: OpdRecordCreateRequest
    ): Result<OpdRecordDto> = safeApiCall {
        client.post("/opd/patients/$patientId/records") {
            bearerAuth(token)
            setBody(request)
        }.body()
    }

    suspend fun suggestDiagnosis(
        token: String,
        request: AiDiagnosisRequest
    ): Result<AiDiagnosisResponse> = safeApiCall {
        client.post("/ai/suggest-diagnosis") {
            bearerAuth(token)
            setBody(request)
        }.body()
    }
}
