# Noni Tura — Timeline to Alpha Testing

**Prepared:** 12 June 2026  
**Target Alpha Date:** 31 July 2026 (7 weeks)  
**Scope:** KMM Android/iOS/Web + FastAPI backend, including Surgeon, Parent, Nurse, AI Diagnosis, and Consent Form modules.

---

## 1. What "Alpha" Means for This Project

An **Alpha build** is a feature-complete, internally testable release that:

- Has all core user flows implemented end-to-end (surgeon, nurse, parent).
- Runs on real Android/iOS devices and web browsers.
- Uses the production-like backend and real notification services.
- Is distributed to a small internal test group (the 2 surgeons, a few parents, staff nurses).
- May still contain bugs, but no known blockers in the critical path.

Alpha does **not** include public store release, full compliance certification, or WhatsApp template guarantees (those are Beta/Launch gates).

---

## 2. Current Snapshot (from `Agent config/todo.md`)

### Backend — Mostly Complete
- OTP auth with surgeon/nurse/parent role detection ✅
- Database schema (doctors, patients, nurses, OPD, IPD, appointments, consent, AI logs) ✅
- OPD, admission, pre-op/intra-op/post-op, ward round, discharge endpoints ✅
- AI diagnosis suggest endpoint ✅
- Consent form PDF generation backend ✅
- Appointment booking backend ✅

### KMM UI — Significant Work Remaining
- Surgeon dashboard, patient list, OPD form, patient profile ✅
- Auth screens in progress / partially done
- WhatsApp preview, follow-up cron, template approvals pending
- Surgical templates manager UI pending
- Schedule screen (S11) pending
- Parent home, surgery status, OPD records, booking flows pending
- Surgeon & parent profiles pending
- Nurse dashboard, nurse OPD form, nurse schedule pending
- AI diagnosis panel in OPD form pending
- Consent form digital signature + viewer pending

---

## 3. Proposed Timeline to Alpha

### Week 1 — 15 Jun → 21 Jun 2026
**Theme: Finish foundation + surgeon core flows**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Complete Task 1.4 KMM Auth UI (Login/Verify + role routing) | Dev 1 | Auth works for surgeon/nurse/parent on Android, iOS, Web |
| Mon–Wed | Task 3.3 Follow-up reminder cron + push | Dev 2 | Daily 9 AM IST cron with idempotency |
| Wed–Fri | Task 3.2 WhatsApp Preview Screen (S12) | Dev 1 | Preview + edit + send WA/SMS fallback |
| Wed–Fri | Task 3.4 Nurse notification preferences & events | Dev 3 | Nurse gets relevant push events |
| Fri–Sun | Task 4.3 Surgical Templates CRUD backend + KMM UI | Dev 2 / Dev 1 | Templates can be saved, loaded in pre-op/intra-op |

**Milestone:** Surgeon OPD → admission → discharge path is UI-complete and notification-enabled.

---

### Week 2 — 22 Jun → 28 Jun 2026
**Theme: IPD completion + schedule + AI panel**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Task 4.9 Consent Form UI (signature + PDF view) | Dev 1 | Digital signature capture, consent stored in Cloudinary |
| Mon–Wed | Task 5.1 Schedule Screen (S11 OT & OPD) | Dev 2 | Week strip, slot cards, booking creation |
| Wed–Thu | Task AI.4 KMM AI Diagnosis Panel in OPD form | Dev 3 | Collapsible suggestion panel, audit logging |
| Thu–Fri | Task 5.2 Nurse schedule view & management | Dev 2 | Nurse can view/create schedule entries |
| Fri–Sun | Integration: surgeon full flow regression on Android | All | Zero broken flows in surgeon journey |

**Milestone:** Surgeon module is internally testable; schedule and AI panel integrated.

---

### Week 3 — 29 Jun → 5 Jul 2026
**Theme: Parent flows**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Task 6.1 Parent Home (P3) | Dev 1 | Live surgery status card, appointments, consults |
| Tue–Wed | Task 6.2 Surgery Status Detail (P4) | Dev 1 | 5-stage timeline with amber pulse animation |
| Wed–Thu | Task 6.3 OPD Records List + Detail (P5, P6) | Dev 2 | PDF download, WA share, read-only cards |
| Thu–Fri | Task 6.4 Consent form view/download in Documents tab | Dev 2 | Parent can view/download signed consent |
| Fri–Sun | Parent flow integration on iOS + Android | All | Parent journey works end-to-end |

**Milestone:** Parent module is internally testable; real-time updates verified.

---

### Week 4 — 6 Jul → 12 Jul 2026
**Theme: Appointment booking + profiles + nurse module**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Task 7.1 3-Step Booking Flow KMM UI (P7–P10) | Dev 1 | Slot selection, confirmation, success animation |
| Tue–Wed | Task 8.1 Surgeon Profile (S13) + Task 9.1 Parent Profile (P11) | Dev 2 | Stats, toggles, dialer, logout |
| Wed–Thu | Task 9.2 Consent form history in parent profile | Dev 2 | Historical consent list |
| Thu–Fri | Task NM.3–NM.8 Nurse Dashboard, OPD Form, Patient List, Clinical Notes, Appointment Manager, Schedule | Dev 3 | Nurse role screens ready |
| Fri–Sun | Nurse module integration + permission tests | All | Nurse cannot breach doctor pool isolation |

