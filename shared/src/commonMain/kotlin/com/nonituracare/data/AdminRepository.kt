package com.nonituracare.data

import com.nonituracare.data.dto.AdminStatsDto
import com.nonituracare.data.dto.AdminSurgicalTemplateDto
import com.nonituracare.data.dto.ContentTemplateCreateRequest
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.ContentTemplateUpdateRequest
import com.nonituracare.data.dto.DoctorDto
import com.nonituracare.data.dto.HospitalAffiliationDto
import com.nonituracare.data.dto.HospitalCreateRequest
import com.nonituracare.data.dto.HospitalRefDto
import com.nonituracare.data.dto.NurseCreateRequest
import com.nonituracare.data.dto.NurseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
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

    suspend fun listDoctorHospitals(doctorId: String): Result<List<HospitalAffiliationDto>> = safeApiCall {
        client.get("/admin/doctors/$doctorId/hospitals").body()
    }

    suspend fun addDoctorHospital(
        doctorId: String,
        hospitalId: String? = null,
        hospitalName: String? = null,
        makePrimary: Boolean = false
    ): Result<Unit> = safeApiCall {
        client.post("/admin/doctors/$doctorId/hospitals") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "hospital_id" to hospitalId,
                    "hospital_name" to hospitalName,
                    "make_primary" to makePrimary
                )
            )
        }.body()
    }

    suspend fun removeDoctorHospital(doctorId: String, hospitalId: String): Result<Unit> = safeApiCall {
        client.delete("/admin/doctors/$doctorId/hospitals/$hospitalId").body()
    }

    suspend fun getStats(): Result<AdminStatsDto> = safeApiCall {
        client.get("/admin/stats").body()
    }

    suspend fun listHospitals(): Result<List<HospitalRefDto>> = safeApiCall {
        client.get("/admin/hospitals").body()
    }

    suspend fun createHospital(request: HospitalCreateRequest): Result<HospitalRefDto> = safeApiCall {
        client.post("/admin/hospitals") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun listNurses(): Result<List<NurseDto>> = safeApiCall {
        client.get("/admin/nurses").body()
    }

    suspend fun createNurse(request: NurseCreateRequest): Result<NurseDto> = safeApiCall {
        client.post("/admin/nurses") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateNurseStatus(nurseId: String, isActive: Boolean): Result<NurseDto> = safeApiCall {
        client.patch("/admin/nurses/$nurseId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("is_active" to isActive))
        }.body()
    }

    suspend fun listContentTemplates(): Result<List<ContentTemplateDto>> = safeApiCall {
        client.get("/admin/consent-content-templates").body()
    }

    suspend fun createContentTemplate(request: ContentTemplateCreateRequest): Result<ContentTemplateDto> = safeApiCall {
        client.post("/admin/consent-content-templates") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateContentTemplate(
        id: String,
        request: ContentTemplateUpdateRequest
    ): Result<ContentTemplateDto> = safeApiCall {
        client.patch("/admin/consent-content-templates/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteContentTemplate(id: String): Result<Unit> = safeApiCall {
        client.delete("/admin/consent-content-templates/$id")
    }

    suspend fun listSurgicalTemplates(doctorId: String? = null): Result<List<AdminSurgicalTemplateDto>> = safeApiCall {
        client.get("/admin/surgical-templates") {
            if (doctorId != null) {
                url.parameters.append("doctor_id", doctorId)
            }
        }.body()
    }
}
