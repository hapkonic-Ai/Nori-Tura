# Noni Tura — Agent Todo List

## Legend
- `[ ]` = Pending
- `[~]` = In Progress
- `[x]` = Done
- `→` = Blocked by dependency

---

## Phase 1 — KMM Foundation & Auth
- [x] Task 1.1 — KMM Repo & Project Setup (shared module, androidApp, iosApp, webApp)
- [x] Task 1.2 — Database Schema (all tables including `nurses`, `consent_forms`, `ai_diagnosis_logs`)
- [x] Task 1.3 — OTP Auth Backend (three-way role detection: surgeon / nurse / parent)
- [x] Task 1.4 — OTP Auth KMM UI (P1 Login, P2 Verify, role-based routing)

## Phase 2 — Surgeon & Nurse OPD Consult Flow
- [x] Task 2.1 — Surgeon Dashboard (S1)
- [x] Task 2.2 — Patient List (S2)
- [x] Task 2.3 — OPD Consult Form (S3) + AI Suggestive Diagnosis Panel
- [x] Task 2.4 — Patient Profile (S10)
- [~] Task 2.5 — Nurse Dashboard (N1) → depends on 1.3
- [x] Task 2.6 — Nurse OPD Form (N2) → backend done
- [x] Task 2.7 — Nurse Patient List (N3) → backend done

## Phase 3 — WhatsApp & Push Notifications
- [x] Task 3.1 — WhatsApp + Push Service Backend stubs (Meta Cloud API + FCM) → backend stubs done
- [ ] Task 3.2 — WhatsApp Preview Screen (S12)
- [x] Task 3.3 — Follow-up Reminder Cron + Push
- [ ] Task 3.4 — Nurse notification preferences & events

## Phase 4 — IPD & Surgery Flow + Consent Forms
- [x] Task 4.1 — Admit Patient (S4) → backend done
- [x] Task 4.2 — Pre-op Notes (S5) → backend done
- [ ] Task 4.3 — Surgical Templates Manager
- [x] Task 4.4 — Intra-op Notes (S6) → backend done
- [x] Task 4.5 — Post-op Notes (S7) → backend done
- [x] Task 4.6 — Daily Ward Round (S8) → backend done
- [x] Task 4.7 — Discharge (S9) → backend done
- [x] Task 4.8 — Consent Form Generation Backend → backend done
- [x] Task 4.9 — Consent Form UI (digital signature + PDF view) → depends on 4.8
- [x] Task 4.10 — Nurse admission creation (no surgical decision) → backend done

## Phase 5 — Schedule
- [ ] Task 5.1 — Schedule Screen (S11 — OT & OPD)
- [~] Task 5.2 — Nurse schedule view & management → depends on 5.1

## Phase 6 — Parent Flow: Surgery Tracking & Records
- [~] Task 6.1 — Parent Home (P3) — pending consents visible; live surgery status card remaining
- [ ] Task 6.2 — Surgery Status Detail (P4)
- [ ] Task 6.3 — OPD Records List + Detail (P5, P6)
- [x] Task 6.4 — Consent form view/download in Documents tab

## Phase 7 — Appointment Booking
- [x] Task 7.1 — 3-Step Booking Flow backend (P7, P8, P9, P10) → backend done
- [x] Task 7.2 — Nurse booking on behalf of parent → backend done

## Phase 8 — Surgeon Profile & Settings
- [ ] Task 8.1 — Surgeon Profile (S13)
- [x] Task 8.2 — Nurse management section (view/add/remove nurses) → backend done

## Phase 9 — Parent Profile & Settings
- [x] Task 9.1 — Parent Profile (P11)
- [x] Task 9.2 — Consent form history in parent profile

## Phase 10 — QA, Native Store Deployment & Launch
- [ ] Task 10.1 — Android AAB Generation & Play Store Setup
- [ ] Task 10.2 — iOS IPA Generation & App Store Setup
- [ ] Task 10.3 — Web Wasm Deployment (Vercel)
- [ ] Task 10.4 — Doctor Pool Isolation Security Audit
- [ ] Task 10.5 — WhatsApp Template Approval (Meta)
- [ ] Task 10.6 — E2E Test — Surgeon Flow
- [ ] Task 10.7 — E2E Test — Parent Flow
- [ ] Task 10.8 — E2E Test — Nurse Flow
- [ ] Task 10.9 — Consent Form Compliance Audit (MCI/NABH standards)
- [ ] Task 10.10 — AI Diagnosis Accuracy Smoke Tests
- [ ] Task 10.11 — Production Backend Deploy & Monitoring

## AI Diagnosis Agent Module
- [x] Task AI.1 — Backend LLM integration (OpenAI/Anthropic client)
- [x] Task AI.2 — Prompt engineering & structured output schema
- [x] Task AI.3 — API endpoint `POST /ai/suggest-diagnosis`
- [ ] Task AI.4 — KMM UI panel in OPD form (collapsible suggestions)
- [x] Task AI.5 — Audit logging (`ai_diagnosis_logs` table)

## Consent Form Module
- [x] Task CF.1 — Consent form template engine (HTML→PDF)
- [x] Task CF.2 — Indian MCI/NABH standard field mapping
- [x] Task CF.3 — Backend PDF generation & Cloudinary upload stub
- [x] Task CF.4 — Digital signature capture (canvas/touch)
- [x] Task CF.5 — Consent form viewer in Patient Profile & Parent Profile

## Nurse Module
- [x] Task NM.1 — Nurse table & schema
- [x] Task NM.2 — Nurse OTP auth & JWT claims
- [ ] Task NM.3 — Nurse Dashboard (N1)
- [ ] Task NM.4 — Nurse OPD Form (N2)
- [ ] Task NM.5 — Nurse Patient List (N3)
- [ ] Task NM.6 — Nurse Clinical Notes (SOAP) (N4)
- [ ] Task NM.7 — Nurse Appointment Manager (N5)
- [ ] Task NM.8 — Nurse Schedule Viewer (N6)
- [x] Task NM.9 — Permission middleware on all endpoints

---

## Next Immediate Tasks

1. **Task 4.3 — Surgical Templates CRUD backend + KMM UI**
2. **Task 5.1 — Schedule Screen (S11)**
3. **Task 6.2 — Surgery Status Detail (P4)**
4. **Task AI.4 — KMM AI Diagnosis Panel in OPD form**
5. **Nurse Module screens (N1–N6)**

