package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A doctor-owned surgical template, as seen by admin oversight (GET /admin/surgical-templates).
 * Read-only from the admin side — doctor-owned data stays doctor-editable.
 */
@Serializable
data class AdminSurgicalTemplateDto(
    val id: String,
    @SerialName("doctor_id") val doctorId: String,
    val doctor: DoctorDto? = null,
    val name: String,
    val procedure: String,
    val approach: String? = null,
    val anaesthesia: List<String> = emptyList(),
    val investigations: List<String> = emptyList(),
    @SerialName("risk_level") val riskLevel: String? = null,
    val technique: String? = null,
    @SerialName("special_instructions") val specialInstructions: String? = null,
    @SerialName("procedure_description") val procedureDescription: String? = null,
    val risks: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val complications: List<String> = emptyList(),
    @SerialName("material_risks") val materialRisks: String? = null,
    @SerialName("post_op_care") val postOpCare: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
