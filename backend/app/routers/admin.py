import io
import re
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile, status
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from prisma import Json

from app.core.database import prisma
from app.core.auth_deps import CurrentUser, get_current_staff, get_current_superadmin
from app.services.consent_service import generate_consent_pdf, generate_signed_consent_pdf
from app.utils.excel_parser import parse_excel_bytes, validate_and_prepare_rows

router = APIRouter(prefix="/admin", tags=["Admin"])


class CreateAdminRequest(BaseModel):
    name: str = Field(..., min_length=2)
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    role: str = Field(..., pattern=r"^(admin|superadmin)$")


class UpdateDoctorStatusRequest(BaseModel):
    is_active: bool


class AdminDoctorCreate(BaseModel):
    name: str = Field(..., min_length=2)
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    specialty: str = Field(..., min_length=1)
    hospital_id: Optional[str] = None
    hospital_name: Optional[str] = None
    is_active: bool = True


class AdminDoctorUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=2)
    phone: Optional[str] = Field(None, pattern=r"^\+91[0-9]{10}$")
    specialty: Optional[str] = Field(None, min_length=1)
    hospital_id: Optional[str] = None
    hospital_name: Optional[str] = None
    is_active: Optional[bool] = None


class AdminNurseCreate(BaseModel):
    name: str = Field(..., min_length=2)
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    doctor_id: str
    hospital_id: Optional[str] = None
    is_active: bool = True


class AdminNurseUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=2)
    phone: Optional[str] = Field(None, pattern=r"^\+91[0-9]{10}$")
    doctor_id: Optional[str] = None
    hospital_id: Optional[str] = None
    is_active: Optional[bool] = None


class AdminPatientCreate(BaseModel):
    name: str = Field(..., min_length=1)
    age: int = Field(..., ge=0, le=150)
    gender: str = Field(..., min_length=1)
    blood_group: Optional[str] = None
    allergies: Optional[str] = None
    parent_name: str = Field(..., min_length=1)
    parent_phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    doctor_id: str
    hospital_id: Optional[str] = None


class ConsentContentTemplateCreate(BaseModel):
    name: str = Field(..., min_length=1)
    procedure: str = Field(..., min_length=1)
    procedure_description: Optional[str] = None
    anesthesia: List[str] = []
    risks: List[str] = []
    benefits: List[str] = []
    alternatives: List[str] = []
    possible_complications: List[str] = []
    material_risks: Optional[str] = None
    post_op_care: Optional[str] = None
    expected_recovery: Optional[str] = None
    statutory_reference: Optional[str] = None
    is_active: bool = True


class ConsentContentTemplateUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1)
    procedure: Optional[str] = Field(None, min_length=1)
    procedure_description: Optional[str] = None
    anesthesia: Optional[List[str]] = None
    risks: Optional[List[str]] = None
    benefits: Optional[List[str]] = None
    alternatives: Optional[List[str]] = None
    possible_complications: Optional[List[str]] = None
    material_risks: Optional[str] = None
    post_op_care: Optional[str] = None
    expected_recovery: Optional[str] = None
    statutory_reference: Optional[str] = None
    is_active: Optional[bool] = None


class ConsentLayoutTemplateCreate(BaseModel):
    name: str = Field(..., min_length=1)
    html: Optional[str] = Field(None, min_length=1)
    blocks_json: Optional[Any] = None
    styles_json: Optional[Any] = None
    is_default: bool = False


class ConsentLayoutTemplateUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1)
    html: Optional[str] = Field(None, min_length=1)
    blocks_json: Optional[Any] = None
    styles_json: Optional[Any] = None
    is_default: Optional[bool] = None


def _phone_error(field: str) -> str:
    return f"{field} must be +91 followed by 10 digits"


