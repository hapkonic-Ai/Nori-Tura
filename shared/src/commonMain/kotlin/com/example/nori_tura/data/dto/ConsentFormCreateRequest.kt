package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsentFormCreateRequest(
    @SerialName("admission_id") val admissionId: String,
    @SerialName("form_type") val formType: String,
    val procedure: String,
    val anesthesia: String,
    val risks: String,
    val benefits: String,
    val alternatives: String,
    @SerialName("post_op_care") val postOpCare: String
)
