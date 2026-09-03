package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HospitalAffiliationDto(
    @SerialName("hospital_id") val hospitalId: String,
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("hospital_logo_url") val hospitalLogoUrl: String? = null,
    // Doctor's primary hospital — a sane default to pre-select in a picker.
    // A doctor works at all of their affiliated hospitals at once; there is no
    // "current" one, so this is not something the app switches.
    @SerialName("is_primary") val isPrimary: Boolean = false
)

@Serializable
data class HospitalCreateRequest(
    val name: String,
    val address: String? = null,
    val contact: String? = null,
    @SerialName("registration_number") val registrationNumber: String? = null
)
