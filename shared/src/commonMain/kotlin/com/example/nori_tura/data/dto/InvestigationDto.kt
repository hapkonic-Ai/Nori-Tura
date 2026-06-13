package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvestigationDto(
    val id: String? = null,
    @SerialName("opd_record_id") val opdRecordId: String? = null,
    val name: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
