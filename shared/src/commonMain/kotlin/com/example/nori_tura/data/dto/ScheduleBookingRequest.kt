package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtBookingRequest(
    @SerialName("patient_id") val patientId: String,
    val date: String,
    val time: String,
    val procedure: String,
    val urgency: String = "routine"
)

@Serializable
data class OpdBookingRequest(
    @SerialName("patient_id") val patientId: String,
    val date: String,
    val time: String,
    @SerialName("visit_type") val visitType: String = "opd"
)
