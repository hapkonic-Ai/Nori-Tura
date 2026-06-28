from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import date, datetime, timedelta, time

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_user,
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)
from app.services import whatsapp_service, sms_service

router = APIRouter(prefix="/opd", tags=["OPD"])


class MedicationCreate(BaseModel):
    name: str
    dose: str
    frequency: str
    duration: str


class InvestigationCreate(BaseModel):
    type: str
    status: Optional[str] = "pending"


class OPDRecordCreate(BaseModel):
    visit_type: str
    complaint: str
    examination: str
    diagnosis: Optional[str] = None
    surgical_decision: Optional[str] = None
    planned_procedure: Optional[str] = None
    advice: Optional[str] = None
    follow_up_date: Optional[datetime] = None
    medications: Optional[List[MedicationCreate]] = None
    investigations: Optional[List[InvestigationCreate]] = None
    prescription_image_urls: Optional[List[str]] = None


class WhatsAppPreviewResponse(BaseModel):
    phone: str
    body: str
    can_send_whatsapp: bool
    can_send_sms: bool


class SendMessageRequest(BaseModel):
    channel: str = Field(..., pattern="^(whatsapp|sms)$")
    message: Optional[str] = None


class SendMessageResponse(BaseModel):
    status: str
    channel: str
    message_body: str


async def _require_patient_access(user: CurrentUser, patient_id: str):
    patient = await prisma.patients.find_first(where={"id": patient_id})
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    if user.is_parent():
        if patient.parent_phone != user.phone:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if patient.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return patient


