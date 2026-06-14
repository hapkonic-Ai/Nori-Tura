# Nurse Module

## Overview

The **Nurse Module** provides a dedicated login and workflow for nursing staff. Each nurse is assigned to a specific surgeon/hospital (`doctor_id`) and can perform day-to-day clinical and administrative tasks while being restricted from surgical decision-making and surgeon-only functions.

**Current scope:** 3 nurse logins total.

---

## Nurse Role Summary

Nurses act as clinical support staff. They can:
- Add patients to the surgeon's pool
- Create OPD records (without surgical decision)
- Add clinical notes (SOAP format ward rounds)
- Manage appointments (book, reschedule, cancel on behalf of parents)
- View schedules (read-only)
- Upload documents

Nurses **cannot**:
- Make surgical decisions
- Create/edit surgical templates
- Create intra-op notes
- Create post-op notes
- Sign discharge summaries
- Mark patients "Ready for Discharge"
- Manage other nurses
- Access other surgeons' patient pools

---

## Database Schema

### `nurses`

| Column | Type | Description |
|---|---|---|
| id | UUID (PK) | Unique nurse identifier |
| doctor_id | UUID (FK → doctors) | Assigned surgeon — enforces pool isolation |
| name | String | Nurse's full name |
| phone | String | Registered phone number (used for OTP login) |
| hospital | String | Hospital affiliation |
| fcm_token | String? | Firebase Cloud Messaging device token |
| platform | String? | Device platform (android/ios/web) |
| is_active | Boolean | Whether the nurse login is active |
| created_at | DateTime | Account creation timestamp |

### `opd_records` Enhancement

Add optional columns to track who created the OPD record:

| Column | Type | Description |
|---|---|---|
| created_by | String | `surgeon` or `nurse` |
| nurse_id | UUID? (FK → nurses) | Nurse who created the record (if applicable) |
| review_status | String | `reviewed` or `pending_review` |

---

## Authentication Flow

Nurses use the same OTP authentication flow as surgeons and parents.

### Role Detection

```
IF phone EXISTS in doctors table → role = "surgeon"
ELSE IF phone EXISTS in nurses table (and is_active = true) → role = "nurse"
ELSE → role = "patient_parent"
```

### JWT Payload for Nurse

```json
{
  "phone": "+919876543210",
  "role": "nurse",
  "doctor_id": "uuid-of-assigned-surgeon",
  "nurse_id": "uuid-of-nurse",
  "exp": 1700000000
}
```

### Login Flow

1. Nurse enters phone number on P1 (Login screen)
2. OTP sent via 2Factor.in / MSG91
3. Nurse enters OTP on P2 (Verify screen)
4. Backend detects role = `nurse`
5. JWT issued with `nurse_id` and `doctor_id`
6. App registers FCM token
7. App navigates to N1 (Nurse Dashboard)

---

## Permission Middleware

All backend endpoints must enforce role-based access:

```python
from fastapi import Depends, HTTPException

async def require_nurse_or_surgeon(user=Depends(get_current_user)):
    if user.role not in ["surgeon", "nurse"]:
        raise HTTPException(403, "Access denied")
    return user

async def require_surgeon_only(user=Depends(get_current_user)):
    if user.role != "surgeon":
        raise HTTPException(403, "Surgeon access required")
    return user
```

For endpoints accessible to nurses:
- Extract `doctor_id` from JWT (for surgeons) or resolve via `nurses.doctor_id` (for nurses)
- Scope all queries to that `doctor_id`

---

## Nurse Screens (N1–N6)

### N1 — Nurse Dashboard

**Purpose:** Landing screen for nurses after login.

**UI Components:**
- 4 metric chips:
  1. Patients Added Today
  2. OPDs Recorded Today
  3. Upcoming Appointments (next 24h)
  4. Active IPD Admissions
- Quick action chips:
  - Add Patient
  - New OPD Record
  - Book Appointment
  - Ward Round Notes
- Upcoming appointments list (next 5)
- Active IPD cards (read-only)