async def _resolve_or_create_hospital(
    hospital_id: Optional[str], hospital_name: Optional[str]
) -> Optional[str]:
    """Return hospital_id, creating hospital by name if needed."""
    if hospital_id:
        existing = await prisma.hospitals.find_first(where={"id": hospital_id})
        if existing:
            return existing.id

    if hospital_name and hospital_name.strip():
        name = hospital_name.strip()
        existing = await prisma.hospitals.find_first(
            where={"name": {"equals": name, "mode": "insensitive"}}
        )
        if existing:
            return existing.id
        new_hospital = await prisma.hospitals.create(data={"name": name})
        return new_hospital.id

    return None


@router.get("/doctors")
async def list_doctors(
    search: Optional[str] = Query(None),
    is_active: Optional[bool] = Query(None),
    user: CurrentUser = Depends(get_current_staff),
):
    conditions: List[Dict[str, Any]] = []
    if is_active is not None:
        conditions.append({"is_active": is_active})
    if search:
        query = search.strip()
        conditions.append({
            "OR": [
                {"name": {"contains": query, "mode": "insensitive"}},
                {"phone": {"contains": query, "mode": "insensitive"}},
                {"specialty": {"contains": query, "mode": "insensitive"}},
            ]
        })

    where = {"AND": conditions} if len(conditions) > 1 else (conditions[0] if conditions else {})
    doctors = await prisma.doctors.find_many(
        where=where,
        order={"created_at": "desc"},
        include={"hospital": True},
    )
    return doctors


@router.get("/doctors/pending")
async def list_pending_doctors(user: CurrentUser = Depends(get_current_staff)):
    doctors = await prisma.doctors.find_many(
        where={"is_active": False},
        order={"created_at": "desc"},
    )
    return doctors


