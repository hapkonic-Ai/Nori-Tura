# Noni Tura API Endpoint Reference

**Base URL:** `https://nori-tura.primeworld.tech`

**Health Check:** `GET /health`

---

## Authentication

All authenticated endpoints require:

```http
Authorization: Bearer <access_token>
```

Tokens are returned by `POST /auth/verify-otp` and are valid for 30 days.

### Roles

- `public` — no token required
- `any` — any valid bearer token
- `parent` — role `patient_parent`
- `nurse/surgeon` — role `nurse` or `surgeon`
- `surgeon` — role `surgeon` only
- `staff` — role `admin` or `superadmin`
- `superadmin` — role `superadmin` only

---

## Health

### `GET /health`
**Auth:** public  
**Response:**
```json
{ "status": "ok", "service": "noni-tura-api" }
```

---

## Authentication (`/auth`)

### `POST /auth/send-otp`
Sends an OTP to the given Indian phone number. Returns the OTP in development mode.

**Auth:** public

**Request:**
```json
{ "phone": "+919876543210" }
```

**Response:**
```json
{
  "message": "OTP sent successfully",
  "expires_in_minutes": 5,
  "dev_otp": "123456"
}
```

---

### `POST /auth/register-doctor`
Registers a new doctor as pending approval. Auto-creates the hospital if it does not exist.

**Auth:** public

**Request:**
```json
{
  "name": "Dr. A. Kumar",
  "phone": "+919876543210",
  "hospital": "Sunrise Children Hospital",
  "specialty": "Pediatric Surgery"
}
```

**Response:**
```json
{
  "message": "Registration submitted. Wait for admin approval.",
  "doctor_id": "doc_123",
  "status": "pending_approval"
}
```

---

### `POST /auth/verify-otp`
Verifies the OTP and returns a JWT access token.

**Auth:** public

**Request:**
```json
{
  "phone": "+919876543210",
  "otp": "123456"
}
```

**Response:**
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "role": "surgeon"
}
```

---

### `GET /auth/me`
Returns the current user's role and profile.

**Auth:** any

**Response:**
```json
{
  "role": "surgeon",
  "profile": { "id": "doc_123", "name": "Dr. A. Kumar" }
}
```

---

### `POST /auth/register-fcm`
Stores the user's FCM push token.

**Auth:** any

**Request:**
```json
{
  "fcm_token": "abc...",
  "platform": "android"
}
```

**Response:**
```json
{ "message": "FCM token registered" }
```

---

## Patients (`/patients`)

### `GET /patients`
Lists patients visible to the user.

**Auth:** any

**Query params:** `search`, `diagnosis`, `status`

**Response:**
```json
[
  {
    "id": "pat_1",
    "name": "Riya Sharma",
    "age": 4,
    "gender": "Female",
    "parent_phone": "+919876543211"
  }
]
```

---

### `GET /patients/{patient_id}`
Gets a single patient with OPD/IPD history.

**Auth:** any (scoped)

**Response:** Patient object with nested records.

---

### `POST /patients`
Creates a new patient under the surgeon/nurse's doctor.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "name": "Riya Sharma",
  "age": 4,
  "gender": "Female",
  "blood_group": "O+",
  "allergies": "None",
  "parent_name": "Mrs. Sharma",
  "parent_phone": "+919876543211"
}
```

**Response:** Created patient object (HTTP 201).

---

### `PUT /patients/{patient_id}`
Updates patient demographics.

**Auth:** nurse/surgeon

**Request:**
```json
{ "age": 5, "allergies": "Penicillin" }
```

**Response:** Updated patient object.

---

## OPD (`/opd`)

### `GET /opd/patients/{patient_id}/records`
Lists all OPD records for a patient.

**Auth:** any (scoped)

**Response:** Array of OPD records.

---

### `POST /opd/patients/{patient_id}/records`
Creates an OPD record.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "visit_type": "follow-up",
  "complaint": "Fever and cough",
  "examination": "Chest clear",
  "diagnosis": "URI",
  "surgical_decision": "no_surgery",
  "planned_procedure": null,
  "advice": "Continue meds",
  "follow_up_date": "2026-08-10T09:00:00Z",
  "medications": [
    { "name": "Paracetamol", "dose": "120mg", "frequency": "TDS", "duration": "3 days" }
  ],
  "investigations": [
    { "type": "CBC", "status": "pending" }
  ],
  "prescription_image_urls": []
}
```

**Response:** Created OPD record (HTTP 201).

---

### `GET /opd/records/{record_id}`
Fetches one OPD record.

**Auth:** any (scoped)

**Response:** OPD record object.

---

### `GET /opd/follow-ups`
Lists OPD records with a follow-up on the given date.

**Auth:** nurse/surgeon

**Query params:** `follow_up_date`

**Response:** Array of OPD records with patients.

---

### `GET /opd/follow-ups/{record_id}/preview`
Previews the follow-up reminder message.

**Auth:** nurse/surgeon

**Response:**
```json
{
  "phone": "+919876543211",
  "body": "🏥 Noni Tura - Follow-up Reminder\n\nPatient: Riya Sharma...",
  "can_send_whatsapp": true,
  "can_send_sms": false
}
```

---

### `POST /opd/follow-ups/{record_id}/send`
Sends the follow-up reminder.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "channel": "whatsapp",
  "message": "Optional custom message"
}
```

