package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An admin-curated procedure template, global across the platform (no doctor_id) —
 * the institution's standard wording, as opposed to a [SurgicalTemplateDto] which is
 * one doctor's own. Any doctor/nurse can read these; only admins create/edit them.
 */
@Serializable
data class ContentTemplateDto(
    val id: String,
    val name: String,
    val procedure: String,
    val approach: String? = null,
    val technique: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val investigations: List<String> = emptyList(),
    @SerialName("procedure_description") val procedureDescription: String? = null,
    val anesthesia: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    @SerialName("possible_complications") val possibleComplications: List<String> = emptyList(),
    @SerialName("material_risks") val materialRisks: String? = null,
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("statutory_reference") val statutoryReference: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,

    // Bilingual content (falls back to the legacy single-language fields above
    // when a template hasn't been migrated to bilingual columns yet)
    @SerialName("procedure_description_en") val procedureDescriptionEn: String? = null,
    @SerialName("procedure_description_hi") val procedureDescriptionHi: String? = null,
    @SerialName("anaesthesia_en") val anaesthesiaEn: List<String> = emptyList(),
    @SerialName("anaesthesia_hi") val anaesthesiaHi: List<String> = emptyList(),
    @SerialName("risks_en") val risksEn: List<String> = emptyList(),
    @SerialName("risks_hi") val risksHi: List<String> = emptyList(),
    @SerialName("benefits_en") val benefitsEn: List<String> = emptyList(),
    @SerialName("benefits_hi") val benefitsHi: List<String> = emptyList(),
    @SerialName("alternatives_en") val alternativesEn: List<String> = emptyList(),
    @SerialName("alternatives_hi") val alternativesHi: List<String> = emptyList(),
    @SerialName("possible_complications_en") val possibleComplicationsEn: List<String> = emptyList(),
    @SerialName("possible_complications_hi") val possibleComplicationsHi: List<String> = emptyList(),
    @SerialName("material_risks_en") val materialRisksEn: String? = null,
    @SerialName("material_risks_hi") val materialRisksHi: String? = null,
    @SerialName("post_op_care_en") val postOpCareEn: String? = null,
    @SerialName("post_op_care_hi") val postOpCareHi: String? = null,
    @SerialName("expected_recovery_en") val expectedRecoveryEn: String? = null,
    @SerialName("expected_recovery_hi") val expectedRecoveryHi: String? = null,
    @SerialName("hi_content_status") val hiContentStatus: String = "missing"
)

@Serializable
data class ContentTemplateCreateRequest(
    val name: String,
    val procedure: String,
    val approach: String? = null,
    val technique: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val investigations: List<String> = emptyList(),
    @SerialName("procedure_description") val procedureDescription: String? = null,
    val anesthesia: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    @SerialName("possible_complications") val possibleComplications: List<String> = emptyList(),
    @SerialName("material_risks") val materialRisks: String? = null,
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("statutory_reference") val statutoryReference: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ContentTemplateUpdateRequest(
    val name: String? = null,
    val procedure: String? = null,
    val approach: String? = null,
    val technique: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val investigations: List<String>? = null,
    @SerialName("procedure_description") val procedureDescription: String? = null,
    val anesthesia: List<String>? = null,
    val risks: List<String>? = null,
    val benefits: List<String>? = null,
    val alternatives: List<String>? = null,
    @SerialName("possible_complications") val possibleComplications: List<String>? = null,
    @SerialName("material_risks") val materialRisks: String? = null,
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("statutory_reference") val statutoryReference: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)
