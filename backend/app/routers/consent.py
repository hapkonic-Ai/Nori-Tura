from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel

from prisma import Json

from app.core.database import prisma
from app.core.auth_deps import get_current_surgeon, CurrentUser, resolve_doctor_id
from app.services.consent_service import generate_consent_pdf
from app.core.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/consent", tags=["Consent"])


class ConsentFormCreate(BaseModel):
    admission_id: str
    form_type: str
    procedure: str
    anesthesia: str
    risks: str
    benefits: str
    alternatives: str
    post_op_care: str


class ConsentSignRequest(BaseModel):
    parent_signature_url: str
    witness_name: Optional[str] = None
    witness_signature_url: Optional[str] = None


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
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)

    admission = await prisma.ipd_admissions.find_first(
        where={"id": req.admission_id},
        include={"patient": True, "doctor": True},
    )
    if not admission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admission not found")
    if admission.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    patient = admission.patient
    doctor = admission.doctor

    form_data = {
        "patient_name": patient.name,
        "age": patient.age,
        "gender": patient.gender,
        "parent_name": patient.parent_name,
        "parent_phone": patient.parent_phone,
        "procedure": req.procedure,
        "anesthesia": req.anesthesia,
        "surgeon_name": doctor.name,
        "risks": req.risks,
        "benefits": req.benefits,
        "alternatives": req.alternatives,
        "post_op_care": req.post_op_care,
    }

    pdf_bytes = generate_consent_pdf(form_data)
    filename = f"consent_{admission.id}_{datetime.now(timezone.utc).isoformat()}"
    pdf_url = _upload_consent_pdf(pdf_bytes, filename)

    consent = await prisma.consent_forms.create(
        data={
            "admission_id": req.admission_id,
            "patient_id": patient.id,
            "doctor_id": doctor_id,
            "form_type": req.form_type,
            "content_json": Json(form_data),
            "pdf_url": pdf_url,
            "generated_by": "surgeon",
            "status": "pending",
        }
    )

    return {"consent_form": consent, "pdf_url": pdf_url}


@router.get("/forms/{consent_id}")
async def get_consent_form(
    consent_id: str,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    consent = await prisma.consent_forms.find_first(
        where={"id": consent_id},
    )
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    if consent.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    return consent


@router.post("/forms/{consent_id}/sign")
async def sign_consent_form(
    consent_id: str,
    req: ConsentSignRequest,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    consent = await prisma.consent_forms.find_first(
        where={"id": consent_id},
    )
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    if consent.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    updated = await prisma.consent_forms.update(
        where={"id": consent_id},
        data={
            "parent_signature_url": req.parent_signature_url,
            "witness_name": req.witness_name,
            "witness_signature_url": req.witness_signature_url,
            "signed_at": datetime.now(timezone.utc),
            "status": "signed",
        },
    )
    return updated
