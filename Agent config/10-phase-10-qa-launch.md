# Phase 10 — QA, Native Store Deployment & Launch

**Goal:** Full regression, App Store & Play Store submissions, security audit, consent compliance audit, AI diagnosis accuracy validation, production deploy.  
**Duration:** 2 days  
**Owner:** All 3 devs  
**Screens:** All 30 screens — full regression

---

## Task 10.1 — Android APK/AAB Generation & Play Store Setup

**Description:** Configure Android signing configs for release builds. Generate signed AAB from `androidApp` module. Set up Google Play Console developer account, create app listing, upload AAB to Internal/Beta testing track.

**Steps:**

1. Generate keystore (`keytool -genkey -v -keystore nonitura.jks`)
2. Configure `androidApp/build.gradle.kts` with signing configs
3. Build AAB: `./gradlew :androidApp:bundleRelease`
4. Create Google Play Console account ($25 one-time fee)
5. Create app listing:
   - App name: Noni Tura
   - Category: Medical
   - Content rating: IARC questionnaire (pediatric surgical care)
   - Privacy policy URL
   - App description, screenshots, feature graphic
6. Upload AAB to Internal Testing track
7. Generate internal test link

**Acceptance Criteria:**

- AAB builds successfully without errors
- AAB uploaded to Google Play Console
- App accessible via internal test link
- App installs and runs correctly on physical Android device

---

## Task 10.2 — iOS IPA Generation & App Store Setup

**Description:** Configure Xcode workspace. Set up Apple Developer certificates and provisioning profiles. Build IPA via Xcode. Create App Store Connect listing, upload via Transporter, submit for TestFlight beta review.

**Steps:**

1. Open KMM-generated Xcode workspace (`iosApp/iosApp.xcworkspace`)
2. Configure signing in Xcode:
   - Apple Developer account ($99/year)
   - Development and Distribution certificates
   - Provisioning profiles (development + App Store)
3. Build for generic iOS device (Release)
4. Archive build in Xcode
5. Upload to App Store Connect via Xcode Organizer or Transporter
6. Create App Store Connect listing:
   - App name: Noni Tura
   - Category: Medical
   - Age rating: Complete questionnaire
   - Privacy policy URL
   - App description, screenshots (iPhone and iPad)
7. Submit for TestFlight beta review
8. Send TestFlight invites

**Acceptance Criteria:**

- IPA builds successfully from KMM Xcode workspace
- IPA uploaded to App Store Connect
- TestFlight invite sent and app installs on physical iOS device
- App runs correctly on iOS without crashes or layout issues

---

## Task 10.3 — Web Wasm Deployment

**Description:** Export Compose for Web (Wasm) module. Deploy static files to Vercel. Configure routing fallback for SPA navigation.

**Steps:**

1. Build Wasm module: `./gradlew :webApp:wasmJsBrowserDistribution`
2. Output: static files in `webApp/build/dist/wasmJs/productionExecutable/`
3. Deploy to Vercel:
   - Connect Git repository or use Vercel CLI
   - Set output directory to Wasm build output
   - Configure `vercel.json` for SPA fallback:
     ```json
     {
       "rewrites": [
         { "source": "/(.*)", "destination": "/index.html" }
       ]
     }
     ```
4. Configure custom domain (if applicable)
5. Verify HTTPS enforced

**Acceptance Criteria:**

- Web app live on Vercel URL
- Login and navigation work without 404 errors on refresh
- All screens render correctly in Chrome, Safari, Firefox
- HTTPS enforced

---

## Task 10.4 — Doctor Pool Isolation — Security Audit

**Description:** Comprehensive security audit to verify surgeon pool isolation across all API endpoints. Confirm that Surgeon A cannot access Surgeon B's patients under any circumstances. Every endpoint returning patient-related data must validate JWT's `doctor_id` against resource's `doctor_id`.

**Audit Checklist:**

