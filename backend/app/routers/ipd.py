from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone

from prisma import Json

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_user,
    get_current_surgeon,
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)

router = APIRouter(prefix="/ipd", tags=["IPD"])


class AdmissionCreate(BaseModel):
    patient_id: str
    urgency: str
    bed_no: Optional[str] = None
    ward: Optional[str] = None


class PreOpNoteCreate(BaseModel):
    procedure: str
    approach: Optional[str] = None
    anaesthesia: Optional[str] = None
    investigations: Optional[List[str]] = None
    risk_level: Optional[str] = None
    special_instructions: Optional[str] = None


class IntraOpNoteCreate(BaseModel):
    procedure_done: str
    findings: Optional[str] = None
    technique: Optional[str] = None
    complications: Optional[str] = None
    blood_loss: Optional[str] = None
    ot_start: Optional[datetime] = None
    ot_end: Optional[datetime] = None


class PostOpNoteCreate(BaseModel):
    day_number: int = Field(..., ge=1)
    condition: str
    vitals_json: Dict[str, Any]
    wound_status: Optional[str] = None
    pain_score: Optional[int] = Field(None, ge=0, le=10)
    diet: Optional[str] = None
    medications_json: Optional[Dict[str, Any]] = None


class WardRoundNoteCreate(BaseModel):
    subjective: Optional[str] = None
    objective: Optional[str] = None
    assessment: Optional[str] = None
    plan: Optional[str] = None
    ready_for_discharge: bool = False


class DischargeSummaryCreate(BaseModel):
    condition_at_discharge: str
    procedure_summary: str
    discharge_medications_json: Dict[str, Any]
    wound_care: Optional[str] = None
    activity_restrictions: Optional[str] = None
    diet_instructions: Optional[str] = None
    follow_up_date: Optional[datetime] = None
    red_flags: Optional[str] = None


async def _require_admission_access(user: CurrentUser, admission_id: str):
    admission = await prisma.ipd_admissions.find_first(
        where={"id": admission_id},
        include={"patient": True},
    )
    if not admission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admission not found")

    patient = admission.patient
    if user.is_parent():
        if patient.parent_phone != user.phone:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if admission.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return admission


async def _require_patient_access(user: CurrentUser, patient_id: str):
    patient = await prisma.patients.find_first(where={"id": patient_id})
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    if user.is_parent():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    doctor_id = await resolve_doctor_id(user)
    if patient.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return patient, doctor_id


@router.get("/admissions")
async def list_admissions(user: CurrentUser = Depends(get_current_user)):
    if user.is_parent():
        patients = await prisma.patients.find_many(
            where={"parent_phone": user.phone},
            select={"id": True},
        )
        patient_ids = [p.id for p in patients]
        admissions = await prisma.ipd_admissions.find_many(
            where={"patient_id": {"in": patient_ids}},
            order={"admitted_at": "desc"},
            include={"patient": True},
        )
    else:
        doctor_id = await resolve_doctor_id(user)
        admissions = await prisma.ipd_admissions.find_many(
            where={"doctor_id": doctor_id},
            order={"admitted_at": "desc"},
            include={"patient": True},
        )
    return admissions


@router.post("/admissions", status_code=status.HTTP_201_CREATED)
async def create_admission(
    req: AdmissionCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    patient, doctor_id = await _require_patient_access(user, req.patient_id)

    data = {
        "patient_id": req.patient_id,
        "doctor_id": doctor_id,
        "urgency": req.urgency,
    }

    if user.is_nurse():
        # Nurses may only create an admission with urgency; no bed/ward/surgical details.
        if req.bed_no is not None or req.ward is not None:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Nurses can only set urgency when creating an admission",
            )
    else:
        data["bed_no"] = req.bed_no
        data["ward"] = req.ward

    admission = await prisma.ipd_admissions.create(
        data=data,
        include={"patient": True},
    )
    return admission


