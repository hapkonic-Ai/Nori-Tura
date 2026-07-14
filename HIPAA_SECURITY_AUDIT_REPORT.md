# HIPAA Security Audit Report — Noritura Surgical Care Platform

**Date:** 2026-07-14
**Audit Conducted By:** Lead HIPAA Security Officer
**Platform:** Kotlin Multiplatform Mobile (Android/iOS) + FastAPI Backend + PostgreSQL + Cloudinary CDN
**Audit Standard:** HIPAA Security Rule (45 CFR Part 164) + HIPAA Privacy Rule (45 CFR Parts 160 and 164)
**Audit Scope:** Full codebase review across 8 security domains — Authentication, PHI Encryption, Access Control, File Upload Security, API Security, Audit Controls, Mobile Application Security, and Administrative Safeguards
**Classification:** CONFIDENTIAL — For Internal Compliance Use Only

---

## Executive Summary

The Noritura Surgical Care Platform, a pediatric surgical care management system handling consent forms, intra-operative records, surgical notes, OPD examination data, and parent contact information, was subjected to a comprehensive HIPAA security audit across all technical and administrative safeguard domains. The audit identified **83 discrete findings** across 8 domains, including **14 unique CRITICAL-severity issues**, **38 HIGH**, **24 MEDIUM**, and **7 LOW/INFO** findings. The platform is **not compliant with the HIPAA Security Rule** in its current state and must not be deployed to production handling real patient data without remediation of all CRITICAL and HIGH severity findings.

The overall weighted compliance score across all 8 domains is **28/100**, representing a failing posture across every HIPAA technical safeguard category. The platform demonstrates architectural intent — a role-based access control structure is present, OTPs are hashed before storage, CORS is restricted to named origins, and the consent form generation pipeline is sophisticated — but this intent is systematically undermined by foundational implementation failures. Two findings alone — the unconditional OTP plaintext leak in every API response and the 30-day JWT expiry override — together constitute a complete authentication bypass. Compounded by the total absence of PHI access audit logging across all 18 API routers, the platform cannot detect, respond to, or demonstrate recovery from a data breach, which is a categorical violation of §164.312(b) Audit Controls and §164.308(a)(6) Security Incident Procedures.

The three most urgent issues requiring immediate remediation before any production traffic involving real patient data are: **(1)** the OTP plaintext returned unconditionally in every `/auth/send-otp` API response (`backend/app/routers/auth.py:73`), which eliminates the second authentication factor entirely and allows any party observing network traffic to authenticate as any user; **(2)** the 30-day JWT expiry hardcoded at the call site (`backend/app/routers/auth.py:161`), which overrides the well-designed 1–4 hour role-based expiry table and makes every stolen credential usable for a month with no revocation path; and **(3)** the complete absence of any PHI access audit log across all patient, OPD, IPD, consent, medical record, and document endpoints, which constitutes a direct and categorical violation of §164.312(b) and makes forensic reconstruction of any breach impossible.

---

## Risk Dashboard

| Domain | Score | Total Findings | Critical | High | Medium | Low/Info |
|---|---|---|---|---|---|---|
| Authentication & Session Management | 34/100 | 17 | 3 | 6 | 5 | 3 |
| PHI Data Handling & Encryption | 22/100 | 20 | 3 | 8 | 6 | 3 |
| Access Control & Authorization | 34/100 | 18 | 3 | 6 | 5 | 4 |
| File Upload & Medical Image Security | 30/100 | 16 | 4 | 7 | 3 | 2 |
| API Security & Input Validation | 34/100 | 20 | 2 | 7 | 7 | 4 |
| Audit Controls & Activity Review | 6/100 | 15 | 4 | 6 | 4 | 1 |
| Mobile Application Security | 22/100 | 17 | 4 | 6 | 4 | 3 |
| Administrative & Organizational Safeguards | 27/100 | 17 | 3 | 7 | 5 | 2 |
| **OVERALL** | **28/100** | **83** | **14** | **38** | **24** | **7** |

---

## CRITICAL Findings (Must Fix Before Any Production Deployment)

The following findings each individually constitute either a complete authentication bypass, a total destruction of the audit trail, or a categorical HIPAA violation that cannot be mitigated by compensating controls. All must be resolved before production traffic is processed.

---

### [AUTH-001 / API-001 / AC-001 / ADM-001 / FINDING-002 / ENC-007 / UPLOAD-007] OTP Plaintext Returned in Every API Response Without Environment Gate

**Location:** `backend/app/routers/auth.py:68–74` and `backend/app/services/otp_service.py:30,51`

**HIPAA Violation:** §164.312(d) Person or Entity Authentication — the second authentication factor is nullified when the secret is returned in the challenge response. §164.308(a)(5)(ii)(D) Password Management — authentication secrets must not be disclosed. §164.312(b) Audit Controls — credentials appearing in application logs and API responses create uncontrolled side-channels.

**CWE:** CWE-916 (Use of Password Hash with Insufficient Computational Effort) and CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor)

**Evidence:**
```python
# backend/app/routers/auth.py:68-74
returned_otp = await create_otp_session(req.phone, role)
return {
    "message": "OTP sent successfully",
    "expires_in_minutes": 5,
    "dev_otp": returned_otp   # <-- no environment gate
}

# backend/app/services/otp_service.py:30
print(f"[DEV OTP] Phone: {phone}, OTP: {otp}")  # executes in production

# backend/app/services/otp_service.py:51
return otp  # Return only for dev/testing; production should not return
# ^ comment-level guard; no code enforcement
```

The client-side DTO already deserializes this field: `shared/src/commonMain/kotlin/com/example/nori_tura/data/dto/AuthDto.kt:36` — `@SerialName("dev_otp") val devOtp: String? = null`.

**Impact:** Any party who can observe an API response — a network intermediary, a compromised logging pipeline, a proxy, a developer workstation running a debug build against production — receives the live OTP and can immediately authenticate as any registered phone number. This affects every user role including surgeons, nurses, parents, administrators, and superadmins. All PHI, consent forms, intra-operative records, and surgical notes accessible to those roles are immediately exposed. The attack requires no device access, no prior knowledge of the OTP, and no cryptographic capability.

**Remediation:**

Step 1 — Remove the OTP return from `otp_service.create_otp_session()`. The function must not return the plaintext OTP to its caller in any environment:
```python
# backend/app/services/otp_service.py — create_otp_session()
# Change: return otp
# To:
return True  # return bool indicating session was created
```

Step 2 — Remove `dev_otp` from the route response entirely. If a development convenience is operationally required, gate it strictly and log a startup warning:
```python
# backend/app/routers/auth.py
response_body = {
    "message": "OTP sent successfully",
    "expires_in_minutes": 5,
}
if settings.ENVIRONMENT == "development":
    response_body["dev_otp"] = returned_otp  # only in local dev
return response_body
```

Step 3 — Replace the `print()` call in `otp_service.py:30` with a structured logger suppressed in production:
```python
import logging
logger = logging.getLogger(__name__)
logger.debug("[DEV] OTP session created for phone ending %s", phone[-4:])
```

Step 4 — Remove `devOtp` from `AuthDto.kt` in the client codebase.

Step 5 — Add an integration test asserting the field is absent in non-development environments.

**Effort:** LOW

---

### [AUTH-002 / AC-002 / UPLOAD-009 / FINDING-011 / ENC-008 / AC-007-audit] JWT Token Expiry Hardcoded to 30 Days — Role-Based Short Expiry Completely Bypassed

**Location:** `backend/app/routers/auth.py:161` and `backend/app/core/security.py:11–27`

**HIPAA Violation:** §164.312(a)(2)(iii) Automatic Logoff — sessions must terminate after a predetermined period. §164.312(d) Authentication. §164.308(a)(5)(ii)(D) Password Management.

**CWE:** CWE-613 (Insufficient Session Expiration)

**Evidence:**
```python
# backend/app/core/security.py:11-17 — correct policy, never reached
ROLE_EXPIRATION_HOURS = {
    "surgeon": 4,
    "nurse": 4,
    "patient_parent": 2,
    "admin": 2,
    "superadmin": 1,
}

# backend/app/core/security.py:20-23 — the override mechanism
def create_access_token(data: dict, expires_delta: timedelta | None = None):
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta  # 30-day arg always wins
    ...

# backend/app/routers/auth.py:161 — explicit 30-day override
access_token = create_access_token(token_payload, expires_delta=timedelta(days=30))
```

The `ROLE_EXPIRATION_HOURS` table in `security.py` is dead code. Every role — including superadmin with platform-wide privileges — receives a 30-day token.

**Impact:** A stolen or intercepted JWT token grants full, unauthenticated access to all PHI for 30 days with no revocation path. Combined with plaintext SharedPreferences storage (FINDING-001), ADB backup enabled (FINDING-009), and no server-side logout (AUTH-008), a nurse whose employment is terminated retains full API access for 30 days after their device is surrendered. A token captured from an ADB backup or compromised logging system cannot be invalidated.

**Remediation:**

Step 1 — Remove the `expires_delta` argument from the call site:
```python
# backend/app/routers/auth.py:161
# Change:
access_token = create_access_token(token_payload, expires_delta=timedelta(days=30))
# To:
access_token = create_access_token(token_payload)
# The role-based ROLE_EXPIRATION_HOURS in security.py now applies automatically.
```

Step 2 — Implement a refresh token flow for mobile clients. Issue short-lived access tokens (per `ROLE_EXPIRATION_HOURS`) and a separate 7-day rolling refresh token stored server-side in a `refresh_tokens` table. The refresh token is the revocable, long-lived credential; the access token is the short-lived bearer.

Step 3 — Add a `jti` (JWT ID) claim using `secrets.token_hex(16)` to every issued access token, prerequisite for the revocation mechanism in AUTH-008.

**Effort:** LOW (removing the argument) to MEDIUM (refresh token implementation)

---

### [AUTH-003 / API-002 / AC-007 / AC-010-audit / UPLOAD-014 / FINDING-007] No OTP Brute-Force Protection — 6-Digit OTP Fully Enumerable

**Location:** `backend/app/services/otp_service.py:54–78`, `backend/app/routers/auth.py:53–74`, `backend/prisma/schema.prisma` (otp_sessions model)

**HIPAA Violation:** §164.312(d) Person or Entity Authentication — the authentication control is rendered ineffective by brute-force enumeration. §164.308(a)(5)(ii)(C) Log-in Monitoring.

**CWE:** CWE-307 (Improper Restriction of Excessive Authentication Attempts)

**Evidence:**
```python
# backend/app/services/otp_service.py:54-77
async def verify_otp(phone: str, otp: str) -> dict:
    session = await prisma.otp_sessions.find_first(where={"phone": phone, ...})
    # No attempt counter. No lockout. No rate check.
    if hash_otp(otp) != session.otp_hash:
        raise ValueError("Invalid OTP")
    return {...}
```

```prisma
// backend/prisma/schema.prisma — otp_sessions model
model otp_sessions {
  id         String   @id
  phone      String
  otp_hash   String
  // No: attempt_count Int @default(0)
  // No: locked_at     DateTime?
}
```

No `slowapi`, `fastapi-limiter`, or equivalent middleware appears anywhere in `backend/app/main.py` or `requirements.txt`. A 6-digit numeric OTP has 1,000,000 possible values; with a 5-minute validity window and no throttling, automated enumeration completes in seconds at typical API throughput.

**Impact:** An attacker who knows any registered phone number can brute-force the OTP within the 5-minute window, obtain a 30-day JWT (compounded with AUTH-002), and access all PHI associated with that user's role. The send-otp endpoint also has no rate limit, enabling SMS flooding against any phone number as a denial-of-service attack.

**Remediation:**

Step 1 — Add `attempt_count` and `locked_at` columns to the `otp_sessions` Prisma model:
```prisma
model otp_sessions {
  id            String    @id
  phone         String
  otp_hash      String
  attempt_count Int       @default(0)
  locked_at     DateTime?
  expires_at    DateTime
  created_at    DateTime  @default(now())
}
```

