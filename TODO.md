# Autocomplete Implementation — TODO & Decisions

Status: Defaults confirmed. Ready to implement.

## Context

- Use MedService semantic autocomplete API directly from mobile/web clients.
- Default: `semantic_expansion: false`.
- TUI-based `field_types` not ready yet; use `"all"` now and keep mapping forward-compatible.
- Assume rate limit: **120 requests/minute**.
- API is currently unauthenticated.

---

## Decisions Needed

### API Contract & Behavior

- [x] **Base URL per platform** — Confirmed: `https://med-api.primeworld.tech/api/v1/` for Android, iOS, JS, Wasm.
- [x] **`field_types` handling** — Confirmed: send `"all"` explicitly now; keep parameter configurable for future TUI mapping.
- [x] **`semantic_expansion`** — Confirmed: explicitly send `false` on every request.
- [x] **Minimum query length** — Confirmed: **2 characters**.
- [x] **Rate-limit (429) behavior** — Confirmed: fail silently; optional client-side throttling.
- [x] **Response shape stability** — Confirmed: use `results[].term`; handle missing fields defensively.

### UI / UX Behavior

- [x] **Suggestion limit** — Confirmed: **5 suggestions** for all fields.
- [x] **Suggestion replacement** — Confirmed: **replace full text** on selection (single-value fields).
- [x] **Multi-value fields** — Confirmed: keep **single-value** for the first implementation; multi-value comma support can be added later.
- [x] **Inline completion** — Confirmed: show the most probable suggestion as inline ghost text (non-typed letters in grey) while typing.
- [x] **Highlighting** — Confirmed: highlight matched prefix and sort prefix matches first (using `match_type` / `score`).
- [x] **No-match state** — Confirmed: **hide dropdown** when no suggestions.
- [x] **Error / offline** — Confirmed: **fail silently**.
- [x] **Debounce delay** — Confirmed: **300 ms**.

### Architecture & Integration

- [x] **Direct client call** — Confirmed: call MedService directly from mobile/web, not through FastAPI backend.
- [x] **Separate base URL provider** — Confirmed: add `MedServiceBaseUrlProvider` alongside existing `BaseUrlProvider`.
- [x] **Client-side caching** — Confirmed: add in-memory LRU cache for recent autocomplete queries (last 50).
- [x] **First fields to implement** — Confirmed: Diagnosis, Procedure, Chief Complaint, Investigation Type, Medication Name.
- [x] **Component scope** — Confirmed: build generic `MedicalAutoCompleteTextField`, then swap into existing screens.

### Testing

- [x] **Dev/staging endpoint** — Confirmed: use production `https://med-api.primeworld.tech/api/v1/` for now; switch when a staging URL is provided.
- [x] **Corpus sanity check** — Confirmed: run sample queries against the live API before wiring into UI.

---

## Recommended Defaults

| Decision | Recommended Default |
|---|---|
| Base URL | `https://med-api.primeworld.tech/api/v1/` on all platforms |
| `field_types` | `"all"` for now; make parameter forward-compatible |
| `semantic_expansion` | Explicitly `false` |
| `limit` | 5 |
| Min query length | 2 characters |
| Debounce | 300 ms |
| Inline completion | Show top suggestion as grey ghost text while typing |
| Rate limit (429) | Fail silently + optional client-side throttling |
| Offline / error | Fail silently |
| Caching | In-memory LRU, last 50 queries |
| First fields | Diagnosis, Procedure, Chief Complaint, Investigation Type, Medication Name |
| Component approach | Generic reusable component, then swap fields |

---

## Implementation Tasks (after confirmation)

- [x] Add `MedServiceBaseUrlProvider` with `expect/actual` implementations.
- [x] Create `MedicalTermRepository` with Ktor POST to `/autocomplete`.
- [x] Create `MedicalAutoCompleteTextField` component with debounce and dropdown.
- [x] Add client-side LRU cache for autocomplete responses.
- [x] Swap component into first pilot fields.
- [x] Handle 429/network errors gracefully.
- [x] Add manual sanity checklist.

## Manual Sanity Checklist

- [ ] Android: `./gradlew :androidApp:assembleDebug` builds.
- [ ] iOS: Xcode build for iPhone 17 Pro simulator succeeds.
- [ ] Type `vesico` in Chief Complaint → see grey ghost text and ≤5 suggestions.
- [ ] Type `paracet` in Medication Name → see drug-name suggestions.
- [ ] Tap a suggestion → full text replaced, dropdown closes.
- [ ] Disconnect network → type in field → no crash, no error dialog.
- [ ] Repeat same query quickly → served from cache, no extra network call.
