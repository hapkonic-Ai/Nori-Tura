import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.core.security import decode_token
from app.core.database import prisma
from typing import Dict, Any

security = HTTPBearer()


class CurrentUser:
    def __init__(self, payload: Dict[str, Any]):
        self.phone = payload.get("phone")
        self.role = payload.get("role")
        self.doctor_id = payload.get("doctor_id")
        self.nurse_id = payload.get("nurse_id")
        self.patient_id = payload.get("patient_id")
        self.payload = payload

    def is_surgeon(self) -> bool:
        return self.role == "surgeon"

    def is_nurse(self) -> bool:
        return self.role == "nurse"

    def is_parent(self) -> bool:
        return self.role == "patient_parent"

    def is_admin(self) -> bool:
        return self.role == "admin"

    def is_superadmin(self) -> bool:
        return self.role == "superadmin"

    def is_staff(self) -> bool:
        return self.role in ["admin", "superadmin"]


async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> CurrentUser:
    token = credentials.credentials
    try:
        payload = decode_token(token)
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

    return CurrentUser(payload)


async def get_current_surgeon(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if not user.is_surgeon():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Surgeon access required")
    return user


async def get_current_nurse_or_surgeon(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if user.role not in ["surgeon", "nurse"]:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Nurse or surgeon access required")
    return user


async def get_current_parent(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if not user.is_parent():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Parent access required")
    return user


async def resolve_doctor_id(user: CurrentUser) -> str:
    """Resolve the doctor_id the user is scoped to."""
    if user.is_surgeon():
        return user.doctor_id
    if user.is_nurse():
        nurse = await prisma.nurses.find_first(where={"id": user.nurse_id})
        if not nurse:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Nurse account not found")
        if not nurse.is_active:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Nurse account inactive")
        return nurse.doctor_id
    raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Cannot resolve doctor_id for this role")


async def get_current_staff(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if not user.is_staff():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin or superadmin access required")
    admin = await prisma.admins.find_first(where={"id": user.payload.get("admin_id")})
    if not admin or not admin.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin account inactive")
    return user


async def get_current_superadmin(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if not user.is_superadmin():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Superadmin access required")
    admin = await prisma.admins.find_first(where={"id": user.payload.get("admin_id")})
    if not admin or not admin.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin account inactive")
    return user
