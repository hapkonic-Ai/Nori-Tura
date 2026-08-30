# Autocomplete Local Cache + Telemetry Analysis

This document analyses the feasibility of adding a **persistent, per-field local cache** for selected autocomplete terms, tracking how often each term is chosen per field, and sending that usage data back to the MedService (or via the Nori-Tura backend) so the autocomplete ranking can improve over time.

---

## 1. Goal

1. When a user types in a clinical field (e.g. "Approach", "Anaesthesia"), the app should remember terms they previously selected for that field.
2. Those locally-cached terms should appear higher in future suggestions for the same field, even offline.
3. The app should periodically send anonymised selection histograms to the server as telemetry.
4. The server / MedService should use that telemetry to boost frequently-selected terms and train field-specific ranking.

---

## 2. Current State

- `MedicalTermRepository` keeps a small **in-memory LRU cache** of raw server responses keyed by `query|fieldTypes|limit`. It is lost when the app restarts and does not know which suggestion the user actually picked.
- `MedicalAutoCompleteTextField` shows up to 5 suggestions and replaces the current token when the user taps one. It does not currently emit any "selection" event.
- `com.russhwolf.settings.Settings` is already used for auth token, role, etc., so persistent key-value storage is available on Android/iOS/Web.
- The MedService `/autocomplete` endpoint accepts `query`, `field_types`, `limit`, `fuzzy`, `semantic_expansion` and returns `results` with `term`, `score`, `match_type`. It does not yet accept any user/clinic-specific weighting signal.

---

## 3. Feasibility: Local Per-Field Selection Cache

### 3.1 What to store

For each field type (e.g. `"procedure"`, `"approach"`, `"anaesthesia"`, `"diagnosis"`), store a list of terms the user has selected, plus metadata:

```json
{
  "approach": [
    { "term": "Laparoscopic", "count": 12, "lastUsedAt": 1693372800000 },
    { "term": "Open", "count": 4, "lastUsedAt": 1693286400000 }
  ],
  "anaesthesia": [
    { "term": "General anaesthesia", "count": 18, "lastUsedAt": 1693372800000 },
    { "term": "Spinal anaesthesia", "count": 3, "lastUsedAt": 1693113600000 }
  ]
}
```

### 3.2 Storage options

| Option | Pros | Cons |
|---|---|---|
| `Settings` (multiplatform key-value) | Already in the project; trivial to read/write | Best for small JSON blobs; not a relational store |
| SQLite (via SQLDelight/ROOM) | Structured, queryable, scalable | Adds dependency and migration complexity |
| In-memory only | Simple | Lost on app restart, no offline benefit |

**Recommendation:** Use `Settings` with a single key `autocomplete_selection_cache` storing a JSON string. The dataset is small (a few hundred terms at most), so `Settings` is sufficient and keeps the change lightweight.

### 3.3 When to update the cache

- On every dropdown selection in `MedicalAutoCompleteTextField`:
  - Record the `fieldType` of the field.
  - Increment `count` for the selected term (or insert if new).
  - Update `lastUsedAt`.
- Prune entries older than N days or keep only top-K per field to bound storage.

### 3.4 How to use the cache at query time

1. User types token `lap` in the "Approach" field.
2. Call the server as usual: `POST /autocomplete { query: "lap", field_types: "approach", ... }`.
3. Also read the local cache for `"approach"`.
4. Merge and re-rank:
   - Local cached terms matching the token are boosted to the top.
   - Server suggestions fill the remaining slots.
   - De-duplicate so a cached term is not shown twice.

This gives **offline resilience**: if the server call fails, the app can still show cached suggestions for that field.

### 3.5 Edge cases

- **Comma-separated fields** (e.g. "Anaesthesia (comma-separated)"): the cache should still work because `MedicalAutoCompleteTextField` operates on the current token only.
- **Field type is "all"**: cache under a generic `"all"` bucket, or skip caching when no field context is available.
- **Multi-word selections**: store the full selected phrase, e.g. "General anaesthesia".
- **User clears app data**: cache is cleared with other `Settings`.

