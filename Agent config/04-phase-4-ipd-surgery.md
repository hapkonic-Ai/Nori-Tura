# Phase 4 — Surgeon: IPD & Surgery Flow + Consent Forms

**Goal:** Full surgical workflow — admit → pre-op → intra-op → post-op → ward rounds → discharge. Plus consent form generation at admission/pre-op.  
**Duration:** 1.5 days  
**Owner:** All 3 devs  
**Screens:** S4–S9, N4 (Nurse Ward Round)  

---

## Task 4.1 — Admit Patient (S4)

**Description:** A shared Compose form for admitting a patient to IPD. On submission, the system creates an `ipd_admissions` record and automatically triggers an admission alert via WhatsApp and Push Notification to the parent. **Consent form generation is triggered here** if urgency is not "routine".

**Form Fields:**

- **Patient Selector:** Searchable dropdown. Required.
- **Admission Date/Time:** Date and time pickers. Defaults to current.
- **Bed Number:** Text input.
- **Ward:** Text input or dropdown (Pediatric ICU, General Ward, Semi-Private).
- **Urgency Chips:** Routine / Urgent / Emergency. Required. Color-coded: green / amber / red.
- **Consent Toggle:** Toggle switch — Consent Obtained / Pending. Required.
- **Allergies:** Auto-populated from patient record. Editable.

**NEW — Consent Form Trigger:**
- If Urgency = "Urgent" or "Emergency", after admission creation, a consent form generation prompt appears:
  - "Generate consent form for [procedure]?"
  - If surgeon taps "Generate", navigates to Consent Form Preview (CF1) with patient data pre-filled.
  - If "Skip", admission proceeds without consent form (surgeon can generate later from Pre-op).

**Post-Submit Flow:**

1. Create `ipd_admissions` record with `status = "admitted"`
2. Trigger `admission_alert` WhatsApp template to `parent_phone`
3. Trigger Push Notification to parent's FCM token
4. Trigger Push Notification to nurse's FCM token
5. Log all sends in `whatsapp_logs`
6. Navigate back to Surgeon Dashboard (S1)

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions` | Creates a new IPD admission. Auto-sets `doctor_id`. Triggers WA + Push. |

**Acceptance Criteria:**

- Admission record created with correct fields
- WhatsApp and Push notifications sent to parent and nurse
- Newly admitted patient appears in IPD section on S1
- Consent form prompt appears for urgent/emergency admissions
- Form validates required fields before submission

---

## Task 4.2 — Pre-op Notes (S5)

**Description:** A shared Compose form for recording pre-operative assessment notes, linked to a specific `admission_id`. Includes a template loader button. On completion, admission status updated to "pre-op". **Consent form generation is available here** if not done during admission.

**Form Fields:**

- **Procedure:** Text input. Required.
- **Approach:** Open / Laparoscopic / Robotic / Combined.
- **Anaesthesia Chips:** General / Regional / Local / Sedation.
- **Investigations Checklist:** Pre-operative investigations.
- **Risk Chips:** Low / Moderate / High.
- **Consent Toggle:** Confirming informed consent obtained.
- **Template Loader Button:** Opens bottom sheet listing saved surgical templates.

**NEW — Consent Form Button:**
- "Generate Consent Form" button at the bottom of the screen.
- Tapping opens Consent Form Preview (CF1) with:
  - Patient name, age, guardian name pre-filled
  - Procedure and anaesthesia pre-filled from this form
  - Risk level pre-filled
- Surgeon can review, edit, and generate PDF.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/pre-op` | Creates pre-op notes. Validates `doctor_id` ownership. |
| GET | `/admissions/:id/pre-op` | Retrieves existing pre-op notes. |

**Acceptance Criteria:**

- Pre-op notes saved and linked to correct admission
- Template loader pre-fills fields
- Marking "Complete" updates admission status to "pre-op"
- Consent form generation button visible and functional
- Form renders correctly on all platforms

---

## Task 4.3 — Surgical Templates Manager

**Description:** A CRUD interface for managing named surgical templates. Surgeons can create, save, load, update, and delete templates for common procedures. Templates pre-fill pre-op and intra-op form fields. Templates are scoped to the surgeon's `doctor_id`.

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
| POST | `/templates` | Creates a new template. Auto-sets `doctor_id`. |
| PUT | `/templates/:id` | Updates an existing template. Validates ownership. |
| DELETE | `/templates/:id` | Deletes a template. Validates ownership. |

**Acceptance Criteria:**

- Full CRUD operations work correctly
- Loading a template pre-fills corresponding fields in S5 and S6
- Templates isolated per surgeon
- Template list accessible from Pre-op and Intra-op screens
- **Nurses can view templates (read-only) but cannot create/edit/delete**

---

## Task 4.4 — Intra-op Notes (S6)

**Description:** A shared Compose screen for recording intra-operative notes. OT start time auto-timestamped when screen is opened. On save, triggers `post_surgery_update` WhatsApp and Push to parent.

**Form Fields:**

- **OT Start:** Auto-timestamped. Editable.
- **Procedure Done:** Text input. Required.
- **Findings:** Multi-line text input.
- **Technique:** Multi-line text input.
- **Complications:** Toggle Yes/No. If "Yes", red-bordered text area appears.
- **Blood Loss:** Text input (e.g., "Minimal", "~50 ml").
- **Specimens:** Text input for histopathology/culture.
- **OT End:** Time picker.

