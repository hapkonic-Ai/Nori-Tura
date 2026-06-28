package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsentSignRequest(
    @SerialName("parent_signature_url") val parentSignatureUrl: String,
    @SerialName("witness_name") val witnessName: String? = null,
    @SerialName("witness_relationship") val witnessRelationship: String? = null,
    @SerialName("witness_mobile") val witnessMobile: String? = null,
    @SerialName("witness_signature_url") val witnessSignatureUrl: String? = null
)