**Response:**
```json
{
  "status": "sent",
  "channel": "whatsapp",
  "message_body": "..."
}
```

---

## Appointments (`/appointments`)

### `POST /appointments/request`
A parent requests a new appointment.

**Auth:** parent

**Request:**
```json
{
  "doctor_id": "doc_123",
  "reason": "Fever review",
  "urgency": "urgent",
  "preferred_date": "2026-08-05"
}
```

**Response:**
```json
{
  "id": "appt_1",
  "doctor_id": "doc_123",
  "status": "requested",
  "urgency": "urgent",
  "notes": "Fever review"
}
```

---

### `GET /appointments/available-slots`
Returns available slots for a doctor.

**Auth:** any

**Query params:** `doctor_id`, `days`

**Response:**
```json
{
  "slots": [
    { "slot_datetime": "2026-08-02T09:00:00Z", "is_available": true }
  ]
}
```

---

### `POST /appointments/{appointment_id}/confirm`
Parent confirms a requested appointment.

**Auth:** parent

**Request:**
```json
{
  "slot_datetime": "2026-08-05T10:00:00Z",
  "auto_create_patient": false
}
```

**Response:**
```json
{
  "id": "appt_1",
  "patient_id": "pat_1",
  "slot_datetime": "2026-08-05T10:00:00Z",
  "is_confirmed": true,
  "status": "confirmed"
}
```

---

### `GET /appointments`
Lists appointments for the current user.

**Auth:** any

**Response:** Array of appointment objects.

---

### `POST /appointments`
Directly books an appointment for a patient.

**Auth:** parent / nurse / surgeon

**Request:**
```json
{
  "patient_id": "pat_1",
  "slot_datetime": "2026-08-05T10:00:00Z",
  "visit_type": "follow-up"
}
```

**Response:** Created appointment object.

---

### `PATCH /appointments/{appointment_id}/status`
Updates appointment status.

**Auth:** nurse/surgeon

**Request:**
```json
{ "status": "completed" }
```

**Response:** Updated appointment object.

---

## IPD Admissions (`/ipd`)

### `GET /ipd/admissions`
Lists IPD admissions visible to the user.

**Auth:** any

**Response:** Array of admission objects.

---

### `GET /ipd/admissions/current`
Returns the parent's currently active admission.

**Auth:** parent

**Response:** Active admission object or `null`.

---

### `POST /ipd/admissions`
Creates an admission.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "patient_id": "pat_1",
  "urgency": "urgent",
  "bed_no": "B-12",
  "ward": "Pediatric Ward"
}
```

**Response:** Created admission object (HTTP 201).

---

### `GET /ipd/admissions/{admission_id}`
Gets a single admission with all notes.

**Auth:** any (scoped)

**Response:** Admission object with nested notes.

---

### `POST /ipd/admissions/{admission_id}/pre-op`
Adds a pre-operative note.

**Auth:** surgeon

**Request:**
```json
{
  "procedure": "Herniotomy",
  "approach": "Open",
  "anaesthesia": "General",
  "investigations": ["CBC", "X-ray chest"],
  "risk_level": "low",
  "special_instructions": "Nil per oral after midnight",
  "image_urls": []
}
```

**Response:** Created pre-op note.

---

### `POST /ipd/admissions/{admission_id}/intra-op`
Adds an intra-operative note.

**Auth:** surgeon

**Request:**
```json
{
  "procedure_done": "Herniotomy",
  "findings": "Indirect inguinal hernia",
  "technique": "Standard approach",
  "complications": "None",
  "blood_loss": "Minimal",
  "ot_start": "2026-08-02T08:00:00Z",
  "ot_end": "2026-08-02T09:00:00Z",
  "image_urls": [],
  "video_urls": []
}
```

**Response:** Created intra-op note.

---

### `POST /ipd/admissions/{admission_id}/post-op`
Adds a post-operative daily note.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "day_number": 1,
  "condition": "Stable",
  "vitals_json": { "temp": "37.2", "pulse": 100, "bp": "100/70" },
  "wound_status": "Clean & dry",
  "pain_score": 2,
  "diet": "Soft diet",
  "medications_json": { "analgesic": "Paracetamol" },
  "image_urls": []
}
```

