"""Pydantic schemas for OT (operative) notes and OT note templates.

OT notes replace the old pre-op/intra-op/post-op note trio with one unified
operative record, matching how real operative notes are structured (see the
client-supplied Spitz & Coran operative-note corpus): pre-op diagnosis through
post-op plan in a single document, with ordered operative steps rather than a
single free-text field.
"""

from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class OtNoteTemplateCreate(BaseModel):
    """Request body for a doctor creating their own OT note template."""

    name: str = Field(..., min_length=1)
    procedure: str = Field(..., min_length=1)
    approach: Optional[str] = None

    anaesthesia: Optional[str] = None
    preop_diagnosis: Optional[str] = None
    postop_diagnosis: Optional[str] = None
    operation_performed: Optional[str] = None
    position_preparation: Optional[str] = None
    incision_approach: Optional[str] = None
    procedure_steps: List[str] = []
    closure: Optional[str] = None
    specimen: Optional[str] = None
    implants: Optional[str] = None
    drains: Optional[str] = None
    estimated_blood_loss: Optional[str] = None
    counts: Optional[str] = None
    standard_complications: Optional[str] = None
    postop_plan: Optional[str] = None


class OtNoteTemplateUpdate(BaseModel):
    name: Optional[str] = None
    procedure: Optional[str] = None
    approach: Optional[str] = None
    anaesthesia: Optional[str] = None
    preop_diagnosis: Optional[str] = None
    postop_diagnosis: Optional[str] = None
    operation_performed: Optional[str] = None
    position_preparation: Optional[str] = None
    incision_approach: Optional[str] = None
    procedure_steps: Optional[List[str]] = None
    closure: Optional[str] = None
    specimen: Optional[str] = None
    implants: Optional[str] = None
    drains: Optional[str] = None
    estimated_blood_loss: Optional[str] = None
    counts: Optional[str] = None
    standard_complications: Optional[str] = None
    postop_plan: Optional[str] = None
    is_active: Optional[bool] = None


class TeamMember(BaseModel):
    role: str
    name: str


class OtNoteCreate(BaseModel):
    """Request body for creating an OT note against an admission."""

    template_id: Optional[str] = None

    procedure: str = Field(..., min_length=1)
    approach: Optional[str] = None
    anaesthesia: Optional[str] = None
    preop_diagnosis: Optional[str] = None
    postop_diagnosis: Optional[str] = None
    operation_performed: Optional[str] = None
    position_preparation: Optional[str] = None
    incision_approach: Optional[str] = None
    findings: Optional[str] = None
    procedure_steps: List[str] = []
    closure: Optional[str] = None
    specimen: Optional[str] = None
    implants: Optional[str] = None
    drains: Optional[str] = None
    estimated_blood_loss: Optional[str] = None
    counts: Optional[str] = None
    complications: Optional[str] = None
    postop_plan: Optional[str] = None

    team_members: List[TeamMember] = []
    ot_start: Optional[datetime] = None
    ot_end: Optional[datetime] = None

    image_urls: List[str] = []
    video_urls: List[str] = []

    status: str = "draft"

    @property
    def normalized_status(self) -> str:
        return self.status if self.status in ("draft", "submitted") else "draft"


class OtNoteUpdate(BaseModel):
    procedure: Optional[str] = None
    approach: Optional[str] = None
    anaesthesia: Optional[str] = None
    preop_diagnosis: Optional[str] = None
    postop_diagnosis: Optional[str] = None
    operation_performed: Optional[str] = None
    position_preparation: Optional[str] = None
    incision_approach: Optional[str] = None
    findings: Optional[str] = None
    procedure_steps: Optional[List[str]] = None
    closure: Optional[str] = None
    specimen: Optional[str] = None
    implants: Optional[str] = None
    drains: Optional[str] = None
    estimated_blood_loss: Optional[str] = None
    counts: Optional[str] = None
    complications: Optional[str] = None
    postop_plan: Optional[str] = None
    team_members: Optional[List[TeamMember]] = None
    ot_start: Optional[datetime] = None
    ot_end: Optional[datetime] = None
    image_urls: Optional[List[str]] = None
    video_urls: Optional[List[str]] = None
    status: Optional[str] = None


class OtNoteMediaAdd(BaseModel):
    """Append one media item to an existing OT note (mirrors intra_op_notes'
    media_items append pattern in ipd.py)."""

    url: str
    media_type: str  # "image" | "video"
    label: Optional[str] = None
    description: Optional[str] = None
