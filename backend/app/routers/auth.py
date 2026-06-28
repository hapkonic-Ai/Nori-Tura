from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from typing import Optional
from datetime import timedelta

from app.core.database import prisma
from app.core.security import create_access_token
from app.core.auth_deps import get_current_user, CurrentUser, resolve_doctor_id, get_current_staff

router = APIRouter(prefix="/auth", tags=["Authentication"])


class SendOtpRequest(BaseModel):
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")


class VerifyOtpRequest(BaseModel):
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    otp: str = Field(..., min_length=6, max_length=6)


class RegisterDoctorRequest(BaseModel):
    name: str = Field(..., min_length=2)
    phone: str = Field(..., pattern=r"^\+91[0-9]{10}$")
    hospital: str = Field(..., min_length=1)
    specialty: str = Field(..., min_length=1)


class FcmTokenRequest(BaseModel):
    fcm_token: str
    platform: str = "android"  # android, ios, web


async def _determine_role(phone: str) -> tuple[str, Optional[dict]]:
    """Determine role and associated record for a phone number."""
    admin = await prisma.admins.find_first(where={"phone": phone})
    if admin:
        return ("superadmin" if admin.role == "superadmin" else "admin", {"admin": admin})

    doctor = await prisma.doctors.find_first(where={"phone": phone})
    if doctor:
        if not doctor.is_active:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Doctor account pending approval or inactive")
        return ("surgeon", {"doctor": doctor})

    nurse = await prisma.nurses.find_first(where={"phone": phone, "is_active": True})
    if nurse:
        return ("nurse", {"nurse": nurse})

    return ("patient_parent", {})


@router.post("/send-otp")
async def send_otp(req: SendOtpRequest):
    from app.services.otp_service import create_otp_session

    try:
        role, records = await _determine_role(req.phone)
    except HTTPException:
        # If doctor exists but inactive, still allow sending OTP so they see the inactive message
        doctor = await prisma.doctors.find_first(where={"phone": req.phone})
        if doctor:
            role = "surgeon_inactive"
        else:
            role = "patient_parent"
        records = {}

    returned_otp = await create_otp_session(req.phone, role)

    return {
        "message": "OTP sent successfully",
        "expires_in_minutes": 5,
        "dev_otp": returned_otp
    }


@router.post("/register-doctor", status_code=status.HTTP_201_CREATED)
async def register_doctor(req: RegisterDoctorRequest):
    existing_admin = await prisma.admins.find_first(where={"phone": req.phone})
    if existing_admin:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as admin")

    existing_doctor = await prisma.doctors.find_first(where={"phone": req.phone})
    if existing_doctor:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Doctor already registered")

    existing_nurse = await prisma.nurses.find_first(where={"phone": req.phone})
    if existing_nurse:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Phone already registered as nurse")

    hospital_name = req.hospital.strip() if req.hospital else "Unnamed Hospital"
    hospital = await prisma.hospitals.find_first(
        where={"name": {"equals": hospital_name, "mode": "insensitive"}}
    )
    if not hospital:
        hospital = await prisma.hospitals.create(data={"name": hospital_name})

    doctor = await prisma.doctors.create(
        data={
            "name": req.name,
            "phone": req.phone,
            "hospital_id": hospital.id,
            "specialty": req.specialty,
            "is_active": False,
        }
    )

    return {
        "message": "Registration submitted. Wait for admin approval.",
        "doctor_id": doctor.id,
        "status": "pending_approval"
    }


@router.post("/verify-otp")
async def verify_otp_endpoint(req: VerifyOtpRequest):
    from app.services.otp_service import verify_otp

    try:
        result = await verify_otp(req.phone, req.otp)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))

    session = result["session"]
    role = session.role

    token_payload = {"phone": req.phone, "role": role}

    if role in ["admin", "superadmin"]:
        admin = await prisma.admins.find_first(where={"phone": req.phone})
        if not admin:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found")
        if not admin.is_active:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin account inactive")
        token_payload["admin_id"] = admin.id

    elif role == "surgeon":
        doctor = await prisma.doctors.find_first(where={"phone": req.phone})
        if not doctor:
            raise HTTPException(status_code=404, detail="Doctor not found")
        if not doctor.is_active:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Doctor account pending approval or inactive")
        token_payload["doctor_id"] = doctor.id

    elif role == "surgeon_inactive":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Doctor account pending approval or inactive")

    elif role == "nurse":
        nurse = await prisma.nurses.find_first(where={"phone": req.phone})
        if not nurse:
            raise HTTPException(status_code=404, detail="Nurse not found")
        token_payload["nurse_id"] = nurse.id
        token_payload["doctor_id"] = nurse.doctor_id

    elif role == "patient_parent":
        patient = await prisma.patients.find_first(where={"parent_phone": req.phone})
        if patient:
            token_payload["patient_id"] = patient.id
            token_payload["doctor_id"] = patient.doctor_id

    access_token = create_access_token(token_payload, expires_delta=timedelta(days=30))

    return {
        "access_token": access_token,
        "token_type": "bearer",
        "role": role
    }


@router.get("/me")
async def get_me(user: CurrentUser = Depends(get_current_user)):
    if user.is_superadmin() or user.is_admin():
        admin = await prisma.admins.find_first(where={"id": user.payload.get("admin_id")})
        return {"role": user.role, "profile": admin}

    if user.is_surgeon():
        profile = await prisma.doctors.find_first(where={"id": user.doctor_id})
        return {"role": "surgeon", "profile": profile}

    if user.is_nurse():
        profile = await prisma.nurses.find_first(where={"id": user.nurse_id})
        doctor = await prisma.doctors.find_first(where={"id": profile.doctor_id}) if profile else None
        return {"role": "nurse", "profile": profile, "doctor": doctor}

    patient_id = user.patient_id
    if patient_id and isinstance(patient_id, str) and patient_id.strip():
        patient = await prisma.patients.find_first(where={"id": patient_id})
        doctor = await prisma.doctors.find_first(where={"id": patient.doctor_id}) if patient else None
        return {
            "role": "patient_parent",
            "patient": patient,
            "doctor": doctor
        }

    return {"role": "patient_parent"}


@router.post("/register-fcm")
async def register_fcm(req: FcmTokenRequest, user: CurrentUser = Depends(get_current_user)):
    data = {"fcm_token": req.fcm_token, "platform": req.platform}

    if user.is_surgeon():
        await prisma.doctors.update(where={"id": user.doctor_id}, data=data)
    elif user.is_nurse():
        await prisma.nurses.update(where={"id": user.nurse_id}, data=data)
    elif user.is_staff():
        await prisma.admins.update(where={"id": user.payload.get("admin_id")}, data=data)
    else:
        await prisma.patients.update_many(
            where={"parent_phone": user.phone},
            data=data
        )

    return {"message": "FCM token registered"}
