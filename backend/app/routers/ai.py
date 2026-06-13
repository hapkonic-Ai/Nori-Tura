from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import Optional

from prisma import Json

from app.core.database import prisma
from app.core.auth_deps import get_current_user, CurrentUser, resolve_doctor_id
from app.services.ai_service import suggest_diagnosis

router = APIRouter(prefix="/ai", tags=["AI"])


class SuggestDiagnosisRequest(BaseModel):
    patient_id: str
    complaint: str
    examination: str
    age: Optional[int] = Field(None, ge=0, le=150)
    gender: Optional[str] = None


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


@router.post("/suggest-diagnosis")
async def suggest_diagnosis_endpoint(
    req: SuggestDiagnosisRequest,
    user: CurrentUser = Depends(get_current_user),
):
    patient = await _require_patient_access(user, req.patient_id)

    if user.is_parent():
        doctor_id = patient.doctor_id
        created_by = "parent"
    elif user.is_nurse():
        doctor_id = await resolve_doctor_id(user)
        created_by = "nurse"
    else:
        doctor_id = await resolve_doctor_id(user)
        created_by = "surgeon"

    age = req.age if req.age is not None else patient.age
    gender = req.gender if req.gender is not None else patient.gender

    suggestions = await suggest_diagnosis(
        complaint=req.complaint,
        examination=req.examination,
        age=age,
        gender=gender,
    )

    # Ensure confidence cap and disclaimer are present
    suggestions["confidence"] = min(float(suggestions.get("confidence", 0.0)), 0.90)
    if "disclaimer" not in suggestions:
        suggestions["disclaimer"] = "This is a clinical decision support suggestion, not a diagnosis."

    # Create a lightweight OPD record to satisfy the ai_diagnosis_logs FK
    opd_record = await prisma.opd_records.create(
        data={
            "patient_id": req.patient_id,
            "doctor_id": doctor_id,
            "nurse_id": user.nurse_id if user.is_nurse() else None,
            "created_by": created_by,
            "visit_type": "ai_assist",
            "complaint": req.complaint,
            "examination": req.examination,
        }
    )

    await prisma.ai_diagnosis_logs.create(
        data={
            "opd_record_id": opd_record.id,
            "doctor_id": doctor_id,
            "complaint": req.complaint,
            "examination": req.examination,
            "age": age,
            "gender": gender,
            "suggestions_json": Json(suggestions),
            "model_used": suggestions.get("model_used", "unknown"),
        }
    )

    return {
        "suggestions": suggestions,
        "disclaimer": suggestions["disclaimer"],
        "opd_record_id": opd_record.id,
    }
