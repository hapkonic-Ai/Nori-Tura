# Phase 5 — Surgeon & Nurse: Schedule

**Goal:** OT surgery schedule and OPD consult schedule. Both surgeons and nurses can view; only surgeons can create/modify.  
**Duration:** 0.5 days  
**Owner:** Dev 1  
**Screens:** S11 — OT and OPD Schedule, N6 — Nurse Schedule Viewer  

---

## Task 5.1 — Schedule Screen (S11)

**Description:** A shared Compose screen for managing the surgeon's OT surgery schedule and OPD consult schedule. Uses TopBar tabs to switch between OT and OPD views. A horizontal week strip at the top allows navigation between dates. Slots displayed as cards or list items.

**UI Components:**

- **TopBar Tabs:** OT Schedule / OPD Schedule
- **Week Strip (horizontal scrollable row):** Shows 7 days. Current day highlighted (teal). Left/right arrows for week navigation.
- **OT Schedule Tab:** Surgery slot cards showing patient name, procedure, OT time, urgency pill, status. Empty slots: dashed border with "+" Add button.
- **OPD Schedule Tab:** Consult slot list showing patient name, slot time, visit type, status. Empty slots: dashed border with "+" Add button.
- **FAB:** "+ New Booking"

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/schedule/ot` | Returns OT surgery slots for selected date. Scoped to `doctor_id`. |
| GET | `/schedule/opd` | Returns OPD consult slots for selected date. Scoped to `doctor_id`. |
| POST | `/schedule/ot` | Creates a new OT surgery slot. Parameters: `patient_id`, `date`, `time`, `procedure`, `urgency`. |
| POST | `/schedule/opd` | Creates a new OPD consult slot. Parameters: `patient_id`, `date`, `time`, `visit_type`. |

**Acceptance Criteria:**

- Both OT and OPD tabs render correctly
- Week navigation smooth and responsive
- Slots bookable and appear immediately after creation
- Bookings feed into appointment system (Phase 7)
- Empty slots show dashed border with "Add" button
- Data scoped to authenticated surgeon's `doctor_id`

---

## Task 5.2 — Nurse Schedule Viewer (N6) — NEW

**Description:** A read-only schedule viewer for nurses. Shows the same OT and OPD schedule data as S11 but without the ability to create, edit, or delete bookings. Useful for nurses to prepare for upcoming appointments and admissions.

**UI Components:**

- **TopBar Tabs:** OT Schedule / OPD Schedule (same as S11)
- **Week Strip:** Same as S11 but read-only
- **OT Schedule Tab:** Surgery slot cards without "Add" buttons. Tapping a slot shows a detail bottom sheet with patient name, procedure, time, and ward (if admitted).
- **OPD Schedule Tab:** Consult slot list without "Add" buttons. Tapping shows patient name, visit type, and phone number.
- **No FAB**

**Accent Color:** Coral to indicate read-only nurse view.

**API Endpoints:**
- Same GET endpoints as S11 (`/schedule/ot`, `/schedule/opd`).
- Nurse JWT is validated and `doctor_id` is resolved from `nurses.doctor_id`.

**Acceptance Criteria:**

- Nurse can view full schedule for their assigned surgeon
- No create/edit/delete buttons visible
- Tapping a slot shows detail bottom sheet (read-only)
- Data auto-refreshes on screen resume
- No cross-surgeon data visible
