# Nori Tura — Open Tasks

## Video upload & viewing (Option 1)

Status: Implemented and build-validated.

### Goal
Upload videos from IPD note forms, store them correctly, show a video chip / play button in detail screens, and play the video via the system player or a simple fullscreen player. Keep the existing `/media/{id}` auth contract.

---

### Backend

- [x] **Type-aware upload response**
  - File: `backend/app/routers/uploads.py`
  - Change `POST /uploads/media` from returning `List[str]` to returning a list of objects:
    ```json
    {
      "urls": [
        { "id": "...", "url": "/media/...", "mime_type": "video/mp4", "filename": "..." }
      ]
    }
    ```
  - Update `MediaUploadResponse` Pydantic model accordingly.
  - The backend already records `mime_type` on the `media` row; just expose it in the response.

- [x] **Extend IPD note schema with `video_urls`**
  - File: `backend/prisma/schema.prisma`
  - Add `video_urls String[]` to:
    - `pre_op_notes`
    - `post_op_notes`
    - `ward_round_notes`
    - `discharge_summaries`
  - `intra_op_notes` already has `video_urls`; leave as-is.

- [x] **Update IPD router to persist `video_urls`**
  - File: `backend/app/routers/ipd.py`
  - Add `video_urls: Optional[List[str]] = None` to create/update request models for the four note types above.
  - Persist `video_urls` in each create/update handler (same pattern as `image_urls`).
  - Regenerate the Prisma client / run a migration after the schema change.

- [x] **Verify video retrieval auth still works**
  - `GET /media/{id}` and `POST /media/{id}/access` already proxy/presign any mime type; confirm with a test video.

---

### Shared (KMP)

- [x] **Update upload client contract**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/ApiClient.kt`
  - Create a data class like `UploadedMedia(val url: String, val mimeType: String, val filename: String)`.
  - Change `uploadMedia` to return `Result<List<UploadedMedia>>`.
  - Update every caller to consume the new shape:
    - `ImageAttachmentPicker`
    - `MedicalImagePicker`
    - Any direct `uploadMedia` calls in OPD/IPD flows.

- [x] **Fix media classification in the picker**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/UrlImage.kt`
  - Revert `isImageUrl()` so it does **not** treat `/media/{id}` as an image by default.
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/ImageAttachmentPicker.kt`
  - Track mime type per URL (or use the new `UploadedMedia` list directly).
  - Split the attached list into image thumbnails vs. video/other chips based on `mimeType.startsWith("video/")`.
  - Keep images in the existing thumbnail grid; show videos as `MediaUrlChip`s.

- [x] **Fix `MediaUrlChip` for videos**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/MediaUrlChip.kt`
  - Accept an optional `mimeType` / `isVideo` flag.
  - Show `Icons.Default.VideoLibrary` for `video/*`.
  - Resolve presigned URL via `MediaAccessRepository` for `/media/{id}` paths.
  - For video, call `openUrl(presignedUrl)` to play in the system video player.
  - Ensure `FullscreenImageViewer` is never opened for video URLs.

- [x] **Extend IPD note DTOs with `videoUrls`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/dto/AdmissionDto.kt`
  - Add `@SerialName("video_urls") val videoUrls: List<String>? = null` to:
    - `PreOpNoteDto`
    - `PostOpNoteDto`
    - `WardRoundNoteDto`
    - `DischargeSummaryDto`
  - Add `videoUrls: List<String> = emptyList()` to:
    - `PreOpNoteCreateRequest`
    - `PostOpNoteCreateRequest`
    - `WardRoundNoteCreateRequest`
    - `DischargeSummaryCreateRequest`

- [x] **Update IPD detail screen to show videos**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/AdmissionDetailScreen.kt`
  - Update `NoteCard` to accept a `videoUrls` parameter.
  - For each note type, pass the appropriate `videoUrls` list into `NoteCard`.
  - Inside `NoteCard`, render images with `AuthenticatedUrlImageRow` and videos with a `VideoUrlChipGrid` (reusing `MediaUrlChip`).

