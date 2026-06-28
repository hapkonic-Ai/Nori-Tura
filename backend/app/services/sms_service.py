import logging
from typing import Optional

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)


async def send_sms(phone: str, message: str, sender_id: Optional[str] = None) -> dict:
    """Send an SMS via 2Factor.in's MSD addon service.

    Falls back to a console log when no API key is configured (dev mode).
    """
    settings = get_settings()
    api_key = settings.TWO_FACTOR_API_KEY

    if not api_key:
        logger.info("[SMS stub] to=%s msg=%s", phone, message)
        return {"status": "skipped", "reason": "not_configured"}

    cleaned_phone = phone.lstrip("+")
    sender = sender_id or getattr(settings, "SMS_SENDER_ID", "NONITU") or "NONITU"

    url = f"https://2factor.in/API/V1/{api_key}/ADDON_SERVICES/SEND/MSD"
    params = {
        "phone": cleaned_phone,
        "msg": message,
        "sender": sender,
    }

    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(url, params=params, timeout=10.0)
            response.raise_for_status()
            data = response.json()
            logger.info("SMS sent to %s: %s", phone, data)
            return {"status": "sent", "provider_response": data}
    except Exception as exc:
        logger.exception("SMS send failed for %s", phone)
        return {"status": "failed", "reason": str(exc)}
