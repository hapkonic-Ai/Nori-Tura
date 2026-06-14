# Phase 7 — Parent & Nurse: Appointment Booking

**Goal:** Parent books an OPD consult slot with either surgeon in 3 steps. Nurse can book on behalf of parent.  
**Duration:** 0.5 days  
**Owner:** Dev 1  
**Screens:** P7–P10, N5 (Nurse Appointment Manager)  

---

## Task 7.1 — 3-Step Booking Flow (P7, P8, P9, P10)

**Description:** A streamlined 3-step appointment booking flow allowing parents to book an OPD consult with either of the 2 pediatric surgeons. Uses a pill progress indicator at the top. On confirmation, WhatsApp and Push notifications are automatically sent.

**Step Progress Indicator:**

- Step 1: Select Surgeon (active = filled teal pill)
- Step 2: Select Slot (active = filled teal pill)
- Step 3: Confirm (active = filled teal pill)
- Completed steps show checkmark. Current step highlighted. Future steps outlined.

**Screen P7 — Step 1: Select Surgeon**

- **2 Surgeon Cards (side by side):** Each card displays surgeon name, hospital, specialty, next available date. Tapping selects (teal border) and enables "Next" button.
- **"Next" Button:** Enabled only when surgeon selected.

**Screen P8 — Step 2: Select Slot**

- **Week Strip:** Shows 7 days with dates. Current day highlighted. Left/right arrows.
- **Available Slot Chips (teal):** List of available time slots. Tapping selects (filled teal) and enables "Next".
- **Unavailable Slots:** Grayed out or hidden.
- **"Back" and "Next" Buttons**

**Screen P9 — Step 3: Confirm**

- **Summary Card:** Surgeon name, hospital, selected date/time, patient name, visit type.
- **"Confirm Booking" Button:** Submits booking. Navigates to P10.

**Screen P10 — Booking Confirmed**

- **Full-screen Success State:** Large green checkmark with scale + fade animation.
- **Confirmation Details:** Surgeon name, date, time, booking reference.
- **Auto-sent Notifications:**
  1. WhatsApp appointment confirmation to parent
  2. Push Notification to parent
  3. Push Notification to nurse
  4. Booked slot appears on surgeon's S11 Schedule screen

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/appointments/available-slots?doctor_id=&date=` | Returns available slots for specified surgeon on specified date. |
| POST | `/appointments` | Creates new appointment. Parameters: `patient_id`, `doctor_id`, `slot_datetime`, `visit_type`. Triggers WA + Push. |

**Acceptance Criteria:**

- Booking creates `appointments` record
- Slot removed from available slots (no double-booking)
- WhatsApp and Push confirmation sent to parent and nurse
- Appointment appears on surgeon's S11 Schedule
- Flow works on Android, iOS, and Web
- Progress indicator accurate
- Native success animation on P10

---

## Task 7.2 — Nurse Appointment Manager (N5) — NEW

**Description:** A schedule management screen for nurses. Allows nurses to view upcoming appointments, book appointments on behalf of parents, and cancel/reschedule existing appointments. This is critical because nurses often handle appointment scheduling at the hospital front desk.

**UI Components:**

- **TopBar Tabs:** Upcoming / Today / Past
- **Week Strip (horizontal scrollable row):** Same as S11. Tapping a day filters appointments.
- **Appointment Cards (LazyColumn):** Each card shows:
  - Patient name and age
  - Appointment time
  - Visit type (New / Follow-up)
  - Status pill (Booked / Completed / Cancelled)
  - Surgeon name
- **Swipe Actions (optional):**
  - Swipe right: "Complete" (mark appointment as completed)
  - Swipe left: "Cancel" (cancel appointment with confirmation dialog)
- **FAB:** "+ Book Appointment" — opens a bottom sheet with:
  - Patient selector (searchable dropdown)
  - Surgeon selector (if multiple surgeons)
  - Date picker
  - Time slot picker (fetched from `/appointments/available-slots`)
  - "Book" button

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/nurse/appointments` | Returns appointments scoped to nurse's `doctor_id`. Supports `?date=` and `?status=` filters. |
| POST | `/nurse/appointments` | Creates appointment on behalf of parent. Accepts `patient_id`, `slot_datetime`, `visit_type`. Triggers WA + Push. |
| PATCH | `/nurse/appointments/:id` | Updates appointment status (complete/cancel). Validates `doctor_id`. |

**Acceptance Criteria:**

- Nurse can view all appointments for their assigned surgeon
- Nurse can book appointments on behalf of parents
- Nurse can cancel/reschedule appointments
- Booking triggers same notifications as parent booking
- No cross-surgeon appointments visible
- Native swipe actions work on Android/iOS; buttons on Web
