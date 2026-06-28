"""Pydantic schemas for consent form requests and responses."""

from typing import Optional

from pydantic import BaseModel


class ConsentFormCreate(BaseModel):
    """Request body for creating a new consent form.

    All new compliance fields are optional with sensible defaults so that
    existing clients continue to work without modification.
    """

    # Existing fields
    admission_id: str
    form_type: str
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

    # Specific consents
    consent_for_anesthesia: bool = True
    consent_for_blood_products: bool = False
    consent_for_photography: bool = False


class ConsentSignRequest(BaseModel):
    """Request body for signing a consent form."""

    parent_signature_url: str
    witness_name: Optional[str] = None
    witness_relationship: Optional[str] = None
    witness_mobile: Optional[str] = None
    witness_signature_url: Optional[str] = None


class ConsentFormResponse(BaseModel):
    """Response wrapper for consent form creation."""

    consent_form: dict
    pdf_url: Optional[str] = None