Step 2 — Enforce lockout in `verify_otp()`:
```python
async def verify_otp(phone: str, otp: str) -> dict:
    session = await prisma.otp_sessions.find_first(where={"phone": phone, ...})
    if session.locked_at:
        raise ValueError("OTP session locked. Request a new OTP.")
    if hash_otp(otp) != session.otp_hash:
        new_count = session.attempt_count + 1
        update_data = {"attempt_count": new_count}
        if new_count >= 5:
            update_data["locked_at"] = datetime.now(timezone.utc)
        await prisma.otp_sessions.update(where={"id": session.id}, data=update_data)
        raise ValueError("Invalid OTP")
    ...
```

Step 3 — Add `slowapi` rate limiting to authentication endpoints in `backend/app/main.py` and the auth router:
```python
# requirements.txt: add slowapi
# backend/app/main.py
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# backend/app/routers/auth.py
@router.post("/send-otp")
@limiter.limit("3/10minute")
async def send_otp(request: Request, ...):
    ...

@router.post("/verify-otp")
@limiter.limit("5/5minute")
async def verify_otp_endpoint(request: Request, ...):
    ...
```

**Effort:** MEDIUM

---

### [ENC-001] No Encryption at Rest for Any PHI in the Database

**Location:** `backend/prisma/schema.prisma` (all PHI-bearing models); `backend/app/core/database.py`; `backend/.env:1`

**HIPAA Violation:** §164.312(a)(2)(iv) Encryption and Decryption of ePHI.

**CWE:** CWE-311 (Missing Encryption of Sensitive Data)

**Evidence:**
```prisma
// backend/prisma/schema.prisma — representative PHI fields stored in plaintext
model patients {
  name         String
  age          Int
  allergies    String?      // PHI — plaintext
  parent_phone String       // PHI — plaintext
  parent_name  String
}

model opd_records {
  complaint    String       // PHI — plaintext
  examination  String       // PHI — plaintext
  diagnosis    String?      // PHI — plaintext
  medications  Json         // PHI — plaintext JSON
}

model consent_forms {
  consent_text String       // PHI — full consent text plaintext
  content_json Json         // PHI — full patient/clinical data blob
}
```

```
# backend/.env:1
DATABASE_URL=postgresql://nonitura:nonitura123@localhost:5432/nonitura
# No sslmode=verify-full, no pgcrypto extension
```

No `pgcrypto`, no `sqlalchemy-utils EncryptedType`, no Fernet/AES-GCM application-layer encryption, and no TDE configuration exists anywhere in the codebase. Any database-level compromise — SQL injection, unauthorized DB admin access, a misconfigured cloud snapshot — yields all PHI in cleartext.

**Impact:** Complete PHI disclosure from any database-level breach. This affects patient demographics, diagnoses, surgical notes, parent contact data, consent text, intra-operative records, and examination findings for every patient in the system.

**Remediation:**

Step 1 — Implement field-level encryption for the highest-sensitivity columns using the `cryptography` library's Fernet (AES-128-CBC with HMAC) or AES-256-GCM:
```python
# backend/app/core/encryption.py
from cryptography.fernet import Fernet
import os

_fernet_key = os.environ["PHI_ENCRYPTION_KEY"].encode()
_fernet = Fernet(_fernet_key)

def encrypt_phi(value: str) -> str:
    return _fernet.encrypt(value.encode()).decode()

def decrypt_phi(value: str) -> str:
    return _fernet.decrypt(value.encode()).decode()
```

Apply to: `complaint`, `examination`, `diagnosis`, `allergies`, `parent_phone`, `consent_text`, `content_json` before Prisma writes and after Prisma reads.

Step 2 — Add `?sslmode=verify-full` to `DATABASE_URL` in production:
```
DATABASE_URL=postgresql://nonitura:<pass>@<host>:5432/nonitura?sslmode=verify-full&sslrootcert=/etc/ssl/certs/ca.crt
```

Step 3 — Add a startup validator in `backend/app/core/config.py` that enforces `sslmode=verify-full` is present when `ENVIRONMENT == "production"`.

Step 4 — Consider enabling PostgreSQL transparent data encryption at the managed cloud provider level (AWS RDS encryption-at-rest, GCP Cloud SQL encryption) as a complementary layer.

**Effort:** HIGH

---

### [ENC-002 / AUTH-005 / UPLOAD-006 / FINDING-004 / ENC-003] Android and iOS Apps Explicitly Disable Transport Security Globally

**Location:** `androidApp/src/main/AndroidManifest.xml:13`; `iosApp/iosApp/Info.plist:9–10`; `shared/src/androidMain/kotlin/com/example/nori_tura/data/BaseUrlProvider.android.kt:3`; `shared/src/iosMain/kotlin/com/example/nori_tura/data/BaseUrlProvider.ios.kt:3`

**HIPAA Violation:** §164.312(e)(1) Transmission Security. §164.312(e)(2)(ii) Encryption of ePHI in transit.

**CWE:** CWE-319 (Cleartext Transmission of Sensitive Information)

**Evidence:**
```xml
<!-- androidApp/src/main/AndroidManifest.xml:13 -->
android:usesCleartextTraffic="true"
<!-- No android:networkSecurityConfig attribute. Global cleartext permitted. -->
```

```xml
<!-- iosApp/iosApp/Info.plist:9-10 -->
<key>NSAllowsArbitraryLoads</key>
<true/>
<!-- No NSExceptionDomains. ATS globally disabled. -->
```

```kotlin
// shared/src/androidMain/kotlin/com/example/nori_tura/data/BaseUrlProvider.android.kt:3
actual fun getBaseUrl(): String = "http://10.0.2.2:8000"  // HTTP scheme

// shared/src/iosMain/kotlin/com/example/nori_tura/data/BaseUrlProvider.ios.kt:3
actual fun getBaseUrl(): String = "http://127.0.0.1:8000"  // HTTP scheme
```

No `network_security_config.xml` exists under `res/xml/`. No build-type-specific URL override mechanism exists.

**Impact:** All API traffic — JWT bearer tokens, OTP codes, patient demographics, diagnoses, surgical notes, consent form contents, medical image URLs, and parent phone numbers — traverses unencrypted HTTP connections. Any on-path observer on a hospital Wi-Fi network, a clinical LAN, or an intercepting proxy can read all PHI in transit. This is exploitable without any cryptographic capability.

**Remediation:**

Step 1 — Remove `android:usesCleartextTraffic="true"` from `AndroidManifest.xml`.

Step 2 — Create `androidApp/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
        <!-- Permit cleartext only for emulator loopback in debug builds -->
        <domain-config cleartextTrafficPermitted="true">
            <domain>10.0.2.2</domain>
        </domain-config>
    </debug-overrides>
</network-security-config>
```

Step 3 — Add `android:networkSecurityConfig="@xml/network_security_config"` to the `<application>` tag in `AndroidManifest.xml`.

Step 4 — Remove `NSAllowsArbitraryLoads` from `iosApp/iosApp/Info.plist`. Replace with:
```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSExceptionDomains</key>
    <dict>
        <key>localhost</key>
        <dict>
            <key>NSExceptionAllowsInsecureHTTPLoads</key>
            <true/>
        </dict>
    </dict>
</dict>
```

Step 5 — Update `BaseUrlProvider` using build flavors:
```kotlin
// shared/src/androidMain/kotlin/.../BaseUrlProvider.android.kt
actual fun getBaseUrl(): String = BuildConfig.API_BASE_URL
// In build.gradle.kts:
// debug: buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
// release: buildConfigField("String", "API_BASE_URL", "\"https://api.noritura.in\"")
```

**Effort:** LOW (manifest changes) to MEDIUM (build flavor configuration)

---

### [ENC-006 / UPLOAD-003 / ADM-008] Medical Images and Consent PDFs Stored as Permanently Public Cloudinary URLs

**Location:** `backend/app/services/cloudinary_service.py:51–59`; `backend/app/routers/consent.py:76–85`

**HIPAA Violation:** §164.312(a)(2)(iv) Encryption and Decryption — PHI at rest must be access-controlled. §164.312(a)(1) Access Control.

**CWE:** CWE-284 (Improper Access Control)

**Evidence:**
```python
# backend/app/services/cloudinary_service.py:53-58
result = cloudinary.uploader.upload(
    file_bytes,
    resource_type=resource_type,
    public_id=public_id,
    filename=filename,
    # No: type="authenticated"
    # No: access_control=[{"access_type": "token"}]
    # No: access_mode
)

# backend/app/routers/consent.py:79-84
cloudinary.uploader.upload(
    pdf_bytes,
    resource_type="raw",
    public_id=filename,
    folder="nonitura/consents",
    # Same pattern — consent PDFs with parent signatures are publicly accessible
)
```

Cloudinary's default delivery type is `upload`, producing URLs permanently accessible to any party who possesses the URL. Consent PDFs contain patient name, age, gender, diagnosis, procedure, parent signature images, and parent phone numbers.

**Impact:** Any party who obtains a Cloudinary URL — through browser history, a shared link, log interception, URL enumeration, or a database breach — can access the PHI file permanently with no authentication, no expiry, and no audit trail. The Cloudinary URL namespace is deterministic and partially guessable based on `public_id` patterns.

**Remediation:**

Step 1 — Change all uploads to use authenticated delivery:
```python
# backend/app/services/cloudinary_service.py
result = cloudinary.uploader.upload(
    file_bytes,
    resource_type=resource_type,
    public_id=public_id,
    type="authenticated",        # Requires signed URL for access
    access_control=[{"access_type": "token"}],
)
```

Step 2 — Store the `public_id` in the database rather than the `secure_url`. Generate signed, time-limited delivery URLs at access time:
```python
# backend/app/services/cloudinary_service.py
import time
def generate_signed_url(public_id: str, resource_type: str, expires_in_seconds: int = 3600) -> str:
    expiry = int(time.time()) + expires_in_seconds
    url, _ = cloudinary.utils.cloudinary_url(
        public_id,
        resource_type=resource_type,
        type="authenticated",
        sign_url=True,
        expires_at=expiry,
    )
    return url
```

Step 3 — Update all document retrieval endpoints to call `generate_signed_url()` and return a fresh signed URL per request rather than storing a permanent URL.

Step 4 — Execute a Cloudinary Business Associate Agreement.

**Effort:** MEDIUM

---

### [UPLOAD-001] No File Type Validation or MIME Type Checking on Upload Endpoint

**Location:** `backend/app/routers/uploads.py:23–48`; `backend/app/services/cloudinary_service.py:36–62`

**HIPAA Violation:** §164.312(c)(1) Integrity Controls — ePHI must not be improperly altered or destroyed.

**CWE:** CWE-434 (Unrestricted Upload of File with Dangerous Type)

**Evidence:**
```python
# backend/app/routers/uploads.py:33-45
for upload in files:
    content = await upload.read()       # Entire file into memory, no size check
    url = await upload_media(
        content,
        folder=folder,                  # Caller-controlled, no allowlist
        resource_type=resource_type,    # Caller-controlled
        filename=upload.filename,       # Trusted from client
    )
    # No check on upload.content_type
    # No magic-byte validation
    # No size limit
```

```python
# backend/app/services/cloudinary_service.py:53-58
result = cloudinary.uploader.upload(
    file_bytes,
    resource_type=resource_type,  # From client
    # No transformation, no format enforcement, no malware scan
)
```

**Impact:** An authenticated user can upload executable files, HTML phishing pages, SVG with embedded scripts, or malformed files to the `nonitura/` Cloudinary namespace. These files can be used for stored XSS (if the HTML is ever served directly), phishing, or malware distribution. An attacker can also upload arbitrarily large files to exhaust server memory (entire file is read into memory via `await upload.read()` with no size cap) causing denial of service.

**Remediation:**

Step 1 — Add server-side MIME type validation using `python-magic`:
```python
# backend/app/routers/uploads.py
import magic

ALLOWED_MIMES = {
    "image/jpeg", "image/png", "image/webp",
    "application/pdf",
}
MAX_FILE_SIZE = 20 * 1024 * 1024  # 20 MB

for upload in files:
    content = await upload.read(MAX_FILE_SIZE + 1)
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(413, "File too large")
    detected_mime = magic.from_buffer(content, mime=True)
    if detected_mime not in ALLOWED_MIMES:
        raise HTTPException(400, f"File type not permitted: {detected_mime}")
```

