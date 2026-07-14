# Noni Tura — Alpha End-to-End Test Flow

This document covers the full Alpha test matrix for the KMM app (Android / iOS / Web) and the FastAPI backend. It is intended for internal testers (surgeons, nurses, parents, admins).

---

## 1. Prerequisites

1. **Backend running**
   ```bash
   cd backend
   docker compose up -d
   source .venv/bin/activate
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```
2. **Prisma schema in sync**
   ```bash
   cd backend
   source .venv/bin/activate
   prisma generate
   prisma db push
   ```
3. **Mobile/Web app** pointed at the backend:
   - Android emulator: `http://10.0.2.2:8000`
   - iOS simulator / Web: `http://localhost:8000`
4. A way to read the **dev OTP** returned by `/auth/send-otp` (printed in the API response and backend console).
5. Optional services:
   - **Cloudinary** — for PDF uploads (falls back to data-URI / `null` if not configured).
   - **Meta WhatsApp** — `META_WA_TOKEN` + `META_WA_PHONE_ID`.
   - **Firebase Admin** — `FIREBASE_CREDENTIALS_JSON` for push.
   - **2Factor.in** — `TWO_FACTOR_API_KEY` for SMS fallback.

---

## 2. Test Data Setup

Run the provided seed scripts from the `backend` directory (with the virtual environment activated):

```bash
cd backend
source .venv/bin/activate

# Schema + Prisma client
prisma generate
prisma db push

# Seed records
python scripts/seed_superadmin.py
python scripts/backfill_hospitals.py
python scripts/seed_test_data.py
```

This creates the following test environment:

| Role | Phone | Name / Notes |
|------|-------|--------------|
| Superadmin | `+919999999999` | Super Admin |
| Surgeon (Test Hospital) | `+919876543210` | Dr. Arjun Mehta — Pediatric Surgery |
| Surgeon (CMC) | `+919304155460` | Dr. Priya Sharma — Pediatric Urology |
| Nurse | `+919876543211` | Nurse Renu (under Dr. Arjun Mehta) |
| Nurse | `+919876543212` | Nurse Karthik (under Dr. Arjun Mehta) |
| Nurse | `+919876543213` | Nurse Anjali (under Dr. Priya Sharma) |
| Nurse | `+919876543214` | Nurse David (under Dr. Priya Sharma) |
| Parent A / Patient | `+918000000001` | Parent: Raj Patel — Patient: Aarav Patel |
| Parent B | `+918000000002` | Parent: Sunita Singh — Patient: Diya Singh |
| Parent C | `+918000000003` | Parent: Pooja Gupta — Patient: Vihaan Gupta |
| Parent D (isolation test) | `+918000000004` | Parent: Mohan Reddy — Patient: Isha Reddy |
| Parent E | `+918000000005` | Parent: Fatima Khan — Patient: Kabir Khan |
| Parent F | `+918000000006` | Parent: Suresh Iyer — Patient: Ananya Iyer |

> **Important:** Each phone number must map to only one role. Auth resolves `admin → surgeon → nurse → parent` in that order. If a parent phone is already registered as staff, login will route to the staff role instead of the parent flow.

Seeded data also includes:
- **6 OPD records** with medications and investigations
- **2 IPD admissions** (Aarav Patel in PICU, Vihaan Gupta in Pediatric Ward)
- **3 appointments**
- **2 hospitals**: `Test Hospital` and `CMC`

If you need a fresh database, run:

```bash
cd backend
source .venv/bin/activate
prisma db push --force-reset
python scripts/seed_superadmin.py
python scripts/backfill_hospitals.py
python scripts/seed_test_data.py
```

---

## 3. Authentication, Consent & Role Routing

### Flow 3.0 — Consent Acknowledgment Gate (NEW)

On **every cold launch** the consent dialog is displayed before the OTP form.

