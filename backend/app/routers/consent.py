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
from app.services.rag_service import get_consent_content
from app.core.config import get_settings
from app.schemas.consent import ConsentFormCreate, ConsentOtpVerifyRequest, ConsentSuggestRequest
from app.schemas.consent_acknowledgment import (
    ConsentAcknowledgmentRequest,
    ConsentAcknowledgmentResponse,
    LatestConsentResponse,
)

settings = get_settings()
router = APIRouter(prefix="/consent", tags=["Consent"])


def _apply_surgical_template(template) -> dict:
    """Map a surgical_templates record into consent form defaults per docs/consent-template-mapping.md."""
    defaults: dict = {}

    if template.name:
        defaults["form_type"] = template.name
    if template.procedure:
        defaults["procedure"] = template.procedure
    if template.anaesthesia:
        defaults["anesthesia"] = ", ".join(template.anaesthesia)

    # Build procedureDescription from approach, technique, specialInstructions
    procedure_description_parts = []
    if template.approach:
        procedure_description_parts.append(f"Approach: {template.approach}")
    if template.technique:
        procedure_description_parts.append(f"Technique: {template.technique}")
    if template.special_instructions:
        procedure_description_parts.append(template.special_instructions)
    if procedure_description_parts:
        defaults["procedure_description"] = "\n\n".join(procedure_description_parts)

    if template.risks:
        defaults["risks"] = "\n".join(template.risks)
    if template.complications:
        complications_text = "\n".join(template.complications)
        defaults["material_risks"] = complications_text
        defaults["possible_complications"] = complications_text
    if template.benefits:
        defaults["benefits"] = "\n".join(template.benefits)
    if template.alternatives:
        defaults["alternatives"] = "\n".join(template.alternatives)
    if template.post_op_care:
        defaults["post_op_care"] = template.post_op_care
    if template.expected_recovery:
        defaults["expected_recovery"] = template.expected_recovery

    return defaults


CONSENT_TEXT = """
NONI TURA SURGICAL CARE PLATFORM
Terms & Consent Agreement

By using this platform, you acknowledge:

1. MEDICAL INFORMATION STORAGE
   Your medical information will be stored securely in compliance with Indian healthcare regulations.

2. DATA SCOPE
   Your data is accessible only to your treating surgeon and assigned nursing staff.

3. ELECTRONIC RECORDS
   You consent to electronic storage of medical records including OPD notes, surgical records,
   and imaging studies.

4. DATA DELETION
   You can request data deletion at any time by contacting your surgeon or administrator.

5. PRIVACY COMMITMENT
   Your data will not be shared with third parties without explicit consent, except as required
   by law.

Version: v1.0
Last Updated: 2026-07-14
"""


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


@router.get("/latest", response_model=LatestConsentResponse)
async def get_latest_consent():
    """Return latest consent text and version for display."""
    return {
        "version": "v1.0",
        "title": "Terms & Consent",
        "content": CONSENT_TEXT,
        "requires_acknowledgment": True
    }


