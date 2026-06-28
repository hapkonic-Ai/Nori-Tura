from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional

from app.core.database import prisma
from app.core.auth_deps import get_current_user, CurrentUser, resolve_doctor_id

router = APIRouter(prefix="/documents", tags=["Documents"])


class DocumentCreate(BaseModel):
    patient_id: str
    name: str
    url: str
    type: str
    category: Optional[str] = None
    uploaded_by_role: Optional[str] = None
    recorded_at: Optional[str] = None


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


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_document(
    req: DocumentCreate,
    user: CurrentUser = Depends(get_current_user),
):
    patient = await _require_patient_access(user, req.patient_id)
    doctor_id = patient.doctor_id

    role = req.uploaded_by_role or ("parent" if user.is_parent() else "surgeon")

    hospital_id = patient.hospital_id
    hospital_name = patient.hospital_name
    hospital_logo_url = patient.hospital_logo_url

    document = await prisma.documents.create(
        data={
            "patient_id": req.patient_id,
            "doctor_id": doctor_id,
            "hospital_id": hospital_id,
            "name": req.name,
            "url": req.url,
            "type": req.type,
            "category": req.category,
            "uploaded_by_role": role,
            "recorded_at": req.recorded_at,
            "hospital_name": hospital_name,
            "hospital_logo_url": hospital_logo_url,
        }
    )
    return document


@router.get("/patients/{patient_id}")
async def list_documents(
    patient_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    await _require_patient_access(user, patient_id)
    documents = await prisma.documents.find_many(
        where={"patient_id": patient_id},
        order={"uploaded_at": "desc"},
        include={"hospital": True},
    )
    return documents