1. Open the app fresh (or clear app storage).
2. Verify a **full-screen dialog** appears with the heading *"Terms & Consent"* and scrollable text.
3. Try tapping **Continue** without checking the checkbox.
   **Expected:** Button remains disabled.
4. Scroll to the bottom, check **"I have read and understand the terms & consent"**.
5. Tap **Continue**.
   **Expected:** Dialog dismisses and the phone number + OTP form is now visible.
6. Tap **Exit** on the dialog.
   **Expected:** App exits / navigates back (dialog is non-dismissible on back press).

> The dialog uses `rememberSaveable` so it resets on cold start. This is intentional for the current MVP — users see it every session until persistent storage is added.

### Flow 3.1 — Surgeon login

1. Open app → pass consent gate → enter `+919876543210` → tap **Send OTP**.
2. Enter the dev OTP.

**Expected:** Routed to **Surgeon Home** with Dashboard tab.

### Flow 3.2 — Nurse login

1. Login with `+919876543211` (or any seeded nurse number: `+919876543212`, `+919876543213`, `+919876543214`).

**Expected:** Routed to **Nurse Home**.

### Flow 3.3 — Parent login

1. Login with `+918000000001`.

**Expected:** Routed to **Parent Home** for Aarav Patel.

### Flow 3.4 — Invalid role isolation

1. Try to access surgeon-only endpoints with a nurse/parent token via `curl`.

**Expected:** `403 Forbidden`.

---

## 4. Surgeon Core Flow

### Flow 4.1 — Dashboard

1. Login as surgeon.
2. Verify KPIs: Today Appointments, Scheduled Surgeries, Pre-op, In OT, Recovery.
3. Tap an appointment in **Today's Appointments**.

**Expected:** Navigates to patient profile.

### Flow 4.2 — Patients List & Search

1. Tap **Patients** tab.
2. Verify the Add (+) button is visible above the bottom nav.
3. Search by patient name and parent phone.
4. Tap a patient.

**Expected:** Patient profile opens with demographics, latest OPD, active admission, documents/consents, OPD history.

### Flow 4.3 — Add Patient

1. Tap **+** on Patients tab.
2. Fill form and save.

**Expected:** New patient appears in list and profile opens.

### Flow 4.4 — OPD Consult

1. On patient profile, tap **Add OPD Record**.
2. Fill complaint, examination, diagnosis, surgical decision, advice, medications, investigations, and a **follow-up date**.
3. Save.

**Expected:** Record appears in OPD history; follow-up appears in **Follow-ups** tab on the selected date.

### Flow 4.5 — Schedule (OT / OPD)

1. Tap **Schedule** from dashboard quick action or main nav.
2. Switch between **OT Schedule** and **OPD Schedule** tabs.
3. Use week strip to change date.
4. Tap an empty slot → create booking (select patient, time, procedure/visit type).
5. Verify booked slot appears as a card.

**Expected:** Booking persists after reload; backend `appointments` row created with correct `visit_type`, `slot_datetime`, `procedure`, `urgency`.

### Flow 4.6 — Admissions & Clinical Notes

1. Tap **Admissions** from dashboard.
2. Admit a patient (or tap existing admission).
3. On admission detail:
   - Add **Pre-op Notes**.
   - Apply a surgical template.
   - Add **Intra-op Notes**.
   - Add **Post-op Notes**.
   - Add **Daily Ward Round** entries.
   - Complete **Discharge** with follow-up date.

**Expected:** Admission status progresses (`admitted` → `pre-op` → `in-surgery` → `recovery` → `discharged`).

### Flow 4.7 — Follow-ups & WhatsApp Preview

1. Tap **Follow-ups** tab.
2. Verify tomorrow's follow-ups appear.
3. Tap **Preview & Send** on a follow-up.
4. Edit the SMS message text.
5. Tap **Send via WhatsApp** (requires Meta config) or **Send via SMS**.

**Expected:** Success message shown; `whatsapp_logs` row created; `opd_records.reminder_sent` set to `true`.

### Flow 4.8 — Surgical Templates Manager

1. Tap **Surgical Templates** from dashboard.
2. Create, edit, and delete a template.
3. Go to an admission → pre-op form → **Apply from Template**.

**Expected:** Form pre-fills from the selected template.

---

## 5. Medical Records (NEW)

### Flow 5.0 — Upload Medical Images (Surgeon / Nurse)

1. Login as surgeon.
2. Navigate to any patient profile.
3. Tap **Medical Records** (or the upload button — UI integration pending; use backend directly if UI not wired yet).
4. **Create a record container** via backend:
   ```bash
   curl -X POST http://localhost:8000/medical-records \
     -H "Authorization: Bearer <SURGEON_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"patient_id":"<PATIENT_ID>","title":"Pre-op Imaging","description":"X-ray and MRI before surgery"}'
   ```
   Save returned `id` as `RECORD_ID`.

5. **Add an image** (upload to Cloudinary first to get a URL, or use any HTTPS image URL for testing):
   ```bash
   curl -X POST http://localhost:8000/medical-records/<RECORD_ID>/images \
     -H "Authorization: Bearer <SURGEON_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"image_url":"https://example.com/test.jpg","category":"X-ray","label":"AP chest view","description":"Pre-op chest X-ray","uploaded_by_role":"surgeon"}'
   ```
   **Expected:** `201` with image metadata.

6. Test an **invalid category**:
   ```bash
   -d '{"image_url":"https://example.com/test.jpg","category":"Selfie",...}'
   ```
   **Expected:** `400 Bad Request` — "Category must be one of: X-ray, MRI, CT Scan, ...".

Valid categories: `X-ray`, `MRI`, `CT Scan`, `Ultrasound`, `Prescription`, `Lab Report`, `ECG`, `Photo`, `Other`.

### Flow 5.1 — List & View Records (all roles)

**Surgeon / Nurse:**
```bash
curl http://localhost:8000/medical-records/<PATIENT_ID> \
  -H "Authorization: Bearer <SURGEON_TOKEN>"
```
**Expected:** Array of records with `image_count`.

**Parent (own child only):**
```bash
curl http://localhost:8000/medical-records/<PATIENT_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```
**Expected:** `200` with records list. Parent can read but not create/upload.

**Parent accessing another parent's child:**
```bash
curl http://localhost:8000/medical-records/<OTHER_PATIENT_ID> \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```
**Expected:** `404 Not Found`.

### Flow 5.2 — Record Detail and Images

```bash
curl http://localhost:8000/medical-records/<RECORD_ID>/detail \
  -H "Authorization: Bearer <SURGEON_TOKEN>"

curl http://localhost:8000/medical-records/<RECORD_ID>/images \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```
**Expected:** Full record with `images` array including `category`, `label`, `description`, `uploaded_at`.

### Flow 5.3 — OPD-linked Records

When creating a record linked to an OPD record, pass `opd_record_id`:
```bash
-d '{"patient_id":"<ID>","title":"OPD Visit Images","opd_record_id":"<OPD_ID>"}'
```
After uploading the first image, verify `opd_records.has_medical_records = true` in the database.

---

## 5b. Consent Form Flow

See `CONSENT_TEST_FLOW.md` for detailed consent steps. High-level:

1. Surgeon generates consent from admission or patient profile.
2. Parent sees pending consent on Parent Home.
3. Parent reviews, signs, and views signed PDF.
4. Surgeon cannot sign; parent cannot generate.

---

## 6. Nurse Flow

### Flow 6.1 — Nurse Dashboard

1. Login as nurse.

**Expected:** Nurse Home shows Upcoming Appointments KPI, patient count, and quick actions.

### Flow 6.2 — Nurse Patient List & OPD Form

1. Tap **Patients** / **OPD Notes** quick action.
2. Select a patient.
3. Create an OPD record.

**Expected:** Nurse cannot set surgical decision (backend rejects `403`); review status is `pending_review`.

