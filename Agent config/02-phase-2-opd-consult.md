# Phase 2 — Surgeon & Nurse: OPD Consult Flow

**Goal:** Surgeon can add patients, create OPD consult notes (with AI diagnosis suggestions), tag patients, and set follow-up. Nurse can add patients, create OPD records without surgical decision, and manage clinical notes.  
**Duration:** 1 day  
**Owner:** Dev 1 (KMM UI) + Dev 2 (Backend)  
**Screens:** S1–S3, S10, N1–N3  

---

## Task 2.1 — Surgeon Dashboard (S1)

**Description:** The surgeon dashboard is the primary landing screen after login. It provides an at-a-glance overview of today's clinical activity scoped to the authenticated surgeon's `doctor_id` only. Four metric chips at the top show key counts, followed by surgery cards and IPD ward round cards. A Floating Action Button (FAB) provides quick access to create a new OPD consult or admit a patient.

**UI Components:**

- **4 Metric Chips (horizontal row):**
  1. Today's Surgeries — count of admissions with `status = "in-surgery"` for today
  2. Pre-op Pending — count of admissions with `status = "pre-op"` and no completed pre-op notes
  3. Active IPD — count of admissions with `status` in (admitted, pre-op, in-surgery, recovery)
  4. Today's OPD Consults — count of OPD records created today

- **Surgery Cards (LazyColumn):** Each card shows patient name, procedure, OT time, status pill (color-coded). Tappable to navigate to admission detail.

- **IPD Ward Round Cards (LazyColumn):** Each card shows patient name, ward, bed number, day number, and a "last rounded" timestamp. Tappable to navigate to ward round entry (S8).

- **FAB:** Two options on tap:
  1. New OPD Consult → Navigate to S3
  2. Admit Patient → Navigate to S4

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/surgeon/dashboard` | Returns today's metrics, surgery list, and IPD ward round list scoped to `doctor_id` |

**Acceptance Criteria:**

- All 4 metric chips populated with live data
- Native FAB and cards render correctly on all platforms
- No cross-surgeon data is displayed
- Dashboard auto-refreshes on screen resume

---

## Task 2.2 — Nurse Dashboard (N1) — NEW

**Description:** The nurse dashboard is the primary landing screen for nurses after login. It shows metrics relevant to nursing workflow: patients added today, OPDs recorded, upcoming appointments, and active IPD admissions. A FAB provides quick access to add a patient or create an OPD record.

**UI Components:**

- **4 Metric Chips:**
  1. Patients Added Today
  2. OPDs Recorded Today
  3. Upcoming Appointments (next 24h)
  4. Active IPD Admissions

- **Quick Actions Row:** Horizontal scrollable chips for common actions:
  - Add Patient
  - New OPD Record
  - Book Appointment
  - Ward Round Notes

- **Upcoming Appointments List (LazyColumn):** Shows next 5 appointments with time, patient name, and visit type.

- **Active IPD Cards:** Same as surgeon dashboard but read-only for nurse.

**Accent Color:** Coral (#FF6F61) to differentiate from surgeon screens (Teal).

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/nurse/dashboard` | Returns metrics scoped to the nurse's `doctor_id` |

**Acceptance Criteria:**

- Metrics populated with live data
- Quick actions navigate to correct screens
- No surgical decision buttons visible
- Auto-refreshes on screen resume

---

## Task 2.3 — Patient List (S2 / N3)

**Description:** A searchable, filterable patient list scoped to the authenticated user's `doctor_id`. Patients from the other surgeon's pool must never be visible. The list supports search by name or phone number and filter chips by patient status. An "Add Patient" FAB allows quick creation.

**UI Components:**

