package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpdRecordCreateRequest(
    @SerialName("visit_type") val visitType: String,
    val complaint: String,
    val examination: String,
    val diagnosis: String? = null,
    @SerialName("surgical_decision") val surgicalDecision: String? = null,
    @SerialName("planned_procedure") val plannedProcedure: String? = null,
    val advice: String? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    val medications: List<MedicationCreateDto> = emptyList(),
    val investigations: List<InvestigationCreateDto> = emptyList(),
    @SerialName("prescription_image_urls") val prescriptionImageUrls: List<String> = emptyList()
)
