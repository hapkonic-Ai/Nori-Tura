# Consent Form Module — Manual Test Flow

This document describes how to manually verify the full consent-form module end-to-end on the KMM app (Android / iOS / Web) and the FastAPI backend.

---

## 1. Prerequisites

1. **Backend running**
   ```bash
   cd backend
   docker compose up -d   # PostgreSQL
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```
2. **Mobile/Web app** pointed at the backend:
   - Android emulator: `http://10.0.2.2:8000`
   - iOS simulator / Web: `http://localhost:8000`
3. **Cloudinary** is optional. If not configured, the backend prints upload stubs and `pdf_url` / `signed_pdf_url` will be `null`. PDFs can still be viewed if the data-URI fallback is implemented on the client.
4. A way to read the **dev OTP** returned by `/auth/send-otp` (printed in the API response and backend console).

---

## 2. Test Data Setup

Use these representative Indian phone numbers:

| Role | Phone |
|------|-------|
| Surgeon | `+919876543210` |
| Nurse | `+919876543215` |
| Parent | `+919876543212` |
| Other parent (isolation test) | `+919876543213` |

> **Important:** Each phone number must belong to only one role. The auth flow checks `admin → surgeon → nurse → parent` in that order, so if the parent phone is already registered as a nurse/doctor/admin, login will treat it as staff instead of a parent.

### 2.1 Register & activate the surgeon

```bash
# Register
curl -X POST http://localhost:8000/auth/register-doctor \
  -H "Content-Type: application/json" \
  -d '{"name":"Dr. Priya Mehta","phone":"+919876543210","hospital":"ABC Childrens Hospital","specialty":"Pediatric Surgery"}'

# Activate the doctor (set is_active = true directly in DB, or via admin flow)
# psql "postgresql://postgres:postgres@localhost:5432/noritura" \
#   -c "UPDATE doctors SET is_active = true WHERE phone = '+919876543210';"
```

### 2.2 Create a nurse

Login as the surgeon to obtain a token, then create the nurse:

```bash
# Send OTP
curl -X POST http://localhost:8000/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919876543210"}'

# Verify (use the dev_otp from the response)
curl -X POST http://localhost:8000/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919876543210","otp":"123456"}'

# Create nurse (replace <SURGEON_TOKEN>)
curl -X POST http://localhost:8000/nurses \
  -H "Authorization: Bearer <SURGEON_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Nurse Rani","phone":"+919876543215","hospital":"ABC Childrens Hospital"}'
```

### 2.3 Create a patient (parent phone = `+919876543212`)

```bash
curl -X POST http://localhost:8000/patients \
  -H "Authorization: Bearer <SURGEON_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Rohan Sharma",
    "age":7,
    "gender":"Male",
    "blood_group":"B+",
    "allergies":"Pollen",
    "parent_name":"Mr. Rajesh Sharma",
    "parent_phone":"+919876543212"
  }'
```

Save the returned `patient.id` as `PATIENT_ID`.

### 2.4 Admit the patient

```bash
curl -X POST http://localhost:8000/ipd/admissions \
  -H "Authorization: Bearer <SURGEON_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"patient_id":"<PATIENT_ID>","urgency":"elective","bed_no":"12","ward":"A"}'
```

Save the returned `id` as `ADMISSION_ID`.

---

## 3. Flow A — Surgeon Generates a Consent Form

### From Admission Detail

1. **Login** as the surgeon in the KMM app.
2. Navigate to **Admissions** → tap the admission for Rohan Sharma.
3. Scroll to **Consent Forms** and tap **Add Consent Form**.
4. Fill the form:
   - Form Type: `Surgical Consent`
   - Procedure: `Inguinal Herniotomy`
   - Anesthesia: `General Anesthesia`
   - Risks: `Bleeding, infection, anesthesia complications`
   - Benefits: `Definitive repair of hernia`
   - Alternatives: `Observation, laparoscopic repair`
   - Post-operative Care: `Pain management, wound care`
5. Tap **Generate Consent Form**.

**Expected:**
- You are navigated to the **Consent Form** view.
- Status shows **Pending**.
- **View Generated PDF** opens the unsigned PDF (or browser if Cloudinary is not configured).
- Backend `consent_forms` row has `status = pending`, `pdf_url` populated.

### From Patient Profile (Documents & Consents)

