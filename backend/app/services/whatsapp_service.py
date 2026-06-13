import logging
import os
from typing import Optional

import httpx

logger = logging.getLogger(__name__)

WHATSAPP_API_URL = os.getenv(
    "WHATSAPP_API_URL",
    "https://graph.facebook.com/v19.0/{phone_number_id}/messages",
)
WHATSAPP_PHONE_NUMBER_ID = os.getenv("WHATSAPP_PHONE_NUMBER_ID", "")
WHATSAPP_ACCESS_TOKEN = os.getenv("WHATSAPP_ACCESS_TOKEN", "")


def is_configured() -> bool:
    return bool(WHATSAPP_PHONE_NUMBER_ID and WHATSAPP_ACCESS_TOKEN)


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

    url = WHATSAPP_API_URL.format(phone_number_id=WHATSAPP_PHONE_NUMBER_ID)
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
        "Authorization": f"Bearer {WHATSAPP_ACCESS_TOKEN}",
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