Step 2 — Restrict the `folder` parameter to an allowlist:
```python
from typing import Literal
folder: Literal["noritura", "noritura/medical", "noritura/documents"] = Form("noritura")
```

Step 3 — Limit the number of files per request: `if len(files) > 10: raise HTTPException(400, "Too many files")`.

Step 4 — Add the `python-magic` dependency to `requirements.txt`.

**Effort:** MEDIUM

---

### [AC-001-audit / API-005 / AC-006 / UPLOAD-015 / ADM-009] Zero PHI Access Audit Log — Total Absence Across All 18 API Routers

**Location:** `backend/app/routers/patients.py`, `backend/app/routers/opd.py`, `backend/app/routers/ipd.py`, `backend/app/routers/medical_records.py`, `backend/app/routers/documents.py`, `backend/app/routers/consent.py`, `backend/app/routers/reports.py` (and all remaining routers)

**HIPAA Violation:** §164.312(b) Audit Controls — direct, categorical violation. §164.308(a)(1)(ii)(D) Information System Activity Review. §164.308(a)(5)(ii)(C) Log-in Monitoring.

**CWE:** CWE-778 (Insufficient Logging)

**Evidence:**
```python
# backend/app/routers/patients.py — _fetch_patient() returns full PHI record
# No audit log call before or after the DB read.
patient = await prisma.patients.find_first(
    where={"id": patient_id},
    include={"opd_records": True, "ipd_admissions": True, "consent_forms": True}
)
# Zero logging of who accessed this record.

# backend/app/routers/medical_records.py — get_medical_record_detail()
# Returns medical images. No audit log.

# backend/app/routers/consent.py — get_consent_form()
# Returns signed PDF URLs, consent text, parent signature. No audit log.
```

```python
# Full search result:
# grep -rn "audit\|AuditLog\|access_log\|phi_access" backend/app/
# Returns: 0 results in application code.
```

```prisma
// backend/prisma/schema.prisma
// No audit_log model
// No phi_access_log model
// No access_events model
```

**Impact:** It is impossible to determine who accessed any patient's PHI, when, from where, or how many times. A malicious insider can exfiltrate an entire patient dataset. A breach from a compromised credential cannot be reconstructed. An OCR audit or breach investigation cannot be responded to. The platform cannot demonstrate HIPAA compliance under any regulatory review scenario.

**Remediation:**

Step 1 — Add an `audit_logs` model to `backend/prisma/schema.prisma`:
```prisma
model audit_logs {
  id            String   @id @default(cuid())
  actor_phone   String
  actor_role    String
  actor_id      String?  // doctor_id or admin_id
  action        String   // READ, CREATE, UPDATE, DELETE
  resource_type String   // patient, opd_record, consent_form, etc.
  resource_id   String
  patient_id    String?
  ip_address    String?
  user_agent    String?
  session_id    String?
  created_at    DateTime @default(now())

  @@index([actor_phone, created_at])
  @@index([patient_id, created_at])
  @@index([resource_type, resource_id])
}
```

Step 2 — Implement a FastAPI middleware that writes an audit entry on every PHI-touching request:
```python
# backend/app/middleware/audit.py
from starlette.middleware.base import BaseHTTPMiddleware
import logging

PHI_PATHS = {"/patients", "/opd", "/ipd", "/medical-records",
             "/consent/forms", "/documents", "/reports"}

class AuditLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        if any(request.url.path.startswith(p) for p in PHI_PATHS):
            try:
                user = getattr(request.state, "user", None)
                if user and response.status_code < 400:
                    await prisma.audit_logs.create(data={
                        "actor_phone": user.phone,
                        "actor_role": user.role,
                        "action": "READ" if request.method == "GET" else "WRITE",
                        "resource_type": request.url.path.split("/")[1],
                        "resource_id": request.path_params.get("patient_id", ""),
                        "ip_address": request.client.host,
                        "user_agent": request.headers.get("user-agent", ""),
                    })
            except Exception:
                logging.exception("Audit log write failed")
        return response
```

Step 3 — Register the middleware in `backend/app/main.py`.

Step 4 — Store audit logs in an append-only schema with a dedicated DB user that has INSERT-only permissions. Implement a 6-year retention archiving job per §164.530(j).

**Effort:** HIGH

---

### [AC-004-audit] Audit Log Storage in Same Mutable Database — No Tamper-Proof Separation

**Location:** `backend/prisma/schema.prisma`; `backend/app/core/database.py`

**HIPAA Violation:** §164.312(b) Audit Controls — audit records must be protected against unauthorized modification.

**CWE:** CWE-778 (Insufficient Logging), CWE-284 (Improper Access Control on audit store)

**Evidence:**
```python
# backend/app/core/database.py
prisma = Prisma()  # Single client used for ALL operations
# The same Prisma instance that creates patients, OPD records, and clinical notes
# can also UPDATE or DELETE any log table — no separation of privilege.
```

The `whatsapp_logs` table (the only existing log table) is in the same PostgreSQL database, accessible via the same application credentials, with no row-level security, no PostgreSQL triggers preventing `UPDATE`/`DELETE`, and no write-once guarantees.

**Impact:** Any application-layer SQL injection, any compromised admin account, or any developer with database access can silently delete or modify the only log data that exists. In a breach scenario, an attacker who also has the application DB credentials can cover their tracks completely.

**Remediation:**

Step 1 — Create a separate PostgreSQL role for audit log writes with INSERT-only permissions:
```sql
CREATE ROLE audit_writer;
GRANT INSERT ON audit_logs TO audit_writer;
REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM audit_writer;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY audit_insert_only ON audit_logs FOR INSERT TO audit_writer WITH CHECK (true);
```

Step 2 — Use a separate database connection string (`AUDIT_DB_URL`) bound to the `audit_writer` role for all audit log writes.

Step 3 — For production, route audit logs to an append-only external store: AWS CloudTrail, Google Cloud Audit Logs, or an S3 bucket with Object Lock (Compliance mode, 6-year retention). This ensures that even a full database compromise cannot retroactively erase the audit trail.

Step 4 — Document the audit log retention policy in code and configuration: minimum 6 years per §164.530(j).

**Effort:** HIGH

---

### [AC-003 / API-003 / ADM-003 / ENC-010 / UPLOAD-008 / FINDING-012] Unauthenticated Consent Acknowledgment Endpoint with Attacker-Controlled IP Address

**Location:** `backend/app/routers/consent.py:101–136`; `backend/app/schemas/consent_acknowledgment.py`

**HIPAA Violation:** §164.312(b) Audit Controls — consent audit trail integrity is destroyed. §164.312(d) Authentication. §164.308(a)(1)(ii)(D) Information System Activity Review.

**CWE:** CWE-306 (Missing Authentication for Critical Function), CWE-290 (Authentication Bypass by Spoofing)

**Evidence:**
```python
# backend/app/routers/consent.py:101-102
@router.post("/acknowledge", ...)
async def acknowledge_consent(req: ConsentAcknowledgmentRequest):
    # No: user: CurrentUser = Depends(get_current_user)
    # No authentication. Any caller. Any phone number.

# backend/app/schemas/consent_acknowledgment.py
class ConsentAcknowledgmentRequest(BaseModel):
    phone: str = Field(..., pattern=r'^\+91[0-9]{10}$')  # Caller supplies any phone
    client_ip: Optional[str] = None    # Caller supplies any IP — trivially forgeable
    device_info: Optional[str] = None

# backend/app/routers/consent.py:117,127
await prisma.consent_acknowledgments.upsert(
    ...
    create={"phone": req.phone, "ip_address": req.client_ip, ...},
    # The database record reflects whatever the attacker provided.
)
```

**Impact:** An attacker can forge consent acknowledgment records for any phone number, pre-acknowledging consent before the victim user has seen any consent text. This poisons the legally significant consent audit trail and could be used to bypass any future server-side consent check. Additionally, the `client_ip` field is entirely attacker-controlled, making the forensic evidentiary value of IP-based consent records zero.

**Remediation:**
```python
# backend/app/routers/consent.py
from fastapi import Request as FastAPIRequest

@router.post("/acknowledge", ...)
async def acknowledge_consent(
    req: ConsentAcknowledgmentRequest,
    request: FastAPIRequest,
    user: CurrentUser = Depends(get_current_user),  # Require authentication
):
    # Use authenticated identity — ignore req.phone
    real_ip = request.client.host  # Server-extracted
    # Use X-Forwarded-For only if behind a trusted proxy
    forwarded_for = request.headers.get("X-Forwarded-For")
    if forwarded_for and settings.TRUST_PROXY:
        real_ip = forwarded_for.split(",")[0].strip()

    await prisma.consent_acknowledgments.upsert(
        where={"phone": user.phone},
        create={"phone": user.phone, "ip_address": real_ip, ...},
        update={"ip_address": real_ip, ...},
    )
```

Remove `client_ip` from `ConsentAcknowledgmentRequest`. Remove `phone` from the request schema — derive from authenticated user.

**Effort:** LOW

---

### [ADM-002] Consent Acknowledgment Enforced Only in Android UI — No Server-Side Gate on PHI Access

**Location:** `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/auth/LoginScreen.kt:59–65`; all PHI routers (zero enforcement)

**HIPAA Violation:** §164.508(b)(2) Informed Consent — access to PHI requires documented consent. §164.530(c) Safeguards. §164.508(a)(1) Authorization.

**CWE:** CWE-602 (Client-Side Enforcement of Server-Side Security)

**Evidence:**
```kotlin
// shared/src/commonMain/kotlin/.../presentation/auth/LoginScreen.kt:61
onAcknowledged = { consentAcknowledged = true }
// This sets an in-memory variable. It resets on every app restart.
// No API call to POST /consent/acknowledge is made here.
// Any API client bypasses this entirely.
```

```python
# Zero enforcement in ANY backend router:
# grep -rn "consent_acknowledgments" backend/app/routers/
# Returns: 0 results (outside consent.py itself)
```

**Impact:** Any API client — a modified app, curl, Postman, or any script — bypasses the consent dialog entirely and accesses all PHI without ever presenting or acknowledging consent terms. Even the legitimate app's consent state resets on every process restart.

**Remediation:**

Step 1 — Create a server-side dependency:
```python
# backend/app/core/auth_deps.py
async def require_consent(user: CurrentUser = Depends(get_current_user)):
    consent = await prisma.consent_acknowledgments.find_first(
        where={"phone": user.phone, "consent_version": settings.CURRENT_CONSENT_VERSION}
    )
    if not consent:
        raise HTTPException(
            status_code=403,
            detail="Consent acknowledgment required. Please acknowledge the platform terms."
        )
    return user
```

Step 2 — Apply this dependency to all PHI-access routes:
```python
# backend/app/routers/patients.py
@router.get("/{patient_id}")
async def get_patient(
    patient_id: str,
    user: CurrentUser = Depends(require_consent),  # Replaces get_current_user
    ...
):
```

Step 3 — Fix the Android client to actually call the API:
```kotlin
// LoginScreen.kt
onAcknowledged = {
    viewModel.acknowledgeConsent(phone = phone) // calls POST /consent/acknowledge
}
```

Step 4 — The `POST /consent/acknowledge` endpoint must be called after OTP verification (so the user is authenticated), not before login.

**Effort:** MEDIUM

---

### [ADM-005] PHI Transmitted to External AI Providers Without De-identification or BAA

**Location:** `backend/app/services/ai_service.py:12–48`; `backend/app/routers/ai.py:38–95`

**HIPAA Violation:** §164.308(b)(1) Business Associate Contracts — PHI may only be shared under an executed BAA. §164.502(e) Disclosures to Business Associates. §164.514(b) De-identification Standard.

**CWE:** CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor)

**Evidence:**
```python
# backend/app/services/ai_service.py:38-48
prompt = PEDIATRIC_SURGERY_PROMPT.format(
    complaint=complaint,     # Free-text, may contain patient name
    examination=examination, # Free-text, may contain identifiers
    age=age,
    gender=gender,
    ...
)
# Transmitted to OpenAI or Anthropic API. No de-identification. No BAA evidence.

# backend/app/routers/ai.py:39
user: CurrentUser = Depends(get_current_user)  # Parents can trigger this
# A parent can submit: "My son John Doe, DOB 2018-03-12, has..."
# This free-text goes to OpenAI/Anthropic with no scrubbing.
```

