from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime


class AppointmentRequestCreate(BaseModel):
    doctor_id: str
    reason: Optional[str] = None
    urgency: str = Field(default="routine", pattern="^(routine|urgent|emergency)$")
    preferred_date: Optional[str] = None


class AvailableSlot(BaseModel):
    slot_datetime: datetime
    slot_id: str
    surgeon_name: str
    is_available: bool


class AppointmentConfirmRequest(BaseModel):
    slot_datetime: datetime
    auto_create_patient: bool = False
    patient_data: Optional[dict] = None


class AppointmentRequestResponse(BaseModel):
    id: str
    doctor_id: str
    patient_id: Optional[str] = None
    appointment_type: str
    requested_at: datetime
    requested_by_role: str
    urgency: str
    status: str
    notes: Optional[str] = None
    available_slots: Optional[List[AvailableSlot]] = None

    class Config:
        from_attributes = True


class AppointmentConfirmResponse(BaseModel):
    id: str
    patient_id: str
    doctor_id: str
    slot_datetime: datetime
    is_confirmed: bool
    confirmed_at: datetime
    auto_created_patient: bool
    status: str

    class Config:
        from_attributes = True
