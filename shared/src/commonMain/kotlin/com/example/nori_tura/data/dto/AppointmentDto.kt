package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
    val id: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("slot_datetime") val slotDatetime: String? = null,
    @SerialName("visit_type") val visitType: String? = null,
    val status: String? = null,
    val patient: PatientDto? = null,
    @SerialName("created_at") val createdAt: String? = null
)
