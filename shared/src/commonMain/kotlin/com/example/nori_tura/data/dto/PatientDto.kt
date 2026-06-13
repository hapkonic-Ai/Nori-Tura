package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
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
)
