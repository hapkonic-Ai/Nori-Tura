package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiDiagnosisRequest(
    @SerialName("patient_id") val patientId: String,
    val complaint: String,
    val examination: String,
    val age: Int? = null,
    val gender: String? = null
)