**Impact:** Clinical free-text fields submitted by clinicians routinely contain patient names, dates of birth, and other direct identifiers. These are transmitted to external AI providers. Without an executed BAA, this constitutes an unauthorized disclosure of PHI under §164.502(e). The parent role can also trigger this endpoint, meaning unauthorized parties can submit clinical queries that generate OPD records attributed to the clinical system.

**Remediation:**

Step 1 — Restrict the AI endpoint to clinical staff:
```python
# backend/app/routers/ai.py:39
user: CurrentUser = Depends(get_current_nurse_or_surgeon)  # Remove parent access
```

Step 2 — Implement a PHI scrubbing step before constructing the AI prompt:
```python
# backend/app/services/ai_service.py
import re

def scrub_phi(text: str) -> str:
    # Remove name-like patterns, phone numbers, dates of birth
    text = re.sub(r'\b\d{10}\b', '[PHONE]', text)
    text = re.sub(r'\b\d{1,2}/\d{1,2}/\d{4}\b', '[DATE]', text)
    text = re.sub(r'\b[A-Z][a-z]+ [A-Z][a-z]+\b', '[NAME]', text)
    return text

prompt = PEDIATRIC_SURGERY_PROMPT.format(
    complaint=scrub_phi(complaint),
    examination=scrub_phi(examination),
    ...
)
```

Step 3 — Execute Business Associate Agreements with OpenAI and Anthropic before any production use of the AI endpoint.

Step 4 — Add an explicit consent item covering AI-assisted clinical decision support to the platform consent text.

Step 5 — Add input length limits: `complaint: str = Field(..., max_length=2000)` and `examination: str = Field(..., max_length=2000)` to prevent prompt injection via oversized inputs.

**Effort:** HIGH

---

## HIGH Findings

The following findings are individually significant HIPAA violations or security failures that substantially increase the risk of unauthorized PHI access. Each should be remediated within the first month of a compliance effort.

---

### [AUTH-004 / ENC-004 / UPLOAD-013 / FINDING-001] JWT Bearer Token Stored in Unencrypted SharedPreferences

**Location:** `shared/src/commonMain/kotlin/com/example/nori_tura/data/AuthRepository.kt:55–58`; `gradle/libs.versions.toml:92`

**HIPAA Violation:** §164.312(a)(2)(iv) Encryption and Decryption. §164.312(d) Authentication.

**Evidence:** `multiplatform-settings = { module = "com.russhwolf:multiplatform-settings-no-arg" }` — the `-no-arg` variant uses unencrypted `SharedPreferences` on Android and `NSUserDefaults` on iOS. The JWT is stored at `settings[KEY_TOKEN] = token` and read at every HTTP request. No `EncryptedSharedPreferences`, Android Keystore, or iOS Keychain call exists anywhere in the codebase.

**Impact:** On any rooted Android device or via ADB backup (enabled — see FINDING-009), the JWT is extractable from `/data/data/com.example.nori_tura/shared_prefs/*.xml` in plaintext. With a 30-day token lifetime, this provides a month of unauthorized PHI access from a single device compromise.

**Remediation:** Switch to `com.russhwolf:multiplatform-settings-secure` backed by `EncryptedSharedPreferences` using the Android Keystore on Android and iOS Keychain on iOS. For Android, use `androidx.security:security-crypto`:
```kotlin
// shared/src/androidMain/kotlin/.../data/SecureSettings.android.kt
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**Effort:** MEDIUM

---

### [AUTH-006] Deactivated Nurse Receives Valid JWT at OTP Verification

**Location:** `backend/app/routers/auth.py:148–153`

**HIPAA Violation:** §164.312(d) Authentication — tokens must not be issued to deactivated accounts.

**Evidence:**
```python
# auth.py:149 — no is_active filter
nurse = await prisma.nurses.find_first(where={"phone": req.phone})
# Compare to OTP send at _determine_role() which correctly filters:
nurse = await prisma.nurses.find_first(where={"phone": phone, "is_active": True})
```

**Remediation:**
```python
# auth.py:149
nurse = await prisma.nurses.find_first(where={"phone": req.phone, "is_active": True})
if not nurse:
    raise HTTPException(status_code=403, detail="Account is inactive")
```

**Effort:** LOW

---

### [API-006] Deactivated Surgeon JWT Remains Valid — No DB Validation Per Request

**Location:** `backend/app/core/auth_deps.py:51–54`, `resolve_doctor_id:71–72`

**HIPAA Violation:** §164.312(a)(2)(iii) Automatic Logoff. §164.308(a)(3)(ii)(C) Termination Procedures.

**Evidence:**
```python
# auth_deps.py:71-72 — surgeon active status never checked after token issuance
if user.is_surgeon():
    return user.doctor_id  # No DB lookup. Deactivated surgeons still pass.
# Contrast with nurse check at lines 73-79:
nurse = await prisma.nurses.find_first(where={"id": ..., "is_active": True})
```

**Remediation:**
```python
if user.is_surgeon():
    doctor = await prisma.doctors.find_first(where={"id": user.doctor_id, "is_active": True})
    if not doctor:
        raise HTTPException(403, "Doctor account is inactive")
    return user.doctor_id
```

**Effort:** LOW

---

### [AUTH-008 / AC-013 / FINDING-015 / ADM-014] No Server-Side JWT Revocation — Logout Is Client-Side Only

**Location:** `shared/src/commonMain/kotlin/com/example/nori_tura/data/AuthRepository.kt:75–78`; `backend/app/routers/auth.py` (no logout endpoint)

**HIPAA Violation:** §164.312(a)(2)(iii) Automatic Logoff. §164.308(a)(3)(ii)(C) Termination Procedures.

**Evidence:**
```kotlin
// AuthRepository.kt:75-78
fun clearAll() {
    clearToken()  // Removes from local SharedPreferences only
    clearRole()   // Server-side token remains valid for 30 days
}
// No HTTP call to any /auth/logout endpoint.
```

**Remediation:** Add a `jti` claim to every issued JWT. Implement a `revoked_tokens` table. Add `POST /auth/logout` that inserts the JTI. Check the table in `decode_token()`:
```python
async def decode_token(token: str) -> dict:
    payload = jwt.decode(...)
    jti = payload.get("jti")
    if jti:
        revoked = await prisma.revoked_tokens.find_first(where={"jti": jti})
        if revoked:
            raise HTTPException(401, "Token has been revoked")
    return payload
```

**Effort:** HIGH

---

### [AUTH-009] HS256 Symmetric JWT — Single Secret Compromise Forges All Tokens

**Location:** `backend/app/core/config.py:10`; `backend/app/core/security.py:29,34`

**HIPAA Violation:** §164.312(d) Authentication. §164.312(a)(2)(i) Unique User Identification.

**Evidence:** `JWT_ALGORITHM: str = "HS256"` — a single shared secret signs and verifies all tokens. A leaked secret allows token forgery for any role including superadmin.

**Remediation:** Migrate to RS256 or ES256. Store the private key in a secrets manager (AWS Secrets Manager, HashiCorp Vault). Distribute only the public key to verification paths. If remaining with HS256, enforce a minimum 64-byte cryptographically random secret: `secrets.token_hex(64)` at deployment time, validated at startup.

**Effort:** MEDIUM

---

### [AUTH-010 / AC-014 / ADM-015] OTP Timing Attack — Non-Constant-Time Hash Comparison

**Location:** `backend/app/services/otp_service.py:69`; `backend/app/core/security.py:42`

**HIPAA Violation:** §164.312(d) Authentication — authentication comparisons must be timing-attack resistant.

**Evidence:**
```python
# security.py:42
return hash_otp(plain_otp) == hashed_otp  # Standard Python string ==, not constant-time

# otp_service.py:69
if hash_otp(otp) != session.otp_hash:     # Same issue
```

**Remediation:** One-line fix:
```python
import hmac
# Replace:
return hash_otp(plain_otp) == hashed_otp
# With:
return hmac.compare_digest(hash_otp(plain_otp), hashed_otp)
```

Apply the same fix in `otp_service.py:69`.

**Effort:** LOW

---

### [AUTH-005 / ENC-005] HSTS Header Applied Only When ENVIRONMENT Variable Is Set — Silent Omission Risk

**Location:** `backend/app/main.py:56–57`

**HIPAA Violation:** §164.312(e)(2)(ii) Encryption in Transit.

**Evidence:**
```python
# main.py:56-57
if os.getenv("ENVIRONMENT") == "production":
    response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
# Missing: preload directive. If ENVIRONMENT is unset, HSTS is silently absent.
```

**Remediation:** Apply HSTS unconditionally at the reverse-proxy layer (nginx/caddy). In application code, add `preload` and apply regardless of environment variable state for defense in depth:
```python
response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains; preload"
```

**Effort:** LOW

---

### [AC-004 / API-008 / ADM-010] IDOR: Any Authenticated Parent Can Confirm Another Parent's Appointment

**Location:** `backend/app/routers/appointments.py:138–185`

**HIPAA Violation:** §164.312(a)(1) Access Control. §164.308(a)(3)(ii)(A) Authorization and/or Supervision.

**Evidence:**
```python
# appointments.py:149-155
# Only checks: user.is_parent() and appointment.appointment_type != "requested"
# Never checks: appointment.requesting_parent_phone == user.phone
```

**Remediation:**
```python
# After fetching the appointment:
if (appointment.requesting_parent_phone and
        appointment.requesting_parent_phone != user.phone):
    raise HTTPException(status_code=403, detail="Access denied")
```

**Effort:** LOW

---

### [AC-008] Patient Update Endpoint Allows Nurse to Modify Guardian Phone Number

**Location:** `backend/app/routers/patients.py:191–208`

**HIPAA Violation:** §164.308(a)(3)(ii)(A) Authorization and/or Supervision — nurses can modify PII used for access binding.

**Evidence:**
```python
# patients.py:194
user: CurrentUser = Depends(get_current_user)  # All roles allowed
# patients.py:197-199: only blocks parents, not nurses
# PatientUpdate schema includes parent_phone and parent_name
```

**Remediation:** Block nurses from modifying guardian binding fields:
```python
if user.is_nurse() and any(f in update_data for f in ["parent_phone", "parent_name"]):
    raise HTTPException(403, "Nurses cannot modify guardian binding fields")
