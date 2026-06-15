import logging
import os
from typing import Optional

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

WHATSAPP_API_URL = os.getenv(
    "WHATSAPP_API_URL",
    "https://graph.facebook.com/v19.0/{phone_number_id}/messages",
)


def _get_whatsapp_config():
    """Return WhatsApp credentials from settings, falling back to env vars."""
    settings = get_settings()
    phone_number_id = settings.META_WA_PHONE_ID or os.getenv("WHATSAPP_PHONE_NUMBER_ID", "")
    access_token = settings.META_WA_TOKEN or os.getenv("WHATSAPP_ACCESS_TOKEN", "")
    return phone_number_id, access_token


def is_configured() -> bool:
    phone_number_id, access_token = _get_whatsapp_config()
    return bool(phone_number_id and access_token)


async def send_template_message(
    to: str,
    template_name: str,
    language_code: str = "en",
    body_parameters: Optional[list] = None,
) -> dict:
    """Send a WhatsApp Cloud API template message."""
    if not is_configured():
        logger.warning(
            "WhatsApp Cloud API not configured. Would send template '%s' to %s",
            template_name,
            to,
        )
        return {"status": "skipped", "reason": "not_configured"}

    phone_number_id, access_token = _get_whatsapp_config()
    url = WHATSAPP_API_URL.format(phone_number_id=phone_number_id)
    payload = {
        "messaging_product": "whatsapp",
        "recipient_type": "individual",
        "to": to,
        "type": "template",
        "template": {
            "name": template_name,
            "language": {"code": language_code},
        },
    }
    if body_parameters:
        payload["template"]["components"] = [
            {
                "type": "body",
                "parameters": [
                    {"type": "text", "text": str(p)} for p in body_parameters
                ],
            }
        ]

    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json",
    }

    async with httpx.AsyncClient() as client:
        response = await client.post(url, json=payload, headers=headers)
        response.raise_for_status()
        return response.json()


async def send_follow_up_reminder(
    to: str,
    patient_name: str,
    doctor_name: str,
    follow_up_date: str,
) -> dict:
    """Send a follow-up reminder message to the patient's parent."""
    return await send_template_message(
        to=to,
        template_name="noni_tura_followup_reminder",
        language_code="en",
        body_parameters=[patient_name, doctor_name, follow_up_date],
    )