**Response:** Created post-op note.

---

### `POST /ipd/admissions/{admission_id}/ward-round`
Adds a ward-round SOAP note.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "subjective": "Child comfortable",
  "objective": "Vitals stable",
  "assessment": "Recovering well",
  "plan": "Continue monitoring",
  "ready_for_discharge": false,
  "image_urls": []
}
```

**Response:** Created ward-round note.

---

### `POST /ipd/admissions/{admission_id}/discharge`
Creates a discharge summary.

**Auth:** surgeon

**Request:**
```json
{
  "condition_at_discharge": "Stable",
  "procedure_summary": "Herniotomy performed uneventfully",
  "discharge_medications_json": { "meds": [{ "name": "Paracetamol", "dose": "120mg" }] },
  "wound_care": "Keep dry for 48h",
  "activity_restrictions": "Avoid strenuous play for 2 weeks",
  "diet_instructions": "Normal diet",
  "follow_up_date": "2026-08-10T09:00:00Z",
  "red_flags": "Fever, wound redness",
  "image_urls": []
}
```

**Response:** Created discharge summary.

---

### `POST /ipd/admissions/{admission_id}/intra-op-media`
Appends a media item to the latest intra-op note.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "url": "https://cloudinary.com/video.mp4",
  "media_type": "video",
  "label": "OT footage",
  "description": "During procedure"
}
```

**Response:** Updated intra-op note.

---

### `POST /ipd/admissions/{admission_id}/videos`
Adds a video to the admission-level video list.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "url": "https://cloudinary.com/video.mp4",
  "label": "Counselling video",
  "description": "Shared with parents"
}
```

**Response:** Updated admission object.

---

### `POST /ipd/admissions/{admission_id}/discharge-major-events`
Adds a major event to the discharge summary.

**Auth:** surgeon

**Request:**
```json
{
  "title": "Post-op fever",
  "description": "Settled with antibiotics",
  "event_date": "2026-08-03",
  "severity": "moderate"
}
```

**Response:** Updated discharge summary.

---

## AI (`/ai`)

### `POST /ai/suggest-diagnosis`
Requests AI diagnosis suggestions.

**Auth:** any (scoped)

**Request:**
```json
{
  "patient_id": "pat_1",
  "complaint": "Abdominal pain",
  "examination": "Right iliac fossa tenderness",
  "age": 6,
  "gender": "Male"
}
```

**Response:**
```json
{
  "suggestions": {
    "confidence": 0.72,
    "disclaimer": "This is a clinical decision support suggestion...",
    "model_used": "openai/gpt-4o"
  },
  "disclaimer": "This is a clinical decision support suggestion...",
  "opd_record_id": "opd_1"
}
```

---

## Consent (`/consent`)

### `GET /consent/latest`
Returns the current platform consent text.

**Auth:** public

**Response:**
```json
{
  "version": "v1.0",
  "title": "Terms & Consent",
  "content": "...",
  "requires_acknowledgment": true
}
```

---

### `POST /consent/acknowledge`
Records a user's consent acknowledgment.

**Auth:** public

**Request:**
```json
{
  "phone": "+919876543210",
  "device_info": "Android 14",
  "client_ip": "192.168.1.10"
}
```

**Response:**
```json
{
  "acknowledged": true,
  "acknowledged_at": "2026-08-01T10:00:00Z"
}
```

---

### `POST /consent/suggest-content`
Returns RAG-suggested consent content.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "procedure": "Herniotomy",
  "diagnosis": "Inguinal hernia",
  "patient_age": 4,
  "patient_gender": "Female"
}
```

**Response:** JSON object with suggested risks, benefits, alternatives.

---