```

**Effort:** LOW

---

### [AC-005 / ADM-006] Doctor Phone Numbers and Patient Names Exposed to All Authenticated Users

**Location:** `backend/app/routers/doctors.py:42–88,164–211`

**HIPAA Violation:** §164.502(a) Minimum Necessary Standard. §164.512 Minimum Necessary.

**Evidence:**
```python
# doctors.py:15: phone: str in DoctorResponse — returned to parents
# doctors.py:205: patient_name=appt.patient.name  — returned to any authenticated caller
```

**Remediation:** Remove `phone` from `DoctorResponse` for parent callers, or create a `DoctorPublicResponse` model without phone. Remove `patient_name` from the availability endpoint response entirely; return only `is_booked: bool` to non-staff callers.

**Effort:** MEDIUM

---

### [API-007] Mass Assignment: uploaded_by_role Is User-Controlled, Allowing Audit Record Forgery

**Location:** `backend/app/routers/documents.py:17`; `backend/app/schemas/medical_records.py:25`

**HIPAA Violation:** §164.312(b) Audit Controls — audit records for document uploads can be falsified.

**Evidence:**
```python
# documents.py:45
role = req.uploaded_by_role or ("parent" if user.is_parent() else "surgeon")
# A parent can send uploaded_by_role="surgeon" and it is used verbatim.
```

**Remediation:** Remove `uploaded_by_role` from all request schemas. Derive server-side:
```python
role = "parent" if user.is_parent() else "nurse" if user.is_nurse() else "surgeon"
```

**Effort:** LOW

---

### [API-009] AI Prompt Injection via Unvalidated Clinical Free-Text

**Location:** `backend/app/services/ai_service.py:38–42`; `backend/app/routers/ai.py:16–17`

**HIPAA Violation:** §164.312(b) Audit Controls — manipulated AI logs contain fabricated clinical data. §164.308(a)(1) Risk Management.

**Evidence:**
```python
# ai_service.py:38-42
prompt = PEDIATRIC_SURGERY_PROMPT.format(
    complaint=complaint,    # No max_length, no escaping, no sanitization
    examination=examination
)
```

**Remediation:** Add `max_length=2000` to `complaint` and `examination` fields. Escape curly braces before interpolation: `complaint.replace("{", "").replace("}", "")`. Pass clinical data as structured JSON in a separate user message rather than inline string interpolation.

**Effort:** MEDIUM

---

### [FINDING-005] No FLAG_SECURE on Any Activity — PHI Visible in App Switcher and Screenshots

**Location:** `androidApp/src/main/kotlin/com/example/nori_tura/MainActivity.kt`

**HIPAA Violation:** §164.312(a)(2)(i) Unique User Identification — PHI visible in the app switcher constitutes unauthorized disclosure.

**Evidence:** `FLAG_SECURE` appears in zero Kotlin source files. `MainActivity.kt` contains no `window.addFlags(...)` call. Patient names, diagnoses, and consent form content are captured in the Android recent apps thumbnail.

**Remediation:**
```kotlin
// MainActivity.kt:onCreate() — before setContent {}
window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
```

For iOS, add to `SceneDelegate`:
```swift
func sceneDidEnterBackground(_ scene: UIScene) {
    window?.isHidden = true
}
func sceneWillEnterForeground(_ scene: UIScene) {
    window?.isHidden = false
}
```

**Effort:** LOW

---

### [FINDING-009 / AUTH-013 / ENC-011] android:allowBackup=true Enables Unencrypted PHI Extraction via ADB

**Location:** `androidApp/src/main/AndroidManifest.xml:7`

**HIPAA Violation:** §164.312(a)(2)(iv) Encryption at Rest. §164.310(d)(1) Device and Media Controls.

**Evidence:**
```xml
<!-- AndroidManifest.xml:7 -->
android:allowBackup="true"
<!-- No android:fullBackupContent, no android:dataExtractionRules -->
```

On any developer-mode Android device, `adb backup -apk com.example.nori_tura` extracts the entire SharedPreferences directory containing the plaintext JWT token.

**Remediation:**
```xml
<!-- AndroidManifest.xml -->
android:allowBackup="false"
```

Or, if backup is required for other data, add `android:fullBackupContent="@xml/backup_rules"` pointing to a rules file that excludes SharedPreferences:
```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <exclude domain="sharedpref" path="." />
</full-backup-content>
```

**Effort:** LOW

---

### [FINDING-010] Push Notifications Contain Patient Name and PHI in Visible Notification Body

**Location:** `backend/app/jobs/reminders.py:66–74`

**HIPAA Violation:** §164.312(a)(2)(i) Unique User Identification — lock-screen notifications with patient names constitute unauthorized PHI disclosure.

**Evidence:**
```python
# reminders.py:66-74
push_body = f"Reminder: {patient.name} has a follow-up with Dr. {doctor.name} scheduled on {tomorrow.isoformat()}."
# patient.name and doctor.name are PHI. This appears on the lock screen.
```

**Remediation:** Replace with a data-only FCM message:
```python
push_title = "Medical Appointment Reminder"
push_body = "You have an upcoming appointment. Open the app to view details."
# Use FCM data message (no notification key) so the app handles display after auth.
```

**Effort:** LOW

---

### [AC-006 / ADM-006 AC-005-audit] Admin Privilege Actions Not Audited

**Location:** `backend/app/routers/admin.py:41–66`; `backend/app/routers/nurses.py:58–74`

**HIPAA Violation:** §164.308(a)(3)(ii)(C) Termination Procedures. §164.308(a)(4) Information Access Management.

**Evidence:**
```python
# admin.py:41-51
await prisma.doctors.update(where={"id": doctor_id}, data={"is_active": req.is_active})
# No audit log. No record of which admin changed which doctor's status.
```

**Remediation:** After each admin action write a structured audit entry:
```python
await prisma.admin_audit_logs.create(data={
    "admin_phone": user.phone,
    "action": "DEACTIVATE_DOCTOR",
    "target_type": "doctor",
    "target_id": doctor_id,
    "before_state": {"is_active": current_doctor.is_active},
    "after_state": {"is_active": req.is_active},
    "ip_address": request.client.host,
    "created_at": datetime.now(timezone.utc),
})
```

**Effort:** MEDIUM

---

### [AC-006-audit] PHI Written to Application Logs — Patient Names in Log Messages

**Location:** `backend/app/services/notification_service.py:11`; `backend/app/jobs/reminders.py:55–68`; `backend/app/services/sms_service.py:20`

**HIPAA Violation:** §164.312(b) Audit Controls — application logs containing PHI must be protected with equivalent safeguards.

**Evidence:**
```python
# notification_service.py:11
logger.info("[WhatsApp] to=%s msg=%s", phone, message)
# message contains patient name, doctor name, diagnosis

# sms_service.py:20
logger.info("[SMS stub] to=%s msg=%s", phone, message)
# message body may contain clinical details
```

**Remediation:** Replace PHI-containing log fields with non-identifying references:
```python
logger.info("[WhatsApp] record_id=%s channel=whatsapp status=%s", record_id, delivery_status)
logger.info("[SMS] record_id=%s status=%s", record_id, delivery_status)
# Never log patient names, phone numbers, diagnoses, or message bodies
```

**Effort:** MEDIUM

---

### [FINDING-006] No Biometric or PIN Re-Authentication on App Resume

**Location:** `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/auth/AuthViewModel.kt`; `androidApp/src/main/kotlin/com/example/nori_tura/MainActivity.kt`

**HIPAA Violation:** §164.312(d) Authentication — a single up-front OTP with no re-authentication on resume is insufficient in clinical environments with shared or unattended devices.

**Remediation:** Implement an inactivity timeout using `BiometricPrompt` on Android. Track `last_active_time` in `AuthViewModel`. On `onResume` (Android) or `sceneWillEnterForeground` (iOS), if elapsed time exceeds 5 minutes (configurable per role), prompt biometric re-authentication before navigating to PHI screens.

**Effort:** HIGH

---

### [FINDING-016] Consent PDF URLs Opened in System Browser — PHI Stored in Browser History

**Location:** `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt:173`; `shared/src/androidMain/kotlin/com/example/nori_tura/util/UrlLauncher.android.kt`

**HIPAA Violation:** §164.312(a)(2)(i) — PHI-containing URLs in browser history are accessible to other apps.

**Remediation:** Open consent PDFs in an in-app PDF viewer rather than firing `Intent.ACTION_VIEW`. Generate a time-limited signed Cloudinary URL server-side (per the ENC-006 remediation) returned only after authorization checks. Apply `FLAG_SECURE` (FINDING-005) to prevent screenshots of the in-app viewer.

**Effort:** HIGH (depends on ENC-006 fix for signed URLs)

---

### [ENC-009] Diagnosis Terms in HTTP GET Query Parameters — Logged by All Intermediaries

**Location:** `backend/app/routers/patients.py:73–74`

**HIPAA Violation:** §164.312(e)(2)(ii) Encryption in Transit — diagnosis terms in URL paths are logged by every network intermediary.

**Evidence:**
```python
# patients.py:73-74
search: Optional[str] = Query(None)
diagnosis: Optional[str] = Query(None)
# GET /patients?diagnosis=appendicitis — logged by nginx, CDN, WAF, load balancer
```

**Remediation:** Convert PHI-containing search parameters from GET query parameters to POST request bodies:
```python
# POST /patients/search
class PatientSearchRequest(BaseModel):
    search: Optional[str] = None
    diagnosis: Optional[str] = None
    page: int = 1
    limit: int = 50
```

**Effort:** MEDIUM

---

### [ENC-014 / UPLOAD-005] Consent PDF Hash Computed Over HTML, Not PDF Bytes — Integrity Verification Is Broken

**Location:** `backend/app/services/consent_service.py:162–170`; `backend/app/utils/pdf_integrity.py:39–43`

**HIPAA Violation:** §164.312(c)(1) Integrity Controls — the stored hash does not represent the actual stored artifact.

**Evidence:**
```python
# consent_service.py:162-170
def _compute_document_hash(html: str) -> str:
    return compute_sha256(html.encode("utf-8"))  # Hash of HTML, not PDF

# pdf_integrity.py:39-43
def verify_pdf_hash(pdf_bytes: bytes, expected_hash: str) -> bool:
    return compute_sha256(pdf_bytes) == expected_hash  # Always False by design
```

`verify_pdf_hash` is defined but never called anywhere in the codebase.

**Remediation:**
```python
# consent_service.py — after PDF generation
final_pdf_bytes = _render_pdf(html)
pdf_hash = compute_sha256(final_pdf_bytes)  # Hash of actual PDF bytes
# Store pdf_hash, not the HTML hash
```

Add a verification call in `GET /consent/forms/{id}`:
```python
downloaded_bytes = await download_from_cloudinary(consent.pdf_url)
if not verify_pdf_hash(downloaded_bytes, consent.pdf_hash):
    raise HTTPException(500, "Consent form integrity check failed")
```

**Effort:** LOW

---

### [API-012] No Pagination on PHI List Endpoints — Full Dataset Exportable in One Request

**Location:** `backend/app/routers/patients.py`, `documents.py`, `medical_records.py`, `ipd.py`, `appointments.py`, `admin.py`

**HIPAA Violation:** §164.512 Minimum Necessary — unlimited records returned per request violates minimum necessary access. §164.312(b) Audit Controls — bulk extraction is not detectable without anomaly detection.

**Remediation:** Add pagination to all list endpoints:
```python
@router.get("/")
async def list_patients(
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    ...
):
    patients = await prisma.patients.find_many(
        where=where,
        take=limit,
        skip=offset,
        ...
    )
    return {"data": patients, "limit": limit, "offset": offset}