---

## 4. Feasibility: Telemetry / Diagnostics

### 4.1 What to send

Anonymised, aggregated selection data:

```json
{
  "device_install_id": "anon-uuid-or-null",
  "app_version": "1.2.0",
  "platform": "android",
  "sent_at": "2026-08-30T10:00:00Z",
  "events": [
    {
      "field_type": "approach",
      "selected_term": "Laparoscopic",
      "query": "lap",
      "suggestion_position": 1,
      "timestamp": "2026-08-30T09:58:00Z"
    },
    {
      "field_type": "anaesthesia",
      "selected_term": "General anaesthesia",
      "query": "gen",
      "suggestion_position": 0,
      "timestamp": "2026-08-30T09:59:00Z"
    }
  ],
  "histograms": {
    "approach": { "Laparoscopic": 12, "Open": 4 },
    "anaesthesia": { "General anaesthesia": 18, "Spinal anaesthesia": 3 }
  }
}
```

`device_install_id` should be a stable, non-PII identifier (e.g. random UUID generated on first install). No patient IDs, names, or clinical content other than the selected term itself should be included.

### 4.2 Where to send it

Two options:

| Option | Pros | Cons |
|---|---|---|
| **A. Direct to MedService** (`/autocomplete/telemetry` or similar) | MedService owns ranking; gets data immediately | Adds auth/rate-limit complexity on the public MedService |
| **B. Via Nori-Tura backend** (`POST /analytics/autocomplete`) | Reuses existing JWT/auth, easier rate limiting, can aggregate/forward to MedService | Requires backend work and a forwarding contract |

**Recommendation:** Start with **Option B** — send to the Nori-Tura backend. The backend already authenticates the mobile client, can validate/aggregate the payload, and can forward batch summaries to MedService on a schedule. This keeps the public autocomplete endpoint simple.

### 4.3 When to send

- Batching is essential to avoid draining battery and hitting rate limits.
- Options:
  - When the app goes to background.
  - Every N selections (e.g. 20).
  - Once per day.
  - On explicit "sync" if needed.
- On failure, store locally and retry next time.

### 4.4 Privacy & compliance

- The data is **technical/diagnostic**, not patient-identifiable.
- Still avoid sending free-text typed by the user unless it is the selected suggestion.
- Consider making telemetry opt-out in settings.
- If sending selected clinical terms feels sensitive, send only **histogram buckets** and no per-event timestamps/queries.

---

## 5. How MedService Can Use the Data

The MedService (or an intermediate aggregator) can improve autocomplete in several ways:

### 5.1 Per-field popularity boosting

- Maintain a global counter: `term -> field -> selection_count`.
- When building the suggestion list for a query, multiply the base score by a popularity factor.
- Example: if 80% of surgeons select "Laparoscopic" for approach, it should outrank less common matches even if their prefix score is similar.

### 5.2 Query-to-selection learning

- Learn that typing `lap` in the approach field almost always results in selecting `Laparoscopic`.
- Pre-compute or model `P(selection = term | query, field)`.
- Rank by this probability instead of raw prefix/fuzzy score.

### 5.3 Field-specific embeddings

- Today the API returns `match_type: prefix | fuzzy | semantic`.
- Telemetry can be used to fine-tune field-specific sentence-transformer models or lexical indexes so that, for example, "GA" maps strongly to "General anaesthesia" in the `anaesthesia` field but not elsewhere.

### 5.4 Personalisation (future)

- With per-user or per-clinic telemetry, MedService can personalise suggestions:
  - A paediatric surgery clinic will see different top procedures than a general surgery clinic.
  - A surgeon who frequently does hypospadias repairs will see those terms boosted.

### 5.5 Cold-start and fallback