@router.post("/acknowledge", status_code=status.HTTP_200_OK, response_model=ConsentAcknowledgmentResponse)
async def acknowledge_consent(req: ConsentAcknowledgmentRequest):
    """Log consent acknowledgment."""
    try:
        now = datetime.now(timezone.utc)
        result = await prisma.consent_acknowledgments.upsert(
            where={
                "user_phone_consent_version": {
                    "user_phone": req.phone,
                    "consent_version": "v1.0"
                }
            },
            update={
                "acknowledged": True,
                "acknowledged_at": now,
                "device_info": req.device_info,
                "ip_address": req.client_ip,
                "updated_at": now
            },
            create={
                "user_phone": req.phone,
                "user_role": "pending",
                "consent_version": "v1.0",
                "consent_text": CONSENT_TEXT,
                "acknowledged": True,
                "acknowledged_at": now,
                "ip_address": req.client_ip,
                "device_info": req.device_info,
            }
        )
        return {
            "acknowledged": True,
            "acknowledged_at": result.acknowledged_at
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to acknowledge consent: {str(e)}")


@router.post("/suggest-content")
async def suggest_consent_content(
    req: ConsentSuggestRequest,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    """
    Preview RAG-suggested clinical content for a consent form before creating it.
    Frontend can call this as the surgeon types the procedure/diagnosis, then
    pre-fill the consent form fields for review.

    Returns the same shape as rag_service.get_consent_content, or {} if RAG not configured.
    """
    content = await get_consent_content(
        procedure=req.procedure,
        diagnosis=req.diagnosis,
        patient_age=req.patient_age,
        patient_gender=req.patient_gender,
    )
    return content or {}


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

    # Load optional reusable content template
    content_template = None
    if req.content_template_id:
        content_template = await prisma.consent_content_templates.find_first(
            where={"id": req.content_template_id, "is_active": True}
        )
        if not content_template:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Consent content template not found or inactive",
            )

    # Load optional surgical template and map its fields to consent defaults
    template_defaults: dict = {}
    if req.surgical_template_id:
        surgical_template = await prisma.surgical_templates.find_first(
            where={"id": req.surgical_template_id}
        )
        if not surgical_template:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Surgical template not found",
            )
        template_defaults.update(_apply_surgical_template(surgical_template))

    # Merge consent content template defaults after surgical template so either can be used
    if content_template:
        for field in [
            "procedure_description",
            "risks",
            "benefits",
            "alternatives",
            "possible_complications",
            "material_risks",
            "post_op_care",
            "expected_recovery",
            "statutory_reference",
        ]:
            value = getattr(content_template, field)
            if value is not None:
                if field in ["risks", "benefits", "alternatives", "possible_complications"]:
                    template_defaults[field] = "\n".join(value) if isinstance(value, list) else value
                elif field == "anesthesia":
                    template_defaults[field] = ", ".join(value) if isinstance(value, list) else value
                else:
                    template_defaults[field] = value

    # Query consent RAG for procedure-specific clinical content (risks, complications,
    # alternatives, recovery). RAG output acts as the final default; request fields,
    # surgical template, and content template override it when explicitly provided.
    rag_content = await get_consent_content(
        procedure=req.procedure,
        diagnosis=req.diagnosis,
        patient_age=patient.age,
        patient_gender=patient.gender,
    ) or {}

    # Load optional layout template from DB
    layout_html = None
    if req.layout_template_name:
        layout = await prisma.consent_layout_templates.find_first(
            where={"name": req.layout_template_name}
        )
        if not layout:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Consent layout template not found",
            )
        layout_html = layout.html

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
        "form_type": req.form_type or template_defaults.get("form_type", ""),
        "layout_template_name": req.layout_template_name,

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

        # Clinical — explicit request takes priority, then template defaults, then RAG
        "diagnosis": req.diagnosis,
        "procedure": req.procedure or template_defaults.get("procedure", ""),
        "procedure_description": req.procedure_description or template_defaults.get("procedure_description") or rag_content.get("procedure_description", ""),
        "anesthesia": req.anesthesia or template_defaults.get("anesthesia", ""),
        "benefits": req.benefits or template_defaults.get("benefits") or rag_content.get("benefits", ""),
        "risks": req.risks or template_defaults.get("risks") or rag_content.get("risks", ""),
        "material_risks": req.material_risks or req.risks or template_defaults.get("material_risks") or rag_content.get("material_risks", ""),
        "possible_complications": req.possible_complications or template_defaults.get("possible_complications") or rag_content.get("possible_complications", ""),
        "alternatives": req.alternatives or template_defaults.get("alternatives") or rag_content.get("alternatives", ""),
        "post_op_care": req.post_op_care or template_defaults.get("post_op_care") or rag_content.get("post_op_care", ""),
        "expected_recovery": req.expected_recovery or template_defaults.get("expected_recovery") or rag_content.get("expected_recovery", ""),
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

        # Privacy / statutory text — RAG can supply procedure-specific guideline references
        "privacy_statement": (
            "Personal and medical information will be kept confidential and used only for treatment, "
            "billing, quality assurance, and as required by law."
        ),
        "statutory_reference": rag_content.get(
            "applicable_guidelines",
            "This consent is obtained in accordance with the principles of informed consent laid down by "
            "the National Medical Commission (NMC) and NABH standards for patient rights."
        ),
        "rag_assisted": bool(rag_content),  # audit flag so you know RAG pre-populated this form
    }

    # Create the consent record first so the generated PDF can reference the
    # real consent id in its footer and QR code.
    consent = await prisma.consent_forms.create(
        data={
            "admission_id": req.admission_id,
            "patient_id": patient.id,
            "doctor_id": doctor_id,
            "form_type": form_data["form_type"],
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
            "diagnosis": form_data["diagnosis"],
            "procedure_description": form_data["procedure_description"],
            "expected_recovery": form_data["expected_recovery"],
            "possible_complications": form_data["possible_complications"],
            "material_risks": form_data["material_risks"],
        }
    )

    # Add the generated consent id back into the stored form data so the PDF
    # footer and QR code reference the correct record.
    form_data["consent_id"] = consent.id
    pdf_result = generate_consent_pdf(form_data, template_html=layout_html)
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


