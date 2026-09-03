package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientCreateRequest(
    val name: String,
    val age: Int,
    val gender: String,
    @SerialName("blood_group") val bloodGroup: String? = null,
    val allergies: String? = null,
    @SerialName("parent_name") val parentName: String,
    @SerialName("parent_phone") val parentPhone: String,
    @SerialName("hospital_id") val hospitalId: String? = null,
    @SerialName("hospital_name") val hospitalName: String? = null
)
