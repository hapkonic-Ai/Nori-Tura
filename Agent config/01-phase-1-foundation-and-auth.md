# Phase 1 — KMM Foundation & Auth

**Goal:** KMM project scaffolding, Compose UI setup for 3 targets, full database schema (including new tables), OTP login with three-way role detection  
**Duration:** 1.5 days  
**Owner:** All 3 devs  
**Screens:** P1 — OTP Login, P2 — OTP Verify  

---

## Task 1.1 — KMM Repo & Project Setup

**Description:** Initialize KMM project with Compose Multiplatform. Setup modules: `shared` (commonMain, androidMain, iosMain, webMain), `androidApp`, `iosApp`, `webApp`. Configure Koin for DI, Ktor for networking, SQLDelight for local caching, and Voyager for navigation. Initialize FastAPI backend, connect Neon PostgreSQL via Prisma.

**Files to Create:**

- `shared/build.gradle.kts` — Shared module Gradle configuration with Compose Multiplatform, Koin, Ktor, SQLDelight, Voyager dependencies
- `androidApp/` — Android application entry point with Compose Activity
- `iosApp/` — iOS application entry point (Xcode project referencing shared framework)
- `webApp/` — Web/Wasm application entry point with Compose for Web
- `backend/` — FastAPI project structure with Prisma client initialization
- `prisma/schema.prisma` — Full database schema (defined in Task 1.2)

**Acceptance Criteria:**

- Android, iOS, and Web (Wasm) apps each render a "Hello World" screen natively
- Backend deploys to Render with a health-check endpoint (`GET /health` returns 200)
- All KMM module targets compile and run without errors

---

## Task 1.2 — Database Schema (Full + NEW Tables)

**Description:** Define all tables upfront. The `doctor_id` foreign key on every patient-related table ensures pool isolation between the two surgeons. All tables use UUID primary keys and include `created_at` timestamps for audit trails.

### Existing Tables (unchanged from original)

See [00-project-overview.md](./00-project-overview.md) for the full schema summary. Key tables:
- `doctors`, `patients`, `opd_records`, `medications`, `investigations`
- `ipd_admissions`, `pre_op_notes`, `intra_op_notes`, `post_op_notes`, `ward_round_notes`, `discharge_summaries`
- `appointments`, `documents`, `whatsapp_logs`, `otp_sessions`

### NEW Table: `nurses`

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

### NEW Table: `consent_forms`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique consent form identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| patient_id | UUID (FK → patients) | Associated patient |
| doctor_id | UUID (FK → doctors) | Owning surgeon |
| form_type | String | Type of consent (surgical/anesthesia/blood-transfusion/etc) |
| content_json | JSON | Structured form content |
| pdf_url | String? | Cloudinary URL of generated PDF |
| parent_signature_url | String? | Cloudinary URL of signed document |
| witness_name | String? | Name of witness |
| generated_at | DateTime | Form generation timestamp |
| signed_at | DateTime? | Parent/guardian signing timestamp |

### NEW Table: `ai_diagnosis_logs`

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
| model_used | String | LLM model name |
| created_at | DateTime | Request timestamp |

**Acceptance Criteria:**

- `prisma db push` runs clean without errors
- All foreign key relations are correct
- `doctor_id` isolation is verified — every patient-related table has a `doctor_id` foreign key
- New tables (`nurses`, `consent_forms`, `ai_diagnosis_logs`) are included in the Prisma schema

---

## Task 1.3 — OTP Auth: Backend & Push Token (Three-Way Role Detection)

**Description:** Implement phone-based OTP authentication using 2Factor.in (or MSG91). The system detects the user's role based on whether the phone number exists in the `doctors` table, `nurses` table, or neither — if it does in doctors, the user is a surgeon; if in nurses, the user is a nurse; otherwise, the user is a patient/parent. On successful verification, a JWT token is returned with the role claim embedded. Additionally, a FCM token registration endpoint allows the app to store device tokens for push notifications.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/send-otp` | Sends OTP to the provided phone number via 2Factor.in. Creates an `otp_sessions` record with the hashed OTP, detected role, and expiration time (5 minutes). |
| POST | `/auth/verify-otp` | Verifies the OTP against the stored hash. On success, returns a JWT containing `phone`, `role`, and `doctor_id` (if surgeon/nurse) or `patient_id` (if parent). Marks the session as verified. |
| GET | `/auth/me` | Returns the authenticated user's profile based on the JWT. For surgeons: returns doctor profile. For nurses: returns nurse profile with `doctor_id`. For parents: returns parent and patient details. |
| POST | `/auth/register-fcm` | Registers or updates the Firebase Cloud Messaging device token for the authenticated user. Stores the token and platform in the appropriate table. |

**Role Detection Logic:**

```
IF phone EXISTS in doctors table → role = "surgeon"
ELSE IF phone EXISTS in nurses table → role = "nurse"
ELSE → role = "patient_parent"
```

**JWT Payload Structure:**

```json
{
  "phone": "+919876543210",
  "role": "surgeon" | "nurse" | "patient_parent",
  "doctor_id": "uuid" (if surgeon or nurse),
  "nurse_id": "uuid" (if nurse),
  "patient_id": "uuid" (if parent),
  "exp": 1700000000
}
```

**Acceptance Criteria:**

- OTP is successfully sent via 2Factor.in to an Indian (+91) phone number
- JWT is returned with the correct role claim after verification
- Surgeon phone → `role: "surgeon"`
- Nurse phone → `role: "nurse"` + `nurse_id`
- Parent phone → `role: "patient_parent"`
- FCM token is saved to the appropriate database table
- Expired OTPs are rejected
- Invalid OTPs return 401 with a clear error message

---

## Task 1.4 — OTP Auth: KMM Compose UI

**Description:** Build the P1 (Login) and P2 (Verify) screens using Compose Multiplatform. These screens must render identically on Android, iOS, and Web. After OTP verification, the JWT role determines navigation routing:
- Surgeons → S1 (Surgeon Dashboard)
- Nurses → N1 (Nurse Dashboard)
- Parents → P3 (Parent Home)

The JWT is stored securely in SQLDelight for multiplatform access. On successful login, the app registers the push notification token via the `/auth/register-fcm` endpoint.

**Screen P1 — OTP Login:**

- Phone number input field with +91 prefix (non-editable)
- "Send OTP" button with loading state
- Input validation: 10-digit Indian phone number
- Error display for invalid numbers or network failures

**Screen P2 — OTP Verify:**

- 6-digit OTP input (auto-focused, numeric keyboard)
- "Verify" button with loading state
- Resend OTP option (with cooldown timer, e.g., 30 seconds)
- On success: decode JWT, extract role, navigate to appropriate screen
- On failure: display error message, allow retry

**Post-Verification Flow:**

1. Decode JWT → extract `role`, `doctor_id` or `nurse_id` or `patient_id`
2. Store JWT securely in SQLDelight local storage
3. Register FCM/Push token via `POST /auth/register-fcm`
4. Navigate to:
   - Surgeon → S1 (Surgeon Dashboard)
   - Nurse → N1 (Nurse Dashboard)
   - Parent → P3 (Parent Home)

**Acceptance Criteria:**

- Surgeon phone number routes to the surgeon dashboard (S1)
- Nurse phone number routes to the nurse dashboard (N1)
- Parent phone number routes to the parent home (P3)
- UI renders identically on Android, iOS, and Web (Wasm)
- JWT persists across app restarts (stored in SQLDelight)
- Auto-login on subsequent app opens (if JWT is still valid)