```

**Effort:** MEDIUM

---

### [API-016] python-jose 3.3.0 with CVE-2024-33663 and CVE-2024-33664 in requirements.txt

**Location:** `backend/requirements.txt:8`

**HIPAA Violation:** §164.308(a)(1) Security Management Process — known vulnerabilities in installed dependencies must be remediated.

**Evidence:** `python-jose[cryptography]==3.3.0` — CVE-2024-33663 (algorithm confusion, ECDSA key treated as HMAC secret) and CVE-2024-33664 (alg:none bypass).

**Remediation:** Run `grep -r "from jose" backend/app/` to verify direct usage. If unused, remove from `requirements.txt`. Add `pip-audit` to CI pipeline: `pip-audit -r requirements.txt`. If a dependency requires `python-jose`, pin to a patched version or replace with `joserfc` or `PyJWT`.

**Effort:** LOW

---

### [ADM-007] No Patient Data Deletion Mechanism Despite Explicit Privacy Promise in Consent Text

**Location:** `backend/app/routers/consent.py:38–42`; entire `backend/app/routers/` directory

**HIPAA Violation:** §164.524 Access of Individuals to Protected Health Information. §164.530(j) Documentation.

**Evidence:**
```python
# consent.py:38-42 — the platform's own consent text states:
"You can request data deletion at any time by contacting your surgeon or administrator."
# grep -rn "router.delete" backend/app/routers/ — returns only surgical_templates.py:101
# No patient, OPD, IPD, consent, or document deletion endpoint exists.
```

**Remediation:** Implement a data deletion workflow: `DELETE /patients/{patient_id}/data` that anonymizes PHI fields (replaces name with `[DELETED]`, parent_phone with a hash, nullifies clinical text) subject to surgeon authorization. Document a retention schedule aligned with MoHFW guidelines (8–10 years for surgical records).

**Effort:** HIGH

---

## MEDIUM Findings

The following findings represent meaningful security gaps that should be remediated within the first quarter. They individually represent partial HIPAA violations or conditions that compound the severity of critical findings.

---

**[AUTH-011]** No structured authentication event audit log — all authentication events (OTP send, verify, success, failure, token issue) are handled with `print()` statements or silently. Required by §164.308(a)(5)(ii)(C). **Remediation:** Create an `auth_audit_log` table; emit structured log entries on all auth events using Python's `logging` module. **Effort:** MEDIUM

**[AUTH-012]** JWT secret validation only enforced in production — staging/dev environments accept the weak `.env` default `dev-secret-key-change-in-production`. **Remediation:** Apply minimum-length and default-value checks regardless of `ENVIRONMENT`. **Effort:** LOW

**[AUTH-014]** No concurrent session limit — multiple 30-day JWTs can coexist per user with no detection of simultaneous access from different devices. **Remediation:** Add a `sessions` table tracking JTI per user; enforce configurable maximum concurrent sessions per role. **Effort:** HIGH

**[ENC-012]** Hardcoded weak default JWT secret `"your-secret-key-change-in-production"` in `config.py:9`. Pydantic v2 field ordering may prevent validator from checking it correctly. **Remediation:** Remove the default value entirely; make `JWT_SECRET` required with no default; validate entropy at startup. **Effort:** LOW

**[ENC-013]** OTP plaintext logged to stdout: `print(f"[DEV OTP] Phone: {phone}, OTP: {otp}")` in `otp_service.py:30` executes in any environment where `TWO_FACTOR_API_KEY` is not configured. **Remediation:** Remove the print statement; replace with masked logging. **Effort:** LOW

**[ENC-015 / UPLOAD-012]** QR codes on consent PDFs embed raw `patient_id` and `consent_id` database UUIDs in plaintext JSON. Consent PDFs are printed and physically handled by non-system parties. **Remediation:** Sign the QR payload with HMAC: `hmac.new(secret, f"{consent_id}:{patient_id}".encode(), sha256).hexdigest()[:16]`. **Effort:** LOW

**[ENC-016]** No certificate pinning in the Ktor HTTP client on Android (OkHttp) or iOS (Darwin). The app is vulnerable to MITM attacks via enterprise proxies or user-installed CA certificates. **Remediation:** Add `CertificatePinner` to the OkHttp engine block in `ApiClient.kt`. Implement pin rotation procedure before expiry. **Effort:** MEDIUM

**[ENC-017]** Raw Python exception strings returned in HTTP responses — `detail=str(e)` in `auth.py:122`, `consent.py:136`, and `sms_service.py:42`. These can expose database schema details, table names, and constraint names. **Remediation:** Replace with generic error messages; log the full exception server-side with a correlation ID. **Effort:** LOW

**[ENC-020]** Database connection URL in `.env` lacks `sslmode=require` or `sslmode=verify-full` — cloud deployments where app server and database are on separate hosts transmit all PHI queries over unencrypted TCP. **Remediation:** Append `?sslmode=verify-full&sslrootcert=/path/to/ca.crt` to `DATABASE_URL` in all non-localhost deployments. **Effort:** MEDIUM

**[AC-010]** Parent can submit AI diagnosis requests, generating OPD records and AI logs attributed to clinical staff without surgeon or nurse involvement. **Remediation:** Change dependency to `get_current_nurse_or_surgeon`. **Effort:** LOW

**[AC-012]** Admin role access to doctor-specific stats endpoint (`GET /doctors/me/stats`) returns HTTP 400 rather than 403 because `user.doctor_id` is `None` for admins — inconsistent access control design. **Remediation:** Remove admin/superadmin from the allowed roles; restrict to surgeon only. **Effort:** LOW

**[AC-015]** Admin data access policy is not formally defined — admin PHI access is accidentally blocked by `resolve_doctor_id` raising 403, not by explicit policy. If future routes bypass `resolve_doctor_id`, admins gain unrestricted PHI access. **Remediation:** Explicitly document and enforce admin access scope in a middleware layer or capability matrix. **Effort:** HIGH

**[API-014]** No input length constraints on free-text PHI fields — `complaint`, `examination`, `diagnosis`, `findings`, `technique` — enabling storage abuse and response timeout conditions. **Remediation:** Apply `Field(max_length=5000)` to narrative fields, `Field(max_length=500)` to shorter fields throughout `opd.py`, `ipd.py`, and `ai.py`. **Effort:** LOW

**[API-015]** Jinja2 consent form templates loaded without `autoescape=True`. Patient-controlled data (name, diagnosis, allergies) rendered unescaped into HTML, creating stored XSS risk if HTML is ever served directly and breaking the integrity of the PDF if input contains HTML tags. **Remediation:** Use `Environment(loader=FileSystemLoader(_TEMPLATES_DIR), autoescape=True)` instead of bare `Template()`. **Effort:** LOW

**[API-017]** Appointment confirmation accepts `patient_data: Optional[dict]` — untyped dict bypasses the validated `PatientCreate` schema constraints, creating patient records without validation. **Remediation:** Replace with a typed nested Pydantic model with the same field constraints as `PatientCreate`. **Effort:** LOW

**[AC-008-audit / AC-011-audit]** PHI modifications (patient updates, IPD status changes, intra-op notes) are not audited with before/after state. **Remediation:** Read the current state before each update; write a `change_audit` entry with field-level diff. **Effort:** HIGH

**[AC-012-audit]** Automated reminder job sends PHI-containing messages but does not write to `whatsapp_logs` — sends are invisible in the audit trail. **Remediation:** Write a `whatsapp_logs` record for each automated send with `trigger_type="automated_reminder"` and a `job_run_id`. **Effort:** LOW

**[AC-013-audit]** No logging of file/document access — Cloudinary URLs are issued without any backend tracking of who accessed which medical file and when. **Remediation:** Create a proxy download endpoint that logs access before generating a signed Cloudinary URL redirect. **Effort:** HIGH

**[AC-014-audit]** No monitoring or alerting for suspicious PHI access patterns — bulk downloads, off-hours access, and anomalously high query volumes are undetectable. **Remediation:** Implement structured request logging middleware shipping to a SIEM with alerting rules: >50 distinct patient records in 1 hour by one user, access between 11PM–5AM by clinical staff. **Effort:** HIGH

**[FINDING-013]** OTP input field is not masked — `PasswordVisualTransformation()` is missing from the `OutlinedTextField` in `VerifyOtpScreen.kt:57–65`. OTP digits are displayed in plaintext, vulnerable to shoulder surfing in clinical environments. **Remediation:** Add `visualTransformation = PasswordVisualTransformation()` and `keyboardType = KeyboardType.NumberPassword`. **Effort:** LOW

**[FINDING-014]** JWT secret validation only enforced in production mode; `.env` ships `dev-secret-key-change-in-production`, a low-entropy guessable value. Staging environments handling real patient data are vulnerable. **Remediation:** Enforce entropy check in all environments; generate a random 64-byte secret at deployment; use a secrets manager. **Effort:** MEDIUM

**[ADM-011]** Consent text is hardcoded in the Android app — `ConsentAcknowledgmentDialog.kt:16–39` uses a local `CONSENT_TEXT` constant and never calls `GET /consent/latest`. Users will not see updated consent terms without an app update. **Remediation:** Fetch consent text from `GET /consent/latest` on every app launch; force re-acknowledgment if version differs from last-acknowledged. **Effort:** MEDIUM

**[ADM-012]** No age validation to enforce pediatric scope — `PatientCreate` accepts `age: int = Field(..., ge=0, le=150)` without confirming minors; adult patients receive guardian-consent forms without warning. **Remediation:** Add a business-rule warning or block for `age >= 18` on a pediatric platform; use age-appropriate consent language. **Effort:** MEDIUM

**[ADM-013]** No breach notification mechanism — no failed authentication monitoring, no anomaly detection, no security incident procedure, and no alerting infrastructure. **Remediation:** Implement authentication attempt logging (per AC-002-audit); integrate with a SIEM; document an incident response procedure covering the 60-day HIPAA breach notification window. **Effort:** HIGH

**[AC-008-logretention]** No 6-year audit log retention policy — application logs use default cloud retention (30–90 days), far below the §164.530(j) requirement. **Remediation:** Configure S3/GCS lifecycle policies to 6+ years for all audit log outputs; add `AUDIT_LOG_RETENTION_YEARS: int = 6` to config; implement periodic archiving of database audit tables. **Effort:** MEDIUM

**[AC-009-audit]** Consent acknowledgment records `client_ip` from the request body rather than server-extracted IP — all recorded IPs are attacker-controlled (addressed in CRITICAL remediation; also requires schema migration). **Effort:** LOW

**[AC-015-consent]** Consent form signing does not capture the authenticated signer's identity in the database — `signed_by_user_id`, `signing_ip_address` fields do not exist on `consent_forms`. **Remediation:** Add these fields; populate from the authenticated request context at signing time. **Effort:** MEDIUM

**[UPLOAD-011]** No EXIF or metadata stripping from uploaded medical images — X-rays and CT scans may embed patient name, DOB, and institution in EXIF metadata. **Remediation:** Add `transformation=[{"strip_exif": True}]` to all image uploads in `cloudinary_service.py`. For DICOM inputs, run `pydicom` de-identification server-side before upload. **Effort:** MEDIUM

**[UPLOAD-010]** Parent signature stored as a raw base64 data URI in a database text column rather than uploaded to Cloudinary as an image. No size validation, no content type check, and the data URI is rendered directly into the PDF HTML template. **Remediation:** Upload signature bytes to Cloudinary; store the Cloudinary URL; validate input is a properly formed data URI with PNG or JPEG prefix and a maximum size. **Effort:** MEDIUM

---

## LOW / INFO Findings (Brief List)

**[AUTH-015]** OTP SMS delivery failure is silently ignored — `send_otp_sms()` return value is not checked; users see "OTP sent successfully" when the SMS was never delivered. Fix: check return value; return HTTP 503 on delivery failure. **Effort:** LOW

**[AUTH-016]** No multi-factor step-up authentication for high-risk PHI operations (consent form generation, discharge, patient record deletion). Consider requiring OTP re-verification for consent signing. **Effort:** HIGH

**[AUTH-017]** JWT tokens do not include `iat` (issued-at) or `jti` claims — required for the revocation mechanism and for reconstructing authentication timelines. Add `"iat": datetime.now(timezone.utc)` and `"jti": secrets.token_hex(16)` in `create_access_token()`. **Effort:** LOW

**[ENC-019]** Android release build has `isMinifyEnabled = false` — the production APK is trivially reversible, exposing API endpoint structure and hardcoded development artifacts. Set `isMinifyEnabled = true` and add ProGuard rules. **Effort:** LOW

**[UPLOAD-016]** Cloudinary stub mode returns non-functional stub URLs stored in production database, silently discarding the actual file. Raise an explicit error when Cloudinary is unconfigured in non-development environments. **Effort:** LOW

**[API-018]** `opd.py:257` calls `get_settings()` which is never imported — a `NameError` crash on the follow-up preview endpoint in production. Add `from app.core.config import get_settings` to `opd.py` imports. **Effort:** LOW

**[API-019]** OTP hashed with unsalted SHA-256 — the entire 10^6 OTP space can be precomputed in milliseconds as a rainbow table. Add a random per-session salt stored alongside the hash. **Effort:** LOW

**[API-020]** Admin trigger-follow-up-reminders endpoint has no cooldown — repeated rapid invocations spam patients and may exhaust SMS/WhatsApp API quotas. Add a 1-hour cooldown check using a config/state table. **Effort:** MEDIUM

**[AC-016]** Consent endpoints have no rate limiting — `/consent/acknowledge` can be called in bulk to flood the audit table with fake records. Add IP-based rate limiting of 10 requests/hour. **Effort:** LOW

**[AC-017]** Nurses can add intra-operative media and videos to surgical admissions — these are medico-legal records that should require surgeon-level write access. Change dependency to `get_current_surgeon`. **Effort:** LOW

**[AC-018]** Surgeon stats endpoint incorrectly allows admin/superadmin roles, producing HTTP 400 (not 403) because `user.doctor_id` is None for those roles. Restrict to surgeon only. **Effort:** LOW

**[ADM-015]** OTP comparison uses non-constant-time Python `==` for SHA-256 hash comparison. Replace with `hmac.compare_digest()`. (Duplicated in HIGH section as AUTH-010 — apply the fix once.) **Effort:** LOW

**[ADM-016]** Default JWT secret `"your-secret-key-change-in-production"` in `config.py:6` is a well-known public placeholder present in thousands of public repositories. Make the field required with no default. **Effort:** LOW

**[ADM-017 / INFO]** No Business Associate Agreements documented for WhatsApp (Meta), Firebase (Google), or SMS providers (2Factor.in/MSG91). The WhatsApp follow-up message body includes patient name, doctor name, diagnosis, and medication names. Execute BAAs with all providers before sending PHI through these channels. **Effort:** MEDIUM

**[FINDING-017]** OTP transmitted in GET URL path to 2Factor.in API: `https://2factor.in/API/V1/{key}/SMS/{phone}/{otp}/...` — the OTP appears in the 2Factor.in server access logs. Switch to a POST-based API if the provider supports it. **Effort:** LOW

