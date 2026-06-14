from fastapi import APIRouter, Depends, HTTPException, status, Query
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timedelta, time

from app.core.database import prisma
from app.core.auth_deps import get_current_user, CurrentUser

router = APIRouter(prefix="/doctors", tags=["Doctors"])


class DoctorResponse(BaseModel):
    id: str
    name: str
    hospital: Optional[str] = None
    specialty: Optional[str] = None
    is_active: bool = True


class AvailableSlotsResponse(BaseModel):
    doctor_id: str
    date: str
    slots: List[str]


@router.get("", response_model=List[DoctorResponse])
async def list_doctors(
    is_active: Optional[bool] = True,
    user: CurrentUser = Depends(get_current_user),
):
    """List doctors. Defaults to active doctors only."""
    where = {}
    if is_active is not None:
        where["is_active"] = is_active

    doctors = await prisma.doctors.find_many(where=where)
    return [
        DoctorResponse(
            id=d.id,
            name=d.name,
            hospital=d.hospital,
            specialty=d.specialty,
            is_active=d.is_active,
        )
        for d in doctors
    ]


@router.get("/me/stats")
async def get_doctor_stats(user: CurrentUser = Depends(get_current_user)):
    """Return practice stats for the authenticated doctor."""
    if not user.is_surgeon() and not user.is_admin() and not user.is_superadmin():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only doctors can view these stats",
        )

    doctor_id = user.doctor_id
    if not doctor_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Doctor ID not found in token",
        )

    total_patients = await prisma.patients.count(where={"doctor_id": doctor_id})
    total_surgeries = await prisma.ipd_admissions.count(
        where={"doctor_id": doctor_id}
    )

    # Placeholder success rate; in production compute from outcomes.
    success_rate = 0.984

    return {
        "patients": total_patients,
        "surgeries": total_surgeries,
        "success_rate": success_rate,
    }


@router.get("/available-slots", response_model=AvailableSlotsResponse)
async def get_available_slots(
    doctor_id: str = Query(..., description="Doctor UUID"),
    date: str = Query(..., description="Date in YYYY-MM-DD format"),
    user: CurrentUser = Depends(get_current_user),
):
    """Return available 30-minute appointment slots for a doctor on a given date."""
    try:
        target_date = datetime.strptime(date, "%Y-%m-%d").date()
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid date format. Use YYYY-MM-DD.",
        )

    doctor = await prisma.doctors.find_first(where={"id": doctor_id})
    if not doctor:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found"
        )

    start_dt = datetime.combine(target_date, time(8, 0))
    end_dt = datetime.combine(target_date, time(17, 0))

    existing = await prisma.appointments.find_many(
        where={
            "doctor_id": doctor_id,
            "slot_datetime": {"gte": start_dt, "lt": end_dt},
        }
    )
    booked_slots = {a.slot_datetime for a in existing}

    slots = []
    current = start_dt
    while current < end_dt:
        if current not in booked_slots:
            slots.append(current.isoformat())
        current += timedelta(minutes=30)

    return AvailableSlotsResponse(doctor_id=doctor_id, date=date, slots=slots)
