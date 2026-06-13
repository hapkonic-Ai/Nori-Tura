package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiDiagnosisResponse(
    val suggestions: AiSuggestions = AiSuggestions(),
    val disclaimer: String? = null,
    @SerialName("opd_record_id") val opdRecordId: String? = null
)

@Serializable
data class AiSuggestions(
    @SerialName("differential_diagnosis") val differentialDiagnosis: List<DifferentialDiagnosis> = emptyList(),
    @SerialName("recommended_investigations") val recommendedInvestigations: List<String> = emptyList(),
    val confidence: Double? = null,
    val disclaimer: String? = null,
    @SerialName("model_used") val modelUsed: String? = null
)

@Serializable
data class DifferentialDiagnosis(
    val name: String? = null,
    val reasoning: String? = null
)
