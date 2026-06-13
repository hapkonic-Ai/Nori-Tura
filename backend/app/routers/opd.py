from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_user,
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)

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
        include={"medications": True, "investigations": True},
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
    data = {
        "patient_id": patient_id,
        "doctor_id": doctor_id,
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
        include={"medications": True, "investigations": True},
    )
    return record


@router.get("/records/{record_id}")
async def get_opd_record(
    record_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    record = await prisma.opd_records.find_first(
        where={"id": record_id},
        include={"medications": True, "investigations": True, "patient": True},
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