**Milestone:** All 30 screens exist and are navigable; role-based access enforced.

---

### Week 5 — 13 Jul → 19 Jul 2026
**Theme: QA, security, notifications hardening**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Task 10.4 Doctor Pool Isolation Security Audit | Dev 2 | All endpoints return 403 on cross-doctor access |
| Tue–Wed | Push notification reliability pass (FCM + APNs) | Dev 3 | 95%+ delivery on real devices |
| Wed–Thu | WhatsApp template submission follow-up (Task 10.5) | Dev 1 | All 5 templates approved or fallback SMS configured |
| Thu–Fri | Bug bash: surgeon + parent + nurse flows | All | P0/P1 bugs triaged and assigned |
| Fri–Sun | Performance pass: LazyColumn scroll, PDF generation, image upload | Dev 1 / Dev 3 | No UI jank; uploads < 5s on 3G |

**Milestone:** Security audit passed; notification delivery verified; critical bugs fixed.

---

### Week 6 — 20 Jul → 26 Jul 2026
**Theme: Alpha build preparation**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Task 10.1 Android signed APK/AAB + Play Console Internal Testing | Dev 1 | Internal test link ready |
| Tue–Wed | Task 10.2 iOS TestFlight build | Dev 2 | TestFlight invite ready |
| Wed–Thu | Task 10.3 Web Wasm deploy to Vercel (staging) | Dev 3 | Staging URL live with SPA routing |
| Thu–Fri | Task 10.8 Production backend deploy + Sentry + monitoring | Dev 3 | Render prod service live, Sentry crash reporting on |
| Fri–Sun | E2E smoke tests on all 3 platforms | All | Surgeon, parent, nurse smoke tests pass |

**Milestone:** Alpha binaries available on Android, iOS, and Web staging.

---

### Week 7 — 27 Jul → 2 Aug 2026
**Theme: Alpha release + feedback collection**

| Days | Task | Owner | Deliverable |
|------|------|-------|-------------|
| Mon–Tue | Distribute Alpha to internal testers (surgeons, nurses, 5–10 parents) | All | Testers onboarded with instructions |
| Tue–Thu | Monitor crashes, notifications, WA delivery | Dev 3 | Sentry crash-free rate > 98% |
| Thu–Fri | Collect structured feedback (form + calls) | PM / All | Feedback sheet with severity |
| Fri–Sat | Triage Alpha feedback into Beta backlog | All | Beta sprint plan ready |
| Sun | **Alpha Gate Review** | All | Go/No-Go decision for Beta |

**Milestone:** Alpha testing is underway; feedback captured; Beta scope defined.

---

## 4. Key Milestones

| Milestone | Target Date | Definition of Done |
|-----------|-------------|-------------------|
| Surgeon module complete | 28 Jun 2026 | All 13 surgeon screens functional; OPD→IPD→discharge flow tested |
| Parent module complete | 5 Jul 2026 | All 11 parent screens functional; booking + status tracking tested |
| Nurse module complete | 12 Jul 2026 | All 6 nurse screens functional; permissions enforced |
| Security & notifications hardened | 19 Jul 2026 | Pool isolation audit passed; notifications 95%+ reliable |
| Alpha builds ready | 26 Jul 2026 | Android internal, TestFlight, web staging deployed |
| Alpha release | 31 Jul 2026 | Internal testers active; feedback collected |

---

## 5. Critical Path / Blockers

1. **WhatsApp Template Approval (Meta)** — External dependency. If delayed, use SMS fallback for Alpha.
2. **Apple Developer Account + Provisioning** — Must be active by Week 6 to avoid TestFlight delay.
3. **Google Play Console Account** — One-time $25 fee; required for internal testing.
4. **2Factor.in / MSG91 + Meta WA Phone ID** — Must be provisioned and funded.
5. **Physical iOS test device** — Required for push notification validation (simulator cannot receive APNs).

---

## 6. Resource Assumptions

- **Team:** 3 developers full-time.
- **Client availability:** Surgeons available for 2 feedback sessions/week during Weeks 5–7.
- **Third-party approvals:** WhatsApp templates submitted by end of Week 1.
- **Compliance:** Alpha does not require final MCI/NABH audit; consent forms follow draft standards.

---

## 7. Definition of Alpha Success

Alpha is considered successful when:

- [ ] Both surgeons can log in and complete a full patient journey on their phones.
- [ ] A parent can book an appointment and see live surgery status updates.
- [ ] A nurse can log in, view patients, and create OPD notes.
- [ ] Push notifications arrive on both Android and iOS for all 5 trigger events.
- [ ] WhatsApp or SMS fallback delivers for all 5 trigger events.
- [ ] No cross-doctor or cross-patient data leaks are found.
- [ ] Crash-free rate on Sentry is ≥ 98% over 3 days of active testing.
- [ ] Feedback from ≥ 10 testers is collected and triaged.

---

## 8. After Alpha

- **Week 8–9:** Beta sprint — fix Alpha bugs, polish UX, complete compliance audit.
- **Week 10:** Closed Beta release via Play Store Internal + TestFlight External.
- **Week 11–12:** Public launch preparation (store listings, privacy policy, final submissions).

---

*Timeline owner: Engineering Lead*  
*Next review: 22 Jun 2026 (end of Week 1)*