- [x] **Add video attachment to IPD note forms**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/AdmissionDetailScreen.kt`
  - In `PreOpForm`, `PostOpForm`, `WardRoundForm`, and `DischargeForm`:
    - Add a `videoUrls` state.
    - Add an `ImageAttachmentPicker(allowVideo = true)` bound to video URLs (or a dedicated video picker if you prefer).
    - Include `videoUrls` in the create request sent to the ViewModel.
  - `IntraOpForm` already has `allowVideo = true`; update it to put videos into `videoUrls` instead of `imageUrls`.

---

### Validation checklist

- [x] Upload a video in an **IntraOp** note → stored in `video_urls`.
- [x] Upload a video in **PreOp / PostOp / WardRound / Discharge** notes → stored in the new `video_urls` columns.
- [x] Detail screen shows a video chip with a play icon.
- [x] Tapping the video chip resolves a presigned URL and opens/plays it.
- [x] Images still upload and display correctly.
- [x] `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64` succeeds.
- [x] `./gradlew :androidApp:assembleDebug` succeeds.

---

## Other open items

- [x] **Manual standalone medical-record creation UI**
  - OPD consult images now create medical records automatically, but a dedicated screen to create a `medical_records` row and attach images manually is still deferred.

- [x] **Android emulator MinIO endpoint**
  - When testing on the Android emulator, set `MINIO_PUBLIC_ENDPOINT=10.0.2.2:9100` in `backend/.env` so presigned URLs are reachable from the emulator.


---

# Consent Form Template Autofill (Approach A)

Status: Implemented and build-validated.

## Decisions

- Reuse existing **surgical templates** (`surgical_templates` table + `/surgical-templates` endpoints).
- Search by **template name** and **procedure name** (case-insensitive substring match).
- Allow a **one-off custom form** — no template is saved; surgeons can edit fields after autofill.
- Templates are **shared across the hospital/clinic** (not restricted to the logged-in doctor).
- Autofill is **client-side** from the selected `SurgicalTemplateDto`.

## Field mapping from surgical template → consent form

| Surgical Template Field | Consent Form Field | Notes |
|---|---|---|
| `name` | `formType` | e.g. "Lap Appendectomy Template" |
| `procedure` | `procedure` | |
| `anaesthesia` (list) | `anesthesia` | joined with ", " |
| `approach` | `procedureDescription` | prefixed "Approach: ..." |
| `technique` | `procedureDescription` | prefixed "Technique: ..." |
| `special_instructions` | `procedureDescription` | appended as-is |
| `risk_level` | *(ignored for now)* | Not enough detail to map safely; surgeon fills risks |
| `investigations` | *(ignored for now)* | Not used in consent form |
| All other consent fields | *(remain blank/defaults)* | diagnosis, benefits, alternatives, postOpCare, expectedRecovery, hospital/doctor/guardian, checkboxes |

## Tasks

### Backend

- [x] **Make `GET /surgical-templates` hospital-wide and nurse-visible**
  - File: `backend/app/routers/surgical_templates.py`
  - Change dependency from `get_current_surgeon` to `get_current_nurse_or_surgeon` for the list endpoint only.
  - Resolve the current user's doctor record to obtain `hospital_id`.
  - Query templates where `doctor.hospital_id == current_user_hospital_id`.
  - Leave create/update/delete endpoints surgeon-only.

### Shared (KMP)

- [x] **Add search + "Custom form" option to `TemplatePickerDialog`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/TemplatePickerDialog.kt`
  - Add an `OutlinedTextField` for search query.
  - Filter the list by `template.name.contains(query, ignoreCase = true)` or `template.procedure.contains(query, ignoreCase = true)`.
  - Add a top row "— Custom / Blank form —" that returns `null` (or a sentinel) so the caller can clear autofill.

- [x] **Fetch surgical templates in `ConsentFormViewModel`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormViewModel.kt`
  - Inject `SurgicalTemplateRepository`.
  - Load templates on init and expose a `templates: StateFlow<List<SurgicalTemplateDto>>`.
  - Add a helper function `applyTemplate(template: SurgicalTemplateDto?)` that returns a bundle of prefilled field values (or `null` for custom/blank).

- [x] **Wire template selector into `ConsentFormScreen`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormScreen.kt`
  - At the top of the form, add a card/row showing:
    - Selected template name, or
    - "Custom form" if no template selected.
  - Tapping it opens the searchable `TemplatePickerDialog`.
  - On template selection, set all form states using the mapping above.
  - On "Custom form" selection, reset only the fields that would have been autofilled (or leave them blank if already blank).
  - Keep all fields editable after autofill.