### Flow 6.3 — Nurse Schedule

1. Tap **Schedule** quick action.
2. View OT/OPD slots for the surgeon's pool.
3. Book an empty slot.

**Expected:** Booking created under the surgeon's `doctor_id` with `booked_by = nurse`.

---

## 7. Parent Flow

### Flow 7.1 — Parent Home

1. Login as parent `+918000000001`.

**Expected:**
- Live surgery status card (if child is admitted).
- Next Appointment card.
- Pending consent card (if any).
- Quick actions: OPD Records, Appointments, Profile.

### Flow 7.2 — Surgery Status Detail

1. Tap the surgery status card.

**Expected:** 5-stage timeline (Admitted → Pre-op → In Surgery → Recovery → Discharged) with current stage highlighted.

### Flow 7.3 — OPD Records & Consult Detail

1. Tap **OPD Records** quick action.
2. Tap a record.

**Expected:** Read-only consult detail with diagnosis, medications, advice, follow-up.

### Flow 7.4 — Appointment Request Flow (Backend complete; UI screens pending)

Test via backend `curl` until the 3 appointment screens are built in the app.

**Step 1 — Parent requests an appointment:**
```bash
# Login as parent first to get token
curl -X POST http://localhost:8000/appointments/request \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "doctor_id":"<DOCTOR_ID>",
    "reason":"Post-op follow-up needed",
    "urgency":"routine"
  }'
```
**Expected:** `201` with appointment `id` and `available_slots` array (30 slots, 5 per day for next 6 days). Save `id` as `APPT_ID`.

> Surgeons and nurses calling this endpoint → `403 Forbidden`.

**Step 2 — Get available slots:**
```bash
curl "http://localhost:8000/appointments/available-slots?doctor_id=<DOCTOR_ID>&days=14" \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```
**Expected:** `{"slots": [...]}` with `slot_datetime`, `slot_id`, `surgeon_name`, `is_available`.

**Step 3 — Confirm appointment + auto-create patient:**
```bash
curl -X POST http://localhost:8000/appointments/<APPT_ID>/confirm \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "slot_datetime":"2026-07-20T09:00:00Z",
    "auto_create_patient":true,
    "patient_data":{"name":"New Baby","age":2,"gender":"Female","parent_name":"Test Parent"}
  }'
```
**Expected:**
- `200` with `is_confirmed: true`, `patient_id` now set (newly created patient).
- Verify in DB: `appointments.patient_id` updated, `appointments.requesting_parent_phone` = parent's phone.

**Step 4 — Confirm without auto-creating (existing patient):**
```bash
-d '{"slot_datetime":"2026-07-21T11:00:00Z","auto_create_patient":false}'
```
**Expected:** Appointment confirmed, `patient_id` remains null until a patient is explicitly linked.

**Step 5 — Surgeon cannot confirm:**
```bash
curl -X POST http://localhost:8000/appointments/<APPT_ID>/confirm \
  -H "Authorization: Bearer <SURGEON_TOKEN>" ...
```
**Expected:** `403 Forbidden`.

### Flow 7.5 — Parent Profile

1. Tap **Profile**.
2. View consent history, contact info, logout.

---

## 8. Notifications & Reminders

### Flow 8.1 — Follow-up Reminder Cron

1. Create an OPD record with follow-up date = tomorrow for a patient with parent phone.
2. Wait for 09:00 IST cron, or trigger manually:
   ```bash
   curl -X POST http://localhost:8000/admin/trigger-follow-up-reminders \
     -H "Authorization: Bearer <SUPERADMIN_TOKEN>"
   ```

**Expected:**
- WhatsApp template sent (if configured).
- FCM push sent (if Android FCM token registered).
- `reminder_sent = true` after at least one channel succeeds.

### Flow 8.2 — Push Token Registration

1. Login on Android.

**Expected:** `POST /auth/register-fcm` called with FCM token and platform.

---

## 9. Security & Isolation