---

## HIPAA Compliance Gap Analysis

| HIPAA Safeguard | Requirement | Status | Evidence |
|---|---|---|---|
| **§164.308(a)(1) — Security Management Process** | Risk analysis and management | **MISSING** | No documented risk assessment; no risk register; no formal security management program |
| **§164.308(a)(1)(ii)(D) — Information System Activity Review** | Regular review of audit logs | **MISSING** | Zero audit logs exist; no review process possible |
| **§164.308(a)(3) — Workforce Access Management** | Access based on job function | **PARTIAL** | RBAC architecture exists; admin data access policy is accidental (AC-015); nurse guardian field modification is unblocked (AC-008) |
| **§164.308(a)(3)(ii)(C) — Termination Procedures** | Immediate access revocation on termination | **MISSING** | No token revocation; deactivated surgeons retain 30-day API access; logout is client-side only |
| **§164.308(a)(4) — Information Access Management** | Minimum necessary access | **PARTIAL** | Doctor phone numbers exposed to parents; patient names in availability endpoint; no pagination enforcing minimum necessary |
| **§164.308(a)(5)(ii)(B) — Protection from Malicious Software** | Malware protection procedures | **MISSING** | No file type validation on upload endpoint; no malware scanning |
| **§164.308(a)(5)(ii)(C) — Log-in Monitoring** | Procedures for monitoring login attempts | **MISSING** | No failed authentication logging; no brute-force detection; no lockout |
| **§164.308(a)(5)(ii)(D) — Password Management** | Procedures for creating/managing authentication credentials | **PARTIAL** | OTP hashing present; but OTP returned in response; weak default JWT secret; 30-day expiry override |
| **§164.308(a)(6)(i) — Security Incident Procedures** | Policies and procedures for security incidents | **MISSING** | No incident response plan; no breach detection capability; no alerting |
| **§164.308(a)(6)(ii) — Response and Reporting** | Identifying and responding to security incidents | **MISSING** | Dependent on audit logs that do not exist |
| **§164.308(b)(1) — Business Associate Contracts** | BAAs required before PHI disclosure to BAs | **MISSING** | No BAAs documented for Cloudinary, Firebase, WhatsApp/Meta, OpenAI/Anthropic, or SMS providers |
| **§164.310(d)(1) — Device and Media Controls** | Policies for hardware and electronic media containing PHI | **PARTIAL** | No encryption on mobile credential storage; allowBackup=true; no device management policy |
| **§164.312(a)(1) — Access Control** | Unique user access based on role | **PARTIAL** | RBAC present; IDOR on appointments; mass assignment on uploaded_by_role; no session registry |
| **§164.312(a)(2)(i) — Unique User Identification** | Assign unique identifier for each user | **PARTIAL** | Phone-based identification exists; but forged consent records can impersonate any phone |
| **§164.312(a)(2)(ii) — Emergency Access Procedure** | Procedure for emergency access to ePHI | **MISSING** | No emergency access provision; DoS via upload endpoint could block all access |
| **§164.312(a)(2)(iii) — Automatic Logoff** | Terminate session after inactivity | **MISSING** | 30-day JWT; no server-side session termination; no inactivity timeout on mobile app |
| **§164.312(a)(2)(iv) — Encryption and Decryption** | Encrypt and decrypt ePHI | **MISSING** | No encryption at rest for any PHI fields; cleartext HTTP on mobile platforms; JWT in unencrypted SharedPreferences |
| **§164.312(b) — Audit Controls** | Record and examine PHI access activity | **MISSING** | Zero structured audit logs; total absence across all routers |
| **§164.312(c)(1) — Integrity Controls** | Corroborate that ePHI has not been altered | **PARTIAL** | PDF hash exists but is computed over HTML not PDF bytes; no EXIF stripping; no file type validation |
| **§164.312(d) — Person or Entity Authentication** | Verify entity seeking access | **CRITICAL FAILURE** | OTP returned in response (complete bypass); no brute-force protection; symmetric HS256 single-point-of-failure |
| **§164.312(e)(1) — Transmission Security** | Guard against unauthorized access to ePHI in transit | **CRITICAL FAILURE** | Global cleartext HTTP on Android; NSAllowsArbitraryLoads=true on iOS; no certificate pinning |
| **§164.312(e)(2)(i) — Integrity Controls for Transmission** | Corroborate ePHI not altered in transit | **MISSING** | No message integrity mechanism on API responses |
| **§164.312(e)(2)(ii) — Encryption in Transit** | Encrypt ePHI in transit | **CRITICAL FAILURE** | HTTP base URLs; usesCleartextTraffic=true; Cloudinary public URLs |
| **§164.502(a) — Minimum Necessary** | Limit PHI to minimum necessary | **PARTIAL** | Some role-based filtering; no pagination; doctor phone numbers to parents; patient names in availability |
| **§164.508 — Uses and Disclosures (Consent/Authorization)** | Valid authorization before PHI use | **PARTIAL** | Consent form pipeline exists; but consent enforcement is UI-only; AI disclosure lacks BAA and consent |
| **§164.514(b) — De-identification** | PHI de-identified before third-party disclosure | **MISSING** | Clinical text sent to AI providers without scrubbing |
| **§164.520 — Notice of Privacy Practices** | Notice must reflect current practices | **PARTIAL** | Consent text exists; hardcoded in app; not updated server-side; no re-acknowledgment on version change |
| **§164.524 — Individual Right of Access** | Individuals may access their PHI | **PARTIAL** | Parent can view own records; no data export or deletion mechanism despite stated promise |
| **§164.530(j) — Documentation Retention** | Retain documentation for 6 years | **MISSING** | No retention policy; no archiving; default cloud log retention is 30–90 days |

---

## Remediation Roadmap

### Phase 1 — Immediate (Week 1): Stop Active Authentication Bypass and Critical Transmission Failures

These changes prevent active exploitation. They are low-to-medium effort and provide the highest immediate risk reduction.

1. **Remove `dev_otp` from all API responses** — `backend/app/routers/auth.py:73`. Gate on `settings.ENVIRONMENT == "development"` or remove entirely. Remove `print()` from `otp_service.py:30`. (1 hour)
2. **Remove the 30-day JWT expiry override** — delete `expires_delta=timedelta(days=30)` from `backend/app/routers/auth.py:161`. (5 minutes)
3. **Fix the nurse `is_active` check at token issuance** — `auth.py:149` add `"is_active": True` to the nurse query. (5 minutes)
4. **Fix the surgeon active-status check per request** — `auth_deps.py:71` add DB lookup before returning `doctor_id`. (30 minutes)
5. **Remove cleartext HTTP from Android manifest** — set `android:usesCleartextTraffic="false"` and create `network_security_config.xml`. (2 hours)
6. **Remove NSAllowsArbitraryLoads from iOS Info.plist** — replace with scoped `NSExceptionDomains` for localhost only. (1 hour)
7. **Add FLAG_SECURE to MainActivity** — one line: `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)`. (10 minutes)
8. **Disable android:allowBackup** — set to `"false"` in `AndroidManifest.xml`. (5 minutes)
9. **Add authentication to the consent acknowledgment endpoint** — add `Depends(get_current_user)` and extract real server-side IP. (2 hours)
10. **Replace non-constant-time OTP comparison** — use `hmac.compare_digest()` in `security.py:42` and `otp_service.py:69`. (15 minutes)
11. **Fix OPD router NameError** — add `from app.core.config import get_settings` to `opd.py` imports. (5 minutes)
12. **Replace raw exception strings in HTTP responses** — `detail="Internal server error"` in `consent.py:136`, `auth.py:122`, `sms_service.py:42`. (1 hour)
13. **Gate Cloudinary stub mode** — raise explicit error in non-development environments when Cloudinary is unconfigured. (30 minutes)
14. **Remove python-jose from requirements.txt** if unused by application code. (30 minutes)
15. **Push notification PHI removal** — replace patient name in `reminders.py:66–74` notification body with generic text. (30 minutes)

**Total estimated Phase 1 effort: 2–3 engineering days**

---

### Phase 2 — Short Term (Month 1): Core HIPAA Technical Safeguards

These changes address the most significant remaining HIPAA gaps after the critical bypasses are closed.

1. **Implement OTP brute-force protection** — add `attempt_count`, `locked_at` columns to `otp_sessions`; enforce lockout after 5 failures; add `slowapi` rate limiting to authentication endpoints.
2. **Implement PHI access audit logging** — create `audit_logs` Prisma model; implement `AuditLogMiddleware`; register in `main.py`. Begin logging all PHI endpoint access (patients, OPD, IPD, consent, documents, medical records).
3. **Implement JWT revocation mechanism** — add `jti` claim to all issued tokens; create `revoked_tokens` table; implement `POST /auth/logout` endpoint; check revocation in `decode_token()`.
4. **Switch mobile token storage to encrypted storage** — migrate `multiplatform-settings-no-arg` to `multiplatform-settings-secure`; use `EncryptedSharedPreferences` on Android (Android Keystore) and iOS Keychain on iOS.
5. **Implement Cloudinary authenticated delivery** — change all uploads to `type="authenticated"`; generate signed time-limited URLs at access time; store `public_id` rather than permanent URL.
6. **Add admin action audit logging** — write structured audit entries for all doctor activation/deactivation, admin creation, and nurse deactivation actions.
7. **Remove PHI from application logs** — replace all `logger.info()` calls containing patient names, phone numbers, or message bodies with non-identifying references.
8. **Add IDOR check to appointment confirmation** — check `appointment.requesting_parent_phone == user.phone` before processing confirmation.
9. **Add file type validation and size limits to upload endpoint** — use `python-magic`; restrict to allowlisted MIME types; enforce 20MB per file limit; allowlist `folder` parameter values.
10. **Fix consent PDF hash computation** — compute SHA-256 over PDF bytes (not HTML); call `verify_pdf_hash()` in consent form retrieval endpoint.
11. **Remove `uploaded_by_role` from all request schemas** — derive server-side from authenticated user's role.
12. **Add input length constraints to all free-text PHI fields** — `Field(max_length=5000)` on narrative fields throughout `opd.py`, `ipd.py`, `ai.py`.
13. **Implement Jinja2 autoescape for consent templates** — use `Environment(autoescape=True)` in `consent_service.py`.
14. **Restrict AI endpoint to clinical staff** — change to `Depends(get_current_nurse_or_surgeon)`; add PHI scrubbing before AI prompt construction.
15. **Add pagination to all PHI list endpoints** — `take=limit, skip=offset` on all `find_many()` calls; maximum 200 records per page.
16. **Fix HSTS header** — apply unconditionally with `preload` directive; enforce at reverse-proxy layer.
17. **Add server-side consent enforcement** — implement `require_consent` dependency; apply to all PHI-access routes; fix Android client to call `POST /consent/acknowledge` on acknowledgment.
18. **Document and apply OTP salting** — add per-session salt to `otp_sessions` table; update `hash_otp()` to use HMAC-SHA256 with salt.

**Total estimated Phase 2 effort: 3–5 engineering weeks**

---

### Phase 3 — Medium Term (Quarter 1): Architectural Security Hardening and Compliance Infrastructure

These changes complete the HIPAA compliance posture, address systemic architectural risks, and establish the ongoing compliance infrastructure.

