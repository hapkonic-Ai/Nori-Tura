import logging
from typing import Optional

from app.core.firebase import get_messaging

logger = logging.getLogger(__name__)


async def send_whatsapp(phone: str, message: str) -> bool:
    """Send WhatsApp message. Stubbed if no Meta credentials."""
    logger.info("[WhatsApp] to=%s msg=%s", phone, message)
    # TODO: Integrate Meta WhatsApp Business API when credentials are available
    return True


async def send_push(fcm_token: Optional[str], title: str, body: str) -> bool:
    """Send a push notification via Firebase Cloud Messaging.

    Falls back to a logged stub when Firebase is not configured or the token is missing.
    """
    if not fcm_token:
        logger.info("[Push] skipped: no FCM token (title=%s)", title)
        return False

    messaging = get_messaging()
    if messaging is None:
        logger.info(
            "[Push stub] token=%s title=%s body=%s",
            fcm_token,
            title,
            body,
        )
        return True

    try:
        message = messaging.Message(
            token=fcm_token,
            notification=messaging.Notification(title=title, body=body),
        )
        messaging.send(message)
        logger.info("[Push] sent to token=%s title=%s", fcm_token, title)
        return True
    except Exception as exc:
        logger.exception("[Push] failed for token=%s: %s", fcm_token, exc)
        return False
