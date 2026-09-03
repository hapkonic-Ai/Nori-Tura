package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String,
    val name: String,
    val phone: String,
    @SerialName("hospital_id") val hospitalId: String? = null,
    // Two response shapes exist across endpoints: GET /doctors/{id} sends flat
    // hospital_name/hospital_logo_url fields, while admin list endpoints
    // (GET /admin/doctors, and the nested "doctor" on a nurse) return the raw
    // `include={"hospital": True}` relation instead — nested under "hospital",
    // with no flat fields at all. Parse both; prefer the flat one when present.
    @SerialName("hospital_name") private val hospitalNameFlat: String? = null,
    @SerialName("hospital_logo_url") private val hospitalLogoUrlFlat: String? = null,
    val hospital: HospitalRefDto? = null,
    val specialty: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
) {
    val hospitalName: String? get() = hospitalNameFlat ?: hospital?.name
    val hospitalLogoUrl: String? get() = hospitalLogoUrlFlat ?: hospital?.logoUrl
}