### Validation checklist

- [x] Create a surgical template in the app (Surgeon dashboard → Surgical Templates).
- [x] Open Admission → Add Consent Form.
- [x] Tap template selector; type in search box and verify filtering by name/procedure.
- [x] Select a template → `formType`, `procedure`, `anesthesia`, and `procedureDescription` are prefilled.
- [x] Edit prefilled fields and submit → consent form creates successfully.
- [x] Select "Custom form" → prefilled fields are cleared/blank.
- [x] Nurse login can also see templates in the consent form screen.
- [x] `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64` succeeds.


---

# Parent-Uploaded History (PDF + Images)

Status: Implemented and build-validated.

## Decisions

- The existing `documents` table and `/documents` endpoints will store parent-uploaded history.
- Doctor profile section name: **"Parent Uploaded History"**.
- Doctor sees **only** documents where `uploaded_by_role = "parent"`.
- Images render as a **thumbnail grid**; PDFs render as document chips.
- Parent can **delete** their uploaded history records after saving.

## Tasks

### Backend

- [x] **Add document delete endpoint**
  - File: `backend/app/routers/documents.py`
  - Add `DELETE /documents/{document_id}`.
  - Verify the caller is either:
    - the parent of the linked patient (`patient.parent_phone == user.phone`), or
    - the treating doctor (`patient.doctor_id == doctor_id`).
  - Delete the `documents` row.
  - *(Optional future cleanup: also remove the linked `media` object from MinIO/Postgres.)*

### Shared (KMP)

- [x] **Add delete call to `DocumentsRepository`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/DocumentsRepository.kt`
  - Add `suspend fun deleteDocument(id: String): Result<Unit>`.

- [x] **Add image upload to parent profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt`
  - Add an `ImageAttachmentPicker` for images alongside the existing `PdfAttachmentPicker`.
  - On "Save Records", create a `DocumentCreateRequest` for each uploaded image:
    - `type = "image"`
    - `category = "previous_health_record"`
    - `uploadedByRole = "parent"`
  - PDF creation stays unchanged (`type = "pdf"`).

- [x] **Fix saved-document viewing in parent profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt`
  - Replace the raw `openUrl(document.url)` in `HealthRecordCard` with `MediaUrlChip(url = document.url)`.
  - `MediaUrlChip` handles both images and PDFs, resolves `/media/{id}` presigned URLs, and opens the correct viewer.

- [x] **Allow parent to delete uploaded history**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileViewModel.kt`
  - Add `deleteDocument(documentId: String)` that calls `DocumentsRepository.deleteDocument` and reloads the profile.
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt`
  - Add a delete button to `HealthRecordCard` (only when `uploadedByRole == "parent"`).

- [x] **Load parent documents in doctor patient profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/surgeon/PatientProfileViewModel.kt`
  - Inject `DocumentsRepository`.
  - Load documents for the patient and expose only `uploadedByRole == "parent"`.

