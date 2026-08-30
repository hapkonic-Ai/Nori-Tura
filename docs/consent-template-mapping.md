# Consent Form ↔ Surgical Template Mapping Guide

This document explains how a **Surgical Template** is transformed into a pre-filled **Consent Form**, which fields are auto-populated, which fields are left empty, and how surgeons should write templates so that consent forms can be generated with minimal manual editing.

---

## 1. Source: Surgical Template

A surgical template is the reusable source of truth for a procedure. It is stored in the `surgical_templates` table and exposed through `/surgical-templates`.

### 1.1 Schema (KMP DTO)

```kotlin
data class SurgicalTemplateDto(
    val id: String,
    val name: String,                         // Template display name
    val procedure: String,                    // Procedure name
    val approach: String? = null,             // Surgical approach (e.g. "Laparoscopic", "Open")
    val anaesthesia: List<String> = emptyList(), // List of anaesthesia types
    val investigations: List<String> = emptyList(), // Pre-op investigations
    val riskLevel: String? = null,            // "low", "moderate", "high", etc.
    val technique: String? = null,            // Step-by-step technique description
    val specialInstructions: String? = null,  // Extra instructions / variations
    val risks: List<String> = emptyList(),    // General / material risks
    val benefits: List<String> = emptyList(), // Expected benefits
    val alternatives: List<String> = emptyList(), // Alternative treatments
    val complications: List<String> = emptyList(), // Procedure-specific complications
    val postOpCare: String? = null,           // Post-operative care instructions
    val expectedRecovery: String? = null      // Expected recovery timeline
)
```

### 1.2 Recommended content for each field

| Field | What to write | Example |
|---|---|---|
| `name` | Human-readable template label | "Laparoscopic Appendectomy – Paediatric" |
| `procedure` | Short procedure name | "Appendectomy" |
| `approach` | How the operation is performed | "Laparoscopic (3-port technique)" |
| `anaesthesia` | One or more anaesthesia types | `["General anaesthesia"]` or `["General", "Caudal epidural"]` |
| `investigations` | Routine pre-op workup | `["CBC", "Coagulation profile", "Chest X-ray"]` |
| `riskLevel` | Risk stratification | "low" / "moderate" / "high" |
| `technique` | Concise operative technique | "Pneumoperitoneum created via Hasson technique; appendix identified, mesoappendix divided with bipolar diathermy; base transfixed and excised." |
| `specialInstructions` | Anything else relevant to consent | "Nasogastric tube may be required; conversion to open procedure if laparoscopy unsafe; intraoperative cholangiogram if indicated." |
| `risks` | General / material risks | `["Bleeding", "Infection", "Anaesthesia reaction"]` |
| `benefits` | Expected benefits | `["Symptom relief", "Definitive treatment", "Prevention of complications"]` |
| `alternatives` | Alternative treatment options | `["Conservative management", "Open surgery", "No treatment"]` |
| `complications` | Procedure-specific complications | `["Injury to adjacent structures", "Recurrence", "Conversion to open"]` |
| `postOpCare` | Post-operative care instructions | "Wound care, activity restrictions, follow-up in 1 week." |
| `expectedRecovery` | Expected recovery timeline | "Hospital stay 1-2 days; return to normal activity in 2 weeks." |

---

## 2. Target: Consent Form

The consent form is created via `POST /consent-forms`. The KMP create request contains many fields, some clinical and some administrative.

### 2.1 Schema (KMP DTO)

```kotlin
data class ConsentFormCreateRequest(
    val admissionId: String,
    val formType: String,                     // e.g. "Lap Appendectomy Template"
    val diagnosis: String,                    // Patient diagnosis
    val procedure: String,                    // Procedure name
    val anesthesia: String,                   // Anaesthesia (single string)
    val risks: String,                        // General / material risks
    val benefits: String,
    val alternatives: String,
    val postOpCare: String,

    // Hospital info (filled from hospital profile if available)
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
    val hospitalContact: String? = null,
    val hospitalRegistrationNumber: String? = null,

    // Doctor info (filled from doctor profile if available)
    val doctorQualification: String? = null,
    val doctorRegistrationNumber: String? = null,

    // Guardian / witness
    val guardianRelationship: String? = null,

    // Enhanced clinical info
    val procedureDescription: String? = null,
    val expectedRecovery: String? = null,
    val possibleComplications: String? = null,
    val materialRisks: String? = null,

    // Defaults
    val language: String? = "English",
    val consentVersion: String? = "v2.1",
    val consentForAnesthesia: Boolean = true,
    val consentForBloodProducts: Boolean = false,
    val consentForPhotography: Boolean = false
)
```

### 2.2 UI form fields

The `ConsentFormScreen` exposes the following editable fields:

- Core clinical: `formType`, `diagnosis`, `procedure`, `procedureDescription`, `anesthesia`
- Risks & recovery: `risks`, `materialRisks`, `possibleComplications`, `benefits`, `alternatives`, `postOpCare`, `expectedRecovery`
- Hospital: `hospitalName`, `hospitalAddress`, `hospitalContact`, `hospitalRegNo`
- Doctor: `doctorQualification`, `doctorRegNo`
- Guardian: `guardianRelationship`
- Checkboxes: `consentForAnesthesia`, `consentForBloodProducts`, `consentForPhotography`

Only the **core clinical** fields above the template mapping line are currently prefilled from a template.

