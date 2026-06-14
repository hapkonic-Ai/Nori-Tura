# AI Suggestive Diagnosis Agent

## Overview

The **AI Suggestive Diagnosis** module provides ranked differential diagnosis suggestions to surgeons during the OPD consultation flow. It is an **assistive tool only** — the final diagnosis and treatment decisions are always the surgeon's responsibility.

The AI agent is triggered from the **OPD Consult Form (S3)** after the surgeon enters the chief complaint and examination findings. It calls a backend LLM service and returns 3–5 ranked possible diagnoses with confidence scores and suggested investigations.

---

## User Flow

1. Surgeon opens OPD Consult Form (S3).
2. Enters **Chief Complaint** and **Examination** fields (each ≥ 10 characters).
3. A collapsible **"AI Suggestive Diagnosis"** panel appears below the Examination field.
4. Backend sends a structured prompt to the LLM.
5. LLM returns ranked diagnoses.
6. UI displays suggestions with confidence percentages and suggested investigations.
7. Surgeon can:
   - Tap a suggestion to auto-fill the **Diagnosis** field.
   - Tap "Refresh Suggestions" to re-run the query.
   - Ignore the panel entirely.
8. On save, the OPD record is stored, and the AI interaction is logged in `ai_diagnosis_logs`.

---

## Database Schema

### `ai_diagnosis_logs`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique log identifier |
| opd_record_id | UUID (FK → opd_records) | Associated OPD record |
| doctor_id | UUID (FK → doctors) | Surgeon who requested suggestion |
| complaint | String | Chief complaint input |
| examination | String | Examination findings input |
| age | Int | Patient age at time of request |
| gender | String | Patient gender |
| suggestions_json | JSON | LLM response: ranked diagnoses, confidence, investigations |
| selected_diagnosis | String? | Diagnosis the surgeon chose (if any) |
| model_used | String | LLM model name (e.g., "gpt-4o", "claude-3-sonnet") |
| created_at | DateTime | Request timestamp |

---

## Backend Implementation

### Dependencies

```python
# backend/requirements.txt
openai>=1.30.0
# OR
anthropic>=0.28.0
```

### Environment Variables

```bash
OPENAI_API_KEY=sk-...
# OR
ANTHROPIC_API_KEY=sk-ant-...
AI_MODEL=gpt-4o  # or claude-3-sonnet-20240229
```

### API Endpoint

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/ai/suggest-diagnosis` | JWT (surgeon or nurse) | Returns ranked differential diagnoses based on clinical input |

### Request Schema

```json
{
  "opd_record_id": "uuid",
  "complaint": "Abdominal pain and vomiting for 2 days",
  "examination": "Tender right lower quadrant, rebound tenderness present, low-grade fever",
  "age": 8,
  "gender": "male"
}
```

### Response Schema

```json
{
  "suggestions": [
    {
      "rank": 1,
      "diagnosis": "Acute Appendicitis",
      "confidence": 0.85,
      "rationale": "Classic presentation of right lower quadrant pain, rebound tenderness, fever, and vomiting in a child.",
      "suggested_investigations": ["CBC", "CRP", "Ultrasound Abdomen", "Urinalysis"],
      "red_flags": ["Peritonitis", "Perforation"]
    },
    {
      "rank": 2,
      "diagnosis": "Mesenteric Adenitis",
      "confidence": 0.60,
      "rationale": "Can mimic appendicitis; often follows upper respiratory infection.",
      "suggested_investigations": ["CBC", "Ultrasound Abdomen"],
      "red_flags": []
    },
    {
      "rank": 3,
      "diagnosis": "Intestinal Obstruction",
      "confidence": 0.35,
      "rationale": "Vomiting with abdominal pain may indicate obstruction, though focal tenderness is less typical.",
      "suggested_investigations": ["X-Ray Abdomen Erect", "Ultrasound Abdomen"],
      "red_flags": ["Bilious vomiting", "Distension"]
    }
  ],
  "disclaimer": "This is an AI-generated suggestion. Final diagnosis is the surgeon's responsibility.",
  "model_used": "gpt-4o",
  "generated_at": "2026-06-10T10:00:00Z"
}
```

### Backend Logic

```python
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import json
import os
from openai import OpenAI

router = APIRouter(prefix="/ai", tags=["AI Diagnosis"])

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

class SuggestDiagnosisRequest(BaseModel):
    opd_record_id: str
    complaint: str
    examination: str
    age: int
    gender: str

class DiagnosisSuggestion(BaseModel):
    rank: int
    diagnosis: str
    confidence: float
    rationale: str
    suggested_investigations: List[str]
    red_flags: List[str]

class SuggestDiagnosisResponse(BaseModel):
    suggestions: List[DiagnosisSuggestion]
    disclaimer: str
    model_used: str
    generated_at: str

@router.post("/suggest-diagnosis", response_model=SuggestDiagnosisResponse)
async def suggest_diagnosis(
    req: SuggestDiagnosisRequest,
    user=Depends(get_current_user)  # resolves doctor_id from JWT
):
    # Optional: verify opd_record belongs to doctor
    # opd = await prisma.opd_records.find_first(where={"id": req.opd_record_id, "doctor_id": user.doctor_id})
    # if not opd: raise HTTPException(404)

    prompt = build_diagnosis_prompt(req.complaint, req.examination, req.age, req.gender)
    
    response = client.chat.completions.create(
        model=os.getenv("AI_MODEL", "gpt-4o"),
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt}
        ],
        response_format={"type": "json_object"},
        temperature=0.3,
        max_tokens=800
    )

    result = json.loads(response.choices[0].message.content)
    
    # Enforce confidence cap and safety rules
    result = sanitize_suggestions(result)
    
    # Log to ai_diagnosis_logs
    await prisma.ai_diagnosis_logs.create(data={
        "opd_record_id": req.opd_record_id,
        "doctor_id": user.doctor_id,
        "complaint": req.complaint,
        "examination": req.examination,
        "age": req.age,
        "gender": req.gender,
        "suggestions_json": result["suggestions"],
        "model_used": os.getenv("AI_MODEL", "gpt-4o"),
    })

    return SuggestDiagnosisResponse(
        suggestions=result["suggestions"],
        disclaimer="This is an AI-generated suggestion. Final diagnosis is the surgeon's responsibility.",
        model_used=os.getenv("AI_MODEL", "gpt-4o"),
        generated_at=datetime.utcnow().isoformat()
    )
