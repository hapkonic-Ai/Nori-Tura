# Consent Form Generation Module

## Overview

The **Consent Form Generation** module creates surgery-specific, legally compliant consent forms for pediatric surgical procedures in India. The forms adhere to **Medical Council of India (MCI)** and **National Accreditation Board for Hospitals (NABH)** standards.

Consent forms are generated:
- During **Admit Patient (S4)** for urgent/emergency admissions
- During **Pre-op Notes (S5)** for planned/elective surgeries
- On-demand from **Patient Profile → Documents Tab (S10)**

Parents/guardians review and digitally sign the consent form before surgery.

---

## Regulatory Context (India)

Indian surgical consent requirements are governed by:
- **Medical Council of India (MCI)** Code of Ethics Regulations
- **NABH Standards** (National Accreditation Board for Hospitals)
- **Indian Contract Act, 1872**
- **Consumer Protection Act, 2019** (medical negligence provisions)

### Key Consent Requirements

1. **Informed Consent** — Patient/guardian must be informed of:
   - Nature of the proposed procedure
   - Risks and complications
   - Alternative treatment options
   - Consequences of refusing treatment

2. **Competent Person** — For pediatric patients, consent must be obtained from a parent or legal guardian.

3. **Voluntary Consent** — Consent must be given without coercion.

4. **Documented Consent** — Written consent is mandatory for surgical procedures.

5. **Witness** — A witness signature is recommended (and often required by hospitals).

6. **Language** — Consent should be in a language the guardian understands. For MVP, forms are in English with option to add Hindi/Marathi/Tamil etc. later.

---

## Database Schema

### `consent_forms`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique consent form identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| patient_id | UUID (FK → patients) | Associated patient |
| doctor_id | UUID (FK → doctors) | Owning surgeon |
| form_type | String | Type: `surgical`, `anesthesia`, `blood_transfusion`, `photography`, `research` |
| content_json | JSON | Structured form content |
| pdf_url | String? | Cloudinary URL of generated unsigned PDF |
| signed_pdf_url | String? | Cloudinary URL of signed PDF |
| parent_signature_url | String? | Cloudinary URL of captured signature image |
| witness_name | String? | Name of witness |
| witness_signature_url | String? | Cloudinary URL of witness signature image |
| generated_at | DateTime | Form generation timestamp |
| signed_at | DateTime? | Parent/guardian signing timestamp |
| generated_by | String | `surgeon` or `nurse` |
| status | String | `pending`, `signed`, `expired`, `superseded` |

### `consent_form_templates`

Optional table for reusable consent form language per procedure type.

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique template identifier |
| doctor_id | UUID? (FK → doctors) | Owner (null for global templates) |
| procedure_type | String | e.g., "herniotomy", "appendectomy", "general" |
| form_type | String | `surgical`, `anesthesia`, etc. |
| template_text | String | HTML/markdown template with placeholders |

---

## Consent Form Content Structure

The `content_json` field stores:

```json
{
  "hospital_name": "ABC Children's Hospital",
  "hospital_address": "Mumbai, Maharashtra",
  "date": "2026-06-10",
  "patient": {
    "name": "Rohan Sharma",
    "age": "7 years",
    "gender": "Male",
    "uhid": "NT-2026-001"
  },
  "guardian": {
    "name": "Mr. Rajesh Sharma",
    "relationship": "Father",
    "phone": "+919876543210"
  },
  "doctor": {
    "name": "Dr. Priya Mehta",
    "qualification": "M.Ch. Pediatric Surgery",
    "registration_number": "MMC-12345"
  },
  "procedure": {
    "name": "Inguinal Herniotomy",
    "description": "Surgical repair of inguinal hernia via small incision in the groin",
    "approach": "Open",
    "anesthesia": "General Anesthesia",
    "estimated_duration": "45-60 minutes"
  },
  "risks": [
    "Bleeding requiring transfusion",
    "Infection at surgical site",
    "Injury to surrounding structures (vas deferens, vessels)",
    "Recurrence of hernia",
    "Anesthesia-related complications",
    "Scar formation"
  ],
  "alternatives": [
    "Observation (for asymptomatic hernia)",
    "Laparoscopic repair",
    "Delayed surgery"
  ],
  "consequences_of_refusal": [
    "Risk of hernia incarceration",
    "Risk of strangulation requiring emergency surgery",
    "Potential bowel damage"
  ],
  "special_instructions": [
    "Bilateral exploration may be performed if indicated",
    "Post-operative pain management will be provided"
  ]
}
```

---

## Consent Form PDF Layout

### Header Section
- Hospital name and address
- Form title: "INFORMED CONSENT FOR SURGICAL PROCEDURE"
- Form date and reference number

### Patient & Guardian Details
- Patient name, age, gender, UHID
- Guardian name, relationship, phone number

### Procedure Details
- Proposed procedure name
- Description in plain language
- Type of anesthesia
- Expected duration

### Doctor Declaration
> I, Dr. [Name], have explained to the patient/guardian the nature of the proposed procedure, the possible risks and complications, the available alternatives, and the consequences of refusing treatment. I have answered all questions to the best of my ability.

### Risks & Complications
- Bulleted list of common and serious risks

### Alternative Treatments
- Bulleted list

### Consent Statement
> I, [Guardian Name], parent/legal guardian of [Patient Name], have read and understood the information provided above. I have had the opportunity to ask questions and all my questions have been answered satisfactorily. I voluntarily consent to the proposed surgical procedure and anesthesia.

### Signature Blocks
- Parent/Guardian signature
- Witness signature
- Doctor signature
- Date and time

### Footer
- Disclaimer: "This consent form complies with MCI and NABH standards for informed consent."
- Page number

---

## Backend Implementation

### Dependencies

```python
# backend/requirements.txt
weasyprint>=62.0
# OR
reportlab>=4.0
jinja2>=3.1.0
```

### API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/consent/generate` | JWT (surgeon or nurse) | Generates a new consent form and returns PDF URL |
| GET | `/consent/:id` | JWT (surgeon/nurse/parent of patient) | Returns consent form details |
| GET | `/consent/:id/pdf` | JWT (surgeon/nurse/parent of patient) | Returns generated PDF |
| POST | `/consent/:id/sign` | JWT (parent of patient) | Submits digital signature and witness name, generates signed PDF |

### Request: POST /consent/generate

```json
{
  "admission_id": "uuid",
  "patient_id": "uuid",
  "form_type": "surgical",
  "witness_name": "Mrs. Sunita Sharma"
}
```

### Response: POST /consent/generate

```json
{
  "id": "uuid",
  "admission_id": "uuid",
  "patient_id": "uuid",
  "form_type": "surgical",
  "pdf_url": "https://res.cloudinary.com/.../consent-unsigned.pdf",
  "status": "pending",
  "generated_at": "2026-06-10T10:00:00Z"
}
```

### Request: POST /consent/:id/sign

```json
{
  "signature_image_base64": "data:image/png;base64,iVBORw0KGgo...",
  "witness_name": "Mrs. Sunita Sharma",
  "witness_signature_base64": "data:image/png;base64,iVBORw0KGgo..."
}
```

### Response: POST /consent/:id/sign

```json
{
  "id": "uuid",
  "status": "signed",
  "signed_pdf_url": "https://res.cloudinary.com/.../consent-signed.pdf",
  "signed_at": "2026-06-10T10:30:00Z"
}
```

### Backend Logic (Pseudocode)

```python
@router.post("/consent/generate")
async def generate_consent(
    req: GenerateConsentRequest,
    user=Depends(get_current_user)
):
    # Verify admission/patient belongs to user's doctor_id
    admission = await prisma.ipd_admissions.find_first(
        where={"id": req.admission_id, "doctor_id": user.doctor_id}
    )
    if not admission:
        raise HTTPException(404)

    patient = await prisma.patients.find_first(
        where={"id": req.patient_id, "doctor_id": user.doctor_id}
    )
    doctor = await prisma.doctors.find_first(where={"id": user.doctor_id})

    # Build content_json
    content = build_consent_content(patient, doctor, admission, req.form_type)

    # Generate HTML from Jinja2 template
    html = render_consent_template(content)

    # Convert HTML to PDF
    pdf_bytes = weasyprint.HTML(string=html).write_pdf()

    # Upload to Cloudinary
    pdf_url = await upload_to_cloudinary(pdf_bytes, f"consent-{uuid}.pdf")

    # Create consent_forms record
    consent = await prisma.consent_forms.create(data={
        "admission_id": req.admission_id,
        "patient_id": req.patient_id,
        "doctor_id": user.doctor_id,
        "form_type": req.form_type,
        "content_json": content,
        "pdf_url": pdf_url,
        "status": "pending",
        "generated_by": user.role,  # "surgeon" or "nurse"
        "witness_name": req.witness_name
    })

    return consent


@router.post("/consent/{consent_id}/sign")
async def sign_consent(
    consent_id: str,
    req: SignConsentRequest,
    user=Depends(get_current_parent_user)
):
    consent = await prisma.consent_forms.find_first(
        where={"id": consent_id, "patient_id": user.patient_id}
    )
    if not consent:
        raise HTTPException(404)

    # Upload signature images to Cloudinary
    signature_url = await upload_base64_image(req.signature_image_base64)
    witness_url = await upload_base64_image(req.witness_signature_base64) if req.witness_signature_base64 else None

    # Generate signed PDF (overlay signatures on unsigned PDF or re-render)
    signed_pdf_url = await generate_signed_pdf(consent, signature_url, witness_url)

    # Update consent record
    updated = await prisma.consent_forms.update(
        where={"id": consent_id},
        data={
            "parent_signature_url": signature_url,
            "witness_signature_url": witness_url,
            "signed_pdf_url": signed_pdf_url,
            "signed_at": datetime.utcnow(),
            "status": "signed"
        }
    )

    return updated
```