1. **Migrate JWT signing to RS256 or ES256** — generate RSA/ECDSA key pair; store private key in secrets manager (AWS Secrets Manager, HashiCorp Vault); distribute public key to verification paths; rotate annually.
2. **Implement field-level encryption for highest-sensitivity PHI columns** — encrypt `complaint`, `examination`, `diagnosis`, `allergies`, `parent_phone`, `consent_text`, `content_json` using Fernet/AES-256-GCM at application layer; add `PHI_ENCRYPTION_KEY` to secrets management.
3. **Add database connection SSL enforcement** — add `sslmode=verify-full` to production `DATABASE_URL`; configure PostgreSQL to require SSL; add startup validator.
4. **Implement tamper-proof audit log storage** — create separate audit DB role with INSERT-only permissions; route audit writes to append-only S3 bucket with Object Lock (Compliance mode, 6-year retention); implement archiving job for audit tables older than 90 days.
5. **Implement concurrent session management** — create `sessions` table tracking JTI per user; enforce maximum concurrent session counts per role; detect anomalous concurrent access.
6. **Implement biometric re-authentication on mobile** — `BiometricPrompt` on Android; `LocalAuthentication` on iOS; configurable inactivity timeout (5 min clinical, 15 min parent); trigger on `onResume`/`sceneWillEnterForeground`.
7. **Implement certificate pinning** — add `CertificatePinner` to OkHttp engine configuration in `ApiClient.kt`; configure iOS `URLSession` delegate; document pin rotation procedure.
8. **Implement monitoring and alerting infrastructure** — integrate structured request logs with ELK stack or Datadog; define alerting rules (>50 patient records/hour, off-hours access, >10 failed auth/5min); create weekly access review report.
9. **Implement patient data deletion workflow** — `DELETE /patients/{patient_id}/data` with surgeon authorization; PHI field anonymization; 6-year surgical record retention schedule per MoHFW guidelines.
10. **Enable Android code obfuscation** — `isMinifyEnabled = true` and `isShrinkResources = true` in release build; add ProGuard rules for Kotlin serialization, Ktor, and Compose.
11. **Sign QR codes on consent PDFs** — replace raw database IDs with HMAC-signed opaque tokens in QR payloads; verification endpoint returns only status, not PHI.
12. **Implement EXIF stripping for medical image uploads** — add `transformation=[{"strip_exif": True}]` to all image uploads; implement DICOM de-identification for DICOM file types.
13. **Move diagnosis search to POST request body** — convert all PHI-containing GET query parameters to POST endpoints.
14. **Implement consent form proxy downloader** — all PDF/image access through backend endpoint generating signed Cloudinary URLs; log access; open in-app PDF viewer rather than system browser.
15. **Restrict AI endpoint with de-identification and BAA** — PHI scrubbing pipeline; BAAs with OpenAI and Anthropic; parent AI consent item in platform consent text.
16. **Implement data retention archiving** — configure 6-year retention for all audit logs, access logs, and consent records; document formal retention policy.
17. **Implement log retention policy** — ship uvicorn/application logs to durable store (S3 with 6-year lifecycle policy); configure log rotation; add `AUDIT_LOG_RETENTION_YEARS: int = 6` to config.
18. **Implement refresh token rotation** — short-lived access tokens per `ROLE_EXPIRATION_HOURS`; 7-day rolling refresh tokens stored server-side; single-use enforcement; revocable on logout.

**Total estimated Phase 3 effort: 2–3 engineering months**

---

## Third-Party Risk Assessment

### Cloudinary

**Role:** Storage and delivery of all PHI files — X-rays, CT scans, MRI images, surgical photography, signed consent PDFs, prescription images.

**Current Risk Level:** CRITICAL

**Issues:**
- All PHI files are stored and served as publicly accessible URLs with no access control, no URL expiry, and no authentication requirement.
- No Business Associate Agreement documented or referenced anywhere in the codebase.
- Medical images may retain EXIF metadata containing patient identifiers.
- The Cloudinary stub mode silently stores non-functional URLs in the production database when credentials are missing.

**Required Actions:**
1. Execute a Business Associate Agreement with Cloudinary (available through Cloudinary's enterprise program) before any production PHI is stored.
2. Migrate all uploads to `type="authenticated"` with signed URL delivery.
3. Enable Cloudinary's malware scanning add-on.
4. Add `transformation=[{"strip_exif": True}]` to all image upload calls.
5. Implement a backend proxy endpoint for all PHI file access.
6. Consider whether US-based CDN storage is appropriate for Indian healthcare data under MoHFW digital health guidelines.

---

### Firebase (Google Firebase Cloud Messaging)

**Role:** Push notifications to parents and clinical staff containing appointment reminders.

**Current Risk Level:** HIGH

**Issues:**
- Push notification bodies contain patient names and doctor names in plaintext, appearing on device lock screens.
- No BAA documented with Google/Firebase.
- Firebase credentials stored in `firebase_credentials.json` — verify this file is excluded from source control.

**Required Actions:**
1. Execute a BAA with Google (Google Cloud's BAA program covers Firebase).
2. Replace PHI-containing notification bodies with generic text; use data-only FCM messages.
3. Verify `firebase_credentials.json` is in `.gitignore` and not committed to any repository.

---

### WhatsApp Business API (Meta)

**Role:** Outbound clinical follow-up reminders to parents containing patient name, doctor name, diagnosis, and medication details.

**Current Risk Level:** HIGH

**Issues:**
- No BAA documented with Meta for WhatsApp Business API.
- The WhatsApp follow-up message body constructed in `opd.py:_build_follow_up_message()` includes diagnosis and medication details — this constitutes a PHI disclosure to Meta.
- WhatsApp message template parameters include `patient_name` and `doctor_name`.

**Required Actions:**
1. Evaluate whether Meta's WhatsApp Business API offers a BAA for healthcare providers. If not, this channel cannot legally transmit PHI and must be restricted to de-identified appointment reminders (date/time only).
2. Remove diagnosis and medication details from all message body templates.
3. Restrict message content to: "You have an upcoming appointment. Open the Noritura app to view details."

---

### AI Providers (OpenAI and Anthropic)

**Role:** Clinical decision support — differential diagnosis suggestions based on complaint and examination free-text submitted by clinicians.

**Current Risk Level:** CRITICAL

**Issues:**
- Clinical free-text fields (complaint, examination) routinely contain patient identifiers when entered by clinicians.
- No de-identification step before transmission.
- No BAA documented with OpenAI or Anthropic.
- Parents can currently trigger AI diagnosis, sending arbitrary free-text to external providers.
- No input length limits prevent large-scale data exfiltration via the AI endpoint.

**Required Actions:**
1. Pause the AI diagnosis feature for production use until BAAs are executed with OpenAI and Anthropic (both offer BAA programs for healthcare customers) and the PHI scrubbing pipeline is implemented.
2. Restrict the AI endpoint to clinical staff only.
3. Implement PHI scrubbing before prompt construction.
4. Add explicit consent for AI-assisted clinical decision support in the platform consent text.
5. Consider on-premise or private cloud AI deployment (e.g., Azure OpenAI with a BAA) to eliminate the third-party disclosure risk entirely.

---

### 2Factor.in / MSG91 (SMS Provider)

**Role:** OTP delivery via SMS.

**Current Risk Level:** MEDIUM

**Issues:**
- OTP is transmitted as a URL path parameter in a GET request to the 2Factor.in API: the OTP appears in 2Factor.in's server access logs.
- No BAA documented — phone numbers are PHI (patient demographic data) under HIPAA.
- No validation of `send_otp_sms()` return value — failed deliveries are silently treated as successes.

**Required Actions:**
1. Evaluate whether 2Factor.in offers a POST-based API; if so, migrate to avoid OTP in URL paths.
2. Verify whether 2Factor.in or MSG91 will execute a BAA; if not, evaluate an alternative SMS provider with BAA capability.
3. Fix `send_otp_sms()` return value handling — surface delivery failures to the user.

---

## Recommendations for HIPAA Certification Readiness

The Noritura platform has demonstrated genuine architectural intent toward security — role-based access control is consistently structured, consent form generation is technically sophisticated, OTPs are hashed rather than stored in plaintext, and security header middleware is correctly applied. However, the gap between intent and implementation is severe enough that no reasonable certification pathway is available without completing at minimum Phase 1 and Phase 2 of the remediation roadmap.

The following recommendations are provided to establish the structural foundation for HIPAA compliance:

**1. Designate a HIPAA Security Officer.** Under §164.308(a)(2), a covered entity must designate a security official responsible for developing and implementing security policies. This individual should own the remediation roadmap and conduct quarterly risk assessments.

**2. Execute a formal Risk Analysis before production launch.** §164.308(a)(1)(ii)(A) requires a thorough assessment of potential risks to ePHI confidentiality, integrity, and availability. The findings in this report constitute the technical input to that risk analysis; it must be documented, signed, and retained.

**3. Execute Business Associate Agreements with all third-party processors before handling any real patient data.** This includes Cloudinary, Firebase, any AI provider used in production, the SMS provider, and WhatsApp Business API. BAA execution is a precondition for legal operation, not a post-launch activity.

**4. Establish an append-only audit log infrastructure before ingesting real patient data.** The complete absence of PHI access logging is the single most consequential gap identified in this audit. Without audit logs, the platform cannot detect a breach, respond to an OCR investigation, or demonstrate compliance. The audit infrastructure should be established in Phase 2 and must be operational from the first production patient record.

**5. Conduct a workforce training program covering HIPAA requirements.** §164.308(a)(5) requires security awareness training for all workforce members. Developers, clinical staff, and administrators must be trained on PHI handling, authentication policies, and incident reporting procedures before they interact with production data.

**6. Implement a formal incident response plan.** §164.308(a)(6) requires documented policies for identifying, responding to, and reporting security incidents. The 60-day breach notification window under §164.400 requires that breach detection infrastructure (audit logs, monitoring, alerting) exist before any production PHI is handled.

**7. Perform a penetration test against the remediated codebase before production launch.** Given the number of authentication bypass conditions identified in this audit, an independent penetration test should be conducted after Phase 2 remediation is complete and before any real patient data is processed. Specifically test OTP brute force protection, IDOR conditions, and the audit log integrity mechanisms.

**8. Establish a vulnerability management program.** Add `pip-audit` and `npm audit` (or equivalent) to the CI/CD pipeline. Configure automated dependency scanning to alert on newly disclosed CVEs in installed libraries. Review and patch within 30 days of disclosure for high/critical CVEs affecting authentication or PHI processing libraries.

**9. Configure database-level encryption before production deployment.** Enable encryption-at-rest at the PostgreSQL managed service level (AWS RDS, GCP Cloud SQL) as an interim measure while field-level encryption is implemented in Phase 3. This provides a defense-in-depth layer against database snapshot exposure.

**10. Establish a formal data retention schedule.** Document retention periods for all data categories: 6 years for audit logs and consent records (§164.530(j)), 8–10 years for surgical records (MoHFW guidance), and shorter periods for OTP sessions (immediate deletion on use), revoked tokens (delete on JWT natural expiry), and temporary files. Implement archiving workflows accordingly.

**11. Address the Indian regulatory dimension.** The platform handles pediatric health data in India. In addition to HIPAA (applicable if any US-based covered entity is involved), review compliance requirements under the Digital Personal Data Protection Act 2023 (DPDPA), MoHFW's Electronic Health Record Standards, and the National Health Authority's Health Data Management Policy. These frameworks have specific requirements for consent, data localization, and breach notification that may impose stricter timelines and controls than HIPAA alone.

**12. Implement a formal code review process specifically for security-sensitive code paths.** Several of the CRITICAL findings in this report — the 30-day JWT override, the unconditional OTP leak — are single-line issues that would have been caught by a security-focused code review. Establish mandatory peer review for all changes to authentication endpoints, PHI-handling routers, and cryptographic functions.

---

**Report prepared by:** Lead HIPAA Security Officer
**Date:** 2026-07-14
**Next Review Date:** 2026-10-14 (Quarterly) or upon completion of Phase 2 remediation

**Distribution:** Engineering Lead, Clinical Operations Lead, Legal Counsel, HIPAA Security Officer
**Retention:** This document must be retained for a minimum of 6 years from creation date per §164.530(j).