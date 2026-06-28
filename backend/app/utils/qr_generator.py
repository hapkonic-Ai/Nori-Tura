"""QR code generation utilities for consent form verification."""

import base64
import json
import logging
from io import BytesIO
from typing import Optional

logger = logging.getLogger(__name__)


def generate_consent_qr_payload(
    consent_id: str,
    patient_id: str,
    signed_at: Optional[str] = None,
) -> str:
    """Build a compact, URL-safe JSON payload for a consent form QR code.

    The payload is intentionally minimal and does not contain PII or clinical
    details. It is meant for verification lookups only.
    """
    payload = {
        "cid": consent_id,
        "pid": patient_id,
    }
    if signed_at:
        payload["signed_at"] = signed_at
    return json.dumps(payload, separators=(",", ":"))


def generate_qr_data_uri(
    data: str,
    box_size: int = 4,
    border: int = 2,
    error_correction: Optional[str] = None,
) -> Optional[str]:
    """Generate a QR code PNG and return it as a base64 data URI.

    Returns None if the qrcode/Pillow libraries are unavailable.
    """
    try:
        import qrcode
        from qrcode.constants import ERROR_CORRECT_M
    except ImportError as exc:  # pragma: no cover
        logger.warning("qrcode library not available (%s); skipping QR generation", exc)
        return None

    try:
        qr = qrcode.QRCode(
            version=None,
            error_correction=ERROR_CORRECT_M,
            box_size=box_size,
            border=border,
        )
        qr.add_data(data)
        qr.make(fit=True)

        img = qr.make_image(fill_color="black", back_color="white")
        buffer = BytesIO()
        img.save(buffer, format="PNG")
        buffer.seek(0)
        b64 = base64.b64encode(buffer.read()).decode("ascii")
        return f"data:image/png;base64,{b64}"
    except Exception as exc:  # pragma: no cover
        logger.warning("QR generation failed: %s", exc)
        return None


def generate_consent_qr_data_uri(
    consent_id: str,
    patient_id: str,
    signed_at: Optional[str] = None,
) -> Optional[str]:
    """Generate a QR code data URI for a consent form verification section."""
    payload = generate_consent_qr_payload(consent_id, patient_id, signed_at)
    return generate_qr_data_uri(payload)