### `POST /consent/forms`
Generates a consent form PDF for an admission.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "admission_id": "adm_1",
  "form_type": "surgical",
  "diagnosis": "Inguinal hernia",
  "procedure": "Herniotomy",
  "anesthesia": "General anesthesia",
  "risks": "Bleeding, infection",
  "benefits": "Definitive repair",
  "alternatives": "Observation",
  "post_op_care": "Wound care",
  "consent_for_anesthesia": true,
  "consent_for_blood_products": false,
  "consent_for_photography": false
}
```

**Response:**
```json
{
  "consent_form": { "id": "cf_1" },
  "pdf_url": "https://cloudinary.com/consent_..."
}
```

---

### `GET /consent/forms/{consent_id}`
Retrieves a consent form.

**Auth:** any (scoped)

**Response:** Consent form object.

---

### `POST /consent/forms/{consent_id}/sign`
Signs a consent form.

**Auth:** any (scoped)

**Request:**
```json
{
  "parent_signature_url": "https://cloudinary.com/sig.png",
  "witness_name": "Rahul Verma",
  "witness_relationship": "Uncle",
  "witness_mobile": "+919876543212",
  "witness_signature_url": "https://cloudinary.com/wit_sig.png"
}
```

**Response:** Updated consent form object.

---

## Nurses (`/nurses`)

### `GET /nurses`
Lists nurses under the current surgeon.

**Auth:** surgeon

**Response:** Array of nurse objects.

---

### `POST /nurses`
Creates a nurse linked to the surgeon.

**Auth:** surgeon

**Request:**
```json
{
  "name": "Nurse Priya",
  "phone": "+919876543213",
  "hospital": "Sunrise Children Hospital"
}
```

**Response:** Created nurse object (HTTP 201).

---

### `PATCH /nurses/{nurse_id}/deactivate`
Deactivates a nurse.

**Auth:** surgeon

**Response:** Updated nurse object.

---

## Documents (`/documents`)

### `POST /documents`
Stores a document reference for a patient.

**Auth:** any (scoped)

**Request:**
```json
{
  "patient_id": "pat_1",
  "name": "CBC Report",
  "url": "https://cloudinary.com/cbc.pdf",
  "type": "pdf",
  "category": "Lab Report",
  "uploaded_by_role": "surgeon",
  "recorded_at": "2026-08-01T10:00:00Z"
}
```

**Response:** Created document object.

---

### `GET /documents/patients/{patient_id}`
Lists documents for a patient.

**Auth:** any (scoped)

**Response:** Array of document objects.

---

## Surgical Templates (`/surgical-templates`)

### `GET /surgical-templates`
Lists the surgeon's reusable surgical templates.

**Auth:** surgeon

**Response:** Array of template objects.

---

### `POST /surgical-templates`
Creates a surgical template.

**Auth:** surgeon

**Request:**
```json
{
  "name": "Herniotomy Template",
  "procedure": "Herniotomy",
  "approach": "Open",
  "anaesthesia": ["General"],
  "investigations": ["CBC", "X-ray chest"],
  "risk_level": "low",
  "technique": "Standard inguinal approach",
  "special_instructions": "Nil per oral after midnight"
}
```

**Response:** Created template object (HTTP 201).

---

### `GET /surgical-templates/{template_id}`
Gets a single template.

**Auth:** surgeon

**Response:** Template object.

---

### `PATCH /surgical-templates/{template_id}`
Updates a template.

**Auth:** surgeon

**Request:** Any subset of creation fields.

**Response:** Updated template object.

---

### `DELETE /surgical-templates/{template_id}`
Deletes a template.

**Auth:** surgeon

**Response:** HTTP 204 (no body).

---

## Admin (`/admin`)

### `GET /admin/doctors`
Lists all doctors.

**Auth:** staff

**Response:** Array of doctor objects.

---

### `GET /admin/doctors/pending`
Lists doctors awaiting approval.

**Auth:** staff

**Response:** Array of inactive doctor objects.

---

### `PATCH /admin/doctors/{doctor_id}/status`
Activates or deactivates a doctor.

**Auth:** staff

**Request:**
```json
{ "is_active": true }
```

**Response:** Updated doctor object.

---

### `POST /admin/admins`
Creates a new admin/superadmin.

**Auth:** superadmin

**Request:**
```json
{
  "name": "Admin User",
  "phone": "+919876543214",
  "role": "admin"
}
```

**Response:** Created admin object (HTTP 201).

---

### `GET /admin/admins`
Lists all admins.

**Auth:** superadmin

**Response:** Array of admin objects.

---

### `POST /admin/trigger-follow-up-reminders`
Manually triggers the daily follow-up reminder job.

**Auth:** staff

**Response:**
```json
{ "message": "Follow-up reminders triggered" }
```

---

## Doctors (`/doctors`)

### `GET /doctors/{doctor_id}`
Gets public profile for a doctor.

**Auth:** any

**Response:** Doctor profile object.

---

### `GET /doctors`
Lists doctors.

**Auth:** any

**Query params:** `is_active`

**Response:** Array of doctor profile objects.

---

### `GET /doctors/me/stats`
Returns patient/surgery counts for the logged-in doctor.

**Auth:** surgeon / admin / superadmin

**Response:**
```json
{
  "patients": 120,
  "surgeries": 45,
  "success_rate": 0.984
}
```

---

### `GET /doctors/available-slots`
Returns available 30-minute OPD slots.

**Auth:** any

**Query params:** `doctor_id`, `date`

**Response:**
```json
{
  "doctor_id": "doc_123",
  "date": "2026-08-05",
  "slots": ["2026-08-05T08:00:00", "2026-08-05T08:30:00"]
}
```

---

### `GET /doctors/{doctor_id}/availability`
Returns all slots for a date with booked/available flags.

**Auth:** any

**Query params:** `date`

**Response:**
```json
{
  "doctor_id": "doc_123",
  "date": "2026-08-05",
  "slots": [
    { "time": "2026-08-05T08:00:00", "is_booked": false }
  ]
}
```

---

## Schedule (`/schedule`)

### `GET /schedule/ot`
Lists OT/surgery appointments for a date.

**Auth:** nurse/surgeon

**Query params:** `target_date`

**Response:** Array of appointment objects.

---

### `GET /schedule/opd`
Lists OPD appointments for a date.

**Auth:** nurse/surgeon

**Query params:** `target_date`

**Response:** Array of appointment objects.

---

### `POST /schedule/ot`
Books an OT slot.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "patient_id": "pat_1",
  "date": "2026-08-05",
  "time": "09:00",
  "procedure": "Herniotomy",
  "urgency": "routine"
}
```

