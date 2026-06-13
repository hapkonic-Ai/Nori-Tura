import jwt
import hashlib
import secrets
import string
from datetime import datetime, timedelta, timezone
from typing import Optional, Dict, Any
from app.core.config import get_settings

settings = get_settings()


def create_access_token(data: Dict[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(hours=settings.JWT_EXPIRATION_HOURS)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt


def decode_token(token: str) -> Dict[str, Any]:
    return jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])


def hash_otp(otp: str) -> str:
    return hashlib.sha256(otp.encode()).hexdigest()


def verify_otp(plain_otp: str, hashed_otp: str) -> bool:
    return hash_otp(plain_otp) == hashed_otp


def generate_otp(length: int = 6) -> str:
    return "".join(secrets.choice(string.digits) for _ in range(length))


def hash_string(value: str) -> str:
    return hashlib.sha256(value.encode()).hexdigest()
