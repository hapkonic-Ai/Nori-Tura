package com.nonituracare.data.dto

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

    // Per-generation capture fields — filled in by the nurse when generating the
    // PDF (no more in-app e-signing; the printed form is signed by hand).
    @SerialName("blood_transfusion_consent") val bloodTransfusionConsent: String? = null, // "consented" | "refused"
    @SerialName("consent_for_photo_medical_record") val consentForPhotoMedicalRecord: Boolean = false,
    @SerialName("consent_for_photo_deidentified_teaching") val consentForPhotoDeidentifiedTeaching: Boolean = false,
    @SerialName("consent_for_photo_publication") val consentForPhotoPublication: Boolean = false,
    @SerialName("specimen_handling_consented") val specimenHandlingConsented: Boolean? = null,
    @SerialName("interpreter_used") val interpreterUsed: Boolean = false,

    // Templates (content_template_id lets the backend select bilingual _en/_hi
    // content columns per `language`)
    @SerialName("content_template_id") val contentTemplateId: String? = null
)
