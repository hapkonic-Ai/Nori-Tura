package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ConsentFormDto(
    val id: String,
    @SerialName("admission_id") val admissionId: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("form_type") val formType: String,
    @SerialName("content_json") val contentJson: JsonObject? = null,
    @SerialName("pdf_url") val pdfUrl: String? = null,
    @SerialName("signed_pdf_url") val signedPdfUrl: String? = null,
    @SerialName("parent_signature_url") val parentSignatureUrl: String? = null,
    @SerialName("witness_name") val witnessName: String? = null,
    @SerialName("witness_signature_url") val witnessSignatureUrl: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    @SerialName("generated_by") val generatedBy: String? = null,
    val status: String? = null
)
