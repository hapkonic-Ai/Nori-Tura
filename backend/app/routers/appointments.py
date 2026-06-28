from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.encoders import jsonable_encoder
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

router = APIRouter(prefix="/appointments", tags=["Appointments"])


class AppointmentCreate(BaseModel):
    patient_id: str
    slot_datetime: datetime
    visit_type: str


class AppointmentStatusUpdate(BaseModel):
    status: str


async def _require_patient_access(user: CurrentUser, patient_id: str) -> str:
    """Validate access and return the doctor_id the patient belongs to."""
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
        return doctor_id

    return patient.doctor_id


@router.get("")
async def list_appointments(user: CurrentUser = Depends(get_current_user)):
    if user.is_parent():
        patients = await prisma.patients.find_many(
            where={"parent_phone": user.phone},
        )
        patient_ids = [p.id for p in patients]
        appointments = await prisma.appointments.find_many(
            where={"patient_id": {"in": patient_ids}},
            order={"slot_datetime": "desc"},
            include={"patient": True},
        )
    else:
        doctor_id = await resolve_doctor_id(user)
        appointments = await prisma.appointments.find_many(
            where={"doctor_id": doctor_id},
            order={"slot_datetime": "desc"},
            include={"patient": True},
        )
    return jsonable_encoder(appointments)


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_appointment(
    req: AppointmentCreate,
    user: CurrentUser = Depends(get_current_user),
):
    if user.is_parent():
        booked_by = "parent"
    elif user.role in ["surgeon", "nurse"]:
        booked_by = "nurse" if user.is_nurse() else "surgeon"
    else:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    doctor_id = await _require_patient_access(user, req.patient_id)

    appointment = await prisma.appointments.create(
        data={
            "patient_id": req.patient_id,
            "doctor_id": doctor_id,
            "slot_datetime": req.slot_datetime,
            "visit_type": req.visit_type,
            "booked_by": booked_by,
        },
        include={"patient": True},
    )
    return jsonable_encoder(appointment)


@router.patch("/{appointment_id}/status")
async def update_appointment_status(
    appointment_id: str,
    req: AppointmentStatusUpdate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    appointment = await prisma.appointments.find_first(
        where={"id": appointment_id},
    )
    if not appointment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Appointment not found")
    if appointment.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    updated = await prisma.appointments.update(
        where={"id": appointment_id},
        data={"status": req.status},
    )
    return jsonable_encoder(updated)
