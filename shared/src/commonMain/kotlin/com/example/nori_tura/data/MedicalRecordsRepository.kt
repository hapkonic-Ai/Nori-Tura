package com.example.nori_tura.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecordImageDto(
    val id: String,
    val image_url: String,
    val category: String,
    val label: String? = null,
    val description: String? = null,
    val uploaded_by_role: String,
    val uploaded_at: String  // ISO datetime
)

@Serializable
data class MedicalRecordDto(
    val id: String,
    val patient_id: String,
    val title: String,
    val description: String? = null,
    val image_count: Int,
    val created_at: String  // ISO datetime
)

@Serializable
data class MedicalRecordDetailDto(
    val id: String,
    val patient_id: String,
    val title: String,
    val description: String? = null,
    val image_count: Int,
    val created_at: String,
    val images: List<MedicalRecordImageDto> = emptyList()
)

@Serializable
data class CreateMedicalRecordRequest(
    val patient_id: String,
    val title: String,
    val description: String? = null,
    val opd_record_id: String? = null,
    val admission_id: String? = null
)

@Serializable
data class AddMedicalImageRequest(
    val image_url: String,
    val category: String,
    val label: String? = null,
    val description: String? = null,
    val uploaded_by_role: String = "surgeon"
)

class MedicalRecordsRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun listMedicalRecords(patientId: String): Result<List<MedicalRecordDto>> = safeApiCall {
        client.get("/medical-records/$patientId").body()
    }

    suspend fun getMedicalRecordDetail(recordId: String): Result<MedicalRecordDetailDto> = safeApiCall {
        client.get("/medical-records/$recordId/detail").body()
    }

    suspend fun createMedicalRecord(
        patientId: String,
        title: String,
        description: String? = null,
        opdRecordId: String? = null,
        admissionId: String? = null
    ): Result<MedicalRecordDto> = safeApiCall {
        client.post("/medical-records") {
            contentType(ContentType.Application.Json)
            setBody(CreateMedicalRecordRequest(
                patient_id = patientId,
                title = title,
                description = description,
                opd_record_id = opdRecordId,
                admission_id = admissionId
            ))
        }.body()
    }

    suspend fun addImageToRecord(
        recordId: String,
        imageUrl: String,
        category: String,
        label: String? = null,
        description: String? = null,
        uploadedByRole: String = "surgeon"
    ): Result<MedicalRecordImageDto> = safeApiCall {
        client.post("/medical-records/$recordId/images") {
            contentType(ContentType.Application.Json)
            setBody(AddMedicalImageRequest(
                image_url = imageUrl,
                category = category,
                label = label,
                description = description,
                uploaded_by_role = uploadedByRole
            ))
        }.body()
    }

    suspend fun getRecordImages(recordId: String): Result<List<MedicalRecordImageDto>> = safeApiCall {
        client.get("/medical-records/$recordId/images").body()
    }
}
