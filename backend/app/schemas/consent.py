"""Pydantic schemas for consent form requests and responses."""

from typing import Optional

from pydantic import BaseModel, field_validator


class ConsentFormCreate(BaseModel):
    """Request body for creating a new consent form.

    All new compliance fields are optional with sensible defaults so that
    existing clients continue to work without modification.
    """

    # Existing fields
    admission_id: str
    form_type: Optional[str] = None
    diagnosis: str
    procedure: str
    anesthesia: str
    risks: str
    benefits: str
    alternatives: str
    post_op_care: str

    # Hospital information
    hospital_name: Optional[str] = None
    hospital_address: Optional[str] = None
    hospital_contact: Optional[str] = None
    hospital_registration_number: Optional[str] = None

    # Doctor information
    doctor_qualification: Optional[str] = None
    doctor_registration_number: Optional[str] = None

    # Guardian information
    guardian_relationship: Optional[str] = None

    # Clinical information
    procedure_description: Optional[str] = None
    expected_recovery: Optional[str] = None
    possible_complications: Optional[str] = None
    material_risks: Optional[str] = None

    # Consent metadata
    language: Optional[str] = "English"
    consent_version: Optional[str] = "v2.1"

    # Templates
    surgical_template_id: Optional[str] = None
    content_template_id: Optional[str] = None
    layout_template_name: Optional[str] = None

    # Specific consents
    consent_for_anesthesia: bool = True
    consent_for_blood_products: bool = False

    # Per-generation capture fields (nurse-supplied when generating the PDF,
    # not reusable template content). Replaces the old single
    # `consent_for_photography` boolean with the three independent consents
    # the client's consent-form corpus actually asks for.
    blood_transfusion_consent: Optional[str] = None  # "consented" | "refused" | None
    consent_for_photo_medical_record: bool = False
    consent_for_photo_deidentified_teaching: bool = False
    consent_for_photo_publication: bool = False
    specimen_handling_consented: Optional[bool] = None
    interpreter_used: bool = False

    @field_validator("language")
    @classmethod
    def _validate_language(cls, value: Optional[str]) -> str:
        value = value or "English"
        if value not in ("English", "Hindi"):
            raise ValueError('language must be "English" or "Hindi"')
        return value

    @field_validator("blood_transfusion_consent")
    @classmethod
    def _validate_blood_transfusion_consent(cls, value: Optional[str]) -> Optional[str]:
        if value is not None and value not in ("consented", "refused"):
            raise ValueError('blood_transfusion_consent must be "consented" or "refused"')
        return value


class ConsentSuggestRequest(BaseModel):
    """Request body for the /consent/suggest-content endpoint."""

    procedure: str
    diagnosis: str
    patient_age: Optional[int] = None
    patient_gender: Optional[str] = None


class ConsentFormResponse(BaseModel):
    """Response wrapper for consent form creation."""

    consent_form: dict
    pdf_url: Optional[str] = None
