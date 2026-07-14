from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.encoders import jsonable_encoder
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime, timedelta, timezone
import uuid

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_user,
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)
from app.schemas.appointments_enhanced import (
    AppointmentRequestCreate,
    AppointmentConfirmRequest,
    AppointmentRequestResponse,
    AppointmentConfirmResponse,
    AvailableSlot,
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


def _generate_available_slots(doctor_id: str, days: int = 30) -> List[AvailableSlot]:
    """Generate mock available appointment slots for a surgeon."""
    # For MVP, return 5 slots per day for the next N days starting tomorrow
    slots = []
    now = datetime.now(timezone.utc)
    for day_offset in range(1, days + 1):
        slot_date = now + timedelta(days=day_offset)
        # Generate 5 slots: 9am, 11am, 2pm, 3pm, 4pm
        for hour in [9, 11, 14, 15, 16]:
            slot_datetime = slot_date.replace(hour=hour, minute=0, second=0, microsecond=0)
            slots.append(
                AvailableSlot(
                    slot_datetime=slot_datetime,
                    slot_id=str(uuid.uuid4()),
                    surgeon_name="Dr. Surgeon",
                    is_available=True
                )
            )
    return slots[:30]  # Limit to first 30 slots


@router.post("/request", response_model=AppointmentRequestResponse, status_code=status.HTTP_201_CREATED)
async def request_appointment(
    req: AppointmentRequestCreate,
    user: CurrentUser = Depends(get_current_user)
):
    """Parent requests an appointment."""
    if not user.is_parent():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Only parents can request appointments")

    # Verify doctor exists
    doctor = await prisma.doctors.find_first(where={"id": req.doctor_id, "is_active": True})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    # Create appointment request (without patient_id yet)
    appointment = await prisma.appointments.create(
        data={
            "patient_id": str(uuid.uuid4()),  # Placeholder, will be updated on confirm
            "doctor_id": req.doctor_id,
            "slot_datetime": datetime.now(timezone.utc),  # Placeholder
            "visit_type": "consultation",
            "appointment_type": "requested",
            "requested_at": datetime.now(timezone.utc),
            "requested_by_role": "patient_parent",
            "urgency": req.urgency,
            "notes": req.reason,
            "status": "requested"
        }
    )

    # Generate available slots
    available_slots = _generate_available_slots(req.doctor_id)

    return {
        "id": appointment.id,
        "doctor_id": appointment.doctor_id,
        "appointment_type": "requested",
        "requested_at": appointment.requested_at,
        "requested_by_role": "patient_parent",
        "urgency": req.urgency,
        "status": "requested",
        "notes": req.reason,
        "available_slots": available_slots
    }


@router.get("/available-slots")
async def get_available_slots(
    doctor_id: str,
    days: int = 30,
    user: CurrentUser = Depends(get_current_user)
):
    """Get available appointment slots for a doctor."""
    # Verify doctor exists
    doctor = await prisma.doctors.find_first(where={"id": doctor_id, "is_active": True})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    # Generate mock slots for MVP
    slots = _generate_available_slots(doctor_id, min(days, 30))
    return {"slots": slots}


@router.post("/{appointment_id}/confirm", response_model=AppointmentConfirmResponse, status_code=status.HTTP_200_OK)
async def confirm_appointment(
    appointment_id: str,
    req: AppointmentConfirmRequest,
    user: CurrentUser = Depends(get_current_user)
):
    """Confirm appointment from available slots."""
    if not user.is_parent():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Only parents can confirm appointments")

    # Get appointment
    appointment = await prisma.appointments.find_first(where={"id": appointment_id})
    if not appointment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Appointment not found")

    # Verify parent
    if appointment.appointment_type != "requested":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Appointment already confirmed")

    # Auto-create patient if needed
    patient_id = appointment.patient_id
    if req.auto_create_patient and req.patient_data:
        patient_data = req.patient_data
        patient = await prisma.patients.create(
            data={
                "doctor_id": appointment.doctor_id,
                "name": patient_data.get("name", "New Patient"),
                "age": patient_data.get("age", 0),
                "gender": patient_data.get("gender", "Not specified"),
                "parent_name": patient_data.get("parent_name", user.phone),
                "parent_phone": user.phone
            }
        )
        patient_id = patient.id

    # Update appointment
    confirmed = await prisma.appointments.update(
        where={"id": appointment_id},
        data={
            "patient_id": patient_id,
            "slot_datetime": req.slot_datetime,
            "is_confirmed": True,
            "confirmed_at": datetime.now(timezone.utc),
            "appointment_type": "direct",
            "status": "confirmed",
            "auto_created_patient": req.auto_create_patient
        }
    )

    return {
        "id": confirmed.id,
        "patient_id": confirmed.patient_id,
        "doctor_id": confirmed.doctor_id,
        "slot_datetime": confirmed.slot_datetime,
        "is_confirmed": confirmed.is_confirmed,
        "confirmed_at": confirmed.confirmed_at,
        "auto_created_patient": confirmed.auto_created_patient,
        "status": confirmed.status
    }


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