---

## 3. Current Autofill Mapping

The mapping lives in `ConsentFormViewModel.applyTemplate()`.

| Surgical Template Field | Consent Form Field | Transform |
|---|---|---|
| `name` | `formType` | copied as-is |
| `procedure` | `procedure` | copied as-is |
| `anaesthesia` (list) | `anesthesia` | joined with `, ` |
| `approach` | `procedureDescription` | prefixed `"Approach: …"` |
| `technique` | `procedureDescription` | prefixed `"Technique: …"` |
| `specialInstructions` | `procedureDescription` | appended as-is |
| `risks` (list) | `risks` | joined with newline |
| `complications` (list) | `materialRisks` | joined with newline |
| `complications` (list) | `possibleComplications` | joined with newline |
| `benefits` (list) | `benefits` | joined with newline |
| `alternatives` (list) | `alternatives` | joined with newline |
| `postOpCare` | `postOpCare` | copied as-is |
| `expectedRecovery` | `expectedRecovery` | copied as-is |

Everything else in the consent form is **not** prefilled and must be entered manually.

### 3.1 Example

Template:

```json
{
  "name": "Laparoscopic Appendectomy",
  "procedure": "Appendectomy",
  "approach": "Laparoscopic (3-port)",
  "anaesthesia": ["General anaesthesia"],
  "technique": "Pneumoperitoneum, appendiceal mobilisation, endoloop ligation.",
  "specialInstructions": "Conversion to open if unsafe.",
  "risks": ["Bleeding", "Infection", "Anaesthesia reaction"],
  "benefits": ["Definitive treatment", "Symptom relief"],
  "alternatives": ["Conservative antibiotics", "Open appendectomy"],
  "complications": ["Injury to bowel", "Conversion to open", "Wound infection"],
  "postOpCare": "Wound care, oral analgesics, light diet, review in 1 week.",
  "expectedRecovery": "Hospital stay 1-2 days; full activity in 2 weeks."
}
```

Prefilled consent fields:

```
formType              = "Laparoscopic Appendectomy"
procedure             = "Appendectomy"
anesthesia            = "General anaesthesia"
procedureDescription  = "Approach: Laparoscopic (3-port)\n\nTechnique: Pneumoperitoneum, appendiceal mobilisation, endoloop ligation.\n\nConversion to open if unsafe."
risks                 = "Bleeding\nInfection\nAnaesthesia reaction"
materialRisks         = "Injury to bowel\nConversion to open\nWound infection"
possibleComplications = "Injury to bowel\nConversion to open\nWound infection"
benefits              = "Definitive treatment\nSymptom relief"
alternatives          = "Conservative antibiotics\nOpen appendectomy"
postOpCare            = "Wound care, oral analgesics, light diet, review in 1 week."
expectedRecovery      = "Hospital stay 1-2 days; full activity in 2 weeks."
```

---

## 4. Fields That Are NOT Autofilled (Gaps)

These fields remain blank after template selection and must be filled by the surgeon or pulled from another source:

| Consent Field | Why it is not autofilled | Suggested future source |
|---|---|---|
| `diagnosis` | Patient-specific | Admission / OPD record |
| `hospitalName` / `hospitalAddress` / `hospitalContact` / `hospitalRegistrationNumber` | Comes from hospital profile | Hospital profile lookup |
| `doctorQualification` / `doctorRegistrationNumber` | Comes from doctor profile | Doctor profile lookup |
| `guardianRelationship` | Patient-specific | Parent profile / admission |

All clinical fields (`procedure`, `anesthesia`, `procedureDescription`, `risks`, `materialRisks`, `possibleComplications`, `benefits`, `alternatives`, `postOpCare`, `expectedRecovery`) are now prefilled from the surgical template when populated.

---

## 5. Recommendations for Writing Templates

To get the most complete consent forms with the least editing:

1. **Fill all template fields**, especially `approach`, `technique`, and `specialInstructions`. These become the `procedureDescription`.
2. **Use `anaesthesia` as a list** even if there is only one entry; the UI will join them nicely.
3. **Keep `specialInstructions` consent-focused** — mention conversion to open, additional procedures, drains, blood transfusion possibility, etc.
4. **Create one template per common procedure** with a clear `name` so the searchable picker is easy to use.
5. **For now, manually enter risks/benefits/alternatives** after autofill. Future backend work can extend the template schema to include these.

---

## 6. Recently Implemented Extensions

The following fields were added to `SurgicalTemplateDto` / `SurgicalTemplateCreateRequest` and are now mapped into the consent form:

```kotlin
val risks: List<String> = emptyList(),
val benefits: List<String> = emptyList(),
val alternatives: List<String> = emptyList(),
val complications: List<String> = emptyList(),
val postOpCare: String? = null,
val expectedRecovery: String? = null
```

Future possible additions:

- `indications: List<String>` → map to a new `indications` consent field or append to `procedureDescription`.
- `hospitalId` override per template.
- Template versioning so consent forms generated from an older template remain traceable.

---

## 7. Files Involved

- `shared/src/commonMain/kotlin/com/example/nori_tura/data/dto/SurgicalTemplateDto.kt`
- `shared/src/commonMain/kotlin/com/example/nori_tura/data/dto/ConsentFormCreateRequest.kt`
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormViewModel.kt`
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormScreen.kt`
- `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/TemplatePickerDialog.kt`
