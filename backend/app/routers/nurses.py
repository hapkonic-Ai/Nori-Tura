from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import Optional


from app.core.database import prisma
from app.core.auth_deps import get_current_surgeon, CurrentUser, resolve_doctor_id

router = APIRouter(prefix="/nurses", tags=["Nurses"])


class NurseCreate(BaseModel):
    name: str
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    hospital: Optional[str] = None


@router.get("")
async def list_nurses(user: CurrentUser = Depends(get_current_surgeon)):
    doctor_id = await resolve_doctor_id(user)
    nurses = await prisma.nurses.find_many(
        where={"doctor_id": doctor_id},
        order={"created_at": "desc"},
        include={"hospital": True},
    )
    return nurses


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_nurse(
    req: NurseCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)

    existing = await prisma.nurses.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone number already registered")

    doctor = await prisma.doctors.find_unique(
        where={"id": doctor_id},
        include={"hospital": True},
    )
    hospital_id = doctor.hospital_id if doctor else None

    nurse = await prisma.nurses.create(
        data={
            "doctor_id": doctor_id,
            "name": req.name,
            "phone": req.phone,
            "hospital_id": hospital_id,
        },
        include={"hospital": True},
    )
    return nurse


@router.patch("/{nurse_id}/deactivate")
async def deactivate_nurse(
    nurse_id: str,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    nurse = await prisma.nurses.find_first(where={"id": nurse_id})
    if not nurse:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nurse not found")
    if nurse.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    updated = await prisma.nurses.update(
        where={"id": nurse_id},
        data={"is_active": False},
    )
    return updated
