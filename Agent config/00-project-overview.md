# 00 — Project Overview

## Noni Tura Surgical Care Platform

**Client:** 2 Pediatric Surgeons, India  
**Build Tool / AI Assistant:** Claude Opus (Vibe Coding)  
**Total Duration:** 10 Days MVP  
**Total Screens:** 30 (13 Surgeon + 6 Nurse + 11 Parent)

---

## 1. What is Noni Tura?

Noni Tura is a specialized surgical care platform designed for pediatric surgeons in India. The platform enables surgeons and their nursing staff to manage the complete patient workflow — from OPD consultations through surgical procedures to discharge — while giving parents real-time visibility into their child's surgical journey.

The application is built using **Kotlin Multiplatform Mobile (KMM)** with **Compose Multiplatform**, enabling a single shared codebase for UI, business logic, networking, and local storage across **Android**, **iOS**, and **Web (Wasm)** targets.

---

## 2. Technology Stack

### Frontend — KMM + Compose Multiplatform

| Component | Technology | Version |
|---|---|---|
| Framework | KMM (Kotlin Multiplatform Mobile) | — |
| UI | Compose Multiplatform | 1.7.0 |
| Kotlin | Kotlin / KGP | 2.0.21 |
| Gradle | Gradle Wrapper | 8.10.2 |
| AGP | Android Gradle Plugin | 8.7.3 |
| Navigation | Voyager | (catalog) |
| Dependency Injection | Koin | (catalog) |
| Networking | Ktor Client | (catalog) |
| Local Storage / Caching | SQLDelight | (catalog) |
| Serialization | kotlinx.serialization | (catalog) |
| Image Loading | Coil Multiplatform | (catalog) |

### Backend — FastAPI (Python)

| Component | Technology |
|---|---|
| Framework | FastAPI (Python) |
| Database | Neon PostgreSQL |
| ORM | Prisma Client Python |
| Authentication | Phone OTP (2Factor.in or MSG91) |
| WhatsApp | Meta Cloud API (direct) |
| Push Notifications | Firebase Cloud Messaging (Android) + APNs via Firebase (iOS) |
| AI Diagnosis | OpenAI GPT-4 / Anthropic Claude API |
| PDF Generation | reportlab or weasyprint |

### Hosting & Distribution

| Target | Platform |
|---|---|
| Android App | Google Play Store |
| iOS App | Apple App Store |
| Web App | Vercel (Wasm static export) |
| Backend API | Render |
| File Storage | Cloudinary (documents, scans, consent forms) |

### KMM Module Structure

```
shared/
  ├── commonMain/       # Shared UI, business logic, networking, navigation
  ├── androidMain/      # Android-specific implementations
  ├── iosMain/          # iOS-specific implementations
  └── webMain/          # Web/Wasm-specific implementations
androidApp/             # Android application entry point
iosApp/                 # iOS application entry point (Xcode project)
webApp/                 # Web/Wasm application entry point
backend/                # FastAPI backend
prisma/
  └── schema.prisma     # Database schema
```

---

## 3. Roles & Constraints

### Roles

| Role | Description | Count |
|---|---|---|
| **Surgeon** | Pediatric surgeon who manages patients, OPD consults, IPD admissions, surgical workflows, schedules, and surgical templates. Has full CRUD access to their own patient pool. | 2 |
| **Nurse** | Nursing staff assigned to a surgeon/hospital. Can add patients, create OPD records, add clinical notes, manage appointments, view schedules, upload documents. Cannot edit surgical templates, sign discharge summaries, or create intra-op notes. | 3 (current) |
| **Patient/Parent** | Parent of the pediatric patient. Can view surgery status timeline, OPD records, download PDFs, book appointments, view consent forms, and manage notification preferences. Read-only access to their own child's records. | — |

### Role Detection Logic

```
IF phone EXISTS in doctors table → role = "surgeon"
ELSE IF phone EXISTS in nurses table → role = "nurse"
ELSE → role = "patient_parent"
```

