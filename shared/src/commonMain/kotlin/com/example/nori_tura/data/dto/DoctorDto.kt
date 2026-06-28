package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String,
    val name: String,
    val phone: String,
    @SerialName("hospital_id") val hospitalId: String? = null,
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("hospital_logo_url") val hospitalLogoUrl: String? = null,
    val specialty: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