### Flow 9.1 — Doctor Pool Isolation

1. Login as Surgeon A.
2. Try to fetch Surgeon B's patient or appointment via `curl`:
   ```bash
   curl http://localhost:8000/patients/<PATIENT_B_ID> \
     -H "Authorization: Bearer <SURGEON_A_TOKEN>"
   ```

**Expected:** `403 Forbidden`.

### Flow 9.2 — Parent Isolation

1. Login as Parent A (`+918000000001`).
2. Try to access Parent B's (`+918000000002`) child data.

**Expected:** `403 Forbidden`.

### Flow 9.3 — Role-Based Endpoint Access

- Parent calling `POST /opd/patients/{id}/records` → `403`.
- Nurse calling `POST /surgical-templates` → `403`.
- Surgeon calling `POST /consent/forms/{id}/sign` → `403`.

---

## 10. Known Alpha Limitations

- **iOS APNs push** is stubbed; real delivery needs Apple Developer account + APNs cert.
- **WhatsApp template approval** is external; fallback SMS uses 2Factor.in (logs stub if not configured).
- **Cloudinary** is optional; PDFs may be data-URI or `null`.
- **Real OTP/SMS** not sent in dev; OTP is returned in API response.

---

## 11. Smoke Test Checklist

Use before each Alpha release:

**Auth & Consent**
- [ ] Consent dialog appears on cold launch before OTP form.
- [ ] Continue button disabled until checkbox ticked.
- [ ] After consent, OTP login works for all 3 roles (surgeon, nurse, parent).

**Surgeon Core**
- [ ] Surgeon can log in and view dashboard.
- [ ] Surgeon can add a patient and create an OPD record.
- [ ] Follow-up appears in Follow-ups tab and can be previewed/sent.
- [ ] Schedule screen shows OT/OPD bookings and allows creation.
- [ ] Consent can be generated by surgeon and signed by parent.

**Medical Records**
- [ ] Surgeon can create a medical record container.
- [ ] Surgeon can upload an image with valid category, label, description.
- [ ] Invalid category returns `400`.
- [ ] Parent can read their child's medical records (`200`).
- [ ] Parent cannot read another child's records (`404`).
- [ ] OPD-linked record upload sets `opd_records.has_medical_records = true`.

**Appointment Request**
- [ ] Parent can `POST /appointments/request` → gets back `available_slots`.
- [ ] `GET /appointments/available-slots` returns slots for any authenticated user.
- [ ] Parent can `POST /appointments/{id}/confirm` with `auto_create_patient=true` → new patient created.
- [ ] Surgeon calling `/request` or `/confirm` returns `403`.
- [ ] Random doctor_id on request returns `404`.

**Nurse & Parent**
- [ ] Nurse can log in and create OPD notes.
- [ ] Parent can log in and view surgery status / OPD records.
- [ ] Admin can log in and approve pending doctors.

**Security**
- [ ] No cross-doctor / cross-parent data leaks (Flows 9.1–9.3).
- [ ] All platform builds pass (Android, iOS simulator, JS, Wasm).

---

## 12. Known Alpha Limitations (Updated)

- **Appointment UI screens** (AppointmentRequestScreen, AppointmentSlotsScreen, AppointmentConfirmScreen) are not built yet — test the booking flow via `curl` only.
- **Medical Records view screen** is not built yet — test via `curl`; the upload picker component (`MedicalImagePicker`) is available but not wired into any screen.
- **Appointment slots** are mock-generated (5 per day, fixed hours 9/11/14/15/16 IST) — no conflict detection or real surgeon calendar.
- **iOS APNs push** is stubbed; real delivery needs Apple Developer account + APNs cert.
- **WhatsApp template approval** is external; fallback SMS uses 2Factor.in (logs stub if not configured).
- **Cloudinary** is optional; PDFs/images may fall back to stub URLs in dev.
- **Real OTP/SMS** not sent in dev; OTP is returned in API response.
