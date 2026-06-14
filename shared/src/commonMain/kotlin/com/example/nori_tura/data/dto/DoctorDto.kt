package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String,
    val name: String,
    val phone: String,
    val hospital: String? = null,
    val specialty: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
