from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from app.core.database import prisma
from app.core.auth_deps import CurrentUser, get_current_staff, get_current_superadmin

router = APIRouter(prefix="/admin", tags=["Admin"])


class CreateAdminRequest(BaseModel):
    name: str = Field(..., min_length=2)
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    role: str = Field(..., pattern=r"^(admin|superadmin)$")


class UpdateDoctorStatusRequest(BaseModel):
    is_active: bool


@router.get("/doctors")
async def list_doctors(user: CurrentUser = Depends(get_current_staff)):
    doctors = await prisma.doctors.find_many(
        order={"created_at": "desc"},
    )
    return doctors


@router.get("/doctors/pending")
async def list_pending_doctors(user: CurrentUser = Depends(get_current_staff)):
    doctors = await prisma.doctors.find_many(
        where={"is_active": False},
        order={"created_at": "desc"},
    )
    return doctors


@router.patch("/doctors/{doctor_id}/status")
async def update_doctor_status(
    doctor_id: str,
    req: UpdateDoctorStatusRequest,
    user: CurrentUser = Depends(get_current_staff),
):
    doctor = await prisma.doctors.find_first(where={"id": doctor_id})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    updated = await prisma.doctors.update(
        where={"id": doctor_id},
        data={"is_active": req.is_active},
    )
    return updated


@router.post("/admins", status_code=status.HTTP_201_CREATED)
async def create_admin(
    req: CreateAdminRequest,
    user: CurrentUser = Depends(get_current_superadmin),
):
    existing = await prisma.admins.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered")

    admin = await prisma.admins.create(
        data={
            "name": req.name,
            "phone": req.phone,
            "role": req.role,
        }
    )
    return admin


@router.get("/admins")
async def list_admins(user: CurrentUser = Depends(get_current_superadmin)):
    admins = await prisma.admins.find_many(order={"created_at": "desc"})
    return admins
