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
    @SerialName("special_instructions") val specialInstructions: String? = null
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
    @SerialName("special_instructions") val specialInstructions: String? = null
)