---

## Permission Matrix for Consent Forms

| Action | Surgeon | Nurse | Parent |
|---|---|---|---|
| Generate consent form | ✅ | ✅ | ❌ |
| View consent form PDF | ✅ | ✅ | ✅ (own child only) |
| Sign consent form | ❌ | ❌ | ✅ (own child only) |
| Download all consent forms | ✅ | ❌ | ✅ (own child only) |
| Revoke/supersede consent form | ✅ | ❌ | ❌ |

---

## KMM UI Specification

### Consent Form Preview Screen (CF1)

**Trigger:**
- From S4 (Admit Patient) for urgent/emergency cases
- From S5 (Pre-op Notes) via "Generate Consent Form" button
- From S10 Patient Profile → Documents Tab → "Generate Consent Form" FAB

**UI Components:**

- **PDF Viewer:** Renders the generated PDF inline (using platform-specific PDF viewers).
- **Procedure Summary Card:** Shows procedure name, anesthesia, risks count.
- **Guardian Details Card:** Pre-filled from patient record; editable if needed.
- **Witness Name Input:** Text field for witness name.
- **"Generate PDF" Button:** Generates and uploads consent form.
- **"Share PDF" Button:** Opens native share intent with PDF URL.
- **"Done" Button:** Returns to previous screen.

### Parent Consent Signing Screen (CF2)

**Trigger:**
- From P3 (Parent Home) consent status card
- From P11 (Parent Profile) consent form history
- From P4 (Surgery Status Detail) "View Consent Form" button

**UI Components:**

- **PDF Viewer:** Shows unsigned consent form.
- **Signature Pad:** Full-width canvas for parent/guardian to sign with finger/stylus/mouse.
- **Witness Name Input:** Required.
- **Witness Signature Pad:** Optional but recommended.
- **"I have read and understood" Checkbox:** Required before signing.
- **"Sign Consent Form" Button:** Enabled only after checkbox checked and signature captured.
- **Success Animation:** After signing, shows green checkmark and signed PDF viewer.

### Compose Implementation Notes

```kotlin
// Shared/commonMain
@Composable
fun ConsentFormPreviewScreen(
    admissionId: String,
    patientId: String,
    onGenerate: () -> Unit
) {
    // State: pdfUrl, witnessName, isGenerating
    // Use platform-specific PDF viewer via expect/actual
}

@Composable
fun ConsentSigningScreen(
    consentFormId: String,
    onSigned: () -> Unit
) {
    // Signature pad via platform canvas
    // Checkbox for acknowledgment
    // Submit signature image as base64
}
```

---

## PDF Generation Options

### Option A: Jinja2 + WeasyPrint (Recommended)

Pros:
- HTML/CSS styling is easy
- Good typography control
- Supports page breaks, headers, footers

Cons:
- Larger dependency footprint
- May need system fonts for Indian languages

### Option B: ReportLab

Pros:
- Pure Python
- More control over PDF structure

Cons:
- More verbose
- Harder to style

**Recommendation:** Use **Jinja2 + WeasyPrint** for MVP because it allows rapid template iteration and clean output.

---

## Compliance Checklist

Every generated consent form must include:

- [ ] Hospital name and address
- [ ] Date and reference number
- [ ] Patient name, age, gender, UHID
- [ ] Guardian name, relationship, phone
- [ ] Doctor name, qualification, registration number
- [ ] Proposed procedure name and plain-language description
- [ ] Anesthesia type
- [ ] Risks and complications (common + serious)
- [ ] Alternative treatments discussed
- [ ] Consequences of refusing treatment
- [ ] Doctor declaration
- [ ] Guardian consent statement
- [ ] Signature blocks for guardian, witness, doctor
- [ ] MCI/NABH compliance footer
- [ ] Immutable after signing

---

## Testing Checklist

- [ ] Consent form generates successfully from S4, S5, and S10
- [ ] PDF contains all required fields
- [ ] Parent can sign digitally
- [ ] Signed PDF contains signature overlay
- [ ] Nurse can generate but cannot sign
- [ ] Parent cannot generate
- [ ] Cross-patient consent forms are inaccessible
- [ ] Consent form status updates correctly (pending → signed)
- [ ] Works on Android, iOS, and Web
