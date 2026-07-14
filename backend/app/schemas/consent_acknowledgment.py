from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class ConsentAcknowledgmentRequest(BaseModel):
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$", description="Indian phone number in +91 format")
    device_info: Optional[str] = None
    client_ip: Optional[str] = None


class ConsentAcknowledgmentResponse(BaseModel):
    acknowledged: bool
    acknowledged_at: datetime


class LatestConsentResponse(BaseModel):
    version: str
    title: str
    content: str
    requires_acknowledgment: bool
