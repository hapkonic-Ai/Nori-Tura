package com.nonituracare.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminStatsDto(
    val hospitals: Int = 0,
    val doctors: AdminDoctorStats = AdminDoctorStats(),
    val nurses: AdminNurseStats = AdminNurseStats(),
    val patients: Int = 0,
    val admissions: AdminAdmissionStats = AdminAdmissionStats()
)

@Serializable
data class AdminDoctorStats(
    val total: Int = 0,
    val active: Int = 0,
    val pending: Int = 0
)

@Serializable
data class AdminNurseStats(
    val total: Int = 0,
    val active: Int = 0
)

@Serializable
data class AdminAdmissionStats(
    val active: Int = 0,
    val total: Int = 0
)
