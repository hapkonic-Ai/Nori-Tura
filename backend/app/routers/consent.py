from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status

from prisma import Json

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_user,
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)
from app.services.consent_service import generate_consent_pdf, generate_signed_consent_pdf
from app.core.config import get_settings
from app.schemas.consent import ConsentFormCreate, ConsentSignRequest

settings = get_settings()
router = APIRouter(prefix="/consent", tags=["Consent"])


def _generate_consent_number() -> str:
    """Generate a unique, human-readable consent reference number."""
    now = datetime.now(timezone.utc)
    timestamp = now.strftime("%Y%m%d%H%M%S")
    random_suffix = now.strftime("%f")[:4]
    return f"NT-CONSENT-{timestamp}-{random_suffix}"


def _upload_consent_pdf(pdf_bytes: bytes, filename: str) -> Optional[str]:
    """Upload PDF to Cloudinary. Returns URL or None if not configured."""
    if not settings.CLOUDINARY_CLOUD_NAME or not settings.CLOUDINARY_API_KEY or not settings.CLOUDINARY_API_SECRET:
        print(f"[Cloudinary stub] Would upload {filename} ({len(pdf_bytes)} bytes)")
        return None

    try:
        import cloudinary
        import cloudinary.uploader

        cloudinary.config(
            cloud_name=settings.CLOUDINARY_CLOUD_NAME,
            api_key=settings.CLOUDINARY_API_KEY,
            api_secret=settings.CLOUDINARY_API_SECRET,
        )
        result = cloudinary.uploader.upload(
            pdf_bytes,
            resource_type="raw",
            public_id=filename,
            folder="nonitura/consents",
        )
        return result.get("secure_url")
    except Exception as e:
        print(f"Cloudinary upload error: {e}")
        return None