**Response:** Created appointment object.

---

### `POST /schedule/opd`
Books an OPD slot.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "patient_id": "pat_1",
  "date": "2026-08-05",
  "time": "10:00",
  "visit_type": "opd"
}
```

**Response:** Created appointment object.

---

## Alerts (`/alerts`)

### `GET /alerts`
Dashboard alert summary.

**Auth:** nurse/surgeon

**Response:**
```json
{
  "pending_consents": [],
  "today_appointments": [],
  "pending_reviews": [],
  "active_admissions": []
}
```

---

## Uploads (`/uploads`)

### `POST /uploads/media`
Uploads files to Cloudinary.

**Auth:** any

**Content-Type:** `multipart/form-data`

**Form fields:**
- `files`: one or more files
- `resource_type`: `image`, `video` or `raw`
- `folder`: Cloudinary folder

**Response:**
```json
{
  "urls": ["https://res.cloudinary.com/.../image.jpg"]
}
```

---

## Medical Records (`/medical-records`)

### `POST /medical-records`
Creates a medical record container.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "patient_id": "pat_1",
  "title": "Pre-op photos",
  "description": "Wound site images",
  "opd_record_id": "opd_1",
  "admission_id": "adm_1"
}
```

**Response:**
```json
{
  "id": "mr_1",
  "patient_id": "pat_1",
  "title": "Pre-op photos",
  "image_count": 0,
  "created_at": "2026-08-01T10:00:00Z"
}
```

---

### `GET /medical-records/{patient_id}`
Lists medical record containers for a patient.

**Auth:** any (scoped)

**Response:** Array of medical record summaries.

---

### `GET /medical-records/{record_id}/detail`
Gets a medical record container with all images.

**Auth:** any (scoped)

**Response:** Medical record detail object.

---

### `POST /medical-records/{record_id}/images`
Adds an image to a medical record.

**Auth:** nurse/surgeon

**Request:**
```json
{
  "image_url": "https://cloudinary.com/xray.jpg",
  "category": "X-ray",
  "label": "Chest X-ray",
  "description": "PA view",
  "uploaded_by_role": "surgeon"
}
```

**Response:** Created image object.

---

### `GET /medical-records/{record_id}/images`
Lists images in a medical record.

**Auth:** any (scoped)

**Response:** Array of image objects.

---

## Reports (`/reports`)

### `GET /reports/patient-summary/{patient_id}`
Returns a full patient summary.

**Auth:** any (scoped)

**Response:**
```json
{
  "patient": { "id": "pat_1", "name": "Riya Sharma" },
  "summary": {
    "total_opd_visits": 5,
    "total_admissions": 1,
    "active_admission": true
  },
  "opd_records": [],
  "admissions": [],
  "consent_status": {
    "total_consents": 2,
    "signed_consents": 1,
    "pending_consents": 1
  }
}
```

---

## Notes

- All date/time fields use ISO 8601 format.
- Optional integrations (WhatsApp, SMS, Cloudinary, OpenAI, Firebase) return stub/log-only responses until credentials are configured in `backend/.env`.
- Some endpoints return role-based access errors (403) if the logged-in user does not have permission.
