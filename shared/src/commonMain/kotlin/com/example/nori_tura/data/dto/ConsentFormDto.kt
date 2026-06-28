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
    @SerialName("witness_relationship") val witnessRelationship: String? = null,
    @SerialName("witness_mobile") val witnessMobile: String? = null,
    @SerialName("witness_signature_url") val witnessSignatureUrl: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    @SerialName("generated_by") val generatedBy: String? = null,
    val status: String? = null,

    // Enhanced consent metadata
    @SerialName("consent_number") val consentNumber: String? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("guardian_relationship") val guardianRelationship: String? = null,
    @SerialName("pdf_hash") val pdfHash: String? = null,
    @SerialName("signed_pdf_hash") val signedPdfHash: String? = null,

    // Hospital information
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("hospital_address") val hospitalAddress: String? = null,
    @SerialName("hospital_contact") val hospitalContact: String? = null,
    @SerialName("hospital_registration_number") val hospitalRegistrationNumber: String? = null,

    // Department / doctor / clinical
    @SerialName("department") val department: String? = null,
    @SerialName("doctor_qualification") val doctorQualification: String? = null,
    @SerialName("doctor_registration_number") val doctorRegistrationNumber: String? = null,
    @SerialName("diagnosis") val diagnosis: String? = null,
    @SerialName("procedure_description") val procedureDescription: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("possible_complications") val possibleComplications: String? = null,
    @SerialName("material_risks") val materialRisks: String? = null
)