**Accent Color:** Coral (#FF6F61)

**API:** `GET /nurse/dashboard`

---

### N2 — Nurse OPD Form

**Purpose:** Create OPD records without surgical decision authority.

**UI Components:**
- "For Surgeon Review" banner at top
- Patient Selector
- Visit Type chips (New / Follow-up)
- Chief Complaint
- Examination
- Diagnosis (text input, no AI suggestions)
- Advice
- Medications (repeatable rows)
- Investigations (checklist)
- Tag Chips
- Follow-up Date Picker with reminder toggle
- **NO Surgical Decision field**
- **NO AI Diagnosis Panel**

**Post-Save:**
- Record saved with `created_by = "nurse"`, `review_status = "pending_review"`
- Record appears in surgeon's patient profile with "Pending Review" badge
- Surgeon must later add surgical decision and confirm

**API:** `POST /opd-records` (accepts optional `nurse_id`)

---

### N3 — Nurse Patient List

**Purpose:** View and add patients in the assigned surgeon's pool.

**UI Components:**
- Search bar (debounced)
- Filter chips: All / OPD / Pre-op / Post-op / Discharged
- Patient list (LazyColumn)
- "+ Add Patient" FAB

**API:**
- `GET /patients` (scoped to nurse's doctor_id)
- `POST /patients` (auto-sets doctor_id from nurse's doctor_id)

**Notes:** Same screen as S2 but with nurse-appropriate actions. Nurse can add and view patients but has limited edit capabilities.

---

### N4 — Nurse Clinical Notes (SOAP)

**Purpose:** Record daily ward round notes in SOAP format.

**UI Components:**
- Quick vitals inputs (HR, BP, Temp, SpO2, RR)
- SOAP fields:
  - Subjective
  - Objective
  - Assessment
  - Plan
- "Ready for Discharge" button is **hidden** (surgeon-only)
- "Submitted by Nurse" watermark

**API:** `POST /admissions/:id/ward-round` with optional `nurse_id`

**Permission:** Nurse can create ward round notes but cannot mark ready for discharge.

---

### N5 — Nurse Appointment Manager

**Purpose:** View and manage appointments for the assigned surgeon.

**UI Components:**
- TopBar tabs: Upcoming / Today / Past
- Week strip for date filtering
- Appointment cards with:
  - Patient name and age
  - Time
  - Visit type
  - Status pill
  - Surgeon name
- Swipe actions:
  - Right swipe: Mark complete
  - Left swipe: Cancel (with confirmation)
- "+ Book Appointment" FAB opens bottom sheet:
  - Patient selector
  - Surgeon selector (if applicable)
  - Date picker
  - Time slot picker
  - Book button

**API:**
- `GET /nurse/appointments`
- `POST /nurse/appointments`
- `PATCH /nurse/appointments/:id`

**Notes:** Nurse books on behalf of parent; notifications sent to parent and nurse.

---

### N6 — Nurse Schedule Viewer

**Purpose:** Read-only view of OT and OPD schedules.

**UI Components:**
- TopBar tabs: OT Schedule / OPD Schedule
- Week strip
- Read-only slot cards
- No "Add" buttons
- Tapping a slot shows detail bottom sheet with patient name, procedure, time, and ward (if admitted)

**API:**
- `GET /schedule/ot`
- `GET /schedule/opd`

**Permission:** Read-only. Resolved to nurse's doctor_id.

---

## Nurse Permissions Matrix

| Action | Surgeon | Nurse | Parent |
|---|---|---|---|
| Add Patient | ✅ | ✅ | ❌ |
| Edit Patient Demographics | ✅ | ✅ (limited) | ❌ |
| Create OPD Record | ✅ | ✅ | ❌ |
| Edit OPD Record | ✅ | ✅ (own entries, no surgical decision) | ❌ |
| Surgical Decision | ✅ | ❌ | ❌ |
| AI Diagnosis Suggestion | ✅ | ❌ (view only) | ❌ |
| Create Admission | ✅ | ✅ (no surgical decision) | ❌ |
| Pre-op Notes | ✅ | ❌ | ❌ |
| Intra-op Notes | ✅ | ❌ | ❌ |
| Post-op Notes | ✅ | ❌ | ❌ |
| Ward Round Notes | ✅ | ✅ | ❌ |
| Mark Ready for Discharge | ✅ | ❌ | ❌ |
| Discharge Summary | ✅ | ❌ | ❌ |
| Manage Appointments | ✅ | ✅ | ❌ |
| View Schedule | ✅ | ✅ (read-only) | ❌ |
| Surgical Templates | ✅ (CRUD) | ❌ (read-only) | ❌ |
| Consent Forms | ✅ (generate/sign) | ✅ (generate, no sign) | ✅ (view/sign own) |
| Upload Documents | ✅ | ✅ | ❌ |
| View Patient Records | ✅ (own pool) | ✅ (own pool) | ✅ (own child only) |
| Book Appointment | ✅ | ✅ (on behalf of parent) | ✅ |
| Edit Own Profile | ✅ | ✅ | ✅ |
| Manage Nurses | ✅ | ❌ | ❌ |

---

## API Endpoints Specific to Nurses

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/nurse/dashboard` | nurse | Dashboard metrics scoped to nurse's doctor_id |
| GET | `/nurse/appointments` | nurse | Appointments scoped to nurse's doctor_id |
| POST | `/nurse/appointments` | nurse | Book appointment on behalf of parent |
| PATCH | `/nurse/appointments/:id` | nurse | Complete/cancel appointment |
| GET | `/nurse/patients` | nurse | Patient list scoped to nurse's doctor_id |
| POST | `/nurse/patients` | nurse | Add patient (auto-sets doctor_id) |
| POST | `/nurse/opd-records` | nurse | Create OPD record (pending review) |
| POST | `/nurse/ward-round` | nurse | Create ward round note |

**Note:** Many endpoints can reuse the surgeon endpoints but must resolve `doctor_id` from the nurse's profile when `role == "nurse"`.

---

## UI/UX Guidelines for Nurse Screens

### Accent Color
- Use **Coral (#FF6F61)** as the primary accent color for all nurse screens.
- Surgeon screens use **Teal (#00897B)**.
- Parent screens use **Amber (#FFB300)**.

### Differentiation Cues
- TopBar subtitle: "Nurse — [Hospital Name]"
- "Submitted by Nurse" watermark on nurse-created records
- "Pending Surgeon Review" badge on nurse-created OPD records
- Read-only indicators (lock icons, grayed-out buttons) where nurse has no write access

### Navigation
- BottomNavigation or NavigationRail for nurse screens:
  - Dashboard
  - Patients
  - Appointments
  - Schedule
  - Profile

---

## Nurse Management (Surgeon Side)

From **S13 — Surgeon Profile & Settings**, the surgeon can:

- View all nurses assigned to them
- Add a new nurse:
  - Name
  - Phone
  - Hospital
  - On save: creates `nurses` record with `doctor_id` auto-set
- Remove/deactivate a nurse:
  - Sets `is_active = false` (preferred over hard delete for audit)
  - Requires confirmation

**API:**
- `GET /surgeon/nurses`
- `POST /surgeon/nurses`
- `DELETE /surgeon/nurses/:id` (soft delete / deactivate)

---

## Backend Role Resolution Helper

```python
async def resolve_doctor_id(user) -> str:
    """Returns the doctor_id the user is scoped to."""
    if user.role == "surgeon":
        return user.doctor_id
    elif user.role == "nurse":
        nurse = await prisma.nurses.find_first(where={"id": user.nurse_id})
        if not nurse or not nurse.is_active:
            raise HTTPException(403, "Nurse account inactive")
        return nurse.doctor_id
    else:
        raise HTTPException(403, "Invalid role")
```

All patient-related queries should use:

```python
doctor_id = await resolve_doctor_id(user)
# Then: WHERE doctor_id = doctor_id
```

---

## Testing Checklist

- [ ] Nurse can log in with OTP and route to N1
- [ ] Nurse can add a patient
- [ ] Nurse can create an OPD record without surgical decision
- [ ] Nurse-created OPD record shows "Pending Review" to surgeon
- [ ] Nurse can create ward round notes
- [ ] Nurse cannot mark "Ready for Discharge"
- [ ] Nurse can book appointments on behalf of parent
- [ ] Nurse can view schedule (read-only)
- [ ] Nurse cannot access intra-op, post-op, or discharge screens
- [ ] Nurse cannot create/edit surgical templates
- [ ] Nurse cannot access other surgeon's patients
- [ ] Surgeon can add/remove nurses from S13
- [ ] Removed nurse cannot log in
- [ ] Works on Android, iOS, and Web