### Constraints

| Constraint | Detail |
|---|---|
| Number of Surgeons | 2 |
| Number of Nurses | 3 (initially) |
| Patient Pools | Separate per surgeon (isolated via `doctor_id`) |
| Nurse Pool Isolation | Nurses are scoped to a single `doctor_id` |
| Initial Patient Count | < 100 |
| Delivery Timeline | 10 days MVP |
| Native Apps | Android + iOS + Web via KMM shared logic |

---

## 4. Database Schema Summary

### Core Tables (from original plan)

- `doctors` — Surgeon profiles
- `patients` — Patient demographics (scoped by `doctor_id`)
- `opd_records` — OPD consultation notes (scoped by `doctor_id`)
- `medications` — Medications linked to OPD records
- `investigations` — Investigations linked to OPD records
- `ipd_admissions` — IPD admissions (scoped by `doctor_id`)
- `pre_op_notes` — Pre-operative assessment
- `intra_op_notes` — Intra-operative records
- `post_op_notes` — Post-operative daily notes
- `ward_round_notes` — SOAP-format ward rounds
- `discharge_summaries` — Discharge documentation
- `appointments` — OPD/OT bookings (scoped by `doctor_id`)
- `documents` — File uploads (Cloudinary URLs)
- `whatsapp_logs` — WhatsApp message audit trail
- `otp_sessions` — OTP verification sessions

### NEW Tables

#### `nurses`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique nurse identifier |
| doctor_id | UUID (FK → doctors) | Assigned surgeon — enforces pool isolation |
| name | String | Nurse's full name |
| phone | String | Registered phone number (used for OTP login) |
| hospital | String | Hospital affiliation |
| fcm_token | String? | Firebase Cloud Messaging device token |
| platform | String? | Device platform (android/ios/web) |
| created_at | DateTime | Account creation timestamp |

#### `consent_forms`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique consent form identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| patient_id | UUID (FK → patients) | Associated patient |
| doctor_id | UUID (FK → doctors) | Owning surgeon |
| form_type | String | Type of consent (surgical/anesthesia/blood-transfusion/etc) |
| content_json | JSON | Structured form content (procedure, risks, alternatives, declarations) |
| pdf_url | String? | Cloudinary URL of generated PDF |
| parent_signature_url | String? | Cloudinary URL of signed document |
| witness_name | String? | Name of witness |
| generated_at | DateTime | Form generation timestamp |
| signed_at | DateTime? | Parent/guardian signing timestamp |

#### `ai_diagnosis_logs`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique log identifier |
| opd_record_id | UUID (FK → opd_records) | Associated OPD record |
| doctor_id | UUID (FK → doctors) | Surgeon who requested suggestion |
| complaint | String | Chief complaint input |
| examination | String | Examination findings input |
| age | Int | Patient age at time of request |
| gender | String | Patient gender |
| suggestions_json | JSON | LLM response (ranked diagnoses + confidence + suggested investigations) |
| selected_diagnosis | String? | Diagnosis the surgeon chose (if any) |
| model_used | String | LLM model name (e.g., "gpt-4", "claude-3-sonnet") |
| created_at | DateTime | Request timestamp |

---

## 5. Nurse Permissions Matrix