- **Search Bar:** Text input at the top. Filters by patient name or parent phone (debounced, 300ms).
- **Filter Chips:** All / Pre-op / Post-op / OPD / Discharged
- **Patient List (LazyColumn):** Each row shows patient name, age, gender, status pill, last visit date. Tappable to navigate to Patient Profile (S10).
- **FAB:** "+" Add Patient

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/patients` | Returns patients scoped to `doctor_id`. Supports `?search=` and `?status=`. |
| POST | `/patients` | Creates a new patient. Auto-sets `doctor_id` from JWT. |

**Acceptance Criteria:**

- Search works in real-time with debounced input
- Filter chips correctly filter the list
- Patients from the other surgeon are NOT visible
- Native scrolling performance on all platforms
- New patient immediately appears in the list after creation
- Both surgeon and nurse can access this screen (with appropriate FAB visibility)

---

## Task 2.4 — OPD Consult Form (S3) with AI Diagnosis Panel — MODIFIED

**Description:** A comprehensive shared Compose form for creating and editing OPD consultation records. **NEW:** After chief complaint and examination are entered, an AI Suggestive Diagnosis panel appears with ranked possible diagnoses. The surgeon can tap a suggestion to auto-fill the Diagnosis field or ignore it. The form includes auto-save draft functionality — persisted to SQLDelight every 30 seconds. After saving, a WhatsApp preview screen opens automatically.

**Form Fields:**

- **Patient Selector:** Dropdown or searchable list. Required.
- **Visit Type:** Chip selection — New Visit / Follow-up. Required.
- **Chief Complaint:** Multi-line text input. Required.
- **Examination:** Multi-line text input. Required.
- **AI Suggestive Diagnosis Panel (NEW):**
  - Collapsible panel below Examination field
  - Trigger: After Chief Complaint + Examination have ≥ 10 characters each
  - Shows loading shimmer while AI request is in flight
  - Displays 3–5 ranked diagnosis suggestions with confidence percentages
  - Each suggestion is tappable to auto-fill the Diagnosis field
  - Includes disclaimer: "AI-generated suggestion. Final diagnosis is the surgeon's responsibility."
  - "Refresh Suggestions" button to re-run the AI query

- **Diagnosis:** Text input with autocomplete from previous diagnoses. Can be auto-filled from AI panel.
- **Surgical Decision:** Segmented control — Surgery / No Surgery / Deferred. Required. When "Surgery" is selected, an optional "Planned Procedure" text field appears.
- **Advice:** Multi-line text input.
- **Medications (repeatable rows):** Name, Dose, Frequency (OD/BD/TDS/QID/PRN/SOS chips), Duration.
- **Investigations (checklist):** Blood Tests, Imaging, Others, Custom.
- **Tag Chips:** Multi-select — Urgent / Routine / Surgical / Medical / Follow-up.
- **Follow-up Date Picker:** Date picker with "Send Reminder" toggle.

**Auto-Save Draft Logic:**

- Every 30 seconds, current form state is serialized and saved to SQLDelight
- On form screen load, check for existing draft and prompt to resume or discard
- Draft is cleared on successful submission

**Post-Save Flow:**

1. Form submitted via `POST /opd-records` (or `PATCH` for edits)
2. On success, navigate to WhatsApp Preview (S12) with OPD summary pre-filled

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/opd-records` | Creates a new OPD record. Auto-sets `doctor_id` from JWT. |
| PATCH | `/opd-records/:id` | Updates an existing OPD record. Validates ownership. |
| POST | `/ai/suggest-diagnosis` | **NEW** — AI diagnosis suggestion endpoint. Accepts complaint, examination, age, gender. Returns ranked suggestions. |

**Acceptance Criteria:**

- Form saves successfully with all fields
- Surgical decision is mandatory
- Draft persists on app kill/restart
- WhatsApp preview (S12) opens after successful save
- **AI panel appears after complaint + examination are entered**
- **AI suggestions are tappable and auto-fill the Diagnosis field**
- **Disclaimer is visible on every AI suggestion**
- Form renders natively and scrolls smoothly on all platforms

---

## Task 2.5 — Nurse OPD Form (N2) — NEW

**Description:** A simplified OPD form for nurses. It is identical to the surgeon OPD form (S3) **except:**
- **No Surgical Decision field** — this is left blank for the surgeon to fill later.
- **No AI Diagnosis Panel** — nurses do not trigger AI suggestions.
- **"For Surgeon Review" banner** at the top indicating this record needs surgeon approval.

**Form Fields:**
- Patient Selector, Visit Type, Chief Complaint, Examination, Diagnosis (text input, no AI), Advice, Medications, Investigations, Tag Chips, Follow-up Date.

**Post-Save Flow:**
1. Form submitted via `POST /opd-records` with `created_by = "nurse"` and `nurse_id` set.
2. OPD record appears in surgeon's patient list with a "Pending Review" badge.
3. Surgeon can open and add Surgical Decision + AI diagnosis suggestions.

**API Endpoints:**
- Same as S3 but `POST /opd-records` accepts optional `nurse_id` field.

**Acceptance Criteria:**
- Nurse can create OPD records without surgical decision
- Records created by nurse are flagged for surgeon review
- Surgeon can later edit and add surgical decision

---

## Task 2.6 — Patient Profile (S10)

**Description:** A comprehensive shared Compose screen displaying the full patient history organized by TopBar Tabs. Serves as the central hub for all information related to a specific patient.

**UI Structure:**

- **TopBar:** Patient name, age, gender, status pill. Back navigation.
- **Tabs (TopBar horizontal):**
  1. **OPD History Tab:** Vertical timeline of all OPD records. Each entry shows visit date, chief complaint, diagnosis, medication summary. Tapping expands to show full details. Most recent at top.
  2. **Surgery History Tab:** Cards for each IPD admission. Shows admission date, procedure, status pill. Tapping navigates to admission detail.
  3. **IPD Timeline Tab:** Stubbed for now.
  4. **Documents Tab:** File upload list. Includes "Upload Document" FAB that opens multiplatform file picker, uploads to Cloudinary, saves document record.
  5. **Consent Forms Tab (NEW):** List of generated consent forms. Shows form type, generation date, signed status. Tapping opens PDF viewer.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/patients/:id` | Returns patient details. Validates `doctor_id` ownership. |
| GET | `/patients/:id/opd-records` | Returns all OPD records, sorted by `created_at` descending. |
| GET | `/patients/:id/admissions` | Returns all IPD admissions, sorted by `admitted_at` descending. |
| GET | `/patients/:id/consent-forms` | **NEW** — Returns all consent forms for the patient. |

**Acceptance Criteria:**

- Full patient history renders correctly across all tabs
- Tab switching is smooth and animated
- Cloudinary upload works natively
- OPD timeline entries expand/collapse correctly
- Consent forms tab shows generation date and signed status
- No cross-surgeon data is accessible