- [ ] All GET endpoints returning patient data validate `doctor_id` from JWT
- [ ] All POST/PATCH/DELETE endpoints on patient-related resources validate ownership
- [ ] No endpoint allows querying or filtering by another surgeon's `doctor_id`
- [ ] Search and filter operations cannot bypass `doctor_id` scoping
- [ ] JWT token cannot be tampered with to change `doctor_id` (verify signature)
- [ ] Parent endpoints validate `patient_id` from JWT — no cross-patient access
- [ ] **Nurse endpoints validate `nurse_id` and resolve to `doctor_id` — no cross-surgeon access**
- [ ] No data leaks in error messages (e.g., "Patient not found" instead of "Patient belongs to another surgeon")
- [ ] All 25+ backend endpoints tested with both valid and invalid `doctor_id` JWTs

**Test Matrix:**

For each endpoint, test with:
1. Valid JWT (correct `doctor_id`) → expect 200
2. Invalid JWT (wrong `doctor_id`) → expect 403
3. Missing JWT → expect 401
4. Expired JWT → expect 401
5. **Nurse JWT accessing surgeon-only endpoint → expect 403**

**Acceptance Criteria:**

- Zero cross-doctor data leaks across all endpoints
- All 25+ endpoints return 403 on cross-surgeon access attempts
- All endpoints return 401 on missing or expired JWT
- No sensitive data exposed in error messages
- **Nurse cannot access surgeon-only endpoints (intra-op, discharge, templates CRUD)**

---

## Task 10.5 — WhatsApp Template Approval

**Description:** Ensure all 5 WhatsApp message templates submitted in Phase 3 are approved by Meta Business Manager.

**Templates to Verify:**

| # | Template Name | Status |
|---|---|---|
| 1 | `opd_summary` | Pending → Approved |
| 2 | `follow_up_reminder` | Pending → Approved |
| 3 | `admission_alert` | Pending → Approved |
| 4 | `post_surgery_update` | Pending → Approved |
| 5 | `discharge_summary` | Pending → Approved |

**Template Requirements:**

- English language only
- No promotional content — purely transactional/informational
- Must include opt-out line ("Reply STOP to unsubscribe")
- Must comply with Meta's healthcare messaging policies
- Template parameters correctly defined

**Acceptance Criteria:**

- All 5 templates approved by Meta Business Manager
- Test messages delivered successfully to +91 Indian phone numbers
- Each template's parameters correctly populated with real patient data
- Failed template sends logged with clear error messages

---

## Task 10.6 — Consent Form Compliance Audit (NEW)

**Description:** Verify that all generated consent forms comply with Indian Medical Council (MCI) and NABH standards. This is a legal compliance requirement for surgical procedures in India.

**Compliance Checklist:**

- [ ] Consent form includes patient name, age, and guardian name
- [ ] Proposed procedure clearly stated in layman's terms
- [ ] Anesthesia type specified
- [ ] Risks and complications listed (standard list for procedure type)
- [ ] Alternative treatments discussed
- [ ] Doctor declaration included ("I have explained the procedure, risks, and alternatives to the patient/guardian")
- [ ] Patient/guardian declaration included ("I understand the procedure, risks, and alternatives and consent to the surgery")
- [ ] Signature blocks for patient/guardian, witness, and doctor
- [ ] Date and time of consent recorded
- [ ] Form is immutable after signing
- [ ] PDF is tamper-evident (if possible, include checksum or digital signature metadata)

**Acceptance Criteria:**

- All generated consent forms pass compliance checklist
- Legal review confirms MCI/NABH compliance
- No consent form can be modified after signing
- Audit trail exists for every consent form (generation, signing, witness)

---

## Task 10.7 — AI Diagnosis Accuracy Smoke Tests (NEW)

**Description:** Validate that the AI suggestive diagnosis feature provides helpful, non-harmful suggestions. This is not a clinical validation (that requires IRB approval) but a smoke test to ensure the integration works and suggestions are clinically plausible.

**Test Cases:**