@router.get("/doctors/{doctor_id}")
async def get_doctor(
    doctor_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    doctor = await prisma.doctors.find_first(
        where={"id": doctor_id},
        include={"hospital": True},
    )
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")
    return doctor


@router.post("/doctors", status_code=status.HTTP_201_CREATED)
async def create_doctor(
    req: AdminDoctorCreate,
    user: CurrentUser = Depends(get_current_staff),
):
    existing = await prisma.admins.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as admin")

    existing = await prisma.doctors.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Doctor already registered")

    existing = await prisma.nurses.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as nurse")

    hospital_id = await _resolve_or_create_hospital(req.hospital_id, req.hospital_name)

    doctor = await prisma.doctors.create(
        data={
            "name": req.name,
            "phone": req.phone,
            "specialty": req.specialty,
            "hospital_id": hospital_id,
            "is_active": req.is_active,
        },
        include={"hospital": True},
    )
    return doctor


@router.patch("/doctors/{doctor_id}")
async def update_doctor(
    doctor_id: str,
    req: AdminDoctorUpdate,
    user: CurrentUser = Depends(get_current_staff),
):
    doctor = await prisma.doctors.find_first(where={"id": doctor_id})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    update_data = req.model_dump(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No fields to update")

    if "phone" in update_data:
        existing = await prisma.doctors.find_first(where={"phone": update_data["phone"], "NOT": {"id": doctor_id}})
        if existing:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already in use")

    if "hospital_name" in update_data or "hospital_id" in update_data:
        hospital_id = await _resolve_or_create_hospital(
            update_data.get("hospital_id"), update_data.pop("hospital_name", None)
        )
        update_data["hospital_id"] = hospital_id

    updated = await prisma.doctors.update(
        where={"id": doctor_id},
        data=update_data,
        include={"hospital": True},
    )
    return updated


@router.patch("/doctors/{doctor_id}/status")
async def update_doctor_status(
    doctor_id: str,
    req: UpdateDoctorStatusRequest,
    user: CurrentUser = Depends(get_current_staff),
):
    doctor = await prisma.doctors.find_first(where={"id": doctor_id})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    updated = await prisma.doctors.update(
        where={"id": doctor_id},
        data={"is_active": req.is_active},
        include={"hospital": True},
    )
    return updated


@router.get("/nurses")
async def list_nurses(
    search: Optional[str] = Query(None),
    is_active: Optional[bool] = Query(None),
    user: CurrentUser = Depends(get_current_staff),
):
    conditions: List[Dict[str, Any]] = []
    if is_active is not None:
        conditions.append({"is_active": is_active})
    if search:
        query = search.strip()
        conditions.append({
            "OR": [
                {"name": {"contains": query, "mode": "insensitive"}},
                {"phone": {"contains": query, "mode": "insensitive"}},
            ]
        })

    where = {"AND": conditions} if len(conditions) > 1 else (conditions[0] if conditions else {})
    nurses = await prisma.nurses.find_many(
        where=where,
        order={"created_at": "desc"},
        include={"doctor": {"include": {"hospital": True}}, "hospital": True},
    )
    return nurses


@router.get("/nurses/{nurse_id}")
async def get_nurse(
    nurse_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    nurse = await prisma.nurses.find_first(
        where={"id": nurse_id},
        include={"doctor": True, "hospital": True},
    )
    if not nurse:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nurse not found")
    return nurse


@router.post("/nurses", status_code=status.HTTP_201_CREATED)
async def create_nurse(
    req: AdminNurseCreate,
    user: CurrentUser = Depends(get_current_staff),
):
    doctor = await prisma.doctors.find_first(where={"id": req.doctor_id})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    existing = await prisma.admins.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as admin")

    existing = await prisma.doctors.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as doctor")

    existing = await prisma.nurses.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as nurse")

    hospital_id = req.hospital_id or doctor.hospital_id

    nurse = await prisma.nurses.create(
        data={
            "name": req.name,
            "phone": req.phone,
            "doctor_id": req.doctor_id,
            "hospital_id": hospital_id,
            "is_active": req.is_active,
        },
        include={"doctor": True, "hospital": True},
    )
    return nurse


@router.patch("/nurses/{nurse_id}")
async def update_nurse(
    nurse_id: str,
    req: AdminNurseUpdate,
    user: CurrentUser = Depends(get_current_staff),
):
    nurse = await prisma.nurses.find_first(where={"id": nurse_id})
    if not nurse:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nurse not found")

    update_data = req.model_dump(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No fields to update")

    if "phone" in update_data:
        existing = await prisma.nurses.find_first(where={"phone": update_data["phone"], "NOT": {"id": nurse_id}})
        if existing:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already in use")

    if "doctor_id" in update_data:
        doctor = await prisma.doctors.find_first(where={"id": update_data["doctor_id"]})
        if not doctor:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    updated = await prisma.nurses.update(
        where={"id": nurse_id},
        data=update_data,
        include={"doctor": True, "hospital": True},
    )
    return updated


@router.get("/patients")
async def list_patients(
    search: Optional[str] = Query(None),
    doctor_id: Optional[str] = Query(None),
    user: CurrentUser = Depends(get_current_staff),
):
    conditions: List[Dict[str, Any]] = []
    if doctor_id:
        conditions.append({"doctor_id": doctor_id})
    if search:
        query = search.strip()
        conditions.append({
            "OR": [
                {"name": {"contains": query, "mode": "insensitive"}},
                {"parent_phone": {"contains": query, "mode": "insensitive"}},
                {"parent_name": {"contains": query, "mode": "insensitive"}},
            ]
        })

    where = {"AND": conditions} if len(conditions) > 1 else (conditions[0] if conditions else {})
    patients = await prisma.patients.find_many(
        where=where,
        order={"created_at": "desc"},
        include={"doctor": {"include": {"hospital": True}}, "hospital": True},
    )
    return patients


@router.get("/patients/{patient_id}")
async def get_patient(
    patient_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    patient = await prisma.patients.find_first(
        where={"id": patient_id},
        include={"doctor": True, "hospital": True},
    )
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")
    return patient


@router.post("/patients", status_code=status.HTTP_201_CREATED)
async def create_patient(
    req: AdminPatientCreate,
    user: CurrentUser = Depends(get_current_staff),
):
    doctor = await prisma.doctors.find_first(where={"id": req.doctor_id})
    if not doctor:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Doctor not found")

    hospital_id = req.hospital_id or doctor.hospital_id

    existing = await prisma.patients.find_first(
        where={
            "doctor_id": req.doctor_id,
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

    patient = await prisma.patients.create(
        data={
            "doctor_id": req.doctor_id,
            "hospital_id": hospital_id,
            "name": req.name,
            "age": req.age,
            "gender": req.gender,
            "blood_group": req.blood_group,
            "allergies": req.allergies,
            "parent_name": req.parent_name,
            "parent_phone": req.parent_phone,
        },
        include={"doctor": True, "hospital": True},
    )
    return patient


@router.post("/patients/bulk-import", status_code=status.HTTP_200_OK)
async def bulk_import_patients(
    file: UploadFile = File(...),
    user: CurrentUser = Depends(get_current_staff),
):
    if not file.filename or not (file.filename.endswith(".xlsx") or file.filename.endswith(".csv")):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only .xlsx or .csv files are supported",
        )

    contents = await file.read()
    try:
        rows = parse_excel_bytes(contents)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Could not parse file: {str(exc)}") from exc

    try:
        prepared = validate_and_prepare_rows(rows)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    # Resolve doctors and hospitals, then check duplicates again against DB.
    doctor_cache: Dict[str, Optional[Any]] = {}
    hospital_cache: Dict[str, Optional[str]] = {}
    errors: List[str] = []

    for idx, row in enumerate(prepared, start=1):
        doctor_phone = row.get("doctor_phone")
        doctor_id = row.get("doctor_id")

        if doctor_id and doctor_id not in doctor_cache:
            doctor_cache[doctor_id] = await prisma.doctors.find_first(where={"id": doctor_id})

        if doctor_phone and not doctor_id:
            if doctor_phone not in doctor_cache:
                doctor_cache[doctor_phone] = await prisma.doctors.find_first(where={"phone": doctor_phone})
            doc = doctor_cache.get(doctor_phone)
            if doc:
                doctor_id = doc.id
            else:
                errors.append(f"Row {idx}: doctor with phone {doctor_phone} not found")
                continue

        if not doctor_id:
            errors.append(f"Row {idx}: doctor not found")
            continue

        doctor = doctor_cache.get(doctor_id)
        if not doctor:
            errors.append(f"Row {idx}: doctor with id {doctor_id} not found")
            continue

        # Resolve hospital
        hospital_key = (row.get("hospital_id") or "") + "|" + (row.get("hospital_name") or "")
        if hospital_key not in hospital_cache:
            hospital_cache[hospital_key] = await _resolve_or_create_hospital(
                row.get("hospital_id"), row.get("hospital_name")
            )
        hospital_id = hospital_cache[hospital_key]

        # DB duplicate check
        existing = await prisma.patients.find_first(
            where={
                "doctor_id": doctor_id,
                "name": {"equals": row["name"], "mode": "insensitive"},
                "age": row["age"],
                "gender": {"equals": row["gender"], "mode": "insensitive"},
                "parent_phone": row["parent_phone"],
            }
        )
        if existing:
            errors.append(f"Row {idx}: duplicate patient already exists (ID: {existing.id})")
            continue

        row["_doctor_id"] = doctor_id
        row["_hospital_id"] = hospital_id

    if errors:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="\n".join(errors))

    # All validated; create patients
    created_count = 0
    for row in prepared:
        await prisma.patients.create(
            data={
                "doctor_id": row["_doctor_id"],
                "hospital_id": row["_hospital_id"],
                "name": row["name"],
                "age": row["age"],
                "gender": row["gender"],
                "blood_group": row.get("blood_group"),
                "allergies": row.get("allergies"),
                "parent_name": row["parent_name"],
                "parent_phone": row["parent_phone"],
            }
        )
        created_count += 1

    return {"imported": created_count, "total": len(prepared)}


@router.post("/admins", status_code=status.HTTP_201_CREATED)
async def create_admin(
    req: CreateAdminRequest,
    user: CurrentUser = Depends(get_current_superadmin),
):
    existing = await prisma.admins.find_first(where={"phone": req.phone})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered")

    admin = await prisma.admins.create(
        data={
            "name": req.name,
            "phone": req.phone,
            "role": req.role,
        }
    )
    return admin


@router.get("/admins")
async def list_admins(user: CurrentUser = Depends(get_current_superadmin)):
    admins = await prisma.admins.find_many(order={"created_at": "desc"})
    return admins


# -------------------- Hospitals --------------------

@router.get("/hospitals")
async def list_hospitals(
    search: Optional[str] = Query(None),
    user: CurrentUser = Depends(get_current_staff),
):
    where: Dict[str, Any] = {}
    if search:
        where["name"] = {"contains": search.strip(), "mode": "insensitive"}
    hospitals = await prisma.hospitals.find_many(
        where=where,
        order={"name": "asc"},
    )
    return hospitals


@router.post("/hospitals", status_code=status.HTTP_201_CREATED)
async def create_hospital(
    name: str,
    address: Optional[str] = None,
    contact: Optional[str] = None,
    registration_number: Optional[str] = None,
    user: CurrentUser = Depends(get_current_staff),
):
    hospital = await prisma.hospitals.create(
        data={
            "name": name,
            "address": address,
            "contact": contact,
            "registration_number": registration_number,
        }
    )
    return hospital


# -------------------- Consent Content Templates --------------------

@router.get("/consent-content-templates")
async def list_consent_content_templates(
    is_active: Optional[bool] = Query(None),
    user: CurrentUser = Depends(get_current_staff),
):
    where: Dict[str, Any] = {}
    if is_active is not None:
        where["is_active"] = is_active
    templates = await prisma.consent_content_templates.find_many(
        where=where,
        order={"created_at": "desc"},
    )
    return templates


@router.post("/consent-content-templates", status_code=status.HTTP_201_CREATED)
async def create_consent_content_template(
    req: ConsentContentTemplateCreate,
    user: CurrentUser = Depends(get_current_staff),
):
    data = req.model_dump()
    data["created_by_admin_id"] = user.payload.get("admin_id")
    template = await prisma.consent_content_templates.create(data=data)
    return template


@router.get("/consent-content-templates/{template_id}")
async def get_consent_content_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    template = await prisma.consent_content_templates.find_first(where={"id": template_id})
    if not template:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    return template


@router.patch("/consent-content-templates/{template_id}")
async def update_consent_content_template(
    template_id: str,
    req: ConsentContentTemplateUpdate,
    user: CurrentUser = Depends(get_current_staff),
):
    existing = await prisma.consent_content_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")

    update_data = req.model_dump(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No fields to update")
    update_data["updated_at"] = datetime.now(timezone.utc)

    updated = await prisma.consent_content_templates.update(
        where={"id": template_id},
        data=update_data,
    )
    return updated


@router.delete("/consent-content-templates/{template_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_consent_content_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    existing = await prisma.consent_content_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    await prisma.consent_content_templates.delete(where={"id": template_id})
    return None


# -------------------- Consent Layout Templates --------------------

@router.get("/consent-layout-templates")
async def list_consent_layout_templates(user: CurrentUser = Depends(get_current_staff)):
    templates = await prisma.consent_layout_templates.find_many(order={"created_at": "desc"})
    return templates


@router.post("/consent-layout-templates", status_code=status.HTTP_201_CREATED)
async def create_consent_layout_template(
    req: ConsentLayoutTemplateCreate,
    user: CurrentUser = Depends(get_current_staff),
):
    from app.services.consent_layout_renderer import render_layout, validate_blocks

    data = req.model_dump(exclude_unset=True)
    blocks = validate_blocks(data.get("blocks_json"))
    styles = data.get("styles_json") or {}

    # Generate html from blocks/styles if not supplied explicitly.
    if not data.get("html"):
        data["html"] = render_layout(blocks=blocks, styles=styles)

    data["blocks_json"] = Json(blocks)
    data["styles_json"] = Json(styles)

    if req.is_default:
        await prisma.consent_layout_templates.update_many(
            where={"is_default": True},
            data={"is_default": False},
        )
    template = await prisma.consent_layout_templates.create(data=data)
    return template


@router.get("/consent-layout-templates/{template_id}")
async def get_consent_layout_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    template = await prisma.consent_layout_templates.find_first(where={"id": template_id})
    if not template:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    return template


@router.patch("/consent-layout-templates/{template_id}")
async def update_consent_layout_template(
    template_id: str,
    req: ConsentLayoutTemplateUpdate,
    user: CurrentUser = Depends(get_current_staff),
):
    from app.services.consent_layout_renderer import render_layout, validate_blocks

    existing = await prisma.consent_layout_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")

    update_data = req.model_dump(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No fields to update")

    # Regenerate html whenever blocks/styles change, or when html itself is not supplied.
    if "blocks_json" in update_data or "styles_json" in update_data or "html" not in update_data:
        existing_blocks = existing.blocks_json if isinstance(existing.blocks_json, (list, dict)) else None
        if isinstance(existing_blocks, dict):
            existing_blocks = [existing_blocks]

        blocks = validate_blocks(update_data.get("blocks_json") or existing_blocks)
        styles = update_data.get("styles_json")
        if styles is None:
            styles = existing.styles_json if isinstance(existing.styles_json, dict) else {}
        if styles is None:
            styles = {}

        if not update_data.get("html"):
            update_data["html"] = render_layout(blocks=blocks, styles=styles)

        update_data["blocks_json"] = Json(blocks)
        update_data["styles_json"] = Json(styles)

    if update_data.get("is_default"):
        await prisma.consent_layout_templates.update_many(
            where={"is_default": True},
            data={"is_default": False},
        )

    update_data["updated_at"] = datetime.now(timezone.utc)
    updated = await prisma.consent_layout_templates.update(
        where={"id": template_id},
        data=update_data,
    )
    return updated


@router.delete("/consent-layout-templates/{template_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_consent_layout_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    existing = await prisma.consent_layout_templates.find_first(where={"id": template_id})
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")
    await prisma.consent_layout_templates.delete(where={"id": template_id})
    return None


@router.post("/consent-layout-templates/{template_id}/preview")
async def preview_consent_layout_template(
    template_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    template = await prisma.consent_layout_templates.find_first(where={"id": template_id})
    if not template:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found")

    dummy = {
        "consent_id": "preview-id",
        "consent_number": "NT-PREVIEW-00000000000000",
        "version": "v2.1",
        "status": "Pending",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "language": "English",
        "form_type": "Surgical Consent",
        "hospital_name": "Preview Hospital",
        "hospital_address": "123 Preview Road",
        "hospital_contact": "+91 99999 99999",
        "hospital_registration_number": "REG123",
        "patient_name": "Preview Patient",
        "patient_uhid": "UHID123",
        "age": 5,
        "gender": "Male",
        "admission_number": "ADM123",
        "department": "Pediatric Surgery",
        "ward_room": "Ward A / 101",
        "parent_name": "Preview Parent",
        "guardian_relationship": "Parent / Guardian",
        "parent_phone": "+91 88888 88888",
        "surgeon_name": "Dr. Preview",
        "doctor_qualification": "MCh Pediatric Surgery",
        "doctor_registration_number": "REG-DR-123",
        "doctor_declaration_timestamp": datetime.now(timezone.utc).isoformat(),
        "diagnosis": "Preview diagnosis",
        "procedure": "Preview procedure",
        "procedure_description": "This is a preview of the consent layout template.",
        "anesthesia": "General anesthesia",
        "benefits": "Benefit one\nBenefit two",
        "risks": "Risk one\nRisk two",
        "material_risks": "Material risk one\nMaterial risk two",
        "possible_complications": "Complication one\nComplication two",
        "alternatives": "Alternative one\nAlternative two",
        "post_op_care": "Rest and monitoring.",
        "expected_recovery": "Full recovery expected in 2 weeks.",
        "refusal_consequences": "Refusal may worsen condition.",
        "right_to_withdraw": "Consent may be withdrawn at any time.",
        "consent_for_anesthesia": "Yes",
        "consent_for_blood_products": "No",
        "consent_for_photography": "No",
        "privacy_statement": "Data will be kept confidential.",
        "statutory_reference": "NMC / NABH guidelines.",
        "rag_assisted": False,
        "pdf_hash_truncated": "PREVIEW",
    }

    from app.services.consent_layout_renderer import render_layout

    try:
        if template.blocks_json:
            html = render_layout(blocks=template.blocks_json, styles=template.styles_json, form_data=dummy)
        else:
            from jinja2 import Template
            tpl = Template(template.html)
            html = tpl.render(**dummy)
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Template render error: {str(exc)}",
        ) from exc

    return {"html": html}


# -------------------- Consent Form Downloads --------------------

@router.get("/patients/{patient_id}/consent-forms")
async def list_patient_consent_forms(
    patient_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    patient = await prisma.patients.find_first(where={"id": patient_id})
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    forms = await prisma.consent_forms.find_many(
        where={"patient_id": patient_id},
        order={"generated_at": "desc"},
        include={"admission": True, "doctor": True, "patient": True},
    )
    return forms


@router.get("/consent-forms/{consent_id}/download")
async def download_consent_form_pdf(
    consent_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    consent = await prisma.consent_forms.find_first(
        where={"id": consent_id},
        include={"patient": True, "doctor": True, "admission": True},
    )
    if not consent:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Consent form not found")

    # Prefer already-generated signed/unsigned PDF URL
    if consent.signed_pdf_url:
        pdf_url = consent.signed_pdf_url
    elif consent.pdf_url:
        pdf_url = consent.pdf_url
    else:
        pdf_url = None

    if pdf_url:
        return {"pdf_url": pdf_url}

    # Regenerate from stored content_json
    form_data = consent.content_json if isinstance(consent.content_json, dict) else dict(consent.content_json or {})
    form_data["consent_id"] = consent.id

    layout_html = None
    layout_template_name = form_data.get("layout_template_name")
    if layout_template_name:
        layout = await prisma.consent_layout_templates.find_first(
            where={"name": layout_template_name}
        )
        if layout:
            layout_html = layout.html

    if consent.status == "signed":
        pdf_result = generate_signed_consent_pdf(
            form_data=form_data,
            parent_auth_method=consent.parent_auth_method or "otp",
            parent_auth_phone=consent.parent_auth_phone,
            witness_name=consent.witness_name,
            witness_relationship=consent.witness_relationship,
            witness_mobile=consent.witness_mobile,
            witness_verified=bool(consent.witness_verified_at),
            signer_attested=bool(consent.signer_attested_at),
            signed_at=consent.signed_at.isoformat() if consent.signed_at else datetime.now(timezone.utc).isoformat(),
            template_html=layout_html,
        )
    else:
        pdf_result = generate_consent_pdf(form_data, template_html=layout_html)

    pdf_bytes = pdf_result["pdf_bytes"]
    filename = f"consent_{consent.id}.pdf"
    return StreamingResponse(
        io.BytesIO(pdf_bytes),
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename={filename}"},
    )


# -------------------- Medical Summary PDF --------------------

@router.get("/patients/{patient_id}/summary-pdf")
async def download_patient_summary_pdf(
    patient_id: str,
    user: CurrentUser = Depends(get_current_staff),
):
    from app.services.summary_pdf_service import generate_patient_summary_pdf

    patient = await prisma.patients.find_first(where={"id": patient_id})
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    pdf_bytes = await generate_patient_summary_pdf(patient_id)
    filename = f"medical_summary_{patient.name.replace(' ', '_')}_{patient_id}.pdf"
    return StreamingResponse(
        io.BytesIO(pdf_bytes),
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename={filename}"},
    )