def _mask_phone(phone: str) -> str:
    """Mask an Indian mobile number for display, e.g. +91*****43210."""
    if len(phone) >= 5:
        return phone[:3] + "*****" + phone[-4:]
    return phone


@router.post("/forms/{consent_id}/request-otp")
async def request_consent_otp(
    consent_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    """Send a 6-digit OTP to the patient's parent phone for consent signing.

    The OTP is bound to this consent form (purpose="consent_sign",
    context_id=consent_id) so it cannot be replayed for login or for another
    consent form.
    """
    from app.services.otp_service import create_otp_session

    consent = await prisma.consent_forms.find_first(where={"id": consent_id})
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    await _require_consent_access(user, consent)

    if consent.status != "pending":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Consent form already signed")

    patient = await prisma.patients.find_first(where={"id": consent.patient_id})
    if not patient or not patient.parent_phone:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Patient has no registered parent phone")

    returned_otp = await create_otp_session(
        patient.parent_phone,
        role="patient_parent",
        purpose="consent_sign",
        context_id=consent_id,
    )

    return {
        "message": "OTP sent successfully",
        "expires_in_minutes": settings.OTP_EXPIRY_MINUTES,
        "phone": _mask_phone(patient.parent_phone),
        "dev_otp": returned_otp,
    }


@router.post("/forms/{consent_id}/verify-otp")
async def verify_consent_otp_and_sign(
    consent_id: str,
    req: ConsentOtpVerifyRequest,
    user: CurrentUser = Depends(get_current_user),
):
    """Verify the parent OTP and mark the consent form as signed."""
    from app.services.otp_service import verify_otp

    consent = await prisma.consent_forms.find_first(where={"id": consent_id})
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    await _require_consent_access(user, consent)

    if consent.status != "pending":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Consent form already signed")

    patient = await prisma.patients.find_first(where={"id": consent.patient_id})
    if not patient or not patient.parent_phone:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Patient has no registered parent phone")

    try:
        await verify_otp(
            patient.parent_phone,
            req.otp,
            purpose="consent_sign",
            context_id=consent_id,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))

    signed_at = datetime.now(timezone.utc)
    masked_phone = _mask_phone(patient.parent_phone)
    form_data = consent.content_json if isinstance(consent.content_json, dict) else dict(consent.content_json or {})
    form_data["consent_id"] = consent.id
    form_data["status"] = "Signed"

    # Load layout template used when the form was created
    layout_html = None
    layout_template_name = form_data.get("layout_template_name")
    if layout_template_name:
        layout = await prisma.consent_layout_templates.find_first(
            where={"name": layout_template_name}
        )
        if layout:
            layout_html = layout.html

    signed_pdf_result = generate_signed_consent_pdf(
        form_data=form_data,
        parent_auth_method="otp",
        parent_auth_phone=masked_phone,
        witness_name=req.witness_name,
        witness_relationship=req.witness_relationship,
        witness_mobile=req.witness_mobile,
        signed_at=signed_at.isoformat(),
        template_html=layout_html,
    )
    signed_pdf_bytes = signed_pdf_result["pdf_bytes"]
    signed_pdf_hash = signed_pdf_result["pdf_hash"]
    signed_filename = f"signed_consent_{consent_id}_{signed_at.isoformat()}"
    signed_pdf_url = _upload_consent_pdf(signed_pdf_bytes, signed_filename)

    updated = await prisma.consent_forms.update(
        where={"id": consent_id},
        data={
            "parent_auth_method": "otp",
            "parent_auth_phone": masked_phone,
            "otp_verified_at": signed_at,
            "witness_name": req.witness_name,
            "witness_relationship": req.witness_relationship,
            "witness_mobile": req.witness_mobile,
            "signed_at": signed_at,
            "signed_pdf_url": signed_pdf_url,
            "signed_pdf_hash": signed_pdf_hash,
            "status": "signed",
        },
    )
    return updated
