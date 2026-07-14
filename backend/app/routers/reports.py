from fastapi import APIRouter, Depends, HTTPException, status

from app.core.database import prisma
from app.core.auth_deps import get_current_user, CurrentUser, resolve_doctor_id

router = APIRouter(prefix="/reports", tags=["Reports"])


@router.get("/patient-summary/{patient_id}")
async def get_patient_summary(
    patient_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    patient = await prisma.patients.find_first(
        where={"id": patient_id},
    )
    if not patient:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Patient not found")

    # Access control
    if user.is_parent():
        if patient.parent_phone != user.phone:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")
    else:
        doctor_id = await resolve_doctor_id(user)
        if patient.doctor_id != doctor_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied")

    # Fetch IPD admissions with all related notes
    admissions = await prisma.ipd_admissions.find_many(
        where={"patient_id": patient_id},
        order={"admitted_at": "desc"},
        include={
            "pre_op_notes": True,
            "intra_op_notes": True,
            "discharge_summaries": True,
            "consent_forms": {"order_by": {"generated_at": "desc"}},
        },
    )

    # Fetch OPD records with medications and investigations
    opd_records = await prisma.opd_records.find_many(
        where={"patient_id": patient_id},
        order={"created_at": "desc"},
        include={
            "medications": True,
            "investigations": True,
        },
    )

    # Build summary stats
    total_admissions = len(admissions)
    total_opd_visits = len(opd_records)

    last_opd_date = opd_records[0].created_at.isoformat() if opd_records else None
    last_admission_date = admissions[0].admitted_at.isoformat() if admissions else None

    active_statuses = {"admitted", "pre-op", "in-surgery", "recovery"}
    active_admission = any(
        getattr(a, "status", None) in active_statuses for a in admissions
    )

    # Consent stats across all admissions
    all_consents = [cf for a in admissions for cf in (a.consent_forms or [])]
    total_consents = len(all_consents)
    signed_consents = sum(1 for cf in all_consents if getattr(cf, "signed_at", None) is not None)
    pending_consents = total_consents - signed_consents

    return {
        "patient": {
            "id": patient.id,
            "name": patient.name,
            "age": patient.age,
            "gender": patient.gender,
            "blood_group": patient.blood_group,
            "allergies": patient.allergies,
            "parent_name": patient.parent_name,
            "parent_phone": patient.parent_phone,
        },
        "summary": {
            "total_opd_visits": total_opd_visits,
            "total_admissions": total_admissions,
            "last_opd_date": last_opd_date,
            "last_admission_date": last_admission_date,
            "active_admission": active_admission,
        },
        "opd_records": opd_records,
        "admissions": admissions,
        "consent_status": {
            "total_consents": total_consents,
            "signed_consents": signed_consents,
            "pending_consents": pending_consents,
        },
    }
