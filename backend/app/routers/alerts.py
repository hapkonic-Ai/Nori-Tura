from datetime import datetime, timezone, timedelta
from typing import List

from fastapi import APIRouter, Depends
from fastapi.encoders import jsonable_encoder

from app.core.auth_deps import get_current_nurse_or_surgeon, CurrentUser, resolve_doctor_id
from app.core.database import prisma

router = APIRouter(prefix="/alerts", tags=["Alerts"])

ACTIVE_STATUSES = {"admitted", "pre-op", "in-surgery", "recovery"}


@router.get("")
async def get_alerts(user: CurrentUser = Depends(get_current_nurse_or_surgeon)):
    doctor_id = await resolve_doctor_id(user)

    now = datetime.now(timezone.utc)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    today_end = today_start + timedelta(days=1)

    admissions = await prisma.ipd_admissions.find_many(
        where={"doctor_id": doctor_id},
        include={
            "patient": True,
            "consent_forms": {"where": {"status": "pending"}, "order_by": {"generated_at": "desc"}},
        },
    )

    pending_consents: List[dict] = []
    active_admissions: List[dict] = []
    for admission in admissions:
        if admission.status in ACTIVE_STATUSES:
            active_admissions.append(admission)
        for consent in admission.consent_forms or []:
            pending_consents.append(
                {
                    "id": consent.id,
                    "admission_id": admission.id,
                    "patient_id": admission.patient_id,
                    "patient_name": admission.patient.name if admission.patient else None,
                    "procedure": consent.form_type,
                    "generated_at": consent.generated_at,
                }
            )

    today_appointments = await prisma.appointments.find_many(
        where={
            "doctor_id": doctor_id,
            "slot_datetime": {"gte": today_start, "lt": today_end},
        },
        order={"slot_datetime": "asc"},
        include={"patient": True},
    )

    pending_reviews = await prisma.opd_records.find_many(
        where={"doctor_id": doctor_id, "review_status": "pending_review"},
        order={"created_at": "desc"},
        take=20,
        include={"patient": True},
    )

    return jsonable_encoder(
        {
            "pending_consents": pending_consents,
            "today_appointments": today_appointments,
            "pending_reviews": pending_reviews,
            "active_admissions": active_admissions,
        }
    )
