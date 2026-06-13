import json
import logging
import os
from typing import Optional

from app.core.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


PEDIATRIC_SURGERY_PROMPT = """You are a pediatric surgery clinical decision support assistant.

Given the following pediatric patient presentation, provide a concise structured differential diagnosis and recommended workup. Do not give a definitive diagnosis or treatment plan. Emphasize that a qualified pediatric surgeon must examine the patient.

Patient age: {age} years
Gender: {gender}
Chief complaint: {complaint}
Examination findings: {examination}

Return ONLY a valid JSON object with these exact keys:
- differential_diagnosis: list of objects, each with {{"name": string, "reasoning": string}}
- recommended_investigations: list of strings
- confidence: a float between 0.0 and 0.90 (never above 0.90)
- disclaimer: a short string reminding that this is not medical advice

Be cautious and pediatric-surgery focused."""


async def suggest_diagnosis(
    complaint: str,
    examination: str,
    age: Optional[int],
    gender: Optional[str]
) -> dict:
    """Suggest a differential diagnosis for a pediatric surgical presentation."""

    prompt = PEDIATRIC_SURGERY_PROMPT.format(
        age=age if age is not None else "unknown",
        gender=gender if gender is not None else "unknown",
        complaint=complaint,
        examination=examination,
    )

    if settings.OPENAI_API_KEY:
        return await _call_openai(prompt)
    if settings.ANTHROPIC_API_KEY:
        return await _call_anthropic(prompt)

    logger.info("No AI API key configured; returning fallback suggestion")
    return _fallback_response()


async def _call_openai(prompt: str) -> dict:
    import openai

    client = openai.AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
    try:
        response = await client.chat.completions.create(
            model=settings.AI_MODEL,
            messages=[
                {"role": "system", "content": "You are a cautious pediatric surgery clinical assistant."},
                {"role": "user", "content": prompt},
            ],
            response_format={"type": "json_object"},
            temperature=0.2,
        )
        content = response.choices[0].message.content
        return _parse_and_cap(content, model_used=f"openai:{settings.AI_MODEL}")
    except Exception as e:
        logger.error(f"OpenAI error: {e}")
        return _fallback_response(model_used=f"openai:{settings.AI_MODEL}")


async def _call_anthropic(prompt: str) -> dict:
    import anthropic

    client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
    try:
        response = await client.messages.create(
            model="claude-3-5-sonnet-20240620",
            max_tokens=1024,
            temperature=0.2,
            system="You are a cautious pediatric surgery clinical assistant. Return only valid JSON.",
            messages=[{"role": "user", "content": prompt}],
        )
        content = response.content[0].text if response.content else ""
        return _parse_and_cap(content, model_used="anthropic:claude-3-5-sonnet-20240620")
    except Exception as e:
        logger.error(f"Anthropic error: {e}")
        return _fallback_response(model_used="anthropic:claude-3-5-sonnet-20240620")


def _parse_and_cap(content: str, model_used: str) -> dict:
    try:
        data = json.loads(content)
    except json.JSONDecodeError:
        logger.warning("AI response was not valid JSON; using fallback")
        return _fallback_response(model_used=model_used)

    confidence = min(float(data.get("confidence", 0.5)), 0.90)
    data["confidence"] = confidence
    data["model_used"] = model_used
    if "disclaimer" not in data:
        data["disclaimer"] = "This is a clinical decision support suggestion, not a diagnosis. Consult a pediatric surgeon."
    return data


def _fallback_response(model_used: str = "fallback") -> dict:
    return {
        "differential_diagnosis": [
            {"name": "Differential not available offline", "reasoning": "Please review clinically."}
        ],
        "recommended_investigations": ["Clinical examination by pediatric surgeon"],
        "confidence": 0.0,
        "disclaimer": "AI service is not configured. This is not medical advice. Consult a pediatric surgeon.",
        "model_used": model_used,
    }
