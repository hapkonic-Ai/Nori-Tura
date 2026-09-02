package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MedicalImageCreateDto(
    @SerialName("image_url") val imageUrl: String,
    val category: String,
    val label: String? = null,
    val description: String? = null,
    @SerialName("uploaded_by_role") val uploadedByRole: String = "surgeon"
)
