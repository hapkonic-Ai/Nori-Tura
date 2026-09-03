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
    @SerialName("created_at") val createdAt: String? = null
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
