import json
import logging
import types
from typing import Optional

from firebase_admin import credentials, get_app, initialize_app, messaging

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_messaging: Optional[types.ModuleType] = None


def get_messaging() -> Optional[types.ModuleType]:
    """Return Firebase Admin messaging module, initializing if credentials are provided.

    Supports FIREBASE_CREDENTIALS_JSON as either a JSON string or a file path.
    Returns None when credentials are missing or initialization fails.
    """
    global _messaging
    if _messaging is not None:
        return _messaging

    settings = get_settings()
    creds_value = settings.FIREBASE_CREDENTIALS_JSON
    if not creds_value:
        logger.debug("Firebase credentials not configured; push notifications disabled")
        return None

    try:
        creds_value = creds_value.strip()
        if creds_value.startswith("{"):
            cred_info = json.loads(creds_value)
        else:
            with open(creds_value, "r", encoding="utf-8") as f:
                cred_info = json.load(f)

        cred = credentials.Certificate(cred_info)
        try:
            get_app()
        except ValueError:
            initialize_app(cred)

        _messaging = messaging
        logger.info("Firebase Admin SDK initialized for push notifications")
        return _messaging
    except Exception as exc:
        logger.exception("Failed to initialize Firebase Admin SDK: %s", exc)
        return None