@router.get("/admissions/{admission_id}")
async def get_admission(
    admission_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    admission = await prisma.ipd_admissions.find_first(
        where={"id": admission_id},
        include={
            "patient": True,
            "pre_op_notes": True,
            "intra_op_notes": {"take": 1},
            "post_op_notes": {"take": 1},
            "ward_round_notes": {"take": 1},
            "discharge_summaries": True,
            "consent_forms": {"order": {"generated_at": "desc"}},
        },
    )
    if not admission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admission not found")

    patient = admission.patient
    if user.is_parent():
        if patient.parent_phone != user.phone:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if admission.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return admission


@router.post("/admissions/{admission_id}/pre-op", status_code=status.HTTP_201_CREATED)
async def create_pre_op_note(
    admission_id: str,
    req: PreOpNoteCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    await _require_admission_access(user, admission_id)
    note = await prisma.pre_op_notes.create(
        data={
            "admission_id": admission_id,
            "procedure": req.procedure,
            "approach": req.approach,
            "anaesthesia": req.anaesthesia,
            "investigations": req.investigations or [],
            "risk_level": req.risk_level,
            "special_instructions": req.special_instructions,
        }
    )
    return note


@router.post("/admissions/{admission_id}/intra-op", status_code=status.HTTP_201_CREATED)
async def create_intra_op_note(
    admission_id: str,
    req: IntraOpNoteCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    await _require_admission_access(user, admission_id)
    note = await prisma.intra_op_notes.create(
        data={
            "admission_id": admission_id,
            "procedure_done": req.procedure_done,
            "findings": req.findings,
            "technique": req.technique,
            "complications": req.complications,
            "blood_loss": req.blood_loss,
            "ot_start": req.ot_start,
            "ot_end": req.ot_end,
        }
    )
    return note


@router.post("/admissions/{admission_id}/post-op", status_code=status.HTTP_201_CREATED)
async def create_post_op_note(
    admission_id: str,
    req: PostOpNoteCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    await _require_admission_access(user, admission_id)
    note = await prisma.post_op_notes.create(
        data={
            "admission_id": admission_id,
            "day_number": req.day_number,
            "condition": req.condition,
            "vitals_json": Json(req.vitals_json),
            "wound_status": req.wound_status,
            "pain_score": req.pain_score,
            "diet": req.diet,
            "medications_json": Json(req.medications_json) if req.medications_json is not None else None,
        }
    )
    return note


@router.post("/admissions/{admission_id}/ward-round", status_code=status.HTTP_201_CREATED)
async def create_ward_round_note(
    admission_id: str,
    req: WardRoundNoteCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    await _require_admission_access(user, admission_id)
    note = await prisma.ward_round_notes.create(
        data={
            "admission_id": admission_id,
            "nurse_id": user.nurse_id if user.is_nurse() else None,
            "subjective": req.subjective,
            "objective": req.objective,
            "assessment": req.assessment,
            "plan": req.plan,
            "ready_for_discharge": req.ready_for_discharge,
        }
    )
    return note


@router.post("/admissions/{admission_id}/discharge", status_code=status.HTTP_201_CREATED)
async def create_discharge_summary(
    admission_id: str,
    req: DischargeSummaryCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    await _require_admission_access(user, admission_id)

    summary = await prisma.discharge_summaries.create(
        data={
            "admission_id": admission_id,
            "condition_at_discharge": req.condition_at_discharge,
            "procedure_summary": req.procedure_summary,
            "discharge_medications_json": Json(req.discharge_medications_json),
            "wound_care": req.wound_care,
            "activity_restrictions": req.activity_restrictions,
            "diet_instructions": req.diet_instructions,
            "follow_up_date": req.follow_up_date,
            "red_flags": req.red_flags,
        }
    )

    await prisma.ipd_admissions.update(
        where={"id": admission_id},
        data={
            "status": "discharged",
            "discharge_at": datetime.now(timezone.utc),
        },
    )

    return summary
