# Phase 8 — Surgeon Profile & Settings

**Goal:** Surgeon profile, surgical stats, template management, nurse management.  
**Duration:** 0.5 days  
**Owner:** Dev 2  
**Screens:** S13 — Surgeon Profile & Settings  

---

## Task 8.1 — Surgeon Profile (S13)

**Description:** A shared Compose screen combining the surgeon's profile information, surgical statistics, application settings, and nurse management. Organized into distinct sections with clear visual separation.

**UI Sections:**

- **Profile Card (top):**
  - Surgeon name (large text)
  - Hospital name
  - Specialty
  - Phone number (with edit option that triggers OTP re-verification)
  - Profile photo placeholder (circular, tap to upload via Cloudinary)

- **Stats Row (horizontal):**
  - Total Patients: count from `patients` table scoped to `doctor_id`
  - Total Surgeries: count from `ipd_admissions` with `status = "discharged"`
  - OPD Consults: count from `opd_records`
  - Consent Forms Generated: count from `consent_forms` (NEW)
  - All stats calculated live from database

- **Settings Rows (list):**
  - **Surgical Templates:** Taps navigate to Templates Manager (Task 4.3)
  - **WhatsApp Settings:** Toggle WhatsApp notifications on/off, configure message preferences
  - **OPD Defaults:** Set default values for new OPD consults
  - **Reminder Timing:** Configure follow-up reminder time (default: 9:00 AM IST)
  - **Hospital Details:** Edit hospital name, address, contact information
  - **Change Phone Number:** Triggers OTP re-verification flow
  - **Logout (danger red):** Clears JWT, unregisters FCM token, navigates to P1

### NEW — Nurse Management Section

- **Nurse List Card:**
  - Shows all nurses assigned to this surgeon (from `nurses` table where `doctor_id` matches)
  - Each row: Nurse name, phone, hospital, status (Active / Inactive)
  - "Add Nurse" button: Opens a bottom sheet/dialog to add a new nurse
    - Fields: Name, Phone, Hospital
    - On save: Creates `nurses` record with `doctor_id` auto-set
    - Nurse receives no automatic notification; they can log in with OTP using their phone
  - "Remove Nurse" button (swipe or overflow menu): Sets nurse to inactive or deletes record with confirmation

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/surgeon/profile` | Returns surgeon's profile data and live statistics. |
| PATCH | `/surgeon/profile` | Updates surgeon's profile fields. |
| GET | `/surgeon/nurses` | **NEW** — Returns all nurses assigned to this surgeon. |
| POST | `/surgeon/nurses` | **NEW** — Adds a new nurse. Auto-sets `doctor_id`. |
| DELETE | `/surgeon/nurses/:id` | **NEW** — Removes a nurse. Validates ownership. |

**Acceptance Criteria:**

- Stats calculated live from database on each screen load
- Template CRUD operations work from this screen
- Phone change triggers full OTP re-verification flow
- Logout clears all local data and navigates to login screen
- Settings changes persisted to backend
- **Nurse list shows all assigned nurses**
- **Adding a nurse creates record and nurse can immediately log in with OTP**
- **Removing a nurse requires confirmation and validates ownership**