@router.get("/patients/{patient_id}/records")
async def list_opd_records(
    patient_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    await _require_patient_access(user, patient_id)
    records = await prisma.opd_records.find_many(
        where={"patient_id": patient_id},
        order={"created_at": "desc"},
        include={"medications": True, "investigations": True, "hospital": True, "doctor": True},
    )
    return records


@router.post("/patients/{patient_id}/records", status_code=status.HTTP_201_CREATED)
async def create_opd_record(
    patient_id: str,
    req: OPDRecordCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    patient = await _require_patient_access(user, patient_id)
    doctor_id = await resolve_doctor_id(user)

    if user.is_nurse() and req.surgical_decision is not None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Nurses cannot set surgical decision",
        )

    is_nurse = user.is_nurse()

    doctor = await prisma.doctors.find_unique(
        where={"id": doctor_id},
        include={"hospital": True},
    )
    hospital_id = doctor.hospital_id if doctor else None
    hospital_name = doctor.hospital.name if doctor and doctor.hospital else None
    hospital_logo_url = doctor.hospital.logo_url if doctor and doctor.hospital else None

    data = {
        "patient_id": patient_id,
        "doctor_id": doctor_id,
        "hospital_id": hospital_id,
        "hospital_name": hospital_name,
        "hospital_logo_url": hospital_logo_url,
        "nurse_id": user.nurse_id if is_nurse else None,
        "created_by": "nurse" if is_nurse else "surgeon",
        "review_status": "pending_review" if is_nurse else "reviewed",
        "visit_type": req.visit_type,
        "complaint": req.complaint,
        "examination": req.examination,
        "diagnosis": req.diagnosis,
        "surgical_decision": req.surgical_decision,
        "planned_procedure": req.planned_procedure,
        "advice": req.advice,
        "follow_up_date": req.follow_up_date,
        "prescription_image_urls": req.prescription_image_urls or [],
    }

    if req.medications:
        data["medications"] = {
            "create": [m.model_dump() for m in req.medications]
        }
    if req.investigations:
        data["investigations"] = {
            "create": [inv.model_dump() for inv in req.investigations]
        }

    record = await prisma.opd_records.create(
        data=data,
        include={"medications": True, "investigations": True, "hospital": True, "doctor": True, "patient": True},
    )
    return record


@router.get("/records/{record_id}")
async def get_opd_record(
    record_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    record = await prisma.opd_records.find_first(
        where={"id": record_id},
        include={"medications": True, "investigations": True, "patient": True, "hospital": True, "doctor": True},
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Record not found")

    patient = record.patient
    if user.is_parent():
        if patient.parent_phone != user.phone:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if patient.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return record


@router.get("/follow-ups")
async def list_follow_ups(
    follow_up_date: Optional[date] = None,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    target_date = follow_up_date or (date.today() + timedelta(days=1))
    start = datetime.combine(target_date, time.min)
    end = datetime.combine(target_date, time.max)

    doctor_id = await resolve_doctor_id(user)
    records = await prisma.opd_records.find_many(
        where={
            "doctor_id": doctor_id,
            "follow_up_date": {"gte": start, "lte": end},
        },
        order={"follow_up_date": "asc"},
        include={"patient": True, "doctor": True, "medications": True, "investigations": True},
    )
    return records


def _build_follow_up_message(record) -> str:
    patient = record.patient
    doctor = record.doctor
    follow_up = (
        record.follow_up_date.strftime("%d %b %Y")
        if record.follow_up_date
        else "as advised"
    )

    lines = [
        "🏥 Noni Tura - Follow-up Reminder",
        "",
        f"Patient: {patient.name or 'Unknown'}",
        f"Doctor: {doctor.name or 'Unknown'}",
        f"Follow-up: {follow_up}",
    ]

    if record.diagnosis:
        lines.extend(["", f"Diagnosis: {record.diagnosis}"])

    if record.medications:
        meds = [
            f"• {m.name} - {m.dose} {m.frequency} × {m.duration}"
            for m in record.medications
        ]
        lines.extend(["", "Medications:"] + meds)

    if record.advice:
        lines.extend(["", f"Advice: {record.advice}"])

    lines.extend(["", "Reply STOP to unsubscribe"])
    return "\n".join(lines)


@router.get("/follow-ups/{record_id}/preview", response_model=WhatsAppPreviewResponse)
async def preview_follow_up_message(
    record_id: str,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    record = await prisma.opd_records.find_first(
        where={"id": record_id},
        include={"patient": True, "doctor": True, "medications": True},
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Record not found")

    doctor_id = await resolve_doctor_id(user)
    if record.patient.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    phone = record.patient.parent_phone
    if not phone:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Patient has no parent phone")

    body = _build_follow_up_message(record)
    return WhatsAppPreviewResponse(
        phone=phone,
        body=body,
        can_send_whatsapp=whatsapp_service.is_configured(),
        can_send_sms=bool(get_settings().TWO_FACTOR_API_KEY),
    )


@router.post("/follow-ups/{record_id}/send", response_model=SendMessageResponse)
async def send_follow_up_message(
    record_id: str,
    req: SendMessageRequest,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    record = await prisma.opd_records.find_first(
        where={"id": record_id},
        include={"patient": True, "doctor": True, "medications": True},
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Record not found")

    doctor_id = await resolve_doctor_id(user)
    if record.patient.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    phone = record.patient.parent_phone
    if not phone:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Patient has no parent phone")

    default_body = _build_follow_up_message(record)
    message_body = (req.message or default_body).strip()

    if req.channel == "whatsapp":
        follow_up_str = (
            record.follow_up_date.strftime("%Y-%m-%d")
            if record.follow_up_date
            else "as advised"
        )
        result = await whatsapp_service.send_follow_up_reminder(
            to=phone,
            patient_name=record.patient.name or "Patient",
            doctor_name=record.doctor.name or "Doctor",
            follow_up_date=follow_up_str,
        )
        send_status = "sent" if result.get("status") not in ("skipped", "failed") else (result.get("status") or "failed")
    else:  # sms
        result = await sms_service.send_sms(phone, message_body)
        send_status = "sent" if result.get("status") not in ("skipped", "failed") else (result.get("status") or "failed")

    await prisma.whatsapp_logs.create(
        data={
            "patient_id": record.patient.id,
            "doctor_id": doctor_id,
            "trigger_type": "follow_up",
            "message_body": message_body,
            "status": send_status,
        }
    )

    if send_status == "sent":
        await prisma.opd_records.update(
            where={"id": record_id},
            data={"reminder_sent": True},
        )

    return SendMessageResponse(
        status=send_status,
        channel=req.channel,
        message_body=message_body,
    )