@router.post("/forms", status_code=status.HTTP_201_CREATED)
async def create_consent_form(
    req: ConsentFormCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    doctor_id = await resolve_doctor_id(user)

    admission = await prisma.ipd_admissions.find_first(
        where={"id": req.admission_id},
        include={"patient": True, "doctor": {"include": {"hospital": True}}},
    )
    if not admission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admission not found")
    if admission.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    patient = admission.patient
    doctor = admission.doctor

    now = datetime.now(timezone.utc)
    consent_number = _generate_consent_number()

    # Derive defaults from related records when not provided by the client.
    hospital = doctor.hospital
    hospital_name = req.hospital_name or (hospital.name if hospital else None) or "Hospital Name"
    hospital_address = req.hospital_address or (hospital.address if hospital else "") or ""
    hospital_contact = req.hospital_contact or (hospital.contact if hospital else "") or ""
    hospital_registration_number = req.hospital_registration_number or (hospital.registration_number if hospital else "") or ""
    hospital_id = hospital.id if hospital else None
    hospital_logo_url = hospital.logo_url if hospital else None

    doctor_qualification = req.doctor_qualification or doctor.specialty or ""
    doctor_registration_number = req.doctor_registration_number or ""

    department = doctor.specialty or "Pediatric Surgery"
    ward_room = " / ".join(
        part for part in [admission.ward, admission.bed_no] if part
    ) or "—"

    form_data = {
        # Identifiers
        "consent_id": "",  # filled after DB creation
        "patient_id": patient.id,
        "admission_id": req.admission_id,
        "consent_number": consent_number,
        "version": req.consent_version or "v2.1",
        "status": "Pending",
        "generated_at": now.isoformat(),
        "language": req.language or "English",
        "form_type": req.form_type,

        # Hospital
        "hospital_name": hospital_name,
        "hospital_address": hospital_address,
        "hospital_contact": hospital_contact,
        "hospital_registration_number": hospital_registration_number,

        # Patient
        "patient_name": patient.name,
        "patient_uhid": patient.id,  # Using patient.id as UHID fallback
        "age": patient.age,
        "gender": patient.gender,
        "admission_number": admission.id,
        "department": department,
        "ward_room": ward_room,

        # Guardian
        "parent_name": patient.parent_name,
        "guardian_relationship": req.guardian_relationship or "Parent / Guardian",
        "parent_phone": patient.parent_phone,

        # Doctor
        "surgeon_name": doctor.name,
        "doctor_qualification": doctor_qualification,
        "doctor_registration_number": doctor_registration_number,
        "doctor_declaration_timestamp": now.isoformat(),

        # Clinical
        "diagnosis": req.diagnosis,
        "procedure": req.procedure,
        "procedure_description": req.procedure_description or "",
        "anesthesia": req.anesthesia,
        "benefits": req.benefits,
        "risks": req.risks,
        "material_risks": req.material_risks or req.risks,
        "possible_complications": req.possible_complications or "",
        "alternatives": req.alternatives,
        "post_op_care": req.post_op_care,
        "expected_recovery": req.expected_recovery or "",
        "refusal_consequences": (
            "If consent is refused, the treating doctor will explain the consequences, which may include "
            "worsening of the patient's condition, persistent pain, disability, or other serious outcomes."
        ),
        "right_to_withdraw": (
            "The parent/legal guardian has the right to withdraw consent at any time before or during the "
            "procedure without affecting the patient's right to future care and treatment."
        ),

        # Specific consents
        "consent_for_anesthesia": "Yes" if req.consent_for_anesthesia else "No",
        "consent_for_blood_products": "Yes" if req.consent_for_blood_products else "No",
        "consent_for_photography": "Yes" if req.consent_for_photography else "No",

        # Privacy / statutory text
        "privacy_statement": (
            "Personal and medical information will be kept confidential and used only for treatment, "
            "billing, quality assurance, and as required by law."
        ),
        "statutory_reference": (
            "This consent is obtained in accordance with the principles of informed consent laid down by "
            "the National Medical Commission (NMC) and NABH standards for patient rights."
        ),
    }

    # Create the consent record first so the generated PDF can reference the
    # real consent id in its footer and QR code.
    consent = await prisma.consent_forms.create(
        data={
            "admission_id": req.admission_id,
            "patient_id": patient.id,
            "doctor_id": doctor_id,
            "form_type": req.form_type,
            "content_json": Json(form_data),
            "generated_by": user.role,
            "status": "pending",

            # Enhanced metadata
            "consent_number": consent_number,
            "version": req.consent_version or "v2.1",
            "language": req.language or "English",
            "guardian_relationship": req.guardian_relationship,
            "hospital_id": hospital_id,
            "hospital_name": hospital_name,
            "hospital_address": hospital_address,
            "hospital_contact": hospital_contact,
            "hospital_registration_number": hospital_registration_number,
            "hospital_logo_url": hospital_logo_url,
            "department": department,
            "doctor_qualification": doctor_qualification,
            "doctor_registration_number": doctor_registration_number,
            "diagnosis": req.diagnosis,
            "procedure_description": req.procedure_description,
            "expected_recovery": req.expected_recovery,
            "possible_complications": req.possible_complications,
            "material_risks": req.material_risks or req.risks,
        }
    )

    # Add the generated consent id back into the stored form data so the PDF
    # footer and QR code reference the correct record.
    form_data["consent_id"] = consent.id
    pdf_result = generate_consent_pdf(form_data)
    pdf_bytes = pdf_result["pdf_bytes"]
    pdf_hash = pdf_result["pdf_hash"]

    filename = f"consent_{consent.id}_{now.isoformat()}"
    pdf_url = _upload_consent_pdf(pdf_bytes, filename)

    updated = await prisma.consent_forms.update(
        where={"id": consent.id},
        data={
            "content_json": Json(form_data),
            "pdf_url": pdf_url,
            "pdf_hash": pdf_hash,
        },
    )

    return {"consent_form": updated, "pdf_url": pdf_url}


async def _require_consent_access(user: CurrentUser, consent):
    if user.is_parent():
        patient = await prisma.patients.find_first(where={"id": consent.patient_id})
        if not patient or patient.parent_phone != user.phone:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if consent.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")


@router.get("/forms/{consent_id}")
async def get_consent_form(
    consent_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    consent = await prisma.consent_forms.find_first(
        where={"id": consent_id},
    )
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    await _require_consent_access(user, consent)
    return consent


@router.post("/forms/{consent_id}/sign")
async def sign_consent_form(
    consent_id: str,
    req: ConsentSignRequest,
    user: CurrentUser = Depends(get_current_user),
):
    consent = await prisma.consent_forms.find_first(
        where={"id": consent_id},
    )
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    await _require_consent_access(user, consent)

    if consent.status != "pending":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Consent form already signed")

    signed_at = datetime.now(timezone.utc)
    form_data = consent.content_json if isinstance(consent.content_json, dict) else dict(consent.content_json or {})
    form_data["consent_id"] = consent.id
    form_data["status"] = "Signed"

    signed_pdf_result = generate_signed_consent_pdf(
        form_data=form_data,
        parent_signature_url=req.parent_signature_url,
        witness_name=req.witness_name,
        witness_relationship=req.witness_relationship,
        witness_mobile=req.witness_mobile,
        witness_signature_url=req.witness_signature_url,
        signed_at=signed_at.isoformat(),
    )
    signed_pdf_bytes = signed_pdf_result["pdf_bytes"]
    signed_pdf_hash = signed_pdf_result["pdf_hash"]
    signed_filename = f"signed_consent_{consent_id}_{signed_at.isoformat()}"
    signed_pdf_url = _upload_consent_pdf(signed_pdf_bytes, signed_filename)

    updated = await prisma.consent_forms.update(
        where={"id": consent_id},
        data={
            "parent_signature_url": req.parent_signature_url,
            "witness_name": req.witness_name,
            "witness_relationship": req.witness_relationship,
            "witness_mobile": req.witness_mobile,
            "witness_signature_url": req.witness_signature_url,
            "signed_at": signed_at,
            "signed_pdf_url": signed_pdf_url,
            "signed_pdf_hash": signed_pdf_hash,
            "status": "signed",
        },
    )
    return updated
