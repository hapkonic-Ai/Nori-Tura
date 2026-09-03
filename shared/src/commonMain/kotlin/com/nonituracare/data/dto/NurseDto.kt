package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NurseDto(
    val id: String? = null,
    val name: String? = null,
    val phone: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("hospital_id") val hospitalId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    val doctor: DoctorDto? = null,
    val hospital: HospitalRefDto? = null
)

@Serializable
data class NurseCreateRequest(
    val name: String,
    val phone: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("hospital_id") val hospitalId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
