"""OT (operative) note templates and OT notes.

Replaces the old pre-op/intra-op/post-op note trio with one unified operative
record. Templates come from two sources, exactly like the existing
surgical_templates / consent_content_templates split:
  - global templates (is_global=True, doctor_id=None), seeded from the
    client-supplied operative-note corpus (see scripts/seed_ot_note_templates.py)
  - a doctor's own templates (is_global=False, doctor_id=<them>), which they
    create/edit/delete themselves, mirroring surgical_templates.py exactly.
"""

from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status

from prisma import Json

from app.core.database import prisma
from app.core.auth_deps import (
    CurrentUser,
    get_current_surgeon,
    get_current_nurse_or_surgeon,
    resolve_doctor_id,
)
from app.routers.ipd import _require_admission_access
from app.schemas.ot_notes import (
    OtNoteCreate,
    OtNoteMediaAdd,
    OtNoteTemplateCreate,
    OtNoteTemplateUpdate,
    OtNoteUpdate,
)

router = APIRouter(tags=["OT Notes"])


# ── Templates ────────────────────────────────────────────────────────────


@router.get("/ot-note-templates")
async def list_ot_note_templates(
    procedure: str | None = None,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    """Templates visible to the caller: the global (corpus-seeded) library
    plus the calling doctor's own. Optionally filtered by procedure name."""
    doctor_id = await resolve_doctor_id(user)
    where: dict = {
        "is_active": True,
        "OR": [{"is_global": True}, {"doctor_id": doctor_id}],
    }
    if procedure:
        where["procedure"] = procedure

    templates = await prisma.ot_note_templates.find_many(
        where=where,
        order={"name": "asc"},
    )
    return templates


@router.post("/ot-note-templates", status_code=status.HTTP_201_CREATED)
async def create_ot_note_template(
    req: OtNoteTemplateCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    template = await prisma.ot_note_templates.create(
        data={
            "doctor_id": doctor_id,
            "is_global": False,
            **req.model_dump(),
        }
    )
    return template


@router.get("/ot-note-templates/{template_id}")
async def get_ot_note_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    template = await prisma.ot_note_templates.find_first(where={"id": template_id})
    if not template:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    if not template.is_global:
        doctor_id = await resolve_doctor_id(user)
        if template.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    return template


@router.patch("/ot-note-templates/{template_id}")
async def update_ot_note_template(
    template_id: str,
    req: OtNoteTemplateUpdate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    existing = await prisma.ot_note_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    if existing.is_global or existing.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    update_data = req.model_dump(exclude_unset=True)
    updated = await prisma.ot_note_templates.update(where={"id": template_id}, data=update_data)
    return updated


@router.delete("/ot-note-templates/{template_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_ot_note_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_surgeon),
):
    doctor_id = await resolve_doctor_id(user)
    existing = await prisma.ot_note_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    if existing.is_global or existing.doctor_id != doctor_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    await prisma.ot_note_templates.delete(where={"id": template_id})
    return None


# ── Notes ────────────────────────────────────────────────────────────────


@router.post("/ipd/admissions/{admission_id}/ot-notes", status_code=status.HTTP_201_CREATED)
async def create_ot_note(
    admission_id: str,
    req: OtNoteCreate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    admission = await _require_admission_access(user, admission_id)
    doctor_id = await resolve_doctor_id(user)

    if req.template_id:
        template = await prisma.ot_note_templates.find_first(where={"id": req.template_id})
        if not template:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")

    note = await prisma.ot_notes.create(
        data={
            "admission_id": admission_id,
            "doctor_id": doctor_id,
            "template_id": req.template_id,
            "procedure": req.procedure,
            "approach": req.approach,
            "anaesthesia": req.anaesthesia,
            "preop_diagnosis": req.preop_diagnosis,
            "postop_diagnosis": req.postop_diagnosis,
            "operation_performed": req.operation_performed,
            "position_preparation": req.position_preparation,
            "incision_approach": req.incision_approach,
            "findings": req.findings,
            "procedure_steps": req.procedure_steps,
            "closure": req.closure,
            "specimen": req.specimen,
            "implants": req.implants,
            "drains": req.drains,
            "estimated_blood_loss": req.estimated_blood_loss,
            "counts": req.counts,
            "complications": req.complications,
            "postop_plan": req.postop_plan,
            "team_members": Json([m.model_dump() for m in req.team_members]),
            "ot_start": req.ot_start,
            "ot_end": req.ot_end,
            "image_urls": req.image_urls,
            "video_urls": req.video_urls,
            "status": req.normalized_status,
        }
    )
    return note


@router.get("/ipd/admissions/{admission_id}/ot-notes/{note_id}")
async def get_ot_note(
    admission_id: str,
    note_id: str,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    await _require_admission_access(user, admission_id)
    note = await prisma.ot_notes.find_first(where={"id": note_id, "admission_id": admission_id})
    if not note:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="OT note not found")
    return note


@router.patch("/ipd/admissions/{admission_id}/ot-notes/{note_id}")
async def update_ot_note(
    admission_id: str,
    note_id: str,
    req: OtNoteUpdate,
    user: CurrentUser = Depends(get_current_surgeon),
):
    await _require_admission_access(user, admission_id)
    existing = await prisma.ot_notes.find_first(where={"id": note_id, "admission_id": admission_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="OT note not found")

    update_data = req.model_dump(exclude_unset=True, exclude={"team_members"})
    if req.team_members is not None:
        update_data["team_members"] = Json([m.model_dump() for m in req.team_members])

    updated = await prisma.ot_notes.update(where={"id": note_id}, data=update_data)
    return updated


@router.post("/ipd/admissions/{admission_id}/ot-notes/{note_id}/media", status_code=status.HTTP_201_CREATED)
async def add_ot_note_media(
    admission_id: str,
    note_id: str,
    req: OtNoteMediaAdd,
    user: CurrentUser = Depends(get_current_nurse_or_surgeon),
):
    await _require_admission_access(user, admission_id)
    note = await prisma.ot_notes.find_first(where={"id": note_id, "admission_id": admission_id})
    if not note:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="OT note not found")

    existing = note.media_items if note.media_items is not None else []
    updated_items = list(existing) + [
        {
            "url": req.url,
            "media_type": req.media_type,
            "label": req.label,
            "description": req.description,
            "uploaded_at": datetime.now(timezone.utc).isoformat(),
        }
    ]
    url_list_field = "image_urls" if req.media_type == "image" else "video_urls"
    updated_urls = list(getattr(note, url_list_field)) + [req.url]

    updated = await prisma.ot_notes.update(
        where={"id": note_id},
        data={"media_items": Json(updated_items), url_list_field: updated_urls},
    )
    return updated
