package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.example.nori_tura.data.dto.DoctorDto
import com.example.nori_tura.data.dto.PatientDto

@Serializable
data class OpdRecordDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("hospital_id") val hospitalId: String? = null,
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("hospital_logo_url") val hospitalLogoUrl: String? = null,
    val patient: PatientDto? = null,
    val doctor: DoctorDto? = null,
    @SerialName("chief_complaint") val chiefComplaint: String? = null,
    val examination: String? = null,
    val diagnosis: String? = null,
    @SerialName("surgical_decision") val surgicalDecision: String? = null,
    @SerialName("planned_procedure") val plannedProcedure: String? = null,
    val advice: String? = null,
    val medications: List<MedicationDto>? = null,
    val investigations: List<InvestigationDto>? = null,
    @SerialName("prescription_image_urls") val prescriptionImageUrls: List<String>? = null,
    val tags: List<String>? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    @SerialName("reminder_sent") val reminderSent: Boolean = false,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("nurse_id") val nurseId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
