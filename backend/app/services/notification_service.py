import logging
from typing import Optional

logger = logging.getLogger(__name__)


async def send_whatsapp(phone: str, message: str) -> bool:
    """Send WhatsApp message. Stubbed if no Meta credentials."""
    logger.info(f"[WhatsApp] to={phone} msg={message}")
    # TODO: Integrate Meta WhatsApp Business API when credentials are available
    return True


async def send_push(fcm_token: Optional[str], title: str, body: str) -> bool:
    """Send push notification. Stubbed if no Firebase credentials."""
    logger.info(f"[Push] token={fcm_token} title={title} body={body}")
    # TODO: Integrate Firebase Cloud Messaging when credentials are available
    return True
