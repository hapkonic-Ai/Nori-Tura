from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from datetime import date, datetime, time, timedelta
from typing import List, Optional

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)

router = APIRouter(prefix="/schedule", tags=["Schedule"])

OT_VISIT_TYPES = {"surgery", "ot", "procedure"}
OPD_VISIT_TYPES = {"opd", "consult", "follow-up", "follow_up"}


class OtBookingRequest(BaseModel):
    patient_id: str
    date: date
    time: str = Field(..., pattern=r"^\d{2}:\d{2}$")
    procedure: str
    urgency: str = "routine"


class OpdBookingRequest(BaseModel):
    patient_id: str
    date: date
    time: str = Field(..., pattern=r"^\d{2}:\d{2}$")
    visit_type: str = "opd"


def _parse_slot_datetime(slot_date: date, slot_time: str) -> datetime:
    try:
        hour, minute = map(int, slot_time.split(":"))
        return datetime.combine(slot_date, time(hour=hour, minute=minute))
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid time format: {slot_time}",
        ) from exc


async def _require_patient_access(user: CurrentUser, patient_id: str) -> str:
    patient = await prisma.patients.find_first(where={"id": patient_id})
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    doctor_id = await resolve_doctor_id(user)
    if patient.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    return doctor_id


async def _list_slots(user: CurrentUser, target_date: date, allowed_types: set) -> List[dict]:
    doctor_id = await resolve_doctor_id(user)
    start = datetime.combine(target_date, time.min)
    end = datetime.combine(target_date, time.max)

    appointments = await prisma.appointments.find_many(
        where={
            "doctor_id": doctor_id,
            "slot_datetime": {"gte": start, "lte": end},
            "visit_type": {"in": list(allowed_types)},
        },
        order={"slot_datetime": "asc"},
        include={"patient": True},
    )
    return appointments


@router.get("/ot")
async def list_ot_slots(
    target_date: date,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    return await _list_slots(user, target_date, OT_VISIT_TYPES)


@router.get("/opd")
async def list_opd_slots(
    target_date: date,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    return await _list_slots(user, target_date, OPD_VISIT_TYPES)


@router.post("/ot", status_code=status.HTTP_201_CREATED)
async def book_ot_slot(
    req: OtBookingRequest,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    doctor_id = await _require_patient_access(user, req.patient_id)
    slot_datetime = _parse_slot_datetime(req.date, req.time)

    appointment = await prisma.appointments.create(
        data={
            "patient_id": req.patient_id,
            "doctor_id": doctor_id,
            "slot_datetime": slot_datetime,
            "visit_type": "surgery",
            "procedure": req.procedure,
            "urgency": req.urgency,
            "status": "scheduled",
            "booked_by": "nurse" if user.is_nurse() else "surgeon",
        },
        include={"patient": True},
    )
    return appointment


@router.post("/opd", status_code=status.HTTP_201_CREATED)
async def book_opd_slot(
    req: OpdBookingRequest,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    doctor_id = await _require_patient_access(user, req.patient_id)
    slot_datetime = _parse_slot_datetime(req.date, req.time)

    appointment = await prisma.appointments.create(
        data={
            "patient_id": req.patient_id,
            "doctor_id": doctor_id,
            "slot_datetime": slot_datetime,
            "visit_type": req.visit_type,
            "status": "scheduled",
            "booked_by": "nurse" if user.is_nurse() else "surgeon",
        },
        include={"patient": True},
    )
    return appointment
