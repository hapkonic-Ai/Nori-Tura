"""
RAG integration service.

Two independent RAG endpoints:
  RAG_DIAGNOSIS_URL — receives clinical presentation, returns differential context
  RAG_CONSENT_URL   — receives procedure + diagnosis, returns procedure-specific
                       risks/complications/alternatives pre-populated for the consent form

Both endpoints are optional. When not configured the callers fall back to their
existing LLM-only behaviour. The response shape expected from each endpoint is
documented in the relevant function's docstring; adapt the parsing if your RAG
returns a different schema.
"""

import logging
from typing import Optional

import httpx

from app.core.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


def _headers() -> dict:
    h = {"Content-Type": "application/json"}
    if settings.RAG_API_KEY:
        h["Authorization"] = f"Bearer {settings.RAG_API_KEY}"
    return h


async def _post(url: str, payload: dict) -> Optional[dict]:
    """Generic async POST to a RAG endpoint. Returns parsed JSON or None on failure."""
    try:
        async with httpx.AsyncClient(timeout=settings.RAG_TIMEOUT_SECONDS) as client:
            resp = await client.post(url, json=payload, headers=_headers())
            resp.raise_for_status()
            return resp.json()
    except Exception as exc:
        logger.warning("RAG call to %s failed: %s", url, exc)
        return None


async def get_diagnosis_context(
    complaint: str,
    examination: str,
    age: Optional[int],
    gender: Optional[str],
) -> Optional[str]:
    """
    Query the diagnosis RAG with a clinical presentation.

    Expected RAG response shape (adapt if yours differs):
    {
      "context": "<retrieved clinical text to inject into the LLM prompt>",
      "sources": ["<title>", ...]   // optional
    }

    Returns the retrieved context string, or None if RAG is not configured / call failed.
    The caller (ai_service.suggest_diagnosis) injects this as additional context in the
    LLM prompt so the model can cite specific guidelines when forming differentials.
    """
    if not settings.RAG_DIAGNOSIS_URL:
        return None

    payload = {
        "complaint": complaint,
        "examination": examination,
        "age": age,
        "gender": gender,
        "top_k": 5,
    }
    data = await _post(settings.RAG_DIAGNOSIS_URL, payload)
    if not data:
        return None

    context = data.get("context") or data.get("answer") or data.get("result")
    if not context:
        logger.warning("RAG diagnosis response missing 'context' key: %s", list(data.keys()))
    return context or None


async def get_consent_content(
    procedure: str,
    diagnosis: str,
    patient_age: Optional[int],
    patient_gender: Optional[str],
) -> Optional[dict]:
    """
    Query the consent RAG to retrieve procedure-specific clinical content.

    Expected RAG response shape (adapt if yours differs):
    {
      "risks": "...",
      "material_risks": "...",
      "possible_complications": "...",
      "alternatives": "...",
      "post_op_care": "...",
      "expected_recovery": "...",
      "procedure_description": "...",
      "applicable_guidelines": "...",   // e.g. "IAP Surgical Guidelines 2023 §4.2; NMC 2020"
      "sources": ["<title>", ...]
    }

    All keys are optional — only present ones overwrite the consent form defaults.
    Surgeon can always edit these on the frontend before the PDF is generated.
    Returns None if RAG is not configured / call failed.
    """
    if not settings.RAG_CONSENT_URL:
        return None

    payload = {
        "procedure": procedure,
        "diagnosis": diagnosis,
        "patient_age": patient_age,
        "patient_gender": patient_gender,
        "top_k": 8,
    }
    data = await _post(settings.RAG_CONSENT_URL, payload)
    if not data:
        return None

    # Strip unknown keys — only accept the fields we know how to use
    allowed = {
        "risks", "material_risks", "possible_complications", "alternatives",
        "post_op_care", "expected_recovery", "procedure_description",
        "applicable_guidelines",
    }
    result = {k: v for k, v in data.items() if k in allowed and v}
    return result or None
