package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertsResponseDto(
    @SerialName("pending_consents") val pendingConsents: List<ConsentAlertDto> = emptyList(),
    @SerialName("today_appointments") val todayAppointments: List<AppointmentDto> = emptyList(),
    @SerialName("pending_reviews") val pendingReviews: List<OpdRecordDto> = emptyList(),
    @SerialName("active_admissions") val activeAdmissions: List<AdmissionDto> = emptyList()
)

@Serializable
data class ConsentAlertDto(
    val id: String,
    @SerialName("admission_id") val admissionId: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    val procedure: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null
)