- For new users, rely on global telemetry.
- For offline users, rely on the local cache.
- For rare terms, rely on the semantic/fuzzy backend.

---

## 6. Required Changes (No Code Written)

### 6.1 Shared (KMP)

1. **New data class** `AutocompleteSelection(term, fieldType, count, lastUsedAt)`.
2. **New repository** `AutocompleteSelectionCache` backed by `Settings`:
   - `recordSelection(fieldType, term)`
   - `getBoostedSuggestions(fieldType, query, serverSuggestions)`
   - `pruneOldEntries()`
3. **Update `MedicalAutoCompleteTextField`**:
   - Inject `AutocompleteSelectionCache`.
   - On dropdown item click, call `recordSelection(fieldType, suggestion)`.
   - Merge local cache into `suggestions` before rendering.
4. **New telemetry repository** `AutocompleteTelemetryRepository`:
   - In-memory event buffer + persistent queue.
   - `logSelection(fieldType, selectedTerm, query, position)`.
   - `flush()` to send batch to backend.
5. **Hook lifecycle / background** to call `flush()` when appropriate.

### 6.2 Backend (Nori-Tura)

1. **New endpoint** `POST /analytics/autocomplete`:
   - Accepts the telemetry payload.
   - Validates JWT.
   - Stores raw events or aggregated histograms.
2. **Periodic job** that forwards aggregated, anonymised histograms to MedService (e.g. nightly).
3. **Optional admin endpoint** to view most-selected terms per field.

### 6.3 MedService

1. **New ingestion endpoint** `POST /api/v1/autocomplete/feedback` or similar:
   - Accepts aggregated histograms.
   - Updates an in-memory or database popularity table.
2. **Ranking update** in `/autocomplete`:
   - Combine prefix/fuzzy/semantic score with popularity / learned probability.
   - Accept an optional `user_context` or `clinic_context` for personalisation.
3. **Privacy guardrails**:
   - Validate that payloads contain no PHI.
   - Aggregate before using in model training.

---

## 7. Implementation Phases

### Phase 1 — Local cache only
- Build `AutocompleteSelectionCache`.
- Record selections in `MedicalAutoCompleteTextField`.
- Merge cached terms into suggestions.
- Benefit: faster repeat entries, offline fallback, no network cost.

### Phase 2 — Telemetry ingestion
- Add `/analytics/autocomplete` on Nori-Tura backend.
- Batch and send from the app.
- Store aggregated histograms.
- Benefit: data collection for future ranking improvements.

### Phase 3 — Ranking improvement
- MedService consumes histograms and boosts popular terms.
- Optionally add per-clinic/per-surgeon personalisation.
- Benefit: better suggestion order, less typing.

---

## 8. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Cache grows unbounded | Prune to top-K per field or entries older than 90 days. |
| Privacy / PHI leakage | Never send patient identifiers; aggregate before sending; consider opt-out. |
| Rate limiting on MedService | Send telemetry to Nori-Tura backend, not directly to MedService. |
| Stale cache dominating | Use a decay factor so old selections lose weight over time. |
| Network/battery drain from telemetry | Batch events and flush on background or every N events. |
| Local cache diverges from server | Keep server as primary source; use cache only for boosting/fallback. |
| Multi-device inconsistency | Telemetry is global; local cache is per-device. Acceptable for UX enhancement. |

---

## 9. Summary Recommendation

**Yes, this is feasible and valuable.**

1. Implement a small `Settings`-backed per-field selection cache in the shared module. It improves speed, works offline, and requires no backend changes.
2. Add lightweight telemetry collection and batch it to the Nori-Tura backend. This gives MedService the data needed to improve ranking without exposing the autocomplete endpoint to extra auth/rate-limit complexity.
3. Keep server suggestions as the primary source; use local cache and telemetry only for boosting and personalisation.

Start with **Phase 1 (local cache)** because it has immediate user benefit and zero backend dependency. Add telemetry (Phase 2) once the backend endpoint is ready.