- [x] **Add "Parent Uploaded History" section to doctor profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/surgeon/PatientProfileScreen.kt`
  - Add a new section titled **"Parent Uploaded History"**.
  - For each document:
    - If `type == "image"` → render `AuthenticatedUrlImage` in a thumbnail grid (e.g. `LazyRow` of thumbnails that open fullscreen on tap).
    - If `type == "pdf"` → render `MediaUrlChip`.
  - Show an empty message when there are no parent-uploaded documents.

### Validation checklist

- [x] Parent: upload a PDF → saved → tap to view via presigned URL.
- [x] Parent: upload an image → saved → tap to view via presigned URL.
- [x] Parent: delete an uploaded document → document disappears and backend row is removed.
- [x] Doctor: open patient profile → see "Parent Uploaded History" section with the PDF chip and image thumbnail.
- [x] Doctor: tap PDF chip → opens/plays; tap image thumbnail → fullscreen viewer.
- [x] `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64` succeeds.

---

# Post-implementation verification fixes

Status: Applied and validated.

## 1. `POST /documents` create handler

- **File:** `backend/app/routers/documents.py`
- **Issue:** `prisma.documents.create` failed with `data.patient: A value is required but not set` and `data.recorded_at: A value is required but not set` because the generated Prisma client requires relations via `connect` and passing `recorded_at: None` bypassed the `@default(now())`.
- **Fix:** Use `patient: { connect: { id } }`, `doctor: { connect: { id } }`, and optional `hospital: { connect: { id } }`; only include `recorded_at` when the client provides one.

## 2. Upload MIME-type fallback

- **File:** `backend/app/routers/uploads.py`
- **Issue:** Ktor multipart uploads send `application/octet-stream` as the part content type for raw byte arrays, so `.mp4`/`.pdf`/`.jpg` files were stored with the wrong `mime_type`.
- **Fix:** When the client content type is missing or `application/octet-stream`, fall back to `mimetypes.guess_type(filename)`. Verified `.mp4` → `video/mp4`, `.jpg` → `image/jpeg`, `.pdf` → `application/pdf`.

## 3. `DELETE /documents/{document_id}`

- **File:** `backend/app/routers/documents.py`
- **Issue:** Earlier smoke test returned 307 because the create step failed and the delete URL had an empty `document_id`.
- **Fix:** After fixing the create handler, delete returns `204 No Content` and the document is removed.

---

# Medical Autocomplete Local Cache + Telemetry

Status: Spec complete; implementation pending.

**Reference docs:**
- `docs/autocomplete-cache-telemetry-analysis.md` — feasibility, architecture, risks
- `docs/medservice-autocomplete-telemetry-spec.md` — exact payload contract for MedService

## Phase 1 — Local per-field selection cache

Goal: remember terms the user selects per field type and boost them in future suggestions.

- [ ] **Add `AutocompleteSelectionCache`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/AutocompleteSelectionCache.kt`
  - Backed by `com.russhwolf.settings.Settings`.
  - Store per-field term list with `count` and `lastUsedAt`.
  - Prune to top-K entries per field or entries older than 90 days.

- [ ] **Record selections in `MedicalAutoCompleteTextField`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/MedicalAutoCompleteTextField.kt`
  - On dropdown item click, call `AutocompleteSelectionCache.recordSelection(fieldType, term)`.

- [ ] **Merge cached terms into suggestions**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/MedicalAutoCompleteTextField.kt`
  - Read cache for the current `fieldType`.
  - Boost matching cached terms to the top of the dropdown; fill remaining slots with server results.
  - De-duplicate so the same term is not shown twice.
  - If server call fails, still show cached suggestions as offline fallback.

## Phase 2 — Telemetry to MedService

Goal: send anonymised selection data to MedService so it can improve autocomplete ranking.

- [ ] **Mobile telemetry buffer**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/AutocompleteTelemetryRepository.kt`
  - Buffer selection events in memory + persistent queue.
  - Event fields: `field_type`, `query`, `selected_term`, `suggestion_position`, `match_type`, `score`, `screen`, `timestamp`.
  - Flush when buffer reaches 20–50 events, app goes to background, or every 6–24 hours.

- [ ] **Nori-Tura backend ingestion endpoint**
  - File: `backend/app/routers/analytics.py` (new)
  - `POST /analytics/autocomplete` — accept batched events with JWT auth.
  - Validate and store raw events; aggregate into per-field histograms and query-to-selection maps.

- [ ] **Nori-Tura → MedService forwarding**
  - Run a periodic job (e.g. nightly) that sends one batch per hospital to:
    `POST https://med-api.primeworld.tech/api/v1/autocomplete/feedback`
  - Auth: `X-API-Key` provided by MedService.
  - Payload includes `histograms`, `query_selections`, and optional `sampled_events`.
  - See `docs/medservice-autocomplete-telemetry-spec.md` for exact schema.

- [ ] **MedService ranking integration**
  - MedService ingests feedback and updates ranking.
  - Short term: popularity boost `final_score = base_score * (1 + log(selection_count + 1) * field_weight)`.
  - Medium term: build query-to-term learning index.
  - Long term: field-specific reranker trained on sampled raw events.

### Privacy notes

- Do not send patient identifiers, visit IDs, or free-text notes.
- Send only selected suggestion terms, field context, and short typed prefixes.
- `hospital_id` may be included for per-hospital ranking if the hospital consents; otherwise hash or omit it.
- Consider an opt-out toggle in app settings.