| # | Input (Complaint + Examination) | Expected Behavior |
|---|---|---|
| 1 | "Abdominal pain, vomiting" + "Tender right lower quadrant, rebound tenderness" | Suggestions include appendicitis, mesenteric adenitis, intestinal obstruction |
| 2 | "Groin swelling, crying on straining" + "Reducible swelling in right inguinal region" | Suggestions include inguinal hernia, hydrocele, undescended testis |
| 3 | "Failure to pass meconium within 24h" + "Distended abdomen, absent anal opening" | Suggestions include imperforate anus, Hirschsprung disease, intestinal atresia |
| 4 | Vague/minimal input ("fever", "looks unwell") | AI returns "Insufficient information — please provide more examination details" rather than guessing |

**Safety Checks:**

- [ ] AI never suggests a diagnosis with 100% confidence
- [ ] All suggestions include disclaimer
- [ ] AI does not suggest treatment plans (only diagnoses + investigations)
- [ ] AI suggestions are logged in `ai_diagnosis_logs` for audit
- [ ] No PII is sent to LLM API (only complaint, examination, age, gender)

**Acceptance Criteria:**

- All test cases produce clinically plausible suggestions
- Vague inputs trigger "insufficient information" response rather than wild guesses
- No suggestion exceeds 85% confidence
- All suggestions include standard disclaimer
- Audit logs contain all AI interactions

---

## Task 10.8 — E2E Test — Surgeon Flow

**Description:** Complete end-to-end regression test of the entire surgeon journey on Android and iOS. Covers all 13 surgeon screens in sequence.

**Test Flow:**

1. **Login (P1 → P2):** Enter surgeon phone, verify OTP, confirm routing to S1
2. **Dashboard (S1):** Verify metrics, surgery cards, IPD cards, FAB
3. **Add Patient (S2 FAB):** Create patient, verify in Patient List
4. **Create OPD Consult (S3):** Fill all fields, trigger AI diagnosis panel, save, verify WhatsApp preview (S12)
5. **WhatsApp Preview (S12):** Review message, send via WhatsApp, verify delivery
6. **Patient Profile (S10):** Verify OPD history, tabs, document upload, consent forms tab
7. **Admit Patient (S4):** Create IPD admission, verify WA + Push admission alert
8. **Generate Consent Form (CF1):** Generate consent form, verify PDF content
9. **Pre-op Notes (S5):** Fill pre-op form, load template, mark complete, generate consent form
10. **Intra-op Notes (S6):** Fill intra-op form, save, verify WA + Push post_surgery_update
11. **Post-op Notes (S7):** Create post-op entry, verify day calculation
12. **Ward Round (S8):** Create ward round entry, mark ready for discharge
13. **Discharge (S9):** Fill discharge form, save, verify WA + Push discharge_summary
14. **Schedule (S11):** Verify OT and OPD schedules, create booking
15. **Profile & Settings (S13):** Verify stats, template management, nurse management, logout

**Notification Triggers to Verify (4 total):**

| # | Trigger | Expected Notification |
|---|---|---|
| 1 | OPD consult saved | WhatsApp `opd_summary` + Push to parent |
| 2 | Patient admitted | WhatsApp `admission_alert` + Push to parent + Push to nurse |
| 3 | Intra-op notes saved | WhatsApp `post_surgery_update` + Push to parent |
| 4 | Discharge saved | WhatsApp `discharge_summary` + Push to parent |

**Acceptance Criteria:**

- Zero broken flows across all 13 surgeon screens
- Push notifications and WhatsApp messages fire correctly at all 4 trigger points
- All form data saved and retrieved correctly
- Navigation between screens works without errors
- Test passes on both Android and iOS physical devices

---

## Task 10.9 — E2E Test — Nurse Flow (NEW)

**Description:** Complete end-to-end regression test of the entire nurse journey.

**Test Flow:**

1. **Login (P1 → P2):** Enter nurse phone, verify OTP, confirm routing to N1
2. **Nurse Dashboard (N1):** Verify metrics, quick actions, upcoming appointments
3. **Add Patient (N3 FAB):** Create patient, verify in Patient List
4. **Create OPD Record (N2):** Fill OPD form, verify no surgical decision field, verify "Pending Review" badge in surgeon view
5. **Ward Round (N4):** Create ward round notes, verify "Ready for Discharge" button hidden
6. **Appointment Manager (N5):** Book appointment on behalf of parent, verify notifications sent
7. **Schedule Viewer (N6):** View OT and OPD schedule, verify read-only
8. **Profile:** Verify nurse can edit own profile, logout

