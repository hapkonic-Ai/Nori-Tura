# Noni Tura Surgical Care Platform — Agent Master Plan

## Overview

**Noni Tura** is a specialized pediatric surgical care platform for **2 pediatric surgeons in India**, built with **Kotlin Multiplatform Mobile (KMM)** targeting Android, iOS, and Web (Wasm). This plan is the single source of truth for an AI coding agent working on the project.

This version of the plan includes **three major new modules** on top of the original 10-phase MVP:

1. **AI Suggestive Diagnosis** — LLM-powered diagnosis suggestions during OPD consults.
2. **Consent Form Generation** — Indian MCI/NABH-compliant surgical consent forms, auto-generated as PDFs.
3. **Nurse Login Module** — Dedicated nurse role with scoped permissions to manage patients, OPD notes, appointments, and clinical records.

---

## Table of Contents

| # | Document | Description |
|---|---|---|
| — | [system_prompt.md](./system_prompt.md) | AI agent system prompt and rules |
| — | [todo.md](./todo.md) | Prioritized task backlog |
| 0 | [00-project-overview.md](./00-project-overview.md) | Stack, roles, constraints, schema summary |
| 1 | [01-phase-1-foundation-and-auth.md](./01-phase-1-foundation-and-auth.md) | KMM scaffolding, database schema, OTP auth |
| 2 | [02-phase-2-opd-consult.md](./02-phase-2-opd-consult.md) | Surgeon + Nurse OPD flow, AI diagnosis panel |
| 3 | [03-phase-3-notifications.md](./03-phase-3-notifications.md) | WhatsApp & Push automation |
| 4 | [04-phase-4-ipd-surgery.md](./04-phase-4-ipd-surgery.md) | IPD workflow + Consent form generation |
| 5 | [05-phase-5-schedule.md](./05-phase-5-schedule.md) | OT & OPD scheduling |
| 6 | [06-phase-6-parent-tracking.md](./06-phase-6-parent-tracking.md) | Parent surgery tracking & records |
| 7 | [07-phase-7-appointment-booking.md](./07-phase-7-appointment-booking.md) | 3-step parent appointment booking |
| 8 | [08-phase-8-surgeon-profile.md](./08-phase-8-surgeon-profile.md) | Surgeon profile, stats, nurse management |
| 9 | [09-phase-9-parent-profile.md](./09-phase-9-parent-profile.md) | Parent profile & notification preferences |
| 10 | [10-phase-10-qa-launch.md](./10-phase-10-qa-launch.md) | QA, security audit, store deployment |
| AI | [ai-diagnosis-agent.md](./ai-diagnosis-agent.md) | AI suggestive diagnosis feature spec |
| CF | [consent-form-module.md](./consent-form-module.md) | Indian surgical consent form generation |
| NM | [nurse-module.md](./nurse-module.md) | Nurse login, permissions, screens |

---

## Delivery Timeline

| Day | Phase | New Module Integration |
|---|---|---|
| Day 1–1.5 | Phase 1 — Foundation & Auth | Nurse table added to schema; OTP role detection extended |
| Day 2–2.5 | Phase 2 — OPD Consult | AI Diagnosis panel in OPD form; Nurse OPD form variant |
| Day 2.5–3 | Phase 3 — Notifications | Nurse notification events added |
| Day 3–4.5 | Phase 4 — IPD & Surgery | **Consent Form Generation** in Admit/Pre-op; Nurse admission permissions |
| Day 4.5–5 | Phase 5 — Schedule | Nurse schedule view/management |
| Day 5–6 | Phase 6 — Parent Tracking | Consent form download in Documents tab |
| Day 6–6.5 | Phase 7 — Appointment Booking | Nurse can book on behalf of parent |
| Day 6.5–7 | Phase 8 — Surgeon Profile | Nurse management section |
| Day 7–7.5 | Phase 9 — Parent Profile | Consent form history |
| Day 7.5–9.5 | Phase 10 — QA & Launch | Nurse E2E tests, consent compliance audit, AI diagnosis smoke tests |
| Day 9.5–10 | Buffer & Store Submission | Play Store + App Store + Vercel |

---

## Total Screens (Modified)

- **Surgeon:** 13 screens (S1–S13)
- **Nurse:** 6 screens (N1–N6)
- **Parent:** 11 screens (P3–P11, P1–P2 shared)
- **Total:** 30 screens

---

## Key Principles

1. **Doctor Pool Isolation** — Every patient-related query is scoped by `doctor_id`. No exceptions.
2. **Role-Based Access Control** — Surgeon / Nurse / Parent. Each role has explicit permissions.
3. **Indian Healthcare Compliance** — Consent forms follow MCI/NABH standards. Data stays in India where possible.
4. **Version-Pinned & Reproducible** — All versions locked in `gradle/libs.versions.toml`.
5. **AI is Assistive, Not Authoritative** — AI diagnosis suggestions are always presented as suggestions; the surgeon makes the final clinical decision.

---

*KMM Advantage: Single shared codebase for UI (Compose Multiplatform), business logic, networking, and local storage across Android, iOS, and Web. Native performance, direct Play Store and App Store distribution.*
