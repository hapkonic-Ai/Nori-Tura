from fastapi import APIRouter, Depends, HTTPException, status
from typing import List

from app.core.database import prisma
from app.core.auth_deps import get_current_user, get_current_nurse_or_surgeon, CurrentUser, resolve_doctor_id
from app.schemas.medical_records import (
    MedicalRecordCreate,
    MedicalRecordImageCreate,
    MedicalRecordResponse,
    MedicalRecordImageResponse,
    MedicalRecordDetailResponse,
    VALID_CATEGORIES,
)

router = APIRouter(prefix="/medical-records", tags=["Medical Records"])


@router.post("", status_code=status.HTTP_201_CREATED, response_model=MedicalRecordResponse)
async def create_medical_record(
    req: MedicalRecordCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon)
):
    """Create a container for medical record images."""
    doctor_id = await resolve_doctor_id(user)

    # Validate patient access
    patient = await prisma.patients.find_first(
        where={"id": req.patient_id, "doctor_id": doctor_id}
    )
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    record = await prisma.medical_records.create(
        data={
            "patient_id": req.patient_id,
            "doctor_id": doctor_id,
            "title": req.title,
            "description": req.description,
            "opd_record_id": req.opd_record_id,
            "admission_id": req.admission_id,
        }
    )

    return {
        "id": record.id,
        "patient_id": record.patient_id,
        "title": record.title,
        "description": record.description,
        "image_count": 0,
        "created_at": record.created_at
    }


@router.get("/{patient_id}", response_model=List[MedicalRecordResponse])
async def list_medical_records(
    patient_id: str,
    user: CurrentUser = Depends(get_current_user)
):
    """List all medical records for a patient."""
    if user.is_parent():
        patient = await prisma.patients.find_first(
            where={"id": patient_id, "parent_phone": user.phone}
        )
    else:
        doctor_id = await resolve_doctor_id(user)
        patient = await prisma.patients.find_first(
            where={"id": patient_id, "doctor_id": doctor_id}
        )
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    records = await prisma.medical_records.find_many(
        where={"patient_id": patient_id},
        include={"images": True},
        order={"created_at": "desc"}
    )

    return [
        {
            "id": r.id,
            "patient_id": r.patient_id,
            "title": r.title,
            "description": r.description,
            "image_count": len(r.images),
            "created_at": r.created_at
        }
        for r in records
    ]


@router.get("/{record_id}/detail", response_model=MedicalRecordDetailResponse)
async def get_medical_record_detail(
    record_id: str,
    user: CurrentUser = Depends(get_current_user)
):
    """Get full medical record with all images."""
    record = await prisma.medical_records.find_first(
        where={"id": record_id},
        include={"images": {"order_by": {"uploaded_at": "desc"}}}
    )

    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Medical record not found")

    if user.is_parent():
        patient = await prisma.patients.find_first(
            where={"id": record.patient_id, "parent_phone": user.phone}
        )
        if not patient:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if record.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return {
        "id": record.id,
        "patient_id": record.patient_id,
        "title": record.title,
        "description": record.description,
        "image_count": len(record.images),
        "created_at": record.created_at,
        "images": [
            {
                "id": img.id,
                "image_url": img.image_url,
                "category": img.category,
                "label": img.label,
                "description": img.description,
                "uploaded_by_role": img.uploaded_by_role,
                "uploaded_at": img.uploaded_at
            }
            for img in record.images
        ]
    }


@router.post("/{record_id}/images", status_code=status.HTTP_201_CREATED, response_model=MedicalRecordImageResponse)
async def add_image_to_record(
    record_id: str,
    req: MedicalRecordImageCreate,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon)
):
    """Add image with metadata to medical record."""
    # Validate category
    if req.category not in VALID_CATEGORIES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Category must be one of: {', '.join(VALID_CATEGORIES)}"
        )

    # Validate record access
    record = await prisma.medical_records.find_first(
        where={"id": record_id}
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Medical record not found")

    doctor_id = await resolve_doctor_id(user)
    if record.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    # Add image
    image = await prisma.medical_record_images.create(
        data={
            "medical_record_id": record_id,
            "image_url": req.image_url,
            "category": req.category,
            "label": req.label,
            "description": req.description,
            "uploaded_by_role": req.uploaded_by_role,
        }
    )

    # Flag the linked OPD record if applicable
    if record.opd_record_id:
        await prisma.opd_records.update(
            where={"id": record.opd_record_id},
            data={"has_medical_records": True}
        )

    return {
        "id": image.id,
        "image_url": image.image_url,
        "category": image.category,
        "label": image.label,
        "description": image.description,
        "uploaded_by_role": image.uploaded_by_role,
        "uploaded_at": image.uploaded_at
    }


@router.get("/{record_id}/images", response_model=List[MedicalRecordImageResponse])
async def get_record_images(
    record_id: str,
    user: CurrentUser = Depends(get_current_user)
):
    """Get all images for a medical record."""
    record = await prisma.medical_records.find_first(
        where={"id": record_id},
        include={"images": {"order_by": {"uploaded_at": "desc"}}}
    )
    if not record:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Medical record not found")

    if user.is_parent():
        patient = await prisma.patients.find_first(
            where={"id": record.patient_id, "parent_phone": user.phone}
        )
        if not patient:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if record.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    return [
        {
            "id": img.id,
            "image_url": img.image_url,
            "category": img.category,
            "label": img.label,
            "description": img.description,
            "uploaded_by_role": img.uploaded_by_role,
            "uploaded_at": img.uploaded_at
        }
        for img in record.images
    ]
