# TextField Autocomplete Analysis

Analysis of all free-text input fields in the KMM Compose UI and which ones should use medical-domain autocomplete.

## Scope

Scanned all Compose screens and components under:

```
shared/src/commonMain/kotlin/com/example/nori_tura/presentation/
```

Files inspected: 19 Kotlin files containing `OutlinedTextField`, `FormTextField`, `ConsentField`, `SearchField`, or `PhoneInputField`.

## Result Summary

| Category | Distinct Field Types | Examples |
|---|---|---|
| **Needs medical autocomplete** | ~35 | Diagnosis, procedure, complaint, examination, medications, investigations, risks |
| **Could benefit (optional)** | ~5 | Diagnosis search filter, allergies, parent reason for visit, image label |
| **No autocomplete needed** | ~25 | Names, phones, OTP, IDs, dates, numeric fields, dropdown anchors |

---

## 1. Fields That Need Medical Autocomplete (High Priority)

These fields accept free-text clinical or surgical input and should autocomplete from a medical terminology service.

### OPD Consult — `OpdConsultScreen.kt`

| Field | Suggested Vocabulary |
|---|---|
| Chief Complaint | Symptoms, presenting complaints |
| Examination Findings | Clinical exam phrases, signs |
| Diagnosis / Provisional Findings | ICD-10/ICD-11 diagnoses, pediatric surgical diagnoses |
| Planned Procedure | Procedure names |
| Surgical Decision | Surgery advice, decision phrases |
| Advice | General post-visit advice snippets |
| Medication Name | Drug names (generic + brand) |
| Medication Dose | Dose units and strengths |
| Medication Frequency | `OD`, `BD`, `TDS`, `QID`, `SOS`, etc. |
| Medication Duration | Duration phrases (`3 days`, `1 week`, `2 weeks`) |
| Investigation Type | Lab and radiology tests (`CBC`, `LFT`, `USG`, `X-ray`, `MRI`) |

### IPD Admission Notes — `AdmissionDetailScreen.kt`

#### Pre-op

| Field | Suggested Vocabulary |
|---|---|
| Procedure | Procedure names |
| Approach | `Laparoscopic`, `Open`, `Robotic`, `Endoscopic` |
| Anaesthesia | `GA`, `Spinal`, `Local`, `Epidural`, `Sedation` |
| Investigations | Pre-op lab and imaging tests |
| Risk Level | `Low`, `Moderate`, `High`, `ASA I`–`ASA IV` |
| Special Instructions | `NPO`, `bowel prep`, `antibiotic prophylaxis` |

#### Intra-op

| Field | Suggested Vocabulary |
|---|---|
| Procedure Done | Procedure names |
| Findings | Operative findings |
| Technique | Surgical technique phrases |
| Complications | Bleeding, infection, organ injury, etc. |
| Blood Loss | Estimated blood loss phrases |

#### Post-op

| Field | Suggested Vocabulary |
|---|---|
| Condition | `Stable`, `Critical`, `Pain`, `Febrile` |
| Vitals | Vital sign labels and values |
| Wound Status | `Clean`, `Clean-contaminated`, `Infected` |
| Diet | `NPO`, `clear liquids`, `soft diet`, `normal diet` |

#### Ward Round (SOAP)

| Field | Suggested Vocabulary |
|---|---|
| Subjective | Patient-reported symptoms |
| Objective | Exam/observation findings |
| Assessment | Diagnosis/assessment phrases |
| Plan | Plan phrases (`continue`, `start`, `stop`, `review`) |

#### Discharge Summary

| Field | Suggested Vocabulary |
|---|---|
| Condition at Discharge | `Stable`, `Improved` |
| Procedure Summary | Procedure summaries |
| Medications | Drug names + dose/frequency/duration |
| Wound Care | Wound care instructions |
| Activity Restrictions | Activity advice |
| Diet Instructions | Diet advice |
| Red Flags | Warning symptoms |

### Consent Form — `ConsentFormScreen.kt`

| Field | Suggested Vocabulary |
|---|---|
| Diagnosis | Diagnoses |
| Proposed Procedure | Procedure names |
| Procedure Description | Procedure descriptions |
| Anaesthesia | Anaesthesia types |
| General Risks | Common surgical risks |
| Material / Serious Risks | Serious risk phrases |
| Possible Complications | Procedure-specific complications |
| Benefits | Expected benefit phrases |
| Alternatives | Alternative treatment phrases |
| Post-operative Care Instructions | Post-op care phrases |
| Expected Recovery | Recovery timeline phrases |

### Surgical Templates — `SurgicalTemplatesScreen.kt`

| Field | Suggested Vocabulary |
|---|---|
| Template Name | Reusable template names |
| Procedure | Procedure names |
| Approach | Approach types |
| Technique | Technique phrases |
| Anaesthesia | Anaesthesia types |
| Pre-op Investigations | Lab and imaging tests |
| Risk Level | Risk levels |
| Special Instructions / Notes | Instruction phrases |

### OT/Schedule Booking — `ScheduleScreen.kt`

| Field | Suggested Vocabulary |
|---|---|
| Procedure (OT booking) | Procedure names |

---

## 2. Fields That Could Benefit (Medium / Optional)

| Screen | Field | Reason |
|---|---|---|
| `PatientListScreen.kt` | Search by diagnosis | Filter by diagnosis; autocomplete from known diagnoses improves accuracy |
| `AddPatientScreen.kt` | Allergies | Common drug and food allergies (`Penicillin`, `Sulfa`, `Latex`, `NSAIDs`) |
| `AppointmentRequestScreen.kt` | Reason for Visit | Parent describes symptoms; simple symptom autocomplete |
| `MedicalImagePicker.kt` | Label | Anatomical labels (`Abdomen X-ray`, `Ultrasound KUB`, `Wound`) |
| `MedicalImagePicker.kt` | Description | Brief clinical description |

---

## 3. Fields That Do NOT Need Medical Autocomplete

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
These are `OutlinedTextField`s that only display a dropdown selection and do not accept free text:
- Visit Type (`OpdConsultScreen.kt`)
- Gender (`AddPatientScreen.kt`, `AppointmentConfirmScreen.kt`)
- Select Patient (`AdmissionsListScreen.kt`, `ScheduleScreen.kt`)
- Urgency (`AdmissionsListScreen.kt`, `ScheduleScreen.kt`)
- Time (`ScheduleScreen.kt`)
- Visit type / Urgency (`ScheduleScreen.kt` `SimpleDropdown`)
- Medical Image Category (`MedicalImagePicker.kt`)
- Search by name or parent phone (`PatientListScreen.kt` first search field)

---

## 4. Integration Recommendations

1. Build a reusable `MedicalAutoCompleteTextField` component in `presentation/components/`.
2. Add a backend proxy or direct client call to a medical autocomplete service.
3. Use field-aware autocomplete so each TextField requests the right semantic type (e.g., diagnoses vs. procedures vs. medications).
4. Debounce user input (≈300 ms) to avoid excessive network calls.
5. Start with the highest-impact fields:
   - Diagnosis
   - Procedure
   - Chief Complaint
   - Medication Name
   - Investigation Type
6. Keep a small local fallback dictionary for items the external service may not cover (e.g., medication frequencies like `OD`, `BD`, `TDS`).