1. Navigate to **Patients** → tap Rohan Sharma.
2. Scroll to **Documents & Consents**.
3. Tap **Generate New Consent**.
4. Fill and submit the form.

**Expected:** Same as above; the new consent appears in the list on `PatientProfileScreen`.

---

## 4. Flow B — Parent Reviews and Signs the Consent

1. **Logout** the surgeon and **login** as parent `+919876543212`.
   - Use the dev OTP from `/auth/send-otp`.
2. On **Parent Home**, verify:
   - A **"Consent Forms Awaiting Signature"** card appears.
   - It shows the form type, patient name, generated date, and **Pending** status.
3. Tap the pending consent card.
4. On **Review & Sign Consent**:
   - Tap **View Generated PDF** and confirm the unsigned PDF opens.
   - Draw a signature in the **Parent / Guardian Signature** pad.
   - (Optional) Enter a witness name and draw a witness signature.
   - Check **"I have read and understood..."**.
   - Tap **Sign Consent**.

**Expected:**
- A green **Consent signed successfully** card appears.
- **View Signed PDF** button opens the signed PDF containing:
  - The original consent content.
  - The parent signature image.
  - The witness name and signature (if provided).
  - A signed timestamp banner.
- Backend `consent_forms` row now has `status = signed`, `signed_at`, `signed_pdf_url`.

5. Navigate to **Profile** from the parent home quick actions.
6. In **Consent Form History**, tap the same form.

**Expected:** The signed PDF opens.

---

## 5. Flow C — Parent Cannot Generate / Surgeon Cannot Sign

### Parent cannot generate

1. Login as parent `+919876543212`.
2. There is no **Generate Consent** button anywhere in the parent UI.
3. (Backend) Directly call the create endpoint with a parent token:
   ```bash
   curl -X POST http://localhost:8000/consent/forms \
     -H "Authorization: Bearer <PARENT_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"admission_id":"<ADMISSION_ID>","form_type":"x","procedure":"x","anesthesia":"x","risks":"x","benefits":"x","alternatives":"x","post_op_care":"x"}'
   ```

**Expected:** `403 Forbidden`.

### Surgeon cannot sign

1. Login as surgeon.
2. Open the consent form view for the pending consent.
3. There is no signature UI (read-only / PDF view only).
4. (Backend) Directly call sign with a surgeon token:
   ```bash
   curl -X POST http://localhost:8000/consent/forms/<CONSENT_ID>/sign \
     -H "Authorization: Bearer <SURGEON_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"parent_signature_url":"data:image/png;base64,AAA"}'
   ```

**Expected:** `403 Forbidden` (only the linked parent can sign).

---

## 6. Flow D — Nurse Can Generate but Cannot Sign

1. Login as nurse `+919876543215`.
2. Navigate to **Admissions** → tap Rohan Sharma's admission.
3. Tap **Add Consent Form**, fill, and submit.

**Expected:** Consent form created successfully; `generated_by` = `nurse` in the database.

4. Try to sign the consent as the nurse (backend direct call):
   ```bash
   curl -X POST http://localhost:8000/consent/forms/<CONSENT_ID>/sign \
     -H "Authorization: Bearer <NURSE_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"parent_signature_url":"data:image/png;base64,AAA"}'
   ```

**Expected:** `403 Forbidden`.

---

## 7. Flow E — Cross-Patient / Cross-Doctor Isolation

### Cross-patient

1. Create a second patient whose parent phone is `+919876543213`.
2. Login as parent `+919876543213`.
3. Try to open the consent form created for Rohan Sharma:
   ```bash
   curl http://localhost:8000/consent/forms/<ROHAN_CONSENT_ID> \
     -H "Authorization: Bearer <OTHER_PARENT_TOKEN>"
   ```

**Expected:** `403 Forbidden`.

### Cross-doctor

1. Register and activate a second surgeon with phone `+919876543214`.
2. Login as that surgeon.
3. Try to fetch Rohan's consent:
   ```bash
   curl http://localhost:8000/consent/forms/<ROHAN_CONSENT_ID> \
     -H "Authorization: Bearer <OTHER_SURGEON_TOKEN>"
   ```

**Expected:** `403 Forbidden`.

---

## 8. Flow F — Re-signing a Signed Form Is Blocked

1. Login as the parent and sign a consent form.
2. Immediately send another sign request for the same consent ID.

