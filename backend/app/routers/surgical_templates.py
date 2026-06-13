from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from app.core.database import prisma
from app.core.auth_deps import CurrentUser, get_current_surgeon, resolve_doctor_id

router = APIRouter(prefix="/surgical-templates", tags=["Surgical Templates"])


class SurgicalTemplateCreate(BaseModel):
    name: str = Field(..., min_length=1)
    procedure: str = Field(..., min_length=1)
    approach: Optional[str] = None
    anaesthesia: List[str] = []
    investigations: List[str] = []
    risk_level: Optional[str] = None
    technique: Optional[str] = None
    special_instructions: Optional[str] = None


class SurgicalTemplateUpdate(BaseModel):
    name: Optional[str] = None
    procedure: Optional[str] = None
    approach: Optional[str] = None
    anaesthesia: Optional[List[str]] = None
    investigations: Optional[List[str]] = None
    risk_level: Optional[str] = None
    technique: Optional[str] = None
    special_instructions: Optional[str] = None


@router.get("")
async def list_templates(user: CurrentUser = Depends(get_current_surgeon)):
    doctor_id = await resolve_doctor_id(user)
    templates = await prisma.surgical_templates.find_many(
        where={"doctor_id": doctor_id},
        order={"created_at": "desc"},
    )
    return templates


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_template(
    req: SurgicalTemplateCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    template = await prisma.surgical_templates.create(
        data={
            "doctor_id": doctor_id,
            "name": req.name,
            "procedure": req.procedure,
            "approach": req.approach,
            "anaesthesia": req.anaesthesia,
            "investigations": req.investigations,
            "risk_level": req.risk_level,
            "technique": req.technique,
            "special_instructions": req.special_instructions,
        }
    )
    return template


@router.get("/{template_id}")
async def get_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    template = await prisma.surgical_templates.find_first(where={"id": template_id})
    if not template:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    if template.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    return template


@router.patch("/{template_id}")
async def update_template(
    template_id: str,
    req: SurgicalTemplateUpdate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    existing = await prisma.surgical_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    if existing.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    update_data = req.model_dump(exclude_unset=True)
    updated = await prisma.surgical_templates.update(
        where={"id": template_id},
        data=update_data,
    )
    return updated


@router.delete("/{template_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    existing = await prisma.surgical_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    if existing.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    await prisma.surgical_templates.delete(where={"id": template_id})
    return None