```

---

## LLM System Prompt

```text
You are a clinical decision-support assistant for pediatric surgeons in India.
Your role is to suggest possible differential diagnoses based on the provided chief complaint and examination findings.

IMPORTANT RULES:
1. You are NOT making a definitive diagnosis. You are providing SUGGESTIONS only.
2. Output must be valid JSON with the exact structure requested.
3. Provide 3 to 5 ranked differential diagnoses.
4. Confidence scores must be between 0.0 and 0.90 (never 100% confident).
5. Include a brief rationale for each suggestion.
6. Include 2-5 suggested investigations for each diagnosis.
7. Include relevant red flags (if any) that would require urgent attention.
8. Do NOT suggest treatment plans or prescriptions.
9. If the input is too vague or insufficient, return a single suggestion with diagnosis "Insufficient Information" and confidence 0.0, asking for more clinical details.
10. All suggestions must be appropriate for the pediatric population.
11. Avoid rare/esoteric conditions unless strongly indicated by the clinical picture.

Output format:
{
  "suggestions": [
    {
      "rank": 1,
      "diagnosis": "string",
      "confidence": 0.0-0.9,
      "rationale": "string",
      "suggested_investigations": ["string"],
      "red_flags": ["string"]
    }
  ]
}
```

### User Prompt Template

```text
Chief Complaint: {complaint}
Examination Findings: {examination}
Patient Age: {age} years
Patient Gender: {gender}

Based on the above information, provide ranked differential diagnoses in the required JSON format.
```

---

## Safety & Sanitization Rules

Backend must enforce these rules regardless of LLM output:

1. **Confidence Cap:** Any suggestion with confidence > 0.90 is capped at 0.90.
2. **Minimum Input Length:** If complaint or examination is < 10 characters, return error: "Please provide more detailed clinical information."
3. **No Treatment Advice:** Filter out any suggestion that includes medication names or treatment plans.
4. **Pediatric Scope:** If age > 18, return error: "AI diagnosis support is limited to pediatric patients (0-18 years)."
5. **No Definitive Language:** Replace phrases like "The diagnosis is" with "A possible diagnosis is" in rationale.
6. **Insistent Disclaimer:** Every response must include the disclaimer.
7. **PII Scrubbing:** Ensure no patient names, phone numbers, or addresses are sent to the LLM API.

---

## KMM UI Specification

### AI Diagnosis Panel (in OPD Consult Form S3)

**Placement:** Below the "Examination" field, above the "Diagnosis" field.

**Collapsed State:**
- Card with title "AI Suggestive Diagnosis" and a chevron icon.
- Subtitle: "Tap to get AI-powered differential diagnoses"
- Only visible when `complaint.length >= 10 && examination.length >= 10`

**Expanded State:**
- Loading shimmer while fetching suggestions.
- List of suggestion cards:
  - **Rank badge** (e.g., "#1")
  - **Diagnosis name** (bold)
  - **Confidence bar** (horizontal progress bar, color-coded: green < 0.5, amber 0.5-0.75, red > 0.75)
  - **Rationale** (collapsed by default, expandable)
  - **Suggested investigations** (chips)
  - **Red flags** (red text, if any)
  - **"Use This" button** — auto-fills the Diagnosis field and appends rationale to Notes
- "Refresh Suggestions" text button at the bottom.
- Disclaimer footer in small gray text.

**Error State:**
- If backend returns error, show: "Unable to generate suggestions. Please try again."
- If input insufficient, show: "Please provide more detailed complaint and examination findings."

### Compose Implementation Notes

```kotlin
// Shared/commonMain
@Composable
fun AiDiagnosisPanel(
    complaint: String,
    examination: String,
    age: Int,
    gender: String,
    opdRecordId: String,
    onDiagnosisSelected: (String) -> Unit
) {
    if (complaint.length < 10 || examination.length < 10) return
    
    // State management via ViewModel
    // Trigger API call when expanded
    // Display suggestions
}
```

---

## Audit & Logging

Every AI suggestion request must be logged in `ai_diagnosis_logs`:

- Input data (complaint, examination, age, gender)
- Output data (suggestions_json)
- Doctor who requested it
- Associated OPD record
- Model used
- Timestamp

This log is for:
- Clinical audit trails
- Quality improvement
- Debugging LLM behavior
- Compliance with Indian healthcare AI guidelines

---

## Testing Checklist

- [ ] AI panel appears only when complaint + examination are sufficiently detailed
- [ ] Suggestions display with confidence bars and disclaimers
- [ ] Tapping "Use This" auto-fills Diagnosis field
- [ ] Refresh button re-fetches suggestions
- [ ] Backend rejects vague inputs
- [ ] Confidence values never exceed 0.90
- [ ] No PII sent to LLM API
- [ ] Audit logs created for every request
- [ ] Works on Android, iOS, and Web
