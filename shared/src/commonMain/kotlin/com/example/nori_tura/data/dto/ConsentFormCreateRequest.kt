package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsentFormCreateRequest(
    @SerialName("admission_id") val admissionId: String,
    @SerialName("form_type") val formType: String,
    val diagnosis: String,
    val procedure: String,
    val anesthesia: String,
    val risks: String,
    val benefits: String,
    val alternatives: String,
    @SerialName("post_op_care") val postOpCare: String,

    // Hospital information
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("hospital_address") val hospitalAddress: String? = null,
    @SerialName("hospital_contact") val hospitalContact: String? = null,
    @SerialName("hospital_registration_number") val hospitalRegistrationNumber: String? = null,

    // Doctor information
    @SerialName("doctor_qualification") val doctorQualification: String? = null,
    @SerialName("doctor_registration_number") val doctorRegistrationNumber: String? = null,

    // Guardian information
    @SerialName("guardian_relationship") val guardianRelationship: String? = null,

    // Enhanced clinical information
    @SerialName("procedure_description") val procedureDescription: String? = null,
    @SerialName("expected_recovery") val expectedRecovery: String? = null,
    @SerialName("possible_complications") val possibleComplications: String? = null,
    @SerialName("material_risks") val materialRisks: String? = null,

    // Consent metadata
    @SerialName("language") val language: String? = "English",
    @SerialName("consent_version") val consentVersion: String? = "v2.1",

    // Specific consents
    @SerialName("consent_for_anesthesia") val consentForAnesthesia: Boolean = true,
    @SerialName("consent_for_blood_products") val consentForBloodProducts: Boolean = false,
    @SerialName("consent_for_photography") val consentForPhotography: Boolean = false
)
