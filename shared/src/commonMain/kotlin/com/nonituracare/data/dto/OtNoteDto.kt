package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unified operative note — replaces the old pre-op/intra-op/post-op note trio
 * with one document spanning pre-op diagnosis through post-op plan, matching
 * how real operative notes are structured (see [OtNoteTemplateDto]).
 */
@Serializable
data class OtNoteDto(
    val id: String? = null,
    @SerialName("admission_id") val admissionId: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("template_id") val templateId: String? = null,

    val procedure: String,
    val approach: String? = null,
    val anaesthesia: String? = null,
    @SerialName("preop_diagnosis") val preopDiagnosis: String? = null,
    @SerialName("postop_diagnosis") val postopDiagnosis: String? = null,
    @SerialName("operation_performed") val operationPerformed: String? = null,
    @SerialName("position_preparation") val positionPreparation: String? = null,
    @SerialName("incision_approach") val incisionApproach: String? = null,
    val findings: String? = null,
    @SerialName("procedure_steps") val procedureSteps: List<String> = emptyList(),
    val closure: String? = null,
    val specimen: String? = null,
    val implants: String? = null,
    val drains: String? = null,
    @SerialName("estimated_blood_loss") val estimatedBloodLoss: String? = null,
    val counts: String? = null,
    val complications: String? = null,
    @SerialName("postop_plan") val postopPlan: String? = null,

    @SerialName("team_members") val teamMembers: List<TeamMemberDto> = emptyList(),
    @SerialName("ot_start") val otStart: String? = null,
    @SerialName("ot_end") val otEnd: String? = null,

    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_urls") val videoUrls: List<String> = emptyList(),

    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TeamMemberDto(
    val role: String,
    val name: String
)

@Serializable
data class OtNoteCreateRequest(
    @SerialName("template_id") val templateId: String? = null,
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: String? = null,
    @SerialName("preop_diagnosis") val preopDiagnosis: String? = null,
    @SerialName("postop_diagnosis") val postopDiagnosis: String? = null,
    @SerialName("operation_performed") val operationPerformed: String? = null,
    @SerialName("position_preparation") val positionPreparation: String? = null,
    @SerialName("incision_approach") val incisionApproach: String? = null,
    val findings: String? = null,
    @SerialName("procedure_steps") val procedureSteps: List<String> = emptyList(),
    val closure: String? = null,
    val specimen: String? = null,
    val implants: String? = null,
    val drains: String? = null,
    @SerialName("estimated_blood_loss") val estimatedBloodLoss: String? = null,
    val counts: String? = null,
    val complications: String? = null,
    @SerialName("postop_plan") val postopPlan: String? = null,
    @SerialName("team_members") val teamMembers: List<TeamMemberDto> = emptyList(),
    @SerialName("ot_start") val otStart: String? = null,
    @SerialName("ot_end") val otEnd: String? = null,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_urls") val videoUrls: List<String> = emptyList(),
    val status: String = "draft"
)

/**
 * A reusable OT-note template — either the global corpus-seeded library
 * (`isGlobal = true`) or a doctor's own saved template, exactly mirroring how
 * [ContentTemplateDto]/[SurgicalTemplateDto] already coexist for consent forms.
 */
@Serializable
data class OtNoteTemplateDto(
    val id: String,
    val name: String,
    val procedure: String,
    val approach: String? = null,
    @SerialName("is_global") val isGlobal: Boolean = false,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("source_reference") val sourceReference: String? = null,
    val anaesthesia: String? = null,
    @SerialName("preop_diagnosis") val preopDiagnosis: String? = null,
    @SerialName("postop_diagnosis") val postopDiagnosis: String? = null,
    @SerialName("operation_performed") val operationPerformed: String? = null,
    @SerialName("position_preparation") val positionPreparation: String? = null,
    @SerialName("incision_approach") val incisionApproach: String? = null,
    @SerialName("procedure_steps") val procedureSteps: List<String> = emptyList(),
    val closure: String? = null,
    val specimen: String? = null,
    val implants: String? = null,
    val drains: String? = null,
    @SerialName("estimated_blood_loss") val estimatedBloodLoss: String? = null,
    val counts: String? = null,
    @SerialName("standard_complications") val standardComplications: String? = null,
    @SerialName("postop_plan") val postopPlan: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class OtNoteTemplateCreateRequest(
    val name: String,
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: String? = null,
    @SerialName("preop_diagnosis") val preopDiagnosis: String? = null,
    @SerialName("postop_diagnosis") val postopDiagnosis: String? = null,
    @SerialName("operation_performed") val operationPerformed: String? = null,
    @SerialName("position_preparation") val positionPreparation: String? = null,
    @SerialName("incision_approach") val incisionApproach: String? = null,
    @SerialName("procedure_steps") val procedureSteps: List<String> = emptyList(),
    val closure: String? = null,
    val specimen: String? = null,
    val implants: String? = null,
    val drains: String? = null,
    @SerialName("estimated_blood_loss") val estimatedBloodLoss: String? = null,
    val counts: String? = null,
    @SerialName("standard_complications") val standardComplications: String? = null,
    @SerialName("postop_plan") val postopPlan: String? = null
)

@Serializable
data class OtNoteMediaAddRequest(
    val url: String,
    @SerialName("media_type") val mediaType: String,
    val label: String? = null,
    val description: String? = null
)
