package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SurgicalTemplateDto(
    val id: String,
    val name: String,
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: List<String> = emptyList(),
    val investigations: List<String> = emptyList(),
    @SerialName("risk_level") val riskLevel: String? = null,
    val technique: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val risks: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val complications: List<String> = emptyList(),
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SurgicalTemplateCreateRequest(
    val name: String,
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: List<String> = emptyList(),
    val investigations: List<String> = emptyList(),
    @SerialName("risk_level") val riskLevel: String? = null,
    val technique: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val risks: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val complications: List<String> = emptyList(),
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null
)

@Serializable
data class SurgicalTemplateUpdateRequest(
    val name: String? = null,
    val procedure: String? = null,
    val approach: String? = null,
    val anaesthesia: List<String>? = null,
    val investigations: List<String>? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    val technique: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    val risks: List<String>? = null,
    val benefits: List<String>? = null,
    val alternatives: List<String>? = null,
    val complications: List<String>? = null,
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null
)
