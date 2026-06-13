# Noni Tura Surgical Care Platform

## KMM (Kotlin Multiplatform Mobile) — Android, iOS, Web

**Client:** 2 Pediatric Surgeons, India  
**Build Tool / AI Assistant:** Claude Opus (Vibe Coding)  
**Total Duration:** 10 Days MVP  
**Total Screens:** 24 (13 Surgeon + 11 Parent)

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Roles & Constraints](#3-roles--constraints)
4. [Phase 1 — KMM Foundation & Auth](#4-phase-1--kmm-foundation--auth)
5. [Phase 2 — Surgeon: OPD Consult Flow](#5-phase-2--surgeon-opd-consult-flow)
6. [Phase 3 — WhatsApp & Push Notification Automation — OPD](#6-phase-3--whatsapp--push-notification-automation--opd)
7. [Phase 4 — Surgeon: IPD & Surgery Flow](#7-phase-4--surgeon-ipd--surgery-flow)
8. [Phase 5 — Surgeon: Schedule](#8-phase-5--surgeon-schedule)
9. [Phase 6 — Parent Flow: Surgery Tracking & Records](#9-phase-6--parent-flow-surgery-tracking--records)
10. [Phase 7 — Parent Flow: Appointment Booking](#10-phase-7--parent-flow-appointment-booking)
11. [Phase 8 — Surgeon Profile & Settings](#11-phase-8--surgeon-profile--settings)
12. [Phase 9 — Parent Profile & Settings](#12-phase-9--parent-profile--settings)
13. [Phase 10 — QA, Native Store Deployment & Launch](#13-phase-10--qa-native-store-deployment--launch)
14. [Summary — Auto-Triggers & Notifications](#14-summary--auto-triggers--notifications)
15. [Summary — Pricing & Client OpEx](#15-summary--pricing--client-opex)

---

## 1. Project Overview

**Noni Tura** is a specialized surgical care platform designed for 2 pediatric surgeons in India. The platform enables surgeons to manage their complete patient workflow — from OPD consultations through surgical procedures to discharge — while giving parents real-time visibility into their child's surgical journey.

The application is built using **Kotlin Multiplatform Mobile (KMM)** with **Compose Multiplatform**, enabling a single shared codebase for UI, business logic, networking, and local storage across **Android**, **iOS**, and **Web (Wasm)** targets. This architecture delivers native performance and user experience on all three platforms while maintaining code sharing efficiency, and supports direct distribution through the Google Play Store and Apple App Store.

### Key Differentiators

- **Doctor Pool Isolation:** Each surgeon's patient data is strictly siloed. The `doctor_id` foreign key is present on every patient-related table, and all API endpoints enforce JWT-based access control so that Surgeon A can never access Surgeon B's patients.
- **WhatsApp-First Communication:** Automated WhatsApp messages via Meta Cloud API are triggered at 5 critical points in the patient journey — OPD summary, admission alert, post-surgery update, discharge summary, and follow-up reminders — ensuring parents stay informed without manual effort from the surgeon.
- **Push Notifications:** Firebase Cloud Messaging (Android) and APNs via Firebase (iOS) deliver real-time push notifications for admission alerts, surgery updates, discharge summaries, appointment confirmations, and follow-up reminders.
- **3-Step Appointment Booking:** Parents can book an OPD consult slot with either surgeon in just 3 taps — select surgeon, pick a slot, confirm — with WhatsApp and Push confirmation auto-sent.
- **Single Shared Codebase:** Compose Multiplatform renders native UI on all three platforms (Android, iOS, Web/Wasm), meaning every screen is built once and runs everywhere with platform-native look, feel, and performance.

---

## 2. Technology Stack

### Frontend — KMM + Compose Multiplatform

| Component | Technology |
|---|---|
| Framework | KMM (Kotlin Multiplatform Mobile) |
| UI | Compose Multiplatform (Android, iOS, Web/Wasm) |
| Navigation | Voyager or JetBrains Navigation Compose |
| Dependency Injection | Koin |
| Networking | Ktor Client |
| Local Storage / Caching | SQLDelight |
| Serialization | kotlinx.serialization |
| Image Loading | Coil Multiplatform |

### Backend — FastAPI (Python)

| Component | Technology |
|---|---|
| Framework | FastAPI (Python) |
| Database | Neon PostgreSQL |
| ORM | Prisma |
| Authentication | Phone OTP (2Factor.in or MSG91) |
| WhatsApp | Meta Cloud API (direct) |
| Push Notifications | Firebase Cloud Messaging (Android) + APNs via Firebase (iOS) |

### Hosting & Distribution

| Target | Platform |
|---|---|
| Android App | Google Play Store |
| iOS App | Apple App Store |
| Web App | Vercel (Wasm static export) |
| Backend API | Render |
| File Storage | Cloudinary (documents, scans) |

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

| Role | Description |
|---|---|
| **Surgeon** | Pediatric surgeon who manages patients, OPD consults, IPD admissions, surgical workflows, and schedules. Has full CRUD access to their own patient pool. |
| **Patient/Parent** | Parent of the pediatric patient. Can view surgery status timeline, OPD records, download PDFs, book appointments, and manage notification preferences. Read-only access to their own child's records. |

### Constraints

| Constraint | Detail |
|---|---|
| Number of Surgeons | 2 |
| Patient Pools | Separate per surgeon (isolated via `doctor_id`) |
| Initial Patient Count | < 100 |
| Delivery Timeline | 10 days MVP (accounting for KMM setup and Store reviews) |
| Native Apps | Android + iOS + Web via KMM shared logic |
| Role Detection | If phone exists in `doctors` table → surgeon; else → patient/parent |

---

## 4. Phase 1 — KMM Foundation & Auth

**Goal:** KMM project scaffolding, Compose UI setup for 3 targets, database schema, OTP login routing  
**Duration:** 1.5 days  
**Owner:** All 3 devs  
**Screens:** P1 — OTP Login, P2 — OTP Verify

---

### Task 1.1 — KMM Repo & Project Setup

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

### Task 1.2 — Database Schema (Full)

**Description:** Define all tables upfront. The `doctor_id` foreign key on every patient-related table ensures pool isolation between the two surgeons. All tables use UUID primary keys and include `created_at` timestamps for audit trails.

**Tables:**

#### `doctors`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique doctor identifier |
| name | String | Doctor's full name |
| phone | String | Registered phone number (used for OTP login) |
| hospital | String | Hospital affiliation |
| specialty | String | Medical specialty |
| fcm_token | String? | Firebase Cloud Messaging device token |
| platform | String? | Device platform (android/ios/web) |
| created_at | DateTime | Account creation timestamp |

#### `patients`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique patient identifier |
| doctor_id | UUID (FK → doctors) | Owning surgeon — enforces pool isolation |
| name | String | Patient's full name |
| age | Int | Patient age |
| gender | String | Patient gender |
| blood_group | String? | Blood group (e.g., A+, B-, O+) |
| allergies | String? | Known allergies |
| parent_name | String | Parent/guardian name |
| parent_phone | String | Parent's phone number (for WA + Push) |
| fcm_token | String? | Parent's FCM device token |
| platform | String? | Parent's device platform |
| created_at | DateTime | Record creation timestamp |

#### `opd_records`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique OPD record identifier |
| patient_id | UUID (FK → patients) | Associated patient |
| doctor_id | UUID (FK → doctors) | Owning surgeon — enforces pool isolation |
| visit_type | String | Type of visit (new/follow-up) |
| complaint | String | Chief complaint |
| examination | String | Examination findings |
| diagnosis | String | Diagnosis |
| surgical_decision | String | Surgical decision (surgery/no surgery/deferred) |
| advice | String? | Doctor's advice |
| tag | String? | Classification tag |
| follow_up_date | DateTime? | Scheduled follow-up date |
| reminder_sent | Boolean | Whether follow-up reminder has been sent |
| created_at | DateTime | Record creation timestamp |

#### `medications`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique medication identifier |
| opd_record_id | UUID (FK → opd_records) | Associated OPD record |
| name | String | Medication name |
| dose | String | Dosage |
| frequency | String | Frequency of administration |
| duration | String | Duration of course |

#### `investigations`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique investigation identifier |
| opd_record_id | UUID (FK → opd_records) | Associated OPD record |
| type | String | Investigation type (e.g., blood test, X-ray) |
| status | String | Investigation status (pending/completed) |

#### `ipd_admissions`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique admission identifier |
| patient_id | UUID (FK → patients) | Associated patient |
| doctor_id | UUID (FK → doctors) | Owning surgeon — enforces pool isolation |
| bed_no | String? | Assigned bed number |
| ward | String? | Ward name |
| admitted_at | DateTime | Admission timestamp |
| discharge_at | DateTime? | Discharge timestamp (null if still admitted) |
| urgency | String | Urgency level (routine/urgent/emergency) |
| consent_status | String | Consent status (pending/obtained/declined) |
| status | String | Admission status (admitted/pre-op/in-surgery/recovery/discharged) |

#### `pre_op_notes`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique pre-op note identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| procedure | String | Planned procedure |
| approach | String? | Surgical approach |
| anaesthesia | String? | Anaesthesia type |
| risk_level | String? | Risk classification |
| special_instructions | String? | Special instructions |
| completed_at | DateTime? | Completion timestamp |

#### `intra_op_notes`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique intra-op note identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| procedure_done | String | Procedure performed |
| findings | String? | Intra-operative findings |
| technique | String? | Surgical technique used |
| complications | String? | Complications encountered |
| blood_loss | String? | Estimated blood loss |
| ot_start | DateTime? | OT start time |
| ot_end | DateTime? | OT end time |
| created_at | DateTime | Record creation timestamp |

#### `post_op_notes`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique post-op note identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| day_number | Int | Post-operative day number |
| condition | String | Patient condition |
| vitals_json | JSON | Vital signs as structured JSON |
| wound_status | String? | Wound status description |
| pain_score | Int? | Pain score (0-10) |
| diet | String? | Diet instructions |
| medications_json | JSON? | Current medications as structured JSON |
| created_at | DateTime | Record creation timestamp |

#### `ward_round_notes`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique ward round note identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| round_date | DateTime | Date of the ward round |
| subjective | String? | SOAP - Subjective findings |
| objective | String? | SOAP - Objective findings |
| assessment | String? | SOAP - Assessment |
| plan | String? | SOAP - Plan |
| ready_for_discharge | Boolean | Whether patient is ready for discharge |
| created_at | DateTime | Record creation timestamp |

#### `discharge_summaries`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique discharge summary identifier |
| admission_id | UUID (FK → ipd_admissions) | Associated admission |
| condition_at_discharge | String | Patient condition at discharge |
| procedure_summary | String | Summary of procedure performed |
| discharge_medications_json | JSON | Discharge medications as structured JSON |
| wound_care | String? | Wound care instructions |
| activity_restrictions | String? | Activity restriction details |
| diet_instructions | String? | Diet instructions |
| follow_up_date | DateTime? | Follow-up appointment date |
| red_flags | String? | Warning signs to watch for |
| created_at | DateTime | Record creation timestamp |

#### `appointments`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique appointment identifier |
| patient_id | UUID (FK → patients) | Associated patient |
| doctor_id | UUID (FK → doctors) | Owning surgeon |
| slot_datetime | DateTime | Appointment date and time |
| visit_type | String | Type of visit (OPD/consult) |
| status | String | Appointment status (booked/completed/cancelled) |
| created_at | DateTime | Record creation timestamp |

#### `documents`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique document identifier |
| patient_id | UUID (FK → patients) | Associated patient |
| name | String | Document name |
| url | String | Cloudinary URL |
| type | String | Document type (scan/report/consent/x-ray) |
| uploaded_at | DateTime | Upload timestamp |

#### `whatsapp_logs`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique log identifier |
| patient_id | UUID (FK → patients) | Associated patient |
| trigger_type | String | Trigger type (opd_summary/admission_alert/post_surgery_update/discharge_summary/follow_up_reminder) |
| message_body | String | Message content sent |
| sent_at | DateTime | Send timestamp |
| status | String | Delivery status (sent/delivered/failed) |

#### `otp_sessions`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique session identifier |
| phone | String | Phone number for OTP |
| otp_hash | String | Hashed OTP value |
| role | String | Detected role (surgeon/patient_parent) |
| expires_at | DateTime | OTP expiration timestamp |
| verified | Boolean | Whether OTP has been verified |

**Acceptance Criteria:**

- `prisma db push` runs clean without errors
- All foreign key relations are correct
- `doctor_id` isolation is verified — every patient-related table has a `doctor_id` foreign key pointing to the `doctors` table

---

### Task 1.3 — OTP Auth: Backend & Push Token

**Description:** Implement phone-based OTP authentication using 2Factor.in (or MSG91). The system detects the user's role based on whether the phone number exists in the `doctors` table — if it does, the user is a surgeon; otherwise, the user is a patient/parent. On successful verification, a JWT token is returned with the role claim embedded. Additionally, a FCM token registration endpoint allows the app to store device tokens for push notifications.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/send-otp` | Sends OTP to the provided phone number via 2Factor.in. Creates an `otp_sessions` record with the hashed OTP, detected role, and expiration time (5 minutes). |
| POST | `/auth/verify-otp` | Verifies the OTP against the stored hash. On success, returns a JWT containing `phone`, `role`, and `doctor_id` (if surgeon) or `patient_id` (if parent). Marks the session as verified. |
| GET | `/auth/me` | Returns the authenticated user's profile based on the JWT. For surgeons: returns doctor profile. For parents: returns parent and patient details. |
| POST | `/auth/register-fcm` | Registers or updates the Firebase Cloud Messaging device token for the authenticated user. Stores the token and platform in the `doctors` or `patients` table. |

**Role Detection Logic:**

```
IF phone EXISTS in doctors table → role = "surgeon"
ELSE → role = "patient_parent"
```

**JWT Payload Structure:**

```json
{
  "phone": "+919876543210",
  "role": "surgeon" | "patient_parent",
  "doctor_id": "uuid" (if surgeon),
  "patient_id": "uuid" (if parent),
  "exp": 1700000000
}
```

**Acceptance Criteria:**

- OTP is successfully sent via 2Factor.in to an Indian (+91) phone number
- JWT is returned with the correct role claim after verification
- FCM token is saved to the appropriate database table (doctors or patients)
- Expired OTPs are rejected
- Invalid OTPs return 401 with a clear error message

---

### Task 1.4 — OTP Auth: KMM Compose UI

**Description:** Build the P1 (Login) and P2 (Verify) screens using Compose Multiplatform. These screens must render identically on Android, iOS, and Web. After OTP verification, the JWT role determines navigation routing — surgeons are directed to the surgeon dashboard (S1), while parents are directed to the parent home (P3). The JWT is stored securely in SQLDelight for multiplatform access. On successful login, the app registers the push notification token via the `/auth/register-fcm` endpoint.

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

1. Decode JWT → extract `role`, `doctor_id` or `patient_id`
2. Store JWT securely in SQLDelight local storage
3. Register FCM/Push token via `POST /auth/register-fcm`
4. Navigate to:
   - Surgeon → S1 (Surgeon Dashboard)
   - Parent → P3 (Parent Home)

**Acceptance Criteria:**

- Surgeon phone number routes to the surgeon dashboard (S1)
- Parent phone number routes to the parent home (P3)
- UI renders identically on Android, iOS, and Web (Wasm)
- JWT persists across app restarts (stored in SQLDelight)
- Auto-login on subsequent app opens (if JWT is still valid)

---

## 5. Phase 2 — Surgeon: OPD Consult Flow

**Goal:** Surgeon can add patients, create OPD consult notes, tag patients, and set follow-up  
**Duration:** 1 day  
**Owner:** Dev 1 (KMM UI) + Dev 2 (Backend)  
**Screens:** S1 — Surgeon Dashboard, S2 — Patient List, S3 — New OPD Consult Form, S10 — Patient Profile

---

### Task 2.1 — Surgeon Dashboard (S1)

**Description:** The surgeon dashboard is the primary landing screen after login. It provides an at-a-glance overview of today's clinical activity. The screen fetches data scoped to the authenticated surgeon's `doctor_id` only. Four metric chips at the top show key counts, followed by surgery cards and IPD ward round cards. A Floating Action Button (FAB) provides quick access to create a new OPD consult or admit a patient.

**UI Components:**

- **4 Metric Chips (horizontal row):**
  1. Today's Surgeries — count of admissions with `status = "in-surgery"` for today
  2. Pre-op Pending — count of admissions with `status = "pre-op"` and no completed pre-op notes
  3. Active IPD — count of admissions with `status` in (admitted, pre-op, in-surgery, recovery)
  4. Today's OPD Consults — count of OPD records created today

- **Surgery Cards (LazyColumn):** Each card shows patient name, procedure, OT time, status pill (color-coded). Tappable to navigate to the corresponding admission detail.

- **IPD Ward Round Cards (LazyColumn):** Each card shows patient name, ward, bed number, day number, and a "last rounded" timestamp. Tappable to navigate to the ward round entry screen (S8).

- **FAB (Floating Action Button):** Two options on tap:
  1. New OPD Consult → Navigate to S3
  2. Admit Patient → Navigate to S4

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/surgeon/dashboard` | Returns today's metrics, surgery list, and IPD ward round list scoped to the authenticated surgeon's `doctor_id` |

**Acceptance Criteria:**

- All 4 metric chips are populated with live data from the backend
- Native FAB and cards render correctly on Android, iOS, and Web
- No cross-surgeon data is displayed
- Dashboard auto-refreshes on screen resume (pull-to-refresh or auto-fetch)

---

### Task 2.2 — Patient List (S2)

**Description:** A searchable, filterable patient list scoped to the authenticated surgeon's `doctor_id`. Patients from the other surgeon's pool must never be visible. The list supports search by name or phone number and filter chips by patient status. An "Add Patient" FAB allows quick creation of a new patient record.

**UI Components:**

- **Search Bar:** Text input at the top. Filters the list by patient name or parent phone number as the user types (debounced, 300ms).
- **Filter Chips (horizontal scrollable row):** All / Pre-op / Post-op / OPD / Discharged. Each chip filters the patient list by the patient's current status.
- **Patient List (LazyColumn):** Each row shows patient name, age, gender, status pill, and last visit date. Tappable to navigate to the Patient Profile (S10).
- **FAB:** "+" Add Patient — opens a bottom sheet or dialog for quick patient creation.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/patients` | Returns patients scoped to `doctor_id` from JWT. Supports `?search=` and `?status=` query parameters. |
| POST | `/patients` | Creates a new patient record with `doctor_id` auto-set from JWT. Required fields: name, age, gender, parent_name, parent_phone. Optional: blood_group, allergies. |

**Acceptance Criteria:**

- Search works in real-time with debounced input
- Filter chips correctly filter the patient list
- Patients from the other surgeon are NOT visible under any circumstances
- Native scrolling performance on all platforms (LazyColumn with item recycling)
- New patient immediately appears in the list after creation

---

### Task 2.3 — OPD Consult Form (S3)

**Description:** A comprehensive shared Compose form for creating and editing OPD consultation records. The form includes patient selection, visit type, clinical notes, medications, investigations, tagging, and follow-up scheduling. A critical feature is auto-save draft functionality — the form state is persisted to SQLDelight every 30 seconds, ensuring no data loss if the app is killed or crashes. After saving, a WhatsApp preview screen opens automatically.

**Form Fields:**

- **Patient Selector:** Dropdown or searchable list to select an existing patient. Required.
- **Visit Type:** Chip selection — New Visit / Follow-up. Required.
- **Chief Complaint:** Multi-line text input. Required.
- **Examination:** Multi-line text input. Required.
- **Diagnosis:** Text input with autocomplete from previous diagnoses by this surgeon.
- **Surgical Decision:** Segmented control — Surgery / No Surgery / Deferred. Required. When "Surgery" is selected, an optional "Planned Procedure" text field appears.
- **Advice:** Multi-line text input.
- **Medications (repeatable rows):** Each row contains:
  - Medication Name (text input with autocomplete)
  - Dose (text input)
  - Frequency (chip selection: OD/BD/TDS/QID/PRN/SOS)
  - Duration (text input, e.g., "5 days", "2 weeks")
  - "+" button to add a new row, "×" button to remove a row
- **Investigations (checklist):** Pre-defined investigation types with toggle checkboxes:
  - Blood Tests (CBC, LFT, RFT, Electrolytes, Coagulation)
  - Imaging (X-ray, Ultrasound, CT, MRI)
  - Others (Urine, Stool, Culture)
  - Custom investigation text input
- **Tag Chips:** Multi-select chips for quick patient classification — Urgent / Routine / Surgical / Medical / Follow-up.
- **Follow-up Date Picker:** Date picker with a toggle switch for "Send Reminder". When enabled, a follow-up reminder will be sent via WhatsApp + Push on the selected date at 9AM IST.

**Auto-Save Draft Logic:**

- Every 30 seconds, the current form state is serialized and saved to SQLDelight as a draft
- On form screen load, check for an existing draft and prompt the user to resume or discard
- Draft is cleared on successful form submission
- Draft persists across app kill/restart

**Post-Save Flow:**

1. Form is submitted via `POST /opd-records` (or `PATCH /opd-records/:id` for edits)
2. On success, navigate to WhatsApp Preview (S12) with the OPD summary pre-filled
3. The WhatsApp preview allows the surgeon to review, edit, and send the OPD summary to the parent

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/opd-records` | Creates a new OPD record. Auto-sets `doctor_id` from JWT. Returns the created record. |
| PATCH | `/opd-records/:id` | Updates an existing OPD record. Validates `doctor_id` ownership. |

**Acceptance Criteria:**

- Form saves successfully to the database with all fields
- Surgical decision is mandatory (form cannot be submitted without selecting one)
- Draft persists on app kill/restart and can be resumed
- WhatsApp preview (S12) opens after successful save
- Form renders natively and scrolls smoothly on all platforms

---

### Task 2.4 — Patient Profile (S10)

**Description:** A comprehensive shared Compose screen displaying the full patient history organized by TopBar Tabs. This screen serves as the central hub for all information related to a specific patient, including OPD history, surgical history, and uploaded documents.

**UI Structure:**

- **TopBar:** Patient name, age, gender, and status pill. Back navigation.
- **Tabs (TopBar horizontal):**

  1. **OPD History Tab:** Vertical timeline of all OPD consultation records. Each timeline entry shows the visit date, chief complaint, diagnosis, and a brief medication summary. Tapping an entry expands to show full details (examination, advice, surgical decision, investigations). Most recent visits at the top.

  2. **Surgery History Tab:** Cards for each IPD admission/surgical episode. Each card shows admission date, procedure, status pill (admitted/pre-op/in-surgery/recovery/discharged), and a summary. Tapping a card navigates to the full admission detail (pre-op, intra-op, post-op, ward rounds, discharge).

  3. **IPD Timeline Tab:** (Stubbed for now — will be fully implemented in Phase 4) Placeholder with "Coming soon" or a basic list of admissions.

  4. **Documents Tab:** File upload list showing all uploaded documents (scans, reports, consent forms, X-rays). Each item shows the document name, type, and upload date. Includes an "Upload Document" FAB that opens a multiplatform file picker, uploads to Cloudinary, and saves the document record to the database.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/patients/:id` | Returns patient details including demographics, allergies, and blood group. Validates `doctor_id` ownership. |
| GET | `/patients/:id/opd-records` | Returns all OPD records for the patient, sorted by `created_at` descending. |
| GET | `/patients/:id/admissions` | Returns all IPD admissions for the patient, sorted by `admitted_at` descending. |

**Document Upload Flow:**

1. User taps "Upload Document" FAB
2. Multiplatform file picker opens (image or PDF)
3. Selected file is uploaded to Cloudinary via their upload API
4. Cloudinary returns a URL
5. A `documents` record is created in the database with the URL and metadata
6. The document appears in the list

**Acceptance Criteria:**

- Full patient history renders correctly across all tabs
- Tab switching is smooth and animated on all platforms
- Cloudinary upload works natively (file picker opens, upload completes, URL is stored)
- OPD timeline entries expand/collapse correctly
- Surgery history cards navigate to admission details
- No cross-surgeon data is accessible

---

## 6. Phase 3 — WhatsApp & Push Notification Automation — OPD

**Goal:** Auto-send OPD consult summary via WhatsApp, send Push Notifications to parents  
**Duration:** 0.5 days  
**Owner:** Dev 3 (Backend + Firebase)  
**Screens:** S12 — WhatsApp Preview

---

### Task 3.1 — WhatsApp + Push Service (Backend)

**Description:** Integrate Meta Cloud API for WhatsApp messaging and Firebase Admin SDK for push notifications. Five WhatsApp message templates must be created and submitted to Meta Business Manager for approval. The backend automatically triggers push notifications via FCM/APNs tokens stored in the database when OPD records are created or updated.

**WhatsApp Templates (submitted to Meta for approval):**

| Template Name | Trigger Event | Content Summary |
|---|---|---|
| `opd_summary` | OPD consult saved | Diagnosis, medications, advice, follow-up date |
| `follow_up_reminder` | Follow-up date reached (9AM IST cron) | Reminder of upcoming follow-up appointment |
| `admission_alert` | Patient admitted to IPD | Ward, bed number, surgeon name, status |
| `post_surgery_update` | Intra-op notes saved | Procedure completed, patient in recovery |
| `discharge_summary` | Discharge saved | Condition, medications, wound care, follow-up, red flags |

**Template Design Notes:**

- All templates must be in English
- No promotional content — purely informational/transactional
- Each template must include an opt-out line (e.g., "Reply STOP to unsubscribe")
- Templates must conform to Meta's WhatsApp Business API policies for healthcare

**Push Notification Events:**

| Event | Platform | Title | Body |
|---|---|---|---|
| Admission alert | Android (FCM) + iOS (APNs) | "Admission Update" | "Your child has been admitted to [ward]. Surgeon: [name]" |
| Post surgery update | Android (FCM) + iOS (APNs) | "Surgery Update" | "Surgery completed successfully. Your child is in recovery." |
| Discharge summary | Android (FCM) + iOS (APNs) | "Discharge Update" | "Your child has been discharged. Please review the discharge summary." |
| Appointment confirmation | Android (FCM) + iOS (APNs) | "Appointment Confirmed" | "Your appointment with Dr. [name] is confirmed for [date/time]." |
| Follow-up reminder | Android (FCM) + iOS (APNs) | "Follow-up Reminder" | "Your child has a follow-up appointment tomorrow." |

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/whatsapp/send` | Sends a WhatsApp message using a specified template. Parameters: `patient_id`, `template_name`, `template_params`. Logs the message in `whatsapp_logs`. |
| GET | `/whatsapp/logs/:patient_id` | Returns WhatsApp message logs for a specific patient. Useful for auditing and debugging. |

**Acceptance Criteria:**

- Test WhatsApp message is delivered to a +91 Indian phone number
- Push notification is received on both Android (via FCM) and iOS (via APNs)
- All 5 templates are submitted to Meta Business Manager (approval expected by Phase 10)
- Message logs are created in the `whatsapp_logs` table for every send attempt
- Failed sends are logged with status = "failed" and an error description

---

### Task 3.2 — WhatsApp Preview Screen (S12)

**Description:** A Compose Multiplatform screen that displays a phone mockup frame showing the formatted WhatsApp message before sending. This gives the surgeon a chance to review and optionally edit the message content before it is delivered to the parent. The screen also provides a fallback SMS option.

**UI Components:**

- **Phone Mockup Frame:** A visual representation of a phone screen (rounded rectangle with notch/dynamic island styling) containing the formatted WhatsApp message. The message uses WhatsApp-style formatting (bold, line breaks, bullet points) to present the clinical information clearly.

- **Message Preview Content (OPD Summary example):**

  ```
  🏥 Noni Tura - OPD Consult Summary

  Patient: [Name]
  Date: [Date]

  Diagnosis: [Diagnosis]
  Medications:
  • [Med 1] - [Dose] [Frequency] × [Duration]
  • [Med 2] - [Dose] [Frequency] × [Duration]

  Advice: [Advice text]

  Follow-up: [Date]

  Reply STOP to unsubscribe
  ```

- **Edit Message Button:** Opens an editable text field allowing the surgeon to modify the message content before sending.

- **Send via WhatsApp Button (#25D366 green):** Sends the message via the Meta Cloud API. On success, shows a confirmation toast and logs the send.

- **Send via SMS Fallback Button:** If WhatsApp is unavailable or the parent has opted out, sends a shorter SMS version of the message via 2Factor.in's SMS API.

**Acceptance Criteria:**

- Preview renders correctly on all platforms with the phone mockup frame
- Edit message option works and the edited content is sent
- Send via WhatsApp fires the API and a log entry is created in `whatsapp_logs`
- SMS fallback works when WhatsApp fails or is unavailable
- Success/failure feedback is provided to the surgeon

---

### Task 3.3 — Follow-up Reminder Cron + Push

**Description:** Implement a daily scheduled task (cron job) that runs at 9:00 AM IST every day. The cron queries all OPD records where the `follow_up_date` equals today and `reminder_sent` is `false`. For each matching record, it sends both a WhatsApp template message (`follow_up_reminder`) and a Push Notification to the parent, then marks `reminder_sent = true` to prevent duplicates.

**Cron Logic:**

```
EVERY DAY AT 9:00 AM IST:
  1. Query: SELECT * FROM opd_records WHERE follow_up_date = TODAY AND reminder_sent = false
  2. For each record:
     a. Send WhatsApp template "follow_up_reminder" to parent_phone
     b. Send Push Notification to parent's FCM token (stored in patients table)
     c. Update opd_records SET reminder_sent = true WHERE id = record.id
     d. Log both sends in whatsapp_logs
```

**Scheduling Implementation:**

- Use FastAPI's background scheduler (e.g., APScheduler) or a platform-level cron on Render
- Timezone: Asia/Kolkata (IST = UTC+5:30)
- Idempotency: The `reminder_sent` flag ensures no duplicate sends, even if the cron is triggered multiple times

**Acceptance Criteria:**

- Cron fires daily at 9:00 AM IST without manual intervention
- Both WhatsApp and Push notifications are sent for each matching record
- No duplicate messages — `reminder_sent` flag prevents re-sending
- Cron execution is logged for monitoring and debugging
- Edge case: If no records match, the cron completes silently without errors

---

## 7. Phase 4 — Surgeon: IPD & Surgery Flow

**Goal:** Full surgical workflow — admit → pre-op → intra-op → post-op → ward rounds → discharge  
**Duration:** 1.5 days  
**Owner:** All 3 devs  
**Screens:** S4 — Admit Patient, S5 — Pre-op Notes, S6 — Intra-op Notes, S7 — Post-op Notes, S8 — Daily Ward Round, S9 — Discharge

---

### Task 4.1 — Admit Patient (S4)

**Description:** A shared Compose form for admitting a patient to the IPD (In-Patient Department). On submission, the system creates an `ipd_admissions` record and automatically triggers an admission alert via WhatsApp and Push Notification to the parent, keeping them informed in real-time.

**Form Fields:**

- **Patient Selector:** Searchable dropdown to select an existing patient from the surgeon's pool. Required.
- **Admission Date/Time:** Date and time pickers. Defaults to current date/time. Editable.
- **Bed Number:** Text input for the assigned bed number.
- **Ward:** Text input or dropdown for the ward name (e.g., Pediatric ICU, General Ward, Semi-Private).
- **Urgency Chips:** Selection chips — Routine / Urgent / Emergency. Required. Color-coded: green / amber / red.
- **Consent Toggle:** Toggle switch — Consent Obtained / Pending. Required. If pending, a warning banner is displayed.
- **Allergies:** Auto-populated from the patient record. Editable.

**Post-Submit Flow:**

1. Create `ipd_admissions` record with `status = "admitted"` and `doctor_id` from JWT
2. Trigger `admission_alert` WhatsApp template to `parent_phone`
3. Trigger Push Notification to parent's FCM token
4. Log both sends in `whatsapp_logs`
5. Navigate back to the Surgeon Dashboard (S1), where the new admission appears in the IPD section

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions` | Creates a new IPD admission. Auto-sets `doctor_id` from JWT. Triggers WA + Push. Returns the created admission. |

**Acceptance Criteria:**

- Admission record is created in the database with correct fields
- WhatsApp and Push notifications are sent to the parent
- The newly admitted patient appears in the IPD section on the Surgeon Dashboard (S1)
- Form validates required fields before submission
- Urgency chips are visually distinct (color-coded)

---

### Task 4.2 — Pre-op Notes (S5)

**Description:** A shared Compose form for recording pre-operative assessment notes, linked to a specific `admission_id`. The form includes a template loader button that can pre-fill fields from saved surgical templates (Task 4.3). On completion, the admission status is updated to "pre-op" with the notes marked as complete.

**Form Fields:**

- **Procedure:** Text input for the planned surgical procedure. Required.
- **Approach:** Text input or chip selection for the surgical approach (Open / Laparoscopic / Robotic / Combined).
- **Anaesthesia Chips:** Multi-select chips — General / Regional / Local / Sedation.
- **Investigations Checklist:** Toggle checkboxes for pre-operative investigations:
  - Blood: CBC, LFT, RFT, Electrolytes, Coagulation Profile, Blood Group & Cross-match
  - Imaging: X-ray, Ultrasound, CT, MRI
  - Others: Urine Routine, ECG, Chest X-ray
  - Custom investigation text input
- **Risk Chips:** Selection chips for risk classification — Low / Moderate / High.
- **Consent Toggle:** Toggle switch confirming informed consent has been obtained.
- **Template Loader Button:** Opens a bottom sheet or dialog listing saved surgical templates. Selecting a template pre-fills the Procedure, Approach, Anaesthesia, and Investigations fields.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/pre-op` | Creates pre-op notes for the specified admission. Validates `doctor_id` ownership. |
| GET | `/admissions/:id/pre-op` | Retrieves existing pre-op notes for the specified admission. |

**Acceptance Criteria:**

- Pre-op notes are saved to the database and linked to the correct admission
- Template loader pre-fills fields from a saved template
- Marking the form as "Complete" updates the admission status to "pre-op"
- Form renders correctly on all platforms with native chip and toggle components

---

### Task 4.3 — Surgical Templates Manager

**Description:** A CRUD interface for managing named surgical templates. Surgeons can create, save, load, update, and delete templates for common procedures. Templates pre-fill pre-op and intra-op form fields, saving significant time for recurring procedures. Templates are stored in the backend database and scoped to the surgeon's `doctor_id`.

**Template Data Structure:**

```json
{
  "id": "uuid",
  "doctor_id": "uuid",
  "name": "Inguinal Hernia Repair - Pediatric",
  "procedure": "Inguinal Herniotomy",
  "approach": "Open / Laparoscopic",
  "anaesthesia": ["General"],
  "investigations": ["CBC", "Coagulation Profile", "Urine Routine"],
  "risk_level": "Low",
  "technique": "Standard herniotomy with sac ligation",
  "special_instructions": "Consent for bilateral exploration if indicated"
}
```

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/templates` | Returns all templates for the authenticated surgeon. |
| POST | `/templates` | Creates a new template. Auto-sets `doctor_id` from JWT. |
| PUT | `/templates/:id` | Updates an existing template. Validates ownership. |
| DELETE | `/templates/:id` | Deletes a template. Validates ownership. |

**Acceptance Criteria:**

- Full CRUD operations work correctly for templates
- Loading a template pre-fills the corresponding fields in pre-op (S5) and intra-op (S6) forms
- Templates are isolated per surgeon — Surgeon A cannot see Surgeon B's templates
- Template list is accessible from both the Pre-op and Intra-op screens

---

### Task 4.4 — Intra-op Notes (S6)

**Description:** A shared Compose screen for recording intra-operative notes during surgery. The OT start time is automatically timestamped when the screen is opened. The form captures procedure details, findings, technique, complications, blood loss, and specimens. On save, the system triggers a `post_surgery_update` WhatsApp message and Push Notification to the parent.

**Form Fields:**

- **OT Start:** Auto-timestamped when the screen is opened. Editable.
- **Procedure Done:** Text input for the actual procedure performed. Required.
- **Findings:** Multi-line text input for intra-operative findings.
- **Technique:** Multi-line text input for the surgical technique used.
- **Complications:** Toggle switch — Yes / No. If "Yes", a red-bordered text area appears for detailing the complications.
- **Blood Loss:** Text input for estimated blood loss (e.g., "Minimal", "~50 ml", "~200 ml").
- **Specimens:** Text input for specimens sent for histopathology or culture.
- **OT End:** Time picker for the surgery end time.

**Post-Save Flow:**

1. Save intra-op notes to the database
2. Update `ipd_admissions` status to "in-surgery" or "recovery"
3. Trigger `post_surgery_update` WhatsApp template to parent
4. Trigger Push Notification to parent
5. Log both sends in `whatsapp_logs`

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/intra-op` | Creates intra-op notes for the specified admission. Validates `doctor_id` ownership. Triggers WA + Push. |

**Acceptance Criteria:**

- OT start time is auto-set when the screen opens
- Complications toggle activates a visually distinct red-bordered text area
- WhatsApp and Push notifications are fired to the parent on save
- All timestamps are in IST (Asia/Kolkata timezone)

---

### Task 4.5 — Post-op Notes (S7)

**Description:** A shared Compose screen for recording post-operative notes. The post-operative day number is automatically calculated based on the surgery date. Multiple post-op note entries are possible (one per day). The form captures the patient's condition, vitals, wound status, pain score, diet, and current medications.

**Form Fields:**

- **Post-op Day N:** Auto-calculated from the admission's surgery date. Display-only.
- **Condition Chips:** Selection chips — Stable / Improved / Critical / Guarded.
- **Vitals Row (inline inputs, stored as JSON):**
  - Heart Rate (bpm)
  - Blood Pressure (systolic/diastolic)
  - Temperature (°C or °F)
  - SpO2 (%)
  - Respiratory Rate (/min)
- **Wound Status:** Text input or chip selection — Clean / Infected / Healing Well / Needs Attention.
- **Pain Score:** Slider 0-10 with visual labels (0 = No Pain, 10 = Worst Pain). Color gradient from green to red.
- **Diet Chips:** Selection chips — NPO / Clear Fluids / Full Fluids / Soft Diet / Regular.
- **Medications (repeatable rows):** Same structure as OPD consult medications — name, dose, frequency, duration. "+" and "×" buttons for adding/removing rows.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/post-op` | Creates a post-op note for the specified admission. Validates `doctor_id` ownership. |
| GET | `/admissions/:id/post-op` | Returns all post-op notes for the admission, sorted by `day_number`. |

**Acceptance Criteria:**

- Post-op day number is calculated correctly based on the surgery date
- Vitals are stored as a structured JSON object in the `vitals_json` column
- Multiple post-op note entries can be created (one per day)
- Pain score slider provides visual feedback with color gradient
- Form renders correctly on all platforms

---

### Task 4.6 — Daily Ward Round (S8)

**Description:** A shared Compose screen for recording daily ward round notes in SOAP format (Subjective, Objective, Assessment, Plan). The screen provides quick vitals entry at the top, followed by the SOAP fields. A prominent "Ready for Discharge" amber button marks the patient as ready for discharge, which triggers a visual indicator on the Surgeon Dashboard.

**UI Layout:**

- **Quick Vitals (top section):** Inline inputs for Heart Rate, BP, Temperature, SpO2, Respiratory Rate. Same JSON structure as post-op vitals.
- **SOAP Notes:**
  - **Subjective:** Multi-line text input — how the patient/parent reports feeling.
  - **Objective:** Multi-line text input — objective clinical observations.
  - **Assessment:** Multi-line text input with assessment chip suggestions (Improving / Stable / Deteriorating / Complicated).
  - **Plan:** Multi-line text input — treatment plan, next steps, changes to medications.
- **Ready for Discharge Button:** Amber-colored button. On tap, sets `ready_for_discharge = true` for this ward round entry. This triggers a visual indicator on the Surgeon Dashboard (S1) showing that the patient is ready for discharge.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/ward-round` | Creates a ward round note for the specified admission. Validates `doctor_id` ownership. |
| GET | `/admissions/:id/ward-rounds` | Returns all ward round notes for the admission, sorted chronologically. |

**Acceptance Criteria:**

- Multiple ward round entries can be saved per admission (one per day or more)
- Ward round history is displayed in chronological order
- "Ready for Discharge" button triggers a dashboard indicator on S1
- SOAP format fields are clearly labeled and easy to fill
- Quick vitals entry at the top is convenient for fast data entry during rounds

---

### Task 4.7 — Discharge (S9)

**Description:** A shared Compose screen for creating the discharge summary. This is the final step of the IPD journey. The form includes discharge details, a procedure summary drafted from the intra-op notes, medications, wound care instructions, activity restrictions, diet instructions, follow-up date, and red flags. On submission, the system creates a `discharge_summaries` record, updates the admission status to "discharged", and triggers the `discharge_summary` WhatsApp template and Push Notification to the parent.

**Form Fields:**

- **Discharge Date:** Date picker. Defaults to today.
- **Condition at Discharge:** Chip selection — Stable / Improved / Recovered / With Complications.
- **Procedure Summary:** Multi-line text input. Auto-drafted from the intra-op notes (`procedure_done` + `findings`). Editable.
- **Discharge Medications (repeatable rows):** Same structure as OPD medications — name, dose, frequency, duration. "+" and "×" buttons.
- **Wound Care:** Multi-line text input for wound care instructions.
- **Activity Restrictions:** Multi-line text input for activity restriction details.
- **Diet Instructions:** Multi-line text input or chip selection — Regular / Soft Diet / Full Fluids / Special Diet.
- **Follow-up Date:** Date picker for the follow-up appointment. Includes "Send Reminder" toggle.
- **Red Flags:** Multi-line text input for warning signs the parent should watch for (e.g., "Fever > 101°F", "Excessive bleeding", "Vomiting").

**Post-Submit Flow:**

1. Create `discharge_summaries` record
2. Update `ipd_admissions` status to "discharged" and set `discharge_at` to current timestamp
3. Trigger `discharge_summary` WhatsApp template to parent
4. Trigger Push Notification to parent
5. Log both sends in `whatsapp_logs`
6. Navigate back to Surgeon Dashboard — patient moves to the "Discharged" filter in Patient List (S2)

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/discharge` | Creates a discharge summary for the specified admission. Validates `doctor_id` ownership. Updates admission status. Triggers WA + Push. |

**Acceptance Criteria:**

- Discharge summary is created with all fields
- Patient's admission status is updated to "discharged"
- Patient moves to the "Discharged" filter in the Patient List (S2)
- WhatsApp and Push notifications are fired to the parent
- Procedure summary is auto-drafted from intra-op notes and is editable
- Red flags section is prominently displayed

---

## 8. Phase 5 — Surgeon: Schedule

**Goal:** OT surgery schedule and OPD consult schedule  
**Duration:** 0.5 days  
**Owner:** Dev 1  
**Screens:** S11 — OT and OPD Schedule

---

### Task 5.1 — Schedule Screen (S11)

**Description:** A shared Compose screen for managing the surgeon's OT surgery schedule and OPD consult schedule. The screen uses TopBar tabs to switch between OT and OPD views. A horizontal week strip at the top allows navigation between dates. Slots are displayed as cards or list items, and empty slots show a dashed border with an "Add" button. A FAB allows creating new bookings.

**UI Components:**

- **TopBar Tabs:** OT Schedule / OPD Schedule. Smooth tab switching animation.

- **Week Strip (horizontal scrollable row):**
  - Shows 7 days: Mon, Tue, Wed, Thu, Fri, Sat, Sun (with dates)
  - Current day is highlighted (teal background)
  - Left/right arrows for previous/next week navigation
  - Tapping a day filters the schedule to that date

- **OT Schedule Tab:**
  - Surgery slot cards showing: patient name, procedure, OT time, urgency pill, status (scheduled / in-progress / completed)
  - Empty slots: dashed border rectangle with "+" Add button
  - Tapping a slot navigates to the admission detail or allows scheduling a new surgery

- **OPD Schedule Tab:**
  - Consult slot list showing: patient name, slot time, visit type, status (booked / completed / cancelled)
  - Empty slots: dashed border with "+" Add button
  - Tapping a slot navigates to the OPD consult form or patient profile

- **FAB:** "+ New Booking" — opens a bottom sheet or dialog for creating a new OT or OPD booking.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/schedule/ot` | Returns OT surgery slots for the selected date. Scoped to `doctor_id`. |
| GET | `/schedule/opd` | Returns OPD consult slots for the selected date. Scoped to `doctor_id`. |
| POST | `/schedule/ot` | Creates a new OT surgery slot. Parameters: `patient_id`, `date`, `time`, `procedure`, `urgency`. |
| POST | `/schedule/opd` | Creates a new OPD consult slot. Parameters: `patient_id`, `date`, `time`, `visit_type`. |

**Acceptance Criteria:**

- Both OT and OPD tabs render correctly with native feel
- Week navigation is smooth and responsive on all platforms
- Slots are bookable and appear immediately after creation
- Bookings created here feed into the appointment system (Phase 7)
- Empty slots show dashed border with "Add" button
- Schedule data is scoped to the authenticated surgeon's `doctor_id`

---

## 9. Phase 6 — Parent Flow: Surgery Tracking & Records

**Goal:** Parent sees child's live surgery journey, OPD records, shares summaries  
**Duration:** 1 day  
**Owner:** Dev 2 + Dev 3  
**Screens:** P3 — Parent Home, P4 — Surgery Status Detail, P5 — OPD Records List, P6 — Consult Record Detail

---

### Task 6.1 — Parent Home (P3)

**Description:** The primary landing screen for parents after OTP login. The screen provides a comprehensive at-a-glance view of their child's current status, upcoming appointments, and recent consultations. The design prioritizes clarity and reassurance, with the surgery status card as the most prominent element.

**UI Components:**

- **Surgery Status Card (amber, full width):** The most prominent element on the screen.
  - If the child is currently admitted: Shows ward, bed number, surgeon name, and a status pill (Admitted / Pre-op / In Surgery / Recovery) with color coding.
  - If the child is not currently admitted: Shows the last discharge summary with a "View Details" button.
  - Tapping the card navigates to the Surgery Status Detail (P4).

- **Next Appointment Card (teal):** Shows the next upcoming appointment date, time, and surgeon name. Tapping navigates to the appointment detail or booking flow.

- **Follow-up Pill:** A small pill-shaped indicator showing the follow-up date if one is scheduled. Tapping navigates to the OPD record that set the follow-up.

- **Last Consult Card:** A compact card showing the date and diagnosis from the most recent OPD consultation. Tapping navigates to the Consult Record Detail (P6).

- **Educational Content (horizontal LazyRow):** A horizontally scrollable row of educational content cards related to pediatric surgery care. Each card shows a title and a brief description. Tapping opens the full article (could be a web view or a static content screen). Content is pre-defined and static for MVP.

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/home` | Returns all data for the parent's home screen: current admission status, next appointment, follow-up date, last consult, and educational content. Scoped to the authenticated parent's `patient_id`. |

**Acceptance Criteria:**

- Surgery card shows live status matching the current admission record
- Push notifications that are tapped route to this screen first, then to the relevant detail
- No cross-surgeon data is ever displayed — parent sees only their own child's data
- All cards are tappable and navigate to the correct detail screens
- Educational content LazyRow scrolls smoothly

---

### Task 6.2 — Surgery Status Detail (P4)

**Description:** A shared Compose screen that displays a visual timeline of the child's surgical journey. The timeline has 5 nodes representing the major stages from admission to discharge. The currently active node is highlighted with an amber pulse animation, providing parents with a clear visual indicator of where their child is in the surgical process. A "Share on WhatsApp" green button allows the parent to share the current status with family members via the native share intent.

**Timeline Nodes (5 stages):**

| Node | Label | Description |
|---|---|---|
| 1 | Admitted | Patient has been admitted to the ward |
| 2 | Pre-op | Pre-operative assessment is complete |
| 3 | In Surgery | Surgery is currently in progress |
| 4 | Recovery | Surgery completed, patient is recovering |
| 5 | Discharged | Patient has been discharged |

**UI Components:**

- **Timeline Visual:** A horizontal or vertical timeline rendered using Compose Canvas or custom layout. Each node is a circle connected by a line. Completed nodes are filled (green), the active node pulses (amber animation), and future nodes are outlined (gray).

- **Active Node Animation:** The active node uses a Compose Animation (pulse/scale effect) with an amber color. The animation runs continuously to draw the parent's attention to the current stage.

- **Stage Detail Card (below timeline):** Shows details specific to the current stage:
  - Admitted: Ward, bed number, admission date
  - Pre-op: Planned procedure, risk level, consent status
  - In Surgery: OT start time, estimated duration
  - Recovery: Post-op day number, current condition
  - Discharged: Discharge date, follow-up date

- **Share on WhatsApp Button (#25D366 green):** Uses the KMP native share intent to open WhatsApp with a pre-formatted message containing the current surgery status. This leverages the platform's native sharing mechanism rather than the Meta Cloud API (which is for the surgeon to send to the parent).

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/admissions/current` | Returns the current active admission for the authenticated parent's child, including status, timeline details, and stage-specific information. |

**Acceptance Criteria:**

- Timeline accurately reflects the current admission status
- Active node amber pulse animation works on Android, iOS, and Web
- Native share intent opens WhatsApp with the correct pre-formatted message
- All 5 timeline stages render correctly with appropriate visual states
- The screen updates when the admission status changes (pull-to-refresh or push-triggered refresh)

---

### Task 6.3 — OPD Records List + Consult Record Detail (P5, P6)

**Description:** Two shared Compose screens that together provide the parent with access to their child's OPD consultation history and detailed records. P5 is a filterable list of all OPD records, and P6 is a read-only detailed view of a specific consultation. The P6 screen includes the ability to download a PDF of the consultation and share it via WhatsApp.

**Screen P5 — OPD Records List:**

- **Filter Chips (horizontal scrollable row):** All / Surgery Recommended / Follow-up / Routine. Each chip filters the OPD records list.
- **Records List (LazyColumn):** Each row shows the consultation date, chief complaint, diagnosis, and a surgical decision pill (if applicable). Tapping a row navigates to P6.

**Screen P6 — Consult Record Detail:**

- **Read-only Cards:** All consultation details displayed in organized cards — Patient Info, Chief Complaint, Examination, Diagnosis, Surgical Decision, Medications, Investigations, Advice, Follow-up Date.
- **Lock Icon:** A small lock icon next to each card indicates that the data is read-only and cannot be modified by the parent.
- **Download PDF Button:** Generates a PDF of the consultation on the backend and downloads it to the device. Uses the KMP file system to save the file and the native intent to open it with a PDF viewer.
- **Share on WhatsApp + Download PDF (sticky bottom bar):** Two persistent buttons at the bottom of the screen:
  - **Share on WhatsApp:** Opens the native share intent with a summary message
  - **Download PDF:** Downloads the PDF version of the consultation record

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/opd-records` | Returns all OPD records for the authenticated parent's child. Supports `?filter=` query parameter. |
| GET | `/parent/opd-records/:id` | Returns the detailed OPD record for a specific consultation. |
| GET | `/parent/opd-records/:id/pdf` | Generates and returns a PDF of the OPD consultation record. Returns a binary file download. |

**Acceptance Criteria:**

- PDF downloads and opens natively on Android, iOS, and Web
- Lock icon is visible on all read-only cards
- WhatsApp share works via native share intent
- Filter chips correctly filter the OPD records list
- No cross-surgeon or cross-patient data is accessible

---

## 10. Phase 7 — Parent Flow: Appointment Booking

**Goal:** Parent books an OPD consult slot with either surgeon in 3 steps  
**Duration:** 0.5 days  
**Owner:** Dev 1  
**Screens:** P7 — Step 1: Surgeon Select, P8 — Step 2: Slot Select, P9 — Step 3: Confirm, P10 — Booking Confirmed

---

### Task 7.1 — 3-Step Booking Flow (P7, P8, P9, P10)

**Description:** A streamlined 3-step appointment booking flow that allows parents to book an OPD consult with either of the 2 pediatric surgeons. The flow uses a pill progress indicator at the top to show the current step. Each step is a separate screen with forward/back navigation. On confirmation, WhatsApp and Push notifications are automatically sent to the parent.

**Step Progress Indicator:**

A horizontal pill progress indicator at the top of all 4 screens showing 3 steps:

- Step 1: Select Surgeon (active = filled teal pill)
- Step 2: Select Slot (active = filled teal pill)
- Step 3: Confirm (active = filled teal pill)

Completed steps show a checkmark. The current step is highlighted. Future steps are outlined.

---

**Screen P7 — Step 1: Select Surgeon**

- **2 Surgeon Cards (side by side):** Each card displays:
  - Surgeon name
  - Hospital name
  - Specialty
  - Next available date
  - Tappable — selecting a surgeon highlights the card (teal border) and enables the "Next" button

- **"Next" Button:** Enabled only when a surgeon is selected. Navigates to P8.

---

**Screen P8 — Step 2: Select Slot**

- **Week Strip (horizontal scrollable row):** Shows 7 days with dates. Current day highlighted. Left/right arrows for week navigation.
- **Available Slot Chips (teal):** Horizontal or vertical list of available time slots for the selected date. Each chip shows the time (e.g., "10:00 AM", "10:30 AM"). Tapping a chip selects it (filled teal) and enables the "Next" button.
- **Unavailable Slots:** Grayed out or not shown.
- **"Back" and "Next" Buttons:** Back returns to P7. Next navigates to P9.

---

**Screen P9 — Step 3: Confirm**

- **Summary Card:** Displays the complete booking summary:
  - Surgeon name and hospital
  - Selected date and time
  - Patient name
  - Visit type (OPD Consult)
- **"Confirm Booking" Button:** Submits the booking. Navigates to P10.

---

**Screen P10 — Booking Confirmed**

- **Full-screen Success State:** A large green checkmark with a native success animation (scale + fade).
- **Confirmation Details:** Surgeon name, date, time, booking reference number.
- **Auto-sent Notifications:** On booking confirmation, the system automatically:
  1. Sends WhatsApp appointment confirmation to the parent
  2. Sends Push Notification to the parent
  3. The booked slot appears on the surgeon's S11 Schedule screen

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/appointments/available-slots?doctor_id=&date=` | Returns available time slots for the specified surgeon on the specified date. Excludes already booked slots. |
| POST | `/appointments` | Creates a new appointment booking. Parameters: `patient_id`, `doctor_id`, `slot_datetime`, `visit_type`. Triggers WA + Push confirmation. |

**Acceptance Criteria:**

- Booking creates an `appointments` record in the database
- The booked slot is removed from available slots (no double-booking)
- WhatsApp and Push confirmation notifications are sent to the parent
- The booked appointment appears on the surgeon's S11 Schedule screen
- The flow works correctly on Android, iOS, and Web
- Progress indicator accurately reflects the current step
- Native success animation plays on P10

---

## 11. Phase 8 — Surgeon Profile & Settings

**Goal:** Surgeon profile, surgical stats, template management  
**Duration:** 0.5 days  
**Owner:** Dev 2  
**Screens:** S13 — Surgeon Profile & Settings

---

### Task 8.1 — Surgeon Profile (S13)

**Description:** A shared Compose screen combining the surgeon's profile information, surgical statistics, and application settings. The screen is organized into distinct sections with clear visual separation.

**UI Sections:**

- **Profile Card (top):**
  - Surgeon name (large text)
  - Hospital name
  - Specialty
  - Phone number (with edit option that triggers OTP re-verification)
  - Profile photo placeholder (circular, tap to upload via Cloudinary)

- **Stats Row (horizontal):**
  - Total Patients: count from `patients` table scoped to `doctor_id`
  - Total Surgeries: count from `ipd_admissions` with `status = "discharged"` scoped to `doctor_id`
  - OPD Consults: count from `opd_records` scoped to `doctor_id`
  - All stats are calculated live from the database

- **Settings Rows (list):**
  - **Surgical Templates:** Taps navigate to the Templates Manager (Task 4.3)
  - **WhatsApp Settings:** Toggle WhatsApp notifications on/off, configure message preferences
  - **OPD Defaults:** Set default values for new OPD consults (e.g., default visit type, default investigations)
  - **Reminder Timing:** Configure when follow-up reminders are sent (default: 9:00 AM IST). Options: 8:00 AM, 9:00 AM, 10:00 AM
  - **Hospital Details:** Edit hospital name, address, and contact information
  - **Change Phone Number:** Triggers OTP re-verification flow — current phone is replaced after successful OTP verification of the new number
  - **Logout (danger red):** Clears JWT from SQLDelight, unregisters FCM token, navigates to P1 (Login)

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/surgeon/profile` | Returns the surgeon's profile data and live statistics. |
| PATCH | `/surgeon/profile` | Updates the surgeon's profile fields (name, hospital, specialty, phone, WA settings, OPD defaults, reminder timing). |

**Acceptance Criteria:**

- Stats are calculated live from the database on each screen load
- Template CRUD operations work from this screen
- Phone change triggers the full OTP re-verification flow
- Logout clears all local data and navigates to the login screen
- Settings changes are persisted to the backend

---

## 12. Phase 9 — Parent Profile & Settings

**Goal:** Parent profile, child info, notification preferences  
**Duration:** 0.5 days  
**Owner:** Dev 3  
**Screens:** P11 — Parent Profile

---

### Task 9.1 — Parent Profile (P11)

**Description:** A shared Compose screen displaying the parent's profile information, their child's details, and notification preferences. The screen emphasizes the child's medical information (especially allergies) and provides quick access to the treating surgeon's contact.

**UI Sections:**

- **Child Section (prominent):**
  - Child's name, age, gender
  - Blood group
  - **Allergy Badge (red):** Prominently displayed red badge showing known allergies. If no allergies, a green "No Known Allergies" badge is shown instead.
  - Treating surgeon name

- **Treating Surgeon Row:**
  - Surgeon name and hospital
  - **Tap to Call:** Tapping opens the native phone dialer intent with the surgeon's phone number pre-filled. Uses KMP platform-specific implementation for dialer intent.

- **WhatsApp Notification Toggles:**
  - OPD Summary: On/Off toggle
  - Admission Alerts: On/Off toggle
  - Surgery Updates: On/Off toggle
  - Discharge Summary: On/Off toggle
  - Follow-up Reminders: On/Off toggle
  - All toggles are On by default. Toggling Off prevents the corresponding WhatsApp message from being sent (but Push notifications continue).

- **Logout Button:** Clears JWT from SQLDelight, unregisters FCM token, navigates to P1 (Login).

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/profile` | Returns the parent's profile, child details, and current notification preferences. |
| PATCH | `/parent/profile/notifications` | Updates the parent's WhatsApp notification preferences. Each toggle maps to a boolean field. |

**Acceptance Criteria:**

- All toggles persist their state to the backend and survive app restarts
- Native dialer opens with the surgeon's phone number when the surgeon row is tapped
- Allergy badge is prominently displayed in red when allergies exist
- "No Known Allergies" green badge is shown when no allergies are recorded
- Logout clears all local data and navigates to the login screen

---

## 13. Phase 10 — QA, Native Store Deployment & Launch

**Goal:** Full regression, App Store & Play Store submissions, security audit, production deploy  
**Duration:** 2 days  
**Owner:** All 3 devs  
**Screens:** All 24 screens — full regression

---

### Task 10.1 — Android APK/AAB Generation & Play Store Setup

**Description:** Configure the Android signing configs for release builds. Generate a signed Android App Bundle (AAB) from the KMM project's `androidApp` module. Set up a Google Play Console developer account, create the app listing with all required metadata, set the content rating (Medical category), and upload the AAB to the Internal/Beta testing track for initial distribution.

**Steps:**

1. Generate a keystore for signing (`keytool -genkey -v -keystore nonitura.jks`)
2. Configure `androidApp/build.gradle.kts` with signing configs (release build type)
3. Build the AAB: `./gradlew :androidApp:bundleRelease`
4. Create Google Play Console account ($25 one-time fee)
5. Create app listing:
   - App name: Noni Tura
   - Category: Medical
   - Content rating: Complete the IARC questionnaire (pediatric surgical care app)
   - Privacy policy URL (required)
   - App description, screenshots, feature graphic
6. Upload AAB to Internal Testing track
7. Generate internal test link for distribution to test users

**Acceptance Criteria:**

- AAB builds successfully without errors
- AAB is uploaded to Google Play Console
- App is accessible via internal test link
- App installs and runs correctly on a physical Android device

---

### Task 10.2 — iOS IPA Generation & App Store Setup

**Description:** Configure the Xcode workspace generated from the KMM project. Set up Apple Developer certificates and provisioning profiles. Build the IPA via Xcode. Create an App Store Connect listing with all required metadata, upload via Transporter, and submit for TestFlight beta review.

**Steps:**

1. Open the KMM-generated Xcode workspace (`iosApp/iosApp.xcworkspace`)
2. Configure signing in Xcode:
   - Apple Developer account ($99/year)
   - Development and Distribution certificates
   - Provisioning profiles (development + App Store)
3. Build for generic iOS device (Release configuration)
4. Archive the build in Xcode
5. Upload archive to App Store Connect via Xcode Organizer or Transporter
6. Create App Store Connect listing:
   - App name: Noni Tura
   - Category: Medical
   - Age rating: Complete the questionnaire
   - Privacy policy URL (required)
   - App description, screenshots (required for iPhone and iPad)
7. Submit for TestFlight beta review
8. Send TestFlight invites to test users

**Acceptance Criteria:**

- IPA builds successfully from the KMM Xcode workspace
- IPA is uploaded to App Store Connect
- TestFlight invite is sent and the app installs on a physical iOS device
- App runs correctly on iOS without crashes or layout issues

---

### Task 10.3 — Web Wasm Deployment

**Description:** Export the Compose for Web (Wasm) module from the KMM project. Deploy the generated static files to Vercel. Configure routing fallback to ensure that client-side navigation works correctly without 404 errors on page refresh.

**Steps:**

1. Build the Wasm module: `./gradlew :webApp:wasmJsBrowserDistribution`
2. The output is a set of static files (HTML, JS, Wasm) in the `webApp/build/dist/wasmJs/productionExecutable/` directory
3. Deploy to Vercel:
   - Connect the Git repository or use Vercel CLI
   - Set the output directory to the Wasm build output
   - Configure `_redirects` or `vercel.json` for SPA fallback:
     ```json
     {
       "rewrites": [
         { "source": "/(.*)", "destination": "/index.html" }
       ]
     }
     ```
4. Configure custom domain (if applicable)
5. Verify HTTPS is enforced

**Acceptance Criteria:**

- Web app is live on a Vercel URL
- Login and navigation work correctly without 404 errors on refresh
- All screens render correctly in modern browsers (Chrome, Safari, Firefox)
- HTTPS is enforced

---

### Task 10.4 — Doctor Pool Isolation — Security Audit

**Description:** A comprehensive security audit to verify that surgeon pool isolation is correctly enforced across all API endpoints. The audit must confirm that Surgeon A cannot access Surgeon B's patients under any circumstances. Every endpoint that returns patient-related data must validate the JWT's `doctor_id` against the resource's `doctor_id` and return a 403 Forbidden response on mismatch.

**Audit Checklist:**

- [ ] All GET endpoints returning patient data validate `doctor_id` from JWT
- [ ] All POST/PATCH/DELETE endpoints on patient-related resources validate ownership
- [ ] No endpoint allows querying or filtering by another surgeon's `doctor_id`
- [ ] Search and filter operations cannot bypass `doctor_id` scoping
- [ ] JWT token cannot be tampered with to change `doctor_id` (verify signature)
- [ ] Parent endpoints validate `patient_id` from JWT — no cross-patient access
- [ ] No data leaks in error messages (e.g., "Patient not found" instead of "Patient belongs to another surgeon")
- [ ] All 20+ backend endpoints tested with both valid and invalid `doctor_id` JWTs

**Test Matrix:**

For each endpoint, test with:
1. Valid JWT (correct `doctor_id`) → expect 200
2. Invalid JWT (wrong `doctor_id`) → expect 403
3. Missing JWT → expect 401
4. Expired JWT → expect 401

**Acceptance Criteria:**

- Zero cross-doctor data leaks across all endpoints
- All 20+ endpoints return 403 on cross-surgeon access attempts
- All endpoints return 401 on missing or expired JWT
- No sensitive data is exposed in error messages

---

### Task 10.5 — WhatsApp Template Approval

**Description:** Ensure all 5 WhatsApp message templates submitted in Phase 3 (Task 3.1) are approved by Meta Business Manager. Meta's approval process typically takes 24-48 hours, so templates submitted early in Phase 3 should be approved by Phase 10. If any template is rejected, revise and resubmit immediately.

**Templates to Verify:**

| # | Template Name | Status |
|---|---|---|
| 1 | `opd_summary` | Pending → Approved |
| 2 | `follow_up_reminder` | Pending → Approved |
| 3 | `admission_alert` | Pending → Approved |
| 4 | `post_surgery_update` | Pending → Approved |
| 5 | `discharge_summary` | Pending → Approved |

**Template Requirements (per Meta policies):**

- English language only
- No promotional content — purely transactional/informational
- Must include an opt-out line ("Reply STOP to unsubscribe")
- Must comply with healthcare messaging policies
- Template parameters must be correctly defined

**Acceptance Criteria:**

- All 5 templates are approved by Meta Business Manager
- Test messages are delivered successfully to +91 Indian phone numbers
- Each template's parameters are correctly populated with real patient data
- Failed template sends are logged with clear error messages

---

### Task 10.6 — E2E Test — Surgeon

**Description:** Complete end-to-end regression test of the entire surgeon journey on both Android and iOS. The test covers all 13 surgeon screens in sequence, verifying that the complete clinical workflow functions correctly and that all WhatsApp + Push notification triggers fire at the right moments.

**Test Flow:**

1. **Login (P1 → P2):** Enter surgeon phone, verify OTP, confirm routing to Surgeon Dashboard (S1)
2. **Dashboard (S1):** Verify metrics, surgery cards, IPD cards, FAB
3. **Add Patient (S2 FAB):** Create a new patient, verify it appears in Patient List
4. **Create OPD Consult (S3):** Fill all fields, save, verify WhatsApp preview (S12) opens
5. **WhatsApp Preview (S12):** Review message, send via WhatsApp, verify delivery
6. **Patient Profile (S10):** Verify OPD history, tabs, document upload
7. **Admit Patient (S4):** Create IPD admission, verify WA + Push admission alert sent to parent
8. **Pre-op Notes (S5):** Fill pre-op form, load template, mark complete
9. **Intra-op Notes (S6):** Fill intra-op form, save, verify WA + Push post_surgery_update sent to parent
10. **Post-op Notes (S7):** Create post-op entry, verify day calculation
11. **Ward Round (S8):** Create ward round entry, mark ready for discharge
12. **Discharge (S9):** Fill discharge form, save, verify WA + Push discharge_summary sent to parent
13. **Schedule (S11):** Verify OT and OPD schedules, create a new booking
14. **Profile & Settings (S13):** Verify stats, template management, logout

**Notification Triggers to Verify (4 total):**

| # | Trigger | Expected Notification |
|---|---|---|
| 1 | OPD consult saved | WhatsApp `opd_summary` + Push to parent |
| 2 | Patient admitted | WhatsApp `admission_alert` + Push to parent |
| 3 | Intra-op notes saved | WhatsApp `post_surgery_update` + Push to parent |
| 4 | Discharge saved | WhatsApp `discharge_summary` + Push to parent |

**Acceptance Criteria:**

- Zero broken flows across all 13 surgeon screens
- Push notifications and WhatsApp messages fire correctly at all 4 trigger points
- All form data is saved and retrieved correctly
- Navigation between screens works without errors
- Test passes on both Android and iOS physical devices

---

### Task 10.7 — E2E Test — Parent

**Description:** Complete end-to-end regression test of the entire parent journey. The test covers all 11 parent screens in sequence, verifying that the parent sees accurate real-time updates, can download PDFs, and can book appointments.

**Test Flow:**

1. **Login (P1 → P2):** Enter parent phone, verify OTP, confirm routing to Parent Home (P3)
2. **Parent Home (P3):** Verify surgery status card, next appointment, follow-up pill, last consult, educational content
3. **Surgery Status Detail (P4):** Verify timeline with 5 nodes, active node animation, share on WhatsApp
4. **OPD Records List (P5):** Verify filter chips, records list, tap to view detail
5. **Consult Record Detail (P6):** Verify read-only cards, lock icon, download PDF, share on WhatsApp
6. **Appointment Booking — Step 1 (P7):** Select a surgeon
7. **Appointment Booking — Step 2 (P8):** Select an available slot
8. **Appointment Booking — Step 3 (P9):** Review and confirm
9. **Booking Confirmed (P10):** Verify success animation, WA + Push confirmation sent
10. **Parent Profile (P11):** Verify child info, allergy badge, surgeon contact, WA toggles, logout

**Notification Triggers to Verify:**

- Push notifications are received for: admission, surgery update, discharge, appointment confirmation, follow-up reminder
- WhatsApp messages are received for all triggered events (if WA toggles are On)

**Acceptance Criteria:**

- Zero broken flows across all 11 parent screens
- Timeline updates in real-time when the surgeon makes changes
- PDF downloads and opens correctly on the device
- WhatsApp share works via native share intent
- Appointment booking creates a record and sends confirmations
- No cross-patient data is visible

---

### Task 10.8 — Production Backend Deploy & Monitoring

**Description:** Deploy the FastAPI backend to production on Render. Set up the Neon PostgreSQL production branch. Inject Sentry DSN into the KMM shared module for crashlytics across Android, iOS, and Web. Enforce HTTPS on all endpoints. Configure monitoring and alerting.

**Deployment Steps:**

1. **Backend → Render:**
   - Create a new Render Web Service
   - Connect the Git repository
   - Set environment variables: `DATABASE_URL`, `JWT_SECRET`, `2FACTOR_API_KEY`, `META_WA_TOKEN`, `META_WA_PHONE_ID`, `FIREBASE_CREDENTIALS_JSON`, `CLOUDINARY_URL`, `SENTRY_DSN`
   - Configure build command: `pip install -r requirements.txt && prisma generate`
   - Configure start command: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - Enable auto-deploy from the main branch

2. **Neon PostgreSQL → Production Branch:**
   - Create a production branch in Neon
   - Run `prisma db push` against the production database
   - Verify all tables and relations are created correctly
   - Enable Neon's connection pooling for production

3. **Sentry Integration:**
   - Create Sentry project for the application
   - Inject `SENTRY_DSN` into the KMM shared module
   - Initialize Sentry SDK in each platform's entry point:
     - Android: `androidApp/mainActivity`
     - iOS: `iosApp/AppDelegate`
     - Web: `webApp/main`
   - Verify crash reports are captured on all 3 platforms

4. **HTTPS Enforcement:**
   - Render provides HTTPS by default
   - Verify all API endpoints redirect HTTP → HTTPS
   - Set `Secure` and `HttpOnly` flags on cookies (if applicable)
   - Configure CORS to allow only the production frontend domains

5. **Monitoring & Alerting:**
   - Set up Sentry alerts for new errors and high error rates
   - Configure Render health checks for the `/health` endpoint
   - Set up UptimeRobot or similar for external monitoring

**Acceptance Criteria:**

- Backend is live on the production API URL
- Sentry is capturing native crashes on Android, iOS, and Web
- No console errors in the production web app
- HTTPS is enforced on all endpoints
- Health check endpoint returns 200 consistently
- Database connections are stable under load (even with < 100 patients)

---

## 14. Summary — Auto-Triggers & Notifications

### WhatsApp Auto-Triggers

| # | Trigger Event | WhatsApp Template | Recipient |
|---|---|---|---|
| 1 | OPD consult saved | `opd_summary` | Parent |
| 2 | Patient admitted to IPD | `admission_alert` | Parent |
| 3 | Intra-op notes saved | `post_surgery_update` | Parent |
| 4 | Discharge summary saved | `discharge_summary` | Parent |
| 5 | Follow-up date reached (9AM IST daily cron) | `follow_up_reminder` | Parent |

### Push Notification Events

| # | Event | Platform | Recipient |
|---|---|---|---|
| 1 | Admission alert | FCM (Android) + APNs (iOS) | Parent |
| 2 | Post surgery update | FCM (Android) + APNs (iOS) | Parent |
| 3 | Discharge summary | FCM (Android) + APNs (iOS) | Parent |
| 4 | Appointment confirmation | FCM (Android) + APNs (iOS) | Parent |
| 5 | Follow-up reminder | FCM (Android) + APNs (iOS) | Parent |

---

## 15. Summary — Pricing & Client OpEx

### Build Cost

| Item | Cost |
|---|---|
| One-time Build | ₹1,50,000 (includes native iOS/Android/Web KMM architecture) |
| Monthly Maintenance | ₹15,000/month (includes App Store/Play Store compliance updates) |

### Client Monthly Operating Expenses (OpEx)

| Service | Cost |
|---|---|
| WhatsApp API (Meta) | ₹2,000 – ₹6,000/month (usage-based) |
| Apple Developer Account | $99/year (~₹8,300/year) |
| Google Play Account | $25 one-time (~₹2,000) |
| Hosting (Render + Vercel) | ~₹800/month (free tiers initially) |
| Neon PostgreSQL | Free tier sufficient for < 100 patients |
| Firebase Notifications | Free tier |
| Cloudinary (file storage) | Free tier sufficient for MVP |

---

## Doctor Pool Isolation Architecture

The fundamental security architecture of Noni Tura is built on **doctor pool isolation**. This ensures that each surgeon's patient data is completely siloed and inaccessible to the other surgeon.

**Implementation:**

- Every patient-related table includes a `doctor_id` foreign key pointing to the `doctors` table
- All API endpoints extract the `doctor_id` from the JWT token and scope all database queries accordingly
- Any attempt to access a resource belonging to another surgeon returns a 403 Forbidden response
- The patient list, OPD records, IPD admissions, documents, and all other data are filtered by `doctor_id` at the database query level
- No client-side filtering is relied upon for security — all isolation is enforced server-side

**Tables with `doctor_id` foreign key:**

- `patients`
- `opd_records`
- `ipd_admissions`
- `appointments`

(Tables that reference these via `patient_id` or `admission_id` are implicitly scoped through their parent records.)

---

*KMM Advantage: Single shared codebase for UI (Compose Multiplatform), business logic, networking, and local storage across Android, iOS, and Web. Native performance and UX, direct Play Store and App Store distribution.*