```bash
curl -X POST http://localhost:8000/consent/forms/<SIGNED_CONSENT_ID>/sign \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"parent_signature_url":"data:image/png;base64,AAA"}'
```

**Expected:** `400 Bad Request` — "Consent form already signed".

---

## 9. Platform-Specific Checks

Run the full parent + surgeon flow on each target:

| Platform | Build command | Checks |
|----------|---------------|--------|
| Android | `./gradlew :androidApp:installDebug` | Signature pad with finger/stylus, PDF opens in Chrome custom tab, dialer opens from profile. |
| iOS | `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.6' build` | Signature pad with mouse/finger, Safari opens PDF, phone scheme launches dialer. |
| Web / Wasm | `./gradlew :webApp:wasmJsBrowserRun` | Signature pad with mouse, PDF opens in new tab, `tel:` scheme behavior depends on browser. |

---

## 10. Backend Smoke Test (optional)

A minimal Python smoke test is included below for CI/automation:

```python
import httpx, os

BASE = "http://localhost:8000"
SURGEON_PHONE = "+919876543210"
PARENT_PHONE = "+919876543212"


def get_token(phone: str) -> str:
    r = httpx.post(f"{BASE}/auth/send-otp", json={"phone": phone})
    otp = r.json()["dev_otp"]
    r = httpx.post(f"{BASE}/auth/verify-otp", json={"phone": phone, "otp": otp})
    return r.json()["access_token"]


def test_consent_lifecycle():
    surgeon = get_token(SURGEON_PHONE)
    parent = get_token(PARENT_PHONE)

    # Get Rohan's admission
    admissions = httpx.get(
        f"{BASE}/ipd/admissions", headers={"Authorization": f"Bearer {surgeon}"}
    ).json()
    admission_id = next(a["id"] for a in admissions if a["patient"]["name"] == "Rohan Sharma")

    # Create consent
    r = httpx.post(
        f"{BASE}/consent/forms",
        headers={"Authorization": f"Bearer {surgeon}"},
        json={
            "admission_id": admission_id,
            "form_type": "Surgical Consent",
            "procedure": "Herniotomy",
            "anesthesia": "General",
            "risks": "x", "benefits": "x", "alternatives": "x", "post_op_care": "x",
        },
    )
    assert r.status_code == 201
    consent_id = r.json()["consent_form"]["id"]

    # Parent fetches
    r = httpx.get(
        f"{BASE}/consent/forms/{consent_id}",
        headers={"Authorization": f"Bearer {parent}"},
    )
    assert r.status_code == 200

    # Parent signs
    r = httpx.post(
        f"{BASE}/consent/forms/{consent_id}/sign",
        headers={"Authorization": f"Bearer {parent}"},
        json={"parent_signature_url": "data:image/png;base64,iVBORw0KGgo="},
    )
    assert r.status_code == 200
    assert r.json()["status"] == "signed"

    # Re-sign blocked
    r = httpx.post(
        f"{BASE}/consent/forms/{consent_id}/sign",
        headers={"Authorization": f"Bearer {parent}"},
        json={"parent_signature_url": "data:image/png;base64,iVBORw0KGgo="},
    )
    assert r.status_code == 400


if __name__ == "__main__":
    test_consent_lifecycle()
    print("Consent smoke test passed")
```

---

## 11. Known Limitations / Not Implemented

- **"Download All" signed consents as ZIP** is not implemented in this phase.
- **WhatsApp notification toggles** in the parent profile are UI placeholders; backend persistence is part of the notifications phase.
- If Cloudinary is not configured, PDF URLs are `null` and the client falls back to opening the unsigned PDF generated earlier. In production, configure Cloudinary for persistent storage.

---

## 12. Sign-Off Checklist

- [ ] Surgeon can generate a consent form from Admission Detail.
- [ ] Surgeon can generate a consent form from Patient Profile.
- [ ] Parent sees pending consent on Home and Profile.
- [ ] Parent can sign with parent signature, optional witness signature, and required acknowledgment checkbox.
- [ ] Signed PDF embeds parent signature (and witness signature when provided).
- [ ] Parent can view signed consent from Profile history.
- [ ] Nurse can generate consent but cannot sign.
- [ ] Parent cannot generate consent.
- [ ] Cross-patient and cross-doctor access returns 403.
- [ ] Re-signing a signed form returns 400.
- [ ] Builds pass on Android, iOS, and Web/Wasm.
