from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field


from app.core.database import prisma
from app.core.auth_deps import get_current_surgeon, CurrentUser, resolve_doctor_id

router = APIRouter(prefix="/nurses", tags=["Nurses"])


class NurseCreate(BaseModel):
    name: str
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    hospital: str


@router.get("")
async def list_nurses(user: CurrentUser = Depends(get_current_surgeon)):
    doctor_id = await resolve_doctor_id(user)
    nurses = await prisma.nurses.find_many(
        where={"doctor_id": doctor_id},
        order={"created_at": "desc"},
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

    nurse = await prisma.nurses.create(
        data={
            "doctor_id": doctor_id,
            "name": req.name,
            "phone": req.phone,
            "hospital": req.hospital,
        }
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
