package com.example.nori_tura.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val phone: String
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)

@Serializable
data class RegisterDoctorRequest(
    val name: String,
    val phone: String,
    val hospital: String,
    val specialty: String
)

@Serializable
data class RegisterDoctorResponse(
    val message: String,
    @SerialName("doctor_id") val doctorId: String? = null,
    val status: String? = null
)

@Serializable
data class OtpResponse(
    val message: String? = null,
    @SerialName("expires_in_minutes") val expiresInMinutes: Int? = null,
    @SerialName("dev_otp") val devOtp: String? = null
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val access_token: String,
    val role: String
)

@Serializable
data class MeResponse(
    val id: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val name: String? = null,
    val profile: ProfileDto? = null,
    val patient: ProfileDto? = null,
    val doctor: ProfileDto? = null
)

@Serializable
data class ProfileDto(
    val id: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val hospital: String? = null,
    val specialty: String? = null,
    val role: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)
