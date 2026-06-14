# Phase 3 — WhatsApp & Push Notification Automation — OPD

**Goal:** Auto-send OPD consult summary via WhatsApp, send Push Notifications to parents and nurses.  
**Duration:** 0.5 days  
**Owner:** Dev 3 (Backend + Firebase)  
**Screens:** S12 — WhatsApp Preview  

---

## Task 3.1 — WhatsApp + Push Service (Backend)

**Description:** Integrate Meta Cloud API for WhatsApp messaging and Firebase Admin SDK for push notifications. Five WhatsApp message templates must be created and submitted to Meta Business Manager for approval. The backend automatically triggers push notifications via FCM/APNs tokens stored in the database when OPD records are created or updated.

**WhatsApp Templates:**

| Template Name | Trigger Event | Content Summary |
|---|---|---|
| `opd_summary` | OPD consult saved | Diagnosis, medications, advice, follow-up date |
| `follow_up_reminder` | Follow-up date reached (9AM IST cron) | Reminder of upcoming follow-up appointment |
| `admission_alert` | Patient admitted to IPD | Ward, bed number, surgeon name, status |
| `post_surgery_update` | Intra-op notes saved | Procedure completed, patient in recovery |
| `discharge_summary` | Discharge saved | Condition, medications, wound care, follow-up, red flags |

**Push Notification Events:**

| Event | Platform | Title | Body |
|---|---|---|---|
| Admission alert | Android (FCM) + iOS (APNs) | "Admission Update" | "Your child has been admitted to [ward]. Surgeon: [name]" |
| Post surgery update | Android (FCM) + iOS (APNs) | "Surgery Update" | "Surgery completed successfully. Your child is in recovery." |
| Discharge summary | Android (FCM) + iOS (APNs) | "Discharge Update" | "Your child has been discharged. Please review the discharge summary." |
| Appointment confirmation | Android (FCM) + iOS (APNs) | "Appointment Confirmed" | "Your appointment with Dr. [name] is confirmed for [date/time]." |
| Follow-up reminder | Android (FCM) + iOS (APNs) | "Follow-up Reminder" | "Your child has a follow-up appointment tomorrow." |
| Nurse: New Admission | Android (FCM) + iOS (APNs) | "New Admission" | "[Patient name] has been admitted to [ward]." |
| Nurse: Appointment Reminder | Android (FCM) + iOS (APNs) | "Appointment Soon" | "[Patient name] has an appointment at [time]." |

**API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/whatsapp/send` | Sends a WhatsApp message using a specified template. Parameters: `patient_id`, `template_name`, `template_params`. Logs the message in `whatsapp_logs`. |
| GET | `/whatsapp/logs/:patient_id` | Returns WhatsApp message logs for a specific patient. |

**Acceptance Criteria:**

- Test WhatsApp message delivered to a +91 Indian phone number
- Push notification received on both Android (FCM) and iOS (APNs)
- All 5 templates submitted to Meta Business Manager
- Message logs created in `whatsapp_logs` for every send attempt
- Failed sends logged with status = "failed" and error description
- **Nurse receives push notifications for admissions and appointment reminders**

---

## Task 3.2 — WhatsApp Preview Screen (S12)

**Description:** A Compose Multiplatform screen that displays a phone mockup frame showing the formatted WhatsApp message before sending. Gives the surgeon a chance to review and optionally edit the message content before delivery. Also provides a fallback SMS option.

**UI Components:**

- **Phone Mockup Frame:** Visual representation of a phone screen containing the formatted WhatsApp message.
- **Message Preview Content:** Uses WhatsApp-style formatting (bold, line breaks, bullet points).
- **Edit Message Button:** Opens editable text field to modify message before sending.
- **Send via WhatsApp Button (#25D366 green):** Sends via Meta Cloud API. On success, shows confirmation toast.
- **Send via SMS Fallback Button:** If WhatsApp unavailable, sends shorter SMS via 2Factor.in.

**Acceptance Criteria:**

- Preview renders correctly on all platforms
- Edit message option works
- Send via WhatsApp fires the API and logs entry
- SMS fallback works when WhatsApp fails
- Success/failure feedback provided

---

## Task 3.3 — Follow-up Reminder Cron + Push

**Description:** Daily scheduled task (cron) running at 9:00 AM IST. Queries all OPD records where `follow_up_date` equals today and `reminder_sent` is `false`. For each matching record, sends both WhatsApp template message and Push Notification to the parent, then marks `reminder_sent = true`.

**Cron Logic:**

```
EVERY DAY AT 9:00 AM IST:
  1. Query: SELECT * FROM opd_records WHERE follow_up_date = TODAY AND reminder_sent = false
  2. For each record:
     a. Send WhatsApp template "follow_up_reminder" to parent_phone
     b. Send Push Notification to parent's FCM token
     c. Update opd_records SET reminder_sent = true WHERE id = record.id
     d. Log both sends in whatsapp_logs
```

**Scheduling Implementation:**

- Use FastAPI's background scheduler (APScheduler) or platform-level cron on Render
- Timezone: Asia/Kolkata (IST = UTC+5:30)
- Idempotency: `reminder_sent` flag prevents duplicates

**Acceptance Criteria:**

- Cron fires daily at 9:00 AM IST without manual intervention
- Both WhatsApp and Push notifications sent for each matching record
- No duplicate messages — `reminder_sent` flag prevents re-sending
- Cron execution logged for monitoring
- Edge case: If no records match, cron completes silently
