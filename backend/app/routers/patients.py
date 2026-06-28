from fastapi import APIRouter, Depends, HTTPException, status, Query
from pydantic import BaseModel, Field
from typing import Optional

from app.core.database import prisma
from app.core.auth_deps import (
    get_current_user,
    get_current_nurse_or_surgeon,
    CurrentUser,
    resolve_doctor_id,
)

router = APIRouter(prefix="/patients", tags=["Patients"])


class PatientCreate(BaseModel):
    name: str
    age: int = Field(..., ge=0, le=150)
    gender: str
    blood_group: Optional[str] = None
    allergies: Optional[str] = None
    parent_name: str
    parent_phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")


class PatientUpdate(BaseModel):
    name: Optional[str] = None
    age: Optional[int] = Field(None, ge=0, le=150)
    gender: Optional[str] = None
    blood_group: Optional[str] = None
    allergies: Optional[str] = None
    parent_name: Optional[str] = None
    parent_phone: Optional[str] = Field(None, pattern=r"^\+91[0-9]{10}$")


async def _fetch_patient(patient_id: str, user: CurrentUser):
    """Fetch a patient if the current user has access."""
    patient = await prisma.patients.find_first(
        where={"id": patient_id},
        include={
            "opd_records": {"take": 1},
            "ipd_admissions": {
                "order_by": {"admitted_at": "desc"},
                "include": {
                    "pre_op_notes": True,
                    "intra_op_notes": True,
                    "post_op_notes": True,
                    "ward_round_notes": True,
                    "discharge_summaries": True,
                    "consent_forms": {"order_by": {"generated_at": "desc"}},
                },
            },
            "documents": {"order_by": {"uploaded_at": "desc"}},
            "hospital": True,
        },
    )
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


@router.get("")
async def list_patients(
    search: Optional[str] = Query(None),
    diagnosis: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    user: CurrentUser = Depends(get_current_user),
):
    if user.is_parent():
        base_where: dict = {"parent_phone": user.phone}
    else:
        doctor_id = await resolve_doctor_id(user)
        base_where = {"doctor_id": doctor_id}

    conditions = [base_where]

    if search:
        query = search.strip()
        conditions.append({
            "OR": [
                {"name": {"contains": query, "mode": "insensitive"}},
                {"parent_phone": {"contains": query, "mode": "insensitive"}},
            ]
        })

    if diagnosis:
        diag = diagnosis.strip()
        conditions.append({
            "opd_records": {
                "some": {
                    "diagnosis": {"contains": diag, "mode": "insensitive"}
                }
            }
        })

    if status:
        status_val = status.strip().lower()
        if status_val == "outpatient":
            # No active admission (either no admissions or all discharged)
            conditions.append({
                "ipd_admissions": {
                    "none": {"status": {"not": "discharged"}}
                }
            })
        else:
            conditions.append({
                "ipd_admissions": {
                    "some": {"status": {"equals": status_val, "mode": "insensitive"}}
                }
            })

    where = {"AND": conditions} if len(conditions) > 1 else conditions[0]

    patients = await prisma.patients.find_many(
        where=where,
        order={"created_at": "desc"},
        include={
            "hospital": True,
            "ipd_admissions": {
                "where": {"status": {"not": "discharged"}},
                "take": 1,
            },
        },
    )
    return patients


@router.get("/{patient_id}")
async def get_patient(patient_id: str, user: CurrentUser = Depends(get_current_user)):
    return await _fetch_patient(patient_id, user)


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_patient(
    req: PatientCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    doctor_id = await resolve_doctor_id(user)

    existing = await prisma.patients.find_first(
        where={
            "doctor_id": doctor_id,
            "name": {"equals": req.name, "mode": "insensitive"},
            "age": req.age,
            "gender": {"equals": req.gender, "mode": "insensitive"},
            "parent_phone": req.parent_phone,
        }
    )
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"A patient with the same name, age, gender and parent phone already exists (ID: {existing.id}).",
        )

    doctor = await prisma.doctors.find_unique(where={"id": doctor_id})
    hospital_id = doctor.hospital_id if doctor else None

    patient = await prisma.patients.create(
        data={
            "doctor_id": doctor_id,
            "hospital_id": hospital_id,
            "name": req.name,
            "age": req.age,
            "gender": req.gender,
            "blood_group": req.blood_group,
            "allergies": req.allergies,
            "parent_name": req.parent_name,
            "parent_phone": req.parent_phone,
        }
    )
    return patient


@router.put("/{patient_id}")
async def update_patient(
    patient_id: str,
    req: PatientUpdate,
    user: CurrentUser = Depends(get_current_user),
):
    patient = await _fetch_patient(patient_id, user)
    if user.is_parent():
        # Parents cannot update core patient info
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Parents cannot update patient info")

    update_data = req.model_dump(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No fields to update")

    updated = await prisma.patients.update(
        where={"id": patient_id},
        data=update_data,
    )
    return updated
