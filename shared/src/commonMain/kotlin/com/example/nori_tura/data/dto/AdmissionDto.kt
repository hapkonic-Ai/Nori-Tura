package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdmissionDto(
    val id: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    val status: String? = null,
    val ward: String? = null,
    @SerialName("bed_no") val bedNo: String? = null,
    @SerialName("admitted_at") val admittedAt: String? = null,
    @SerialName("discharge_at") val dischargeAt: String? = null,
    val procedure: String? = null,
    val urgency: String? = null,
    val patient: PatientDto? = null,
    @SerialName("pre_op_notes") val preOpNotes: List<PreOpNoteDto> = emptyList(),
    @SerialName("intra_op_notes") val intraOpNotes: List<IntraOpNoteDto> = emptyList(),
    @SerialName("post_op_notes") val postOpNotes: List<PostOpNoteDto> = emptyList(),
    @SerialName("ward_round_notes") val wardRoundNotes: List<WardRoundNoteDto> = emptyList(),
    @SerialName("discharge_summaries") val dischargeSummaries: List<DischargeSummaryDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdmissionCreateRequest(
    @SerialName("patient_id") val patientId: String,
    val urgency: String,
    @SerialName("bed_no") val bedNo: String? = null,
    val ward: String? = null
)

@Serializable
data class PreOpNoteDto(
    val id: String? = null,
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: String? = null,
    val investigations: List<String> = emptyList(),
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PreOpNoteCreateRequest(
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: String? = null,
    val investigations: List<String> = emptyList(),
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null
)

@Serializable
data class IntraOpNoteDto(
    val id: String? = null,
    @SerialName("procedure_done") val procedureDone: String,
    val findings: String? = null,
    val technique: String? = null,
    val complications: String? = null,
    @SerialName("blood_loss") val bloodLoss: String? = null,
    @SerialName("ot_start") val otStart: String? = null,
    @SerialName("ot_end") val otEnd: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class IntraOpNoteCreateRequest(
    @SerialName("procedure_done") val procedureDone: String,
    val findings: String? = null,
    val technique: String? = null,
    val complications: String? = null,
    @SerialName("blood_loss") val bloodLoss: String? = null,
    @SerialName("ot_start") val otStart: String? = null,
    @SerialName("ot_end") val otEnd: String? = null
)

@Serializable
data class PostOpNoteDto(
    val id: String? = null,
    @SerialName("day_number") val dayNumber: Int,
    val condition: String,
    @SerialName("vitals_json") val vitalsJson: Map<String, String?> = emptyMap(),
    @SerialName("wound_status") val woundStatus: String? = null,
    @SerialName("pain_score") val painScore: Int? = null,
    val diet: String? = null,
    @SerialName("medications_json") val medicationsJson: Map<String, String?>? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PostOpNoteCreateRequest(
    @SerialName("day_number") val dayNumber: Int,
    val condition: String,
    @SerialName("vitals_json") val vitalsJson: Map<String, String?> = emptyMap(),
    @SerialName("wound_status") val woundStatus: String? = null,
    @SerialName("pain_score") val painScore: Int? = null,
    val diet: String? = null,
    @SerialName("medications_json") val medicationsJson: Map<String, String?>? = null
)

@Serializable
data class WardRoundNoteDto(
    val id: String? = null,
    val subjective: String? = null,
    val objective: String? = null,
    val assessment: String? = null,
    val plan: String? = null,
    @SerialName("ready_for_discharge") val readyForDischarge: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class WardRoundNoteCreateRequest(
    val subjective: String? = null,
    val objective: String? = null,
    val assessment: String? = null,
    val plan: String? = null,
    @SerialName("ready_for_discharge") val readyForDischarge: Boolean = false
)

@Serializable
data class DischargeSummaryDto(
    val id: String? = null,
    @SerialName("condition_at_discharge") val conditionAtDischarge: String,
    @SerialName("procedure_summary") val procedureSummary: String,
    @SerialName("discharge_medications_json") val dischargeMedicationsJson: Map<String, String?> = emptyMap(),
    @SerialName("wound_care") val woundCare: String? = null,
    @SerialName("activity_restrictions") val activityRestrictions: String? = null,
    @SerialName("diet_instructions") val dietInstructions: String? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    @SerialName("red_flags") val redFlags: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class DischargeSummaryCreateRequest(
    @SerialName("condition_at_discharge") val conditionAtDischarge: String,
    @SerialName("procedure_summary") val procedureSummary: String,
    @SerialName("discharge_medications_json") val dischargeMedicationsJson: Map<String, String?> = emptyMap(),
    @SerialName("wound_care") val woundCare: String? = null,
    @SerialName("activity_restrictions") val activityRestrictions: String? = null,
    @SerialName("diet_instructions") val dietInstructions: String? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    @SerialName("red_flags") val redFlags: String? = null
)
