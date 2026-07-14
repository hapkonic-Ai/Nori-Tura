from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime


VALID_CATEGORIES = [
    "X-ray", "MRI", "CT Scan", "Ultrasound", "Prescription",
    "Lab Report", "ECG", "Photo", "Other"
]


class MedicalRecordCreate(BaseModel):
    patient_id: str
    title: str
    description: Optional[str] = None
    opd_record_id: Optional[str] = None
    admission_id: Optional[str] = None


class MedicalRecordImageCreate(BaseModel):
    image_url: str
    category: str = Field(..., description=f"One of: {', '.join(VALID_CATEGORIES)}")
    label: Optional[str] = None
    description: Optional[str] = None
    uploaded_by_role: str = "surgeon"


class MedicalRecordImageResponse(BaseModel):
    id: str
    image_url: str
    category: str
    label: Optional[str]
    description: Optional[str]
    uploaded_by_role: str
    uploaded_at: datetime

    class Config:
        from_attributes = True


class MedicalRecordResponse(BaseModel):
    id: str
    patient_id: str
    title: str
    description: Optional[str]
    image_count: int
    created_at: datetime

    class Config:
        from_attributes = True


class MedicalRecordDetailResponse(MedicalRecordResponse):
    images: List[MedicalRecordImageResponse] = []
