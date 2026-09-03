package com.nonituracare.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("hospital_id") val hospitalId: String? = null,
    // The `patients` table has no denormalized hospital_name/logo columns (unlike
    // opd_records/ipd_admissions) — GET /patients includes the live relation nested
    // under "hospital" instead of flat fields, so that's what we parse here.
    val hospital: HospitalRefDto? = null,
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    @SerialName("blood_group") val bloodGroup: String? = null,
    val allergies: String? = null,
    @SerialName("parent_name") val parentName: String? = null,
    @SerialName("parent_phone") val parentPhone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("opd_records") val opdRecords: List<OpdRecordDto>? = null,
    @SerialName("ipd_admissions") val ipdAdmissions: List<AdmissionDto>? = null
) {
    val hospitalName: String? get() = hospital?.name
    val hospitalLogoUrl: String? get() = hospital?.logoUrl
}

@Serializable
data class HospitalRefDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val address: String? = null,
    val contact: String? = null,
    @SerialName("registration_number") val registrationNumber: String? = null
)
