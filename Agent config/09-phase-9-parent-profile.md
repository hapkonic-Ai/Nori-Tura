# Phase 9 — Parent Profile & Settings

**Goal:** Parent profile, child info, notification preferences, consent form history.  
**Duration:** 0.5 days  
**Owner:** Dev 3  
**Screens:** P11 — Parent Profile  

---

## Task 9.1 — Parent Profile (P11)

**Description:** A shared Compose screen displaying the parent's profile information, their child's details, notification preferences, and consent form history. Emphasizes child's medical information (especially allergies) and provides quick access to the treating surgeon's contact.

**UI Sections:**

- **Child Section (prominent):**
  - Child's name, age, gender
  - Blood group
  - **Allergy Badge (red):** Prominently displayed red badge showing known allergies. If no allergies, green "No Known Allergies" badge.
  - Treating surgeon name

- **Treating Surgeon Row:**
  - Surgeon name and hospital
  - **Tap to Call:** Opens native phone dialer intent with surgeon's phone number pre-filled.

- **WhatsApp Notification Toggles:**
  - OPD Summary: On/Off
  - Admission Alerts: On/Off
  - Surgery Updates: On/Off
  - Discharge Summary: On/Off
  - Follow-up Reminders: On/Off
  - All toggles On by default. Toggling Off prevents corresponding WhatsApp message (Push notifications continue).

### NEW — Consent Form History Section

- **Consent Form List Card:**
  - Shows all consent forms related to the parent's child
  - Each row: Form type (Surgical / Anesthesia / Blood Transfusion), admission date, signed status (Signed / Pending), generation date
  - Tapping a row opens:
    - If signed: PDF viewer with the signed consent form
    - If pending: Consent Form Signing screen (digital signature + witness name)
  - "Download All" button: Downloads a ZIP of all signed consent forms

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/parent/profile` | Returns parent's profile, child details, notification preferences, and consent form list. |
| PATCH | `/parent/profile/notifications` | Updates WhatsApp notification preferences. |
| GET | `/parent/consent-forms` | **NEW** — Returns all consent forms for the parent's child. |
| POST | `/parent/consent-forms/:id/sign` | **NEW** — Submits digital signature and witness name for a consent form. |

**Acceptance Criteria:**

- All toggles persist state to backend and survive app restarts
- Native dialer opens with surgeon's phone number
- Allergy badge prominently displayed in red when allergies exist
- "No Known Allergies" green badge shown when no allergies
- Logout clears all local data and navigates to login
- **Consent form list shows all forms with correct signed status**
- **Pending consent forms can be signed digitally with signature + witness name**
- **Signed consent forms viewable as PDFs**
- **Download All button creates ZIP of signed forms**