**Post-Save Flow:**

1. Save intra-op notes
2. Update `ipd_admissions` status to "in-surgery" or "recovery"
3. Trigger `post_surgery_update` WhatsApp template to parent
4. Trigger Push Notification to parent
5. Log both sends in `whatsapp_logs`

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/intra-op` | Creates intra-op notes. Validates `doctor_id`. Triggers WA + Push. |

**Acceptance Criteria:**

- OT start time auto-set when screen opens
- Complications toggle activates red-bordered text area
- WhatsApp and Push notifications fired to parent on save
- All timestamps in IST (Asia/Kolkata)
- **Nurse cannot access this screen**

---

## Task 4.5 — Post-op Notes (S7)

**Description:** A shared Compose screen for recording post-operative notes. Post-op day number auto-calculated from surgery date. Multiple entries possible (one per day).

**Form Fields:**

- **Post-op Day N:** Auto-calculated. Display-only.
- **Condition Chips:** Stable / Improved / Critical / Guarded.
- **Vitals Row (inline inputs, stored as JSON):** HR, BP, Temp, SpO2, RR.
- **Wound Status:** Clean / Infected / Healing Well / Needs Attention.
- **Pain Score:** Slider 0-10. Color gradient green→red.
- **Diet Chips:** NPO / Clear Fluids / Full Fluids / Soft Diet / Regular.
- **Medications (repeatable rows):** Same structure as OPD medications.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/post-op` | Creates a post-op note. Validates `doctor_id`. |
| GET | `/admissions/:id/post-op` | Returns all post-op notes, sorted by `day_number`. |

**Acceptance Criteria:**

- Post-op day number calculated correctly
- Vitals stored as structured JSON
- Multiple post-op note entries can be created
- Pain score slider provides visual feedback
- **Nurse cannot access this screen**

---

## Task 4.6 — Daily Ward Round (S8 / N4)

**Description:** A shared Compose screen for recording daily ward round notes in SOAP format. **Both surgeons and nurses can use this screen.** Nurses see the same form but with a "Submitted by Nurse" watermark.

**UI Layout:**

- **Quick Vitals (top section):** Inline inputs for HR, BP, Temp, SpO2, RR.
- **SOAP Notes:**
  - **Subjective:** Multi-line text input.
  - **Objective:** Multi-line text input.
  - **Assessment:** Multi-line text input with assessment chip suggestions.
  - **Plan:** Multi-line text input.
- **Ready for Discharge Button:** Amber-colored. Sets `ready_for_discharge = true`. Triggers visual indicator on S1.

**Role Differentiation:**
- **Surgeon:** Full access. Can mark "Ready for Discharge."
- **Nurse:** Can fill all SOAP fields and vitals. Cannot mark "Ready for Discharge" (button hidden).

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/ward-round` | Creates a ward round note. Validates `doctor_id` ownership. Accepts optional `nurse_id`. |
| GET | `/admissions/:id/ward-rounds` | Returns all ward round notes, sorted chronologically. |

**Acceptance Criteria:**

- Multiple ward round entries per admission
- History displayed in chronological order
- "Ready for Discharge" triggers dashboard indicator on S1
- SOAP fields clearly labeled
- Quick vitals convenient for fast data entry
- **Nurse can create ward round notes but cannot mark ready for discharge**

---

## Task 4.7 — Discharge (S9)

**Description:** A shared Compose screen for creating the discharge summary. Final step of IPD journey. Includes discharge details, procedure summary (auto-drafted from intra-op notes), medications, wound care, activity restrictions, diet, follow-up date, and red flags.

**Form Fields:**

- **Discharge Date:** Date picker. Defaults to today.
- **Condition at Discharge:** Stable / Improved / Recovered / With Complications.
- **Procedure Summary:** Auto-drafted from intra-op notes (`procedure_done` + `findings`). Editable.
- **Discharge Medications (repeatable rows):** Same as OPD medications.
- **Wound Care:** Multi-line text input.
- **Activity Restrictions:** Multi-line text input.
- **Diet Instructions:** Regular / Soft Diet / Full Fluids / Special Diet.
- **Follow-up Date:** Date picker with "Send Reminder" toggle.
- **Red Flags:** Multi-line text input for warning signs.

**Post-Submit Flow:**

1. Create `discharge_summaries` record
2. Update `ipd_admissions` status to "discharged" and set `discharge_at`
3. Trigger `discharge_summary` WhatsApp template to parent
4. Trigger Push Notification to parent
5. Log both sends in `whatsapp_logs`
6. Navigate back to Surgeon Dashboard

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admissions/:id/discharge` | Creates discharge summary. Validates `doctor_id`. Updates admission status. Triggers WA + Push. |

**Acceptance Criteria:**

- Discharge summary created with all fields
- Admission status updated to "discharged"
- Patient moves to "Discharged" filter in Patient List (S2)
- WhatsApp and Push notifications fired to parent
- Procedure summary auto-drafted from intra-op notes and editable
- Red flags section prominently displayed
- **Nurse cannot access this screen**
