package com.example.nori_tura.data

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

class MedicalRecordsRepository(private val apiClient: ApiClient) {

    suspend fun listMedicalRecords(patientId: String): Result<List<MedicalRecordDto>> = apiClient.get(
        "/medical-records/$patientId"
    )

    suspend fun getMedicalRecordDetail(recordId: String): Result<MedicalRecordDetailDto> = apiClient.get(
        "/medical-records/$recordId/detail"
    )

    suspend fun createMedicalRecord(
        patientId: String,
        title: String,
        description: String? = null,
        opdRecordId: String? = null,
        admissionId: String? = null
    ): Result<MedicalRecordDto> = apiClient.post(
        "/medical-records",
        CreateMedicalRecordRequest(
            patient_id = patientId,
            title = title,
            description = description,
            opd_record_id = opdRecordId,
            admission_id = admissionId
        )
    )

    suspend fun addImageToRecord(
        recordId: String,
        imageUrl: String,
        category: String,
        label: String? = null,
        description: String? = null,
        uploadedByRole: String = "surgeon"
    ): Result<MedicalRecordImageDto> = apiClient.post(
        "/medical-records/$recordId/images",
        AddMedicalImageRequest(
            image_url = imageUrl,
            category = category,
            label = label,
            description = description,
            uploaded_by_role = uploadedByRole
        )
    )

    suspend fun getRecordImages(recordId: String): Result<List<MedicalRecordImageDto>> = apiClient.get(
        "/medical-records/$recordId/images"
    )
}
