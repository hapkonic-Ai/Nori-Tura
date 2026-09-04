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
from app.services.consent_service import generate_consent_pdf
from app.services.rag_service import get_consent_content
from app.core.config import get_settings
from app.schemas.consent import ConsentFormCreate, ConsentSuggestRequest
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

    # Prefer an explicit procedure_description; otherwise compose one from
    # approach, technique, specialInstructions.
    if template.procedure_description:
        defaults["procedure_description"] = template.procedure_description
    else:
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
        defaults["possible_complications"] = "\n".join(template.complications)
    # Prefer an explicit material_risks; otherwise fall back to complications,
    # same as before this field existed.
    if template.material_risks:
        defaults["material_risks"] = template.material_risks
    elif template.complications:
        defaults["material_risks"] = "\n".join(template.complications)
    if template.benefits:
        defaults["benefits"] = "\n".join(template.benefits)
    if template.alternatives:
        defaults["alternatives"] = "\n".join(template.alternatives)
    if template.post_op_care:
        defaults["post_op_care"] = template.post_op_care
    if template.expected_recovery:
        defaults["expected_recovery"] = template.expected_recovery

    return defaults


def _join_as_bullets(value) -> str:
    """Join a list field into text for the (white-space: pre-line) template.

    A single-item list (the common case for risks/benefits/alternatives,
    which are usually one sentence in this corpus) renders as plain text.
    Multi-item lists (e.g. possible_complications with 15-25 entries) get a
    bullet per line so they read as a list instead of a run-on paragraph.
    """
    if not isinstance(value, list):
        return value
    if len(value) > 1:
        return "\n".join(f"• {item}" for item in value)
    return "\n".join(value)


