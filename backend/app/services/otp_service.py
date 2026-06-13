import os
import httpx
from datetime import datetime, timedelta, timezone
from app.core.config import get_settings
from app.core.security import generate_otp, hash_otp
from app.core.database import prisma

settings = get_settings()


async def send_otp_sms(phone: str, otp: str) -> bool:
    """Send OTP via 2Factor.in or MSG91. Falls back to console logging in dev."""
    
    # Clean phone number
    if phone.startswith("+"):
        phone = phone[1:]
    
    # Try 2Factor.in
    if settings.TWO_FACTOR_API_KEY:
        url = f"https://2factor.in/API/V1/{settings.TWO_FACTOR_API_KEY}/SMS/{phone}/{otp}/NoniTuraOTP"
        try:
            async with httpx.AsyncClient() as client:
                resp = await client.get(url, timeout=10.0)
                return resp.status_code == 200
        except Exception as e:
            print(f"2Factor.in error: {e}")
            return False
    
    # Fallback: log to console for development
    print(f"[DEV OTP] Phone: {phone}, OTP: {otp}")
    return True


async def create_otp_session(phone: str, role: str = None) -> str:
    otp = generate_otp(settings.OTP_LENGTH)
    hashed = hash_otp(otp)
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=settings.OTP_EXPIRY_MINUTES)
    
    # Invalidate old sessions for this phone
    await prisma.otp_sessions.delete_many(where={"phone": phone})
    
    await prisma.otp_sessions.create(data={
        "phone": phone,
        "otp_hash": hashed,
        "role": role,
        "expires_at": expires_at,
        "verified": False
    })
    
    await send_otp_sms(phone, otp)
    return otp  # Return only for dev/testing; production should not return


async def verify_otp(phone: str, otp: str) -> dict:
    session = await prisma.otp_sessions.find_first(
        where={
            "phone": phone,
            "verified": False,
        },
        order={"created_at": "desc"}
    )
    
    if not session:
        raise ValueError("No active OTP session found")
    
    if session.expires_at < datetime.now(timezone.utc):
        raise ValueError("OTP expired")
    
    if hash_otp(otp) != session.otp_hash:
        raise ValueError("Invalid OTP")
    
    # Mark verified
    await prisma.otp_sessions.update(
        where={"id": session.id},
        data={"verified": True}
    )
    
    return {"session": session}