| Action | Surgeon | Nurse | Parent |
|---|---|---|---|
| Add Patient | ✅ | ✅ | ❌ |
| Edit Patient | ✅ | ✅ (limited) | ❌ |
| Create OPD Record | ✅ | ✅ | ❌ |
| Edit OPD Record | ✅ | ✅ (own entries) | ❌ |
| Surgical Decision | ✅ | ❌ | ❌ |
| AI Diagnosis Suggestion | ✅ | ✅ (view only) | ❌ |
| Create Admission | ✅ | ✅ (no surgical decision) | ❌ |
| Pre-op Notes | ✅ | ❌ | ❌ |
| Intra-op Notes | ✅ | ❌ | ❌ |
| Post-op Notes | ✅ | ❌ | ❌ |
| Ward Round Notes | ✅ | ✅ | ❌ |
| Discharge Summary | ✅ | ❌ | ❌ |
| Manage Appointments | ✅ | ✅ | ❌ |
| View Schedule | ✅ | ✅ | ❌ |
| Surgical Templates | ✅ (CRUD) | ❌ (read-only) | ❌ |
| Consent Forms | ✅ (generate/sign) | ✅ (generate, no sign) | ✅ (view/sign own) |
| Upload Documents | ✅ | ✅ | ❌ |
| View Patient Records | ✅ (own pool) | ✅ (own pool) | ✅ (own child only) |
| Book Appointment | ✅ | ✅ (on behalf of parent) | ✅ |
| Edit Profile | ✅ | ✅ (own) | ✅ (own) |
| Manage Nurses | ✅ | ❌ | ❌ |

---

## 6. Screen Map

### Surgeon Screens (S1–S13)
- S1 — Surgeon Dashboard
- S2 — Patient List
- S3 — OPD Consult Form (with AI Diagnosis Panel)
- S4 — Admit Patient
- S5 — Pre-op Notes
- S6 — Intra-op Notes
- S7 — Post-op Notes
- S8 — Daily Ward Round
- S9 — Discharge Summary
- S10 — Patient Profile
- S11 — OT & OPD Schedule
- S12 — WhatsApp Preview
- S13 — Surgeon Profile & Settings

### Nurse Screens (N1–N6)
- N1 — Nurse Dashboard
- N2 — Nurse OPD Form (no surgical decision)
- N3 — Nurse Patient List
- N4 — Nurse Clinical Notes (SOAP)
- N5 — Nurse Appointment Manager
- N6 — Nurse Schedule Viewer

### Parent Screens (P1–P11)
- P1 — OTP Login (shared)
- P2 — OTP Verify (shared)
- P3 — Parent Home
- P4 — Surgery Status Detail
- P5 — OPD Records List
- P6 — Consult Record Detail
- P7 — Step 1: Select Surgeon
- P8 — Step 2: Select Slot
- P9 — Step 3: Confirm Booking
- P10 — Booking Confirmed
- P11 — Parent Profile & Settings

---

## 7. Security Architecture

### Doctor Pool Isolation

The fundamental security architecture is built on **doctor pool isolation**. This ensures that each surgeon's patient data is completely siloed and inaccessible to the other surgeon. Nurses are also scoped to a single `doctor_id`.

**Implementation:**
- Every patient-related table includes a `doctor_id` foreign key pointing to the `doctors` table.
- All API endpoints extract the `doctor_id` (or `nurse_id` → resolved to `doctor_id`) from the JWT token and scope all database queries accordingly.
- Any attempt to access a resource belonging to another surgeon returns a 404 Not Found (never a data-leaking 403).
- No client-side filtering is relied upon for security — all isolation is enforced server-side.

**Tables with `doctor_id` foreign key:**
- `patients`
- `opd_records`
- `ipd_admissions`
- `appointments`
- `consent_forms`
- `nurses`

---

## 8. Compliance Notes

### Indian Healthcare Standards
- **Consent Forms:** Must comply with MCI (Medical Council of India) and NABH (National Accreditation Board for Hospitals) standards.
- **Data Privacy:** Patient data is sensitive personal information under Indian IT Act. No data leaves Indian jurisdiction without explicit consent.
- **WhatsApp Healthcare Messaging:** Meta Cloud API templates must be purely transactional, include opt-out lines, and comply with healthcare messaging policies.

### AI Diagnosis Disclaimer
- All AI-generated diagnosis suggestions must carry a clear disclaimer: "This is an AI-generated suggestion. Final diagnosis is the surgeon's responsibility."
- AI suggestions must be logged in `ai_diagnosis_logs` for audit purposes.
- No AI suggestion should be persisted as a definitive diagnosis without explicit surgeon confirmation.
