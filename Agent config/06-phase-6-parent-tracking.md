# Phase 6 — Parent Flow: Surgery Tracking & Records

**Goal:** Parent sees child's live surgery journey, OPD records, consent forms, shares summaries.  
**Duration:** 1 day  
**Owner:** Dev 2 + Dev 3  
**Screens:** P3–P6  

---

## Task 6.1 — Parent Home (P3)

**Description:** Primary landing screen for parents after OTP login. Provides at-a-glance view of child's current status, upcoming appointments, recent consultations, and consent form status.

**UI Components:**

- **Surgery Status Card (amber, full width):**
  - If child currently admitted: Shows ward, bed number, surgeon name, status pill (Admitted / Pre-op / In Surgery / Recovery).
  - If not admitted: Shows last discharge summary with "View Details" button.
  - Tapping navigates to Surgery Status Detail (P4).

- **Next Appointment Card (teal):** Shows next upcoming appointment date, time, surgeon name.

- **Follow-up Pill:** Small pill showing follow-up date if scheduled.

- **Last Consult Card:** Compact card showing date and diagnosis from most recent OPD.

- **Consent Form Status Card (NEW):**
  - If a consent form is pending signature: Shows "Consent Form Pending" with red border. Tapping navigates to consent form signing screen.
  - If all signed: Shows "All Consent Forms Signed" with green checkmark.

- **Educational Content (horizontal LazyRow):** Pre-defined static content cards related to pediatric surgery care.

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/home` | Returns all data for parent's home screen. Scoped to authenticated parent's `patient_id`. |

**Acceptance Criteria:**

- Surgery card shows live status matching current admission record
- Push notifications tapped route to this screen first
- No cross-surgeon or cross-patient data displayed
- All cards tappable and navigate to correct detail screens
- **Consent form status card accurately reflects pending/signed state**

---

## Task 6.2 — Surgery Status Detail (P4)

**Description:** A shared Compose screen displaying a visual timeline of the child's surgical journey. 5 nodes from admission to discharge. Active node highlighted with amber pulse animation. "Share on WhatsApp" button allows sharing status with family.

**Timeline Nodes (5 stages):**

| Node | Label | Description |
|---|---|---|
| 1 | Admitted | Patient has been admitted to the ward |
| 2 | Pre-op | Pre-operative assessment is complete |
| 3 | In Surgery | Surgery is currently in progress |
| 4 | Recovery | Surgery completed, patient is recovering |
| 5 | Discharged | Patient has been discharged |

**UI Components:**

- **Timeline Visual:** Horizontal or vertical timeline. Completed nodes filled (green), active node pulses (amber), future nodes outlined (gray).
- **Active Node Animation:** Compose Animation (pulse/scale) with amber color.
- **Stage Detail Card:** Shows details specific to current stage.
- **Share on WhatsApp Button (#25D366 green):** Native share intent with pre-formatted message.
- **View Consent Form Button (NEW):** If consent form exists for this admission, shows "View Consent Form" button. Tapping opens PDF viewer.

**API Endpoint:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/admissions/current` | Returns current active admission with status, timeline details, and consent form status. |

**Acceptance Criteria:**

- Timeline accurately reflects current admission status
- Active node amber pulse animation works on all platforms
- Native share intent opens WhatsApp with correct message
- All 5 timeline stages render correctly
- **Consent form button visible when consent form exists**

---

## Task 6.3 — OPD Records List + Consult Record Detail (P5, P6)

**Description:** Two shared Compose screens providing parent access to child's OPD consultation history. P5 is a filterable list; P6 is a read-only detailed view with PDF download and WhatsApp share.

**Screen P5 — OPD Records List:**

- **Filter Chips:** All / Surgery Recommended / Follow-up / Routine
- **Records List (LazyColumn):** Each row shows consultation date, chief complaint, diagnosis, surgical decision pill. Tapping navigates to P6.

**Screen P6 — Consult Record Detail:**

- **Read-only Cards:** Patient Info, Chief Complaint, Examination, Diagnosis, Surgical Decision, Medications, Investigations, Advice, Follow-up Date.
- **Lock Icon:** Indicates read-only data.
- **Download PDF Button:** Generates PDF on backend and downloads to device.
- **Share on WhatsApp + Download PDF (sticky bottom bar):**
  - Share on WhatsApp: Native share intent
  - Download PDF: Binary file download
- **AI Diagnosis Suggestion Card (NEW):** If AI suggestions were generated for this OPD record, shows a collapsed card with "AI Suggestions Generated" label. Tapping expands to show the ranked suggestions (for transparency). Includes disclaimer.

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/opd-records` | Returns all OPD records for authenticated parent's child. Supports `?filter=`. |
| GET | `/parent/opd-records/:id` | Returns detailed OPD record. |
| GET | `/parent/opd-records/:id/pdf` | Generates and returns PDF of OPD consultation record. |
| GET | `/parent/opd-records/:id/ai-suggestions` | **NEW** — Returns AI diagnosis suggestions for this record (if any). |

**Acceptance Criteria:**

- PDF downloads and opens natively on all platforms
- Lock icon visible on all read-only cards
- WhatsApp share works via native share intent
- Filter chips correctly filter list
- No cross-surgeon or cross-patient data accessible
- **AI suggestions visible to parent for transparency (read-only, with disclaimer)**
