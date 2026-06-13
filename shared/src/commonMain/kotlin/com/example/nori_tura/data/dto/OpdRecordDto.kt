package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpdRecordDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("chief_complaint") val chiefComplaint: String? = null,
    val examination: String? = null,
    val diagnosis: String? = null,
    @SerialName("surgical_decision") val surgicalDecision: String? = null,
    @SerialName("planned_procedure") val plannedProcedure: String? = null,
    val advice: String? = null,
    val medications: List<MedicationDto>? = null,
    val investigations: List<InvestigationDto>? = null,
    val tags: List<String>? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("nurse_id") val nurseId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
