# TextField Autocomplete Analysis

Analysis of all free-text input fields in the KMP Compose UI and which ones should use medical-domain autocomplete.

## Current State

A reusable autocomplete component already exists:

- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/MedicalAutoCompleteTextField.kt`
- It debounces input (300 ms), calls the MedService `/autocomplete` endpoint with `semantic_expansion=false`, shows grey inline ghost text for the top suggestion, and displays up to 5 suggestions in a dropdown.
- `MedicalTermRepository.search(...)` already accepts a `fieldTypes` filter but defaults to `"all"` because TUI-based mapping is not reliable yet.

Currently `MedicalAutoCompleteTextField` is used only in `OpdConsultScreen.kt` for:

- Chief Complaint
- Diagnosis / Provisional Findings
- Planned Procedure
- Medication Name

Most other clinical text fields still use plain `OutlinedTextField` (often via wrapper components like `ConsentField`, `FormTextField`, or directly).

---

## Recommendation: Implementation Strategy

### Option A — wrappers with an optional flag (recommended)

Do **not** try to add an autocomplete flag to Material's `OutlinedTextField` (it is a library component and cannot be modified cleanly). Instead:

1. Keep `MedicalAutoCompleteTextField` as the dedicated autocomplete component.
2. Add an optional parameter to the project's wrapper components:
   - `ConsentField(..., autocomplete: Boolean = false)`
   - `FormTextField(..., autocomplete: Boolean = false)`
3. When `autocomplete = true`, the wrapper renders `MedicalAutoCompleteTextField`.
   When `false`, it keeps the existing `OutlinedTextField` behaviour.
4. For one-off `OutlinedTextField`s, either:
   - convert them to the wrapper with `autocomplete = true`, or
   - replace them directly with `MedicalAutoCompleteTextField`.

This is the least invasive approach and keeps the regular text fields unchanged.

### Option B — dedicated component everywhere

Always use `MedicalAutoCompleteTextField` directly wherever autocomplete is wanted, and leave plain `OutlinedTextField` for non-clinical fields. This is more explicit but leads to duplicated styling parameters.

### Decision

Use **Option A**: wrappers get an optional `autocomplete` (and later `fieldType`) parameter, while `MedicalAutoCompleteTextField` remains the underlying implementation.

---

## 1. Fields That Should Have Autocomplete Enabled (High Priority)

These fields accept free-text clinical or surgical input and should autocomplete from the medical terminology service.

### OPD Consult — `OpdConsultScreen.kt`

| Field | Current Component | Recommendation |
|---|---|---|
| Chief Complaint | `MedicalAutoCompleteTextField` | ✅ Already autocomplete |
| Examination Findings | `OutlinedTextField` | Enable autocomplete (exam phrases / signs) |
| Diagnosis / Provisional Findings | `MedicalAutoCompleteTextField` | ✅ Already autocomplete |
| Planned Procedure | `MedicalAutoCompleteTextField` | ✅ Already autocomplete |
| Surgical Decision | `OutlinedTextField` | Enable autocomplete (decision phrases) |
| Advice | `OutlinedTextField` | Enable autocomplete (advice snippets) |
| Medication Name | `MedicalAutoCompleteTextField` | ✅ Already autocomplete |
| Medication Dose | `OutlinedTextField` | Keep plain (numeric + units) |
| Medication Frequency | `OutlinedTextField` | Keep plain, or local fixed list (`OD`, `BD`, `TDS`, `QID`, `SOS`) |
| Medication Duration | `OutlinedTextField` | Keep plain (numeric + time unit) |
| Investigation Type | `MedicalAutoCompleteTextField` | Add autocomplete (lab/radiology tests) |

### IPD Admission Notes — `AdmissionDetailScreen.kt`

`FormTextField` is the wrapper here. Add `autocomplete: Boolean = false` and enable it for the fields below.

#### Pre-op

| Field | Recommendation |
|---|---|
| Procedure | ✅ Enable autocomplete |
| Approach | ✅ Enable autocomplete (`Laparoscopic`, `Open`, `Robotic`, etc.) |
| Anaesthesia | ✅ Enable autocomplete (`GA`, `Spinal`, `Local`, etc.) |
| Investigations | ✅ Enable autocomplete (lab/imaging tests) |
| Risk Level | Keep plain (short fixed value) |
| Special Instructions | ✅ Enable autocomplete (`NPO`, `bowel prep`, etc.) |

#### Intra-op

| Field | Recommendation |
|---|---|
| Procedure Done | ✅ Enable autocomplete |
| Findings | ✅ Enable autocomplete (operative findings) |
| Technique | ✅ Enable autocomplete (surgical technique phrases) |
| Complications | ✅ Enable autocomplete |
| Blood Loss | Keep plain (numeric / estimate) |
| OT Start / OT End | Keep plain (ISO time) |

#### Post-op

| Field | Recommendation |
|---|---|
| Condition | ✅ Enable autocomplete (`Stable`, `Critical`, etc.) |
| Vitals | Keep plain (key=value structured) |
| Wound Status | ✅ Enable autocomplete (`Clean`, `Infected`, etc.) |
| Diet | ✅ Enable autocomplete (`NPO`, `clear liquids`, etc.) |
| Pain Score | Keep plain (0–10 numeric) |

#### Ward Round (SOAP)

| Field | Recommendation |
|---|---|
| Subjective | ✅ Enable autocomplete (symptoms) |
| Objective | ✅ Enable autocomplete (exam findings) |
| Assessment | ✅ Enable autocomplete (diagnosis phrases) |
| Plan | ✅ Enable autocomplete (plan phrases) |

#### Discharge Summary

| Field | Recommendation |
|---|---|
| Condition at Discharge | ✅ Enable autocomplete |
| Procedure Summary | ✅ Enable autocomplete |
| Medications | ✅ Enable autocomplete (drug names) |
| Wound Care | ✅ Enable autocomplete |
| Activity Restrictions | ✅ Enable autocomplete |
| Diet Instructions | ✅ Enable autocomplete |
| Red Flags | ✅ Enable autocomplete (warning symptoms) |
| Follow-up Date | Keep plain (date/ISO) |

### Consent Form — `ConsentFormScreen.kt`

`ConsentField` is the wrapper here. Enable autocomplete for clinical fields.

| Field | Recommendation |
|---|---|
| Form Type | Keep plain (consent category) |
| Diagnosis | ✅ Enable autocomplete |
| Proposed Procedure | ✅ Enable autocomplete |
| Procedure Description | ✅ Enable autocomplete |
| Anaesthesia | ✅ Enable autocomplete |
| General Risks | ✅ Enable autocomplete |
| Material / Serious Risks | ✅ Enable autocomplete |
| Possible Complications | ✅ Enable autocomplete |
| Benefits | ✅ Enable autocomplete |
| Alternatives | ✅ Enable autocomplete |
| Post-operative Care Instructions | ✅ Enable autocomplete |
| Expected Recovery | ✅ Enable autocomplete |
| Hospital / Doctor / Guardian fields | Keep plain (administrative) |

### Surgical Templates — `SurgicalTemplatesScreen.kt`

| Field | Recommendation |
|---|---|
| Template Name | Keep plain (free label) |
| Procedure | ✅ Enable autocomplete |
| Approach | ✅ Enable autocomplete |
| Technique | ✅ Enable autocomplete |
| Anaesthesia | ✅ Enable autocomplete |
| Pre-op Investigations | ✅ Enable autocomplete |
| Risk Level | Keep plain (short fixed value) |
| Risks | ✅ Enable autocomplete |
| Benefits | ✅ Enable autocomplete |
| Alternatives | ✅ Enable autocomplete |
| Complications | ✅ Enable autocomplete |
| Post-op Care | ✅ Enable autocomplete |
| Expected Recovery | ✅ Enable autocomplete |
| Special Instructions / Notes | ✅ Enable autocomplete |

### OT/Schedule Booking — `ScheduleScreen.kt`

| Field | Recommendation |
|---|---|
| Procedure (OT booking) | ✅ Enable autocomplete |
| Ward / Bed / Urgency / Time | Keep plain |

---

## 2. Fields That Could Benefit (Medium / Optional)

| Screen | Field | Reason |
|---|---|---|
| `PatientListScreen.kt` | Search by diagnosis | Filter accuracy improves with diagnosis autocomplete |
| `AddPatientScreen.kt` | Allergies | Common drug/food allergies (`Penicillin`, `Sulfa`, `Latex`) |
| `AppointmentRequestScreen.kt` | Reason for Visit | Parent describes symptoms; simple symptom autocomplete |
| `MedicalImagePicker.kt` | Label | Anatomical labels (`Abdomen X-ray`, `Ultrasound KUB`) |
| `MedicalImagePicker.kt` | Description | Brief clinical description |

---

## 3. Fields That Should NOT Have Autocomplete

### Personal / Contact Information
- Patient Name
- Parent / Guardian Name
- Witness Name
- Parent Phone
- Doctor Phone (login/register)
- OTP

### Administrative / Identifiers
- Hospital / Clinic
- Specialty
- Hospital Name, Address, Contact, Registration No.
- Doctor Qualification
- Medical Registration No.
- Guardian Relationship

### Numeric / Date / Time
- Age
- Day Number
- Pain Score (0–10)
- Follow-up Date (date picker)
- OT Start / OT End (ISO time strings)

### Logistics
- Ward
- Bed No

### Communication
- WhatsApp/SMS message text (`WhatsAppPreviewScreen.kt`)

### Read-Only Dropdown Anchors
These `OutlinedTextField`s only display a dropdown selection and do not accept free text:
- Visit Type (`OpdConsultScreen.kt`)
- Gender (`AddPatientScreen.kt`, `AppointmentConfirmScreen.kt`)
- Select Patient (`AdmissionsListScreen.kt`, `ScheduleScreen.kt`)
- Urgency (`AdmissionsListScreen.kt`, `ScheduleScreen.kt`)
- Time (`ScheduleScreen.kt`)
- Medical Image Category (`MedicalImagePicker.kt`)
- Search by name or parent phone (`PatientListScreen.kt` first search field)

---

## 4. Migration Plan

1. **Phase 1 — wrappers**
   - Add `autocomplete: Boolean = false` (and optionally `fieldType: String? = null`) to:
     - `ConsentField` in `ConsentFormScreen.kt`
     - `FormTextField` in `AdmissionDetailScreen.kt`
   - When `autocomplete` is true, render `MedicalAutoCompleteTextField`; otherwise keep `OutlinedTextField`.

2. **Phase 2 — enable on high-value fields**
   - In `ConsentFormScreen.kt`: set `autocomplete = true` on diagnosis, procedure, procedureDescription, anesthesia, risks, materialRisks, possibleComplications, benefits, alternatives, postOpCare, expectedRecovery.
   - In `AdmissionDetailScreen.kt`: set `autocomplete = true` on the clinical `FormTextField`s listed above.
   - In `SurgicalTemplatesScreen.kt`: replace the clinical `OutlinedTextField`s with `MedicalAutoCompleteTextField` (or a wrapper).
   - In `OpdConsultScreen.kt`: add autocomplete to Examination Findings, Surgical Decision, Advice, and Investigation Type; keep medication dose/frequency/duration plain.

3. **Phase 3 — field-aware autocomplete (future)**
   - Once TUI mapping is reliable, pass `fieldType` from the wrappers to `MedicalTermRepository.search(...)`.
   - Example mappings: `diagnosis` → diagnosis TUI, `procedure` → procedure TUI, `medication` → substance TUI.

4. **Phase 4 — optional fields**
   - Add autocomplete to allergies, reason for visit, image label/description, and diagnosis search filter if desired.

---

## 5. Files to Touch

- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/MedicalAutoCompleteTextField.kt` — keep as-is, maybe expose `fieldType` later.
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormScreen.kt` — modify `ConsentField` wrapper and enable autocomplete per field.
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/AdmissionDetailScreen.kt` — modify `FormTextField` wrapper and enable autocomplete per field.
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/surgeon/SurgicalTemplatesScreen.kt` — use autocomplete for clinical fields.
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/opd/OpdConsultScreen.kt` — add autocomplete to a few remaining clinical fields.
- `shared/src/commonMain/kotlin/com/example/nori_tura/data/MedicalTermRepository.kt` — already supports `fieldTypes`; no change needed until TUI mapping is ready.
