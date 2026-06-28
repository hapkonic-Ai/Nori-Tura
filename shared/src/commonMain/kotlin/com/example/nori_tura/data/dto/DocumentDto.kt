package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("hospital_id") val hospitalId: String? = null,
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("hospital_logo_url") val hospitalLogoUrl: String? = null,
    val name: String,
    val url: String,
    val type: String,
    val category: String? = null,
    @SerialName("uploaded_by_role") val uploadedByRole: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null
)
