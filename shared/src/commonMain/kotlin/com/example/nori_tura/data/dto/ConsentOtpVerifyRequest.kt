package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsentOtpVerifyRequest(
    val otp: String,
    @SerialName("witness_name") val witnessName: String? = null,
    @SerialName("witness_relationship") val witnessRelationship: String? = null,
    @SerialName("witness_mobile") val witnessMobile: String? = null,
    @SerialName("witness_otp") val witnessOtp: String? = null
)

@Serializable
data class ConsentWitnessOtpRequest(
    @SerialName("witness_mobile") val witnessMobile: String
)

@Serializable
data class ConsentOtpRequestResponse(
    val message: String,
    @SerialName("expires_in_minutes") val expiresInMinutes: Int,
    val phone: String,
    @SerialName("dev_otp") val devOtp: String? = null
)
