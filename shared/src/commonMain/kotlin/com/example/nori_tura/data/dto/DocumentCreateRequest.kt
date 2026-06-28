package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentCreateRequest(
    @SerialName("patient_id") val patientId: String,
    val name: String,
    val url: String,
    val type: String,
    val category: String? = null,
    @SerialName("uploaded_by_role") val uploadedByRole: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null
)