**Permission Tests:**

- [ ] Nurse cannot access intra-op notes screen
- [ ] Nurse cannot access discharge screen
- [ ] Nurse cannot create surgical templates
- [ ] Nurse cannot mark "Ready for Discharge"

**Acceptance Criteria:**

- Zero broken flows across all 6 nurse screens
- All nurse permission restrictions enforced correctly
- Test passes on Android and iOS physical devices

---

## Task 10.10 — E2E Test — Parent Flow

**Description:** Complete end-to-end regression test of the entire parent journey.

**Test Flow:**

1. **Login (P1 → P2):** Enter parent phone, verify OTP, confirm routing to P3
2. **Parent Home (P3):** Verify surgery status card, next appointment, follow-up pill, last consult, consent form status
3. **Surgery Status Detail (P4):** Verify timeline with 5 nodes, active node animation, share on WhatsApp, view consent form
4. **OPD Records List (P5):** Verify filter chips, records list, tap to view detail
5. **Consult Record Detail (P6):** Verify read-only cards, lock icon, download PDF, share on WhatsApp, AI suggestions card
6. **Appointment Booking — Step 1 (P7):** Select surgeon
7. **Appointment Booking — Step 2 (P8):** Select slot
8. **Appointment Booking — Step 3 (P9):** Review and confirm
9. **Booking Confirmed (P10):** Verify success animation, WA + Push confirmation
10. **Parent Profile (P11):** Verify child info, allergy badge, surgeon contact, WA toggles, consent form history, logout

**Acceptance Criteria:**

- Zero broken flows across all 11 parent screens
- Timeline updates in real-time when surgeon makes changes
- PDF downloads and opens correctly
- WhatsApp share works via native share intent
- Appointment booking creates record and sends confirmations
- No cross-patient data visible
- **Consent form can be signed digitally and viewed as PDF**

---

## Task 10.11 — Production Backend Deploy & Monitoring

**Description:** Deploy FastAPI backend to production on Render. Set up Neon PostgreSQL production branch. Inject Sentry DSN into KMM shared module for crashlytics. Enforce HTTPS. Configure monitoring and alerting.

**Deployment Steps:**

1. **Backend → Render:**
   - Create Render Web Service
   - Connect Git repository
   - Environment variables: `DATABASE_URL`, `JWT_SECRET`, `2FACTOR_API_KEY`, `META_WA_TOKEN`, `META_WA_PHONE_ID`, `FIREBASE_CREDENTIALS_JSON`, `CLOUDINARY_URL`, `SENTRY_DSN`, `OPENAI_API_KEY` (NEW for AI diagnosis)
   - Build command: `pip install -r requirements.txt && prisma generate`
   - Start command: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - Auto-deploy from main branch

2. **Neon PostgreSQL → Production Branch:**
   - Create production branch
   - Run `prisma db push` against production database
   - Verify all tables and relations
   - Enable connection pooling

3. **Sentry Integration:**
   - Create Sentry project
   - Inject `SENTRY_DSN` into KMM shared module
   - Initialize Sentry SDK in each platform's entry point
   - Verify crash reports captured on all 3 platforms

4. **HTTPS Enforcement:**
   - Render provides HTTPS by default
   - Verify all API endpoints redirect HTTP → HTTPS
   - Configure CORS to allow only production frontend domains

5. **Monitoring & Alerting:**
   - Sentry alerts for new errors and high error rates
   - Render health checks for `/health` endpoint
   - UptimeRobot for external monitoring

**Acceptance Criteria:**

- Backend live on production API URL
- Sentry capturing native crashes on Android, iOS, and Web
- No console errors in production web app
- HTTPS enforced on all endpoints
- Health check endpoint returns 200 consistently
- Database connections stable under load