def _apply_content_template(content_template, language: str) -> dict:
    """Map a consent_content_templates record into consent form defaults.

    Content fields are stored as bilingual `_en`/`_hi` siblings (plus a
    legacy single-language column kept for backward compatibility). This
    picks the column matching `language`, falling back to the legacy column
    for content templates that haven't been migrated to bilingual columns
    yet, so old templates keep working.
    """
    defaults: dict = {}
    suffix = "_hi" if language == "Hindi" else "_en"

    text_fields = [
        "procedure_description",
        "material_risks",
        "post_op_care",
        "expected_recovery",
        "diagnosis_plain_language",
        "consequences_of_no_treatment",
        "cost_conditional_note",
        "specimen_handling_statement",
        "site_side_instruction",
        "conversion_consequences",
        "lifelong_follow_up_note",
        "pre_op_prep",
    ]
    list_fields = ["risks", "benefits", "alternatives", "possible_complications"]
    joined_list_fields = ["consented_contingencies", "technique_options"]

    for field in text_fields:
        value = getattr(content_template, f"{field}{suffix}", None) or getattr(content_template, field, None)
        if value:
            defaults[field] = value

    for field in list_fields:
        value = getattr(content_template, f"{field}{suffix}", None) or getattr(content_template, field, None)
        if value:
            defaults[field] = _join_as_bullets(value)

    for field in joined_list_fields:
        value = getattr(content_template, f"{field}{suffix}", None)
        if value:
            defaults[field] = ", ".join(value) if isinstance(value, list) else value

    anaesthesia_value = getattr(content_template, f"anaesthesia{suffix}", None) or content_template.anesthesia
    if anaesthesia_value:
        defaults["anesthesia"] = ", ".join(anaesthesia_value) if isinstance(anaesthesia_value, list) else anaesthesia_value

    if content_template.procedure:
        defaults["procedure"] = content_template.procedure
    if content_template.statutory_reference:
        defaults["statutory_reference"] = content_template.statutory_reference
    if content_template.cost_category:
        defaults["cost_category"] = content_template.cost_category
    if content_template.is_lateralizable:
        defaults["is_lateralizable"] = True
    if content_template.conversion_to_open_possible:
        defaults["conversion_to_open_possible"] = True
    if content_template.staging_stage_number and content_template.staging_total_stages:
        defaults["staging_stage_number"] = content_template.staging_stage_number
        defaults["staging_total_stages"] = content_template.staging_total_stages
    if content_template.lifelong_follow_up_flag:
        defaults["lifelong_follow_up_flag"] = True

    # A content template can carry approach/technique/special_instructions
    # too, same as a surgical template — fold them into procedure_description
    # the same way, unless an explicit procedure_description already won.
    description_parts = []
    if content_template.approach:
        description_parts.append(f"Approach: {content_template.approach}")
    if content_template.technique:
        description_parts.append(f"Technique: {content_template.technique}")
    if content_template.special_instructions:
        description_parts.append(content_template.special_instructions)
    if description_parts and "procedure_description" not in defaults:
        defaults["procedure_description"] = "\n\n".join(description_parts)

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
    return f"NC-CONSENT-{timestamp}-{random_suffix}"


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
        if req.language == "Hindi" and content_template.hi_content_status == "missing":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Hindi content is not yet available for this consent template",
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
        if surgical_template.doctor_id != doctor_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access denied",
            )
        template_defaults.update(_apply_surgical_template(surgical_template))

    # Merge consent content template defaults (language-selected) after surgical
    # template so either can be used; content template wins on overlapping fields.
    if content_template:
        template_defaults.update(_apply_content_template(content_template, req.language or "English"))

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
        "material_risks": req.material_risks or template_defaults.get("material_risks") or rag_content.get("material_risks", "") or req.risks,
        "possible_complications": req.possible_complications or template_defaults.get("possible_complications") or rag_content.get("possible_complications", ""),
        "alternatives": req.alternatives or template_defaults.get("alternatives") or rag_content.get("alternatives", ""),
        "post_op_care": req.post_op_care or template_defaults.get("post_op_care") or rag_content.get("post_op_care", ""),
        "expected_recovery": req.expected_recovery or template_defaults.get("expected_recovery") or rag_content.get("expected_recovery", ""),

        # New client-corpus fields — template-supplied only (no per-request override yet)
        "diagnosis_plain_language": template_defaults.get("diagnosis_plain_language", ""),
        "consequences_of_no_treatment": template_defaults.get("consequences_of_no_treatment", ""),
        "consented_contingencies": template_defaults.get("consented_contingencies", ""),
        "cost_category": template_defaults.get("cost_category", ""),
        "cost_conditional_note": template_defaults.get("cost_conditional_note", ""),
        "specimen_handling_statement": template_defaults.get("specimen_handling_statement", ""),
        "site_side_instruction": template_defaults.get("site_side_instruction") if template_defaults.get("is_lateralizable") else "",
        "conversion_to_open_possible": bool(template_defaults.get("conversion_to_open_possible")),
        "conversion_consequences": template_defaults.get("conversion_consequences", ""),
        "staging_stage_number": template_defaults.get("staging_stage_number"),
        "staging_total_stages": template_defaults.get("staging_total_stages"),
        "technique_options": template_defaults.get("technique_options", ""),
        "lifelong_follow_up_flag": bool(template_defaults.get("lifelong_follow_up_flag")),
        "lifelong_follow_up_note": template_defaults.get("lifelong_follow_up_note", ""),
        "pre_op_prep": template_defaults.get("pre_op_prep", ""),

        "refusal_consequences": (
            "अगर सहमति नहीं दी जाती है, तो इलाज करने वाला डॉक्टर इसके परिणाम समझाएगा, जिसमें रोगी की हालत का "
            "बिगड़ना, लगातार दर्द, अपंगता, या अन्य गंभीर परिणाम शामिल हो सकते हैं।"
            if (req.language or "English") == "Hindi" else
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
        "blood_transfusion_consent": req.blood_transfusion_consent,
        "photo_consent_medical_record": req.consent_for_photo_medical_record,
        "photo_consent_deidentified_teaching": req.consent_for_photo_deidentified_teaching,
        "photo_consent_publication": req.consent_for_photo_publication,
        "specimen_handling_consented": req.specimen_handling_consented,
        "interpreter_used": req.interpreter_used,

        # Privacy / statutory text — RAG can supply procedure-specific guideline references
        "privacy_statement": (
            "Personal and medical information will be kept confidential and used only for treatment, "
            "billing, quality assurance, and as required by law."
        ),
        "statutory_reference": template_defaults.get("statutory_reference") or rag_content.get(
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
            "status": "generated",

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

            # Per-generation capture fields
            "blood_transfusion_consent": req.blood_transfusion_consent,
            "photo_consent_medical_record": req.consent_for_photo_medical_record,
            "photo_consent_deidentified_teaching": req.consent_for_photo_deidentified_teaching,
            "photo_consent_publication": req.consent_for_photo_publication,
            "specimen_handling_consented": req.specimen_handling_consented,
            "interpreter_used": req.interpreter_used,
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

    download_time = datetime.now(timezone.utc)
    updated = await prisma.consent_forms.update(
        where={"id": consent.id},
        data={
            "content_json": Json(form_data),
            "pdf_url": pdf_url,
            "pdf_hash": pdf_hash,
            "downloaded_at": download_time,
            "downloaded_by_user_id": user.nurse_id or user.doctor_id,
            "download_count": {"increment": 1},
        },
    )
    await _log_consent_audit(
        updated,
        "pdf_downloaded",
        user,
        detail={"language": req.language or "English", "stage": "generated"},
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


@router.get("/forms/{consent_id}/audit")
async def get_consent_audit_events(
    consent_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    """Return the audit trail for a consent form (OTP requests, signing events)."""
    consent = await prisma.consent_forms.find_first(where={"id": consent_id})
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    await _require_consent_access(user, consent)

    events = await prisma.consent_audit_events.find_many(
        where={"consent_id": consent_id},
        order={"created_at": "asc"},
    )
    return {
        "consent_id": consent_id,
        "consent_number": consent.consent_number,
        "events": [
            {
                "id": e.id,
                "event_type": e.event_type,
                "actor_user_id": e.actor_user_id,
                "actor_role": e.actor_role,
                "actor_phone": e.actor_phone,
                "signer_phone": e.signer_phone,
                "detail": e.detail,
                "created_at": e.created_at.isoformat() if e.created_at else None,
            }
            for e in events
        ],
    }


def _mask_phone(phone: str) -> str:
    """Mask an Indian mobile number for display, e.g. +91*****43210."""
    if len(phone) >= 5:
        return phone[:3] + "*****" + phone[-4:]
    return phone


async def _log_consent_audit(
    consent,
    event_type: str,
    user: CurrentUser,
    signer_phone: Optional[str] = None,
    detail: Optional[dict] = None,
) -> None:
    """Record an audit event for a consent form.

    Captures the consent reference, the initiating user (who operated the
    screen) and the signer's masked phone, documenting the assisted,
    in-person consent workflow.
    """
    # Prefer the id matching the actor's own role (nurse / patient) over the
    # supervising doctor so the audit names the actual initiator.
    actor_user_id = user.nurse_id or user.patient_id or user.doctor_id
    await prisma.consent_audit_events.create(
        data={
            "consent_id": consent.id,
            "consent_number": getattr(consent, "consent_number", None),
            "event_type": event_type,
            "actor_user_id": actor_user_id,
            "actor_role": user.role,
            "actor_phone": _mask_phone(user.phone) if user.phone else None,
            "signer_phone": signer_phone,
            "detail": Json(detail) if detail is not None else Json("{}"),
        }
    )




@router.get("/forms/{consent_id}/download")
async def download_consent_form_pdf(
    consent_id: str,
    language: Optional[str] = None,
    user: CurrentUser = Depends(get_current_user),
):
    """Download the consent PDF for a patient/admission.

    Nurses and doctors use this to fetch the printable form; parents can also
    read their own child's forms via `_require_consent_access`. If `language`
    differs from the language the form was originally generated in, the PDF
    is regenerated on the fly from `content_json` merged with the other
    language's content-template columns (mirrors admin.py's
    `/admin/consent-forms/{id}/download`, but reachable by nurses).
    """
    consent = await prisma.consent_forms.find_first(where={"id": consent_id})
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")
    await _require_consent_access(user, consent)

    requested_language = language or consent.language or "English"
    if requested_language not in ("English", "Hindi"):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail='language must be "English" or "Hindi"')

    form_data = consent.content_json if isinstance(consent.content_json, dict) else dict(consent.content_json or {})
    form_data["consent_id"] = consent.id

    layout_html = None
    layout_template_name = form_data.get("layout_template_name")
    if layout_template_name:
        layout = await prisma.consent_layout_templates.find_first(where={"name": layout_template_name})
        if layout:
            layout_html = layout.html

    # Same-language re-download: prefer the already-generated PDF URL rather
    # than re-rendering, when we have one.
    if requested_language == consent.language and consent.pdf_url:
        pdf_url = consent.pdf_url
        download_time = datetime.now(timezone.utc)
        await prisma.consent_forms.update(
            where={"id": consent.id},
            data={
                "downloaded_at": download_time,
                "downloaded_by_user_id": user.nurse_id or user.doctor_id,
                "download_count": {"increment": 1},
            },
        )
        await _log_consent_audit(consent, "pdf_downloaded", user, detail={"language": requested_language, "stage": "redownload"})
        return {"pdf_url": pdf_url}

    # Different-language (or no cached URL): re-render from the content template
    # in the requested language, without mutating the stored content_json.
    if requested_language != consent.language:
        content_template = None
        procedure = form_data.get("procedure")
        if procedure:
            content_template = await prisma.consent_content_templates.find_first(
                where={"procedure": procedure, "is_active": True}
            )
        if content_template:
            if requested_language == "Hindi" and content_template.hi_content_status == "missing":
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Hindi content is not yet available for this consent's procedure",
                )
            form_data = {**form_data, **_apply_content_template(content_template, requested_language)}
        form_data["language"] = requested_language

    pdf_result = generate_consent_pdf(form_data, template_html=layout_html)
    pdf_bytes = pdf_result["pdf_bytes"]

    # Upload the regenerated PDF so the response is a plain URL — the same
    # shape as POST /consent/forms's response, which the frontend already
    # knows how to open via its cross-platform `openUrl` helper. This avoids
    # needing a separate authenticated byte-stream-to-file-save path in the
    # Kotlin Multiplatform client (Android/iOS/web each have their own APIs
    # for that, none of which exist in this app yet).
    filename = f"consent_{consent.id}_{requested_language.lower()}_{datetime.now(timezone.utc).isoformat()}"
    pdf_url = _upload_consent_pdf(pdf_bytes, filename)

    download_time = datetime.now(timezone.utc)
    await prisma.consent_forms.update(
        where={"id": consent.id},
        data={
            "downloaded_at": download_time,
            "downloaded_by_user_id": user.nurse_id or user.doctor_id,
            "download_count": {"increment": 1},
        },
    )
    await _log_consent_audit(
        consent,
        "pdf_downloaded",
        user,
        detail={"language": requested_language, "stage": "regenerated"},
    )

    return {"pdf_url": pdf_url}
