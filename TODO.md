# Nori Tura — Open Tasks

## Video upload & viewing (Option 1)

Status: Planned. Not started.

### Goal
Upload videos from IPD note forms, store them correctly, show a video chip / play button in detail screens, and play the video via the system player or a simple fullscreen player. Keep the existing `/media/{id}` auth contract.

---

### Backend

- [ ] **Type-aware upload response**
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

- [ ] **Extend IPD note schema with `video_urls`**
  - File: `backend/prisma/schema.prisma`
  - Add `video_urls String[]` to:
    - `pre_op_notes`
    - `post_op_notes`
    - `ward_round_notes`
    - `discharge_summaries`
  - `intra_op_notes` already has `video_urls`; leave as-is.

- [ ] **Update IPD router to persist `video_urls`**
  - File: `backend/app/routers/ipd.py`
  - Add `video_urls: Optional[List[str]] = None` to create/update request models for the four note types above.
  - Persist `video_urls` in each create/update handler (same pattern as `image_urls`).
  - Regenerate the Prisma client / run a migration after the schema change.

- [ ] **Verify video retrieval auth still works**
  - `GET /media/{id}` and `POST /media/{id}/access` already proxy/presign any mime type; confirm with a test video.

---

### Shared (KMP)

- [ ] **Update upload client contract**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/ApiClient.kt`
  - Create a data class like `UploadedMedia(val url: String, val mimeType: String, val filename: String)`.
  - Change `uploadMedia` to return `Result<List<UploadedMedia>>`.
  - Update every caller to consume the new shape:
    - `ImageAttachmentPicker`
    - `MedicalImagePicker`
    - Any direct `uploadMedia` calls in OPD/IPD flows.

- [ ] **Fix media classification in the picker**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/UrlImage.kt`
  - Revert `isImageUrl()` so it does **not** treat `/media/{id}` as an image by default.
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/ImageAttachmentPicker.kt`
  - Track mime type per URL (or use the new `UploadedMedia` list directly).
  - Split the attached list into image thumbnails vs. video/other chips based on `mimeType.startsWith("video/")`.
  - Keep images in the existing thumbnail grid; show videos as `MediaUrlChip`s.

- [ ] **Fix `MediaUrlChip` for videos**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/MediaUrlChip.kt`
  - Accept an optional `mimeType` / `isVideo` flag.
  - Show `Icons.Default.VideoLibrary` for `video/*`.
  - Resolve presigned URL via `MediaAccessRepository` for `/media/{id}` paths.
  - For video, call `openUrl(presignedUrl)` to play in the system video player.
  - Ensure `FullscreenImageViewer` is never opened for video URLs.

- [ ] **Extend IPD note DTOs with `videoUrls`**
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

- [ ] **Update IPD detail screen to show videos**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/AdmissionDetailScreen.kt`
  - Update `NoteCard` to accept a `videoUrls` parameter.
  - For each note type, pass the appropriate `videoUrls` list into `NoteCard`.
  - Inside `NoteCard`, render images with `AuthenticatedUrlImageRow` and videos with a `VideoUrlChipGrid` (reusing `MediaUrlChip`).

- [ ] **Add video attachment to IPD note forms**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/AdmissionDetailScreen.kt`
  - In `PreOpForm`, `PostOpForm`, `WardRoundForm`, and `DischargeForm`:
    - Add a `videoUrls` state.
    - Add an `ImageAttachmentPicker(allowVideo = true)` bound to video URLs (or a dedicated video picker if you prefer).
    - Include `videoUrls` in the create request sent to the ViewModel.
  - `IntraOpForm` already has `allowVideo = true`; update it to put videos into `videoUrls` instead of `imageUrls`.

---

### Validation checklist

- [ ] Upload a video in an **IntraOp** note → stored in `video_urls`.
- [ ] Upload a video in **PreOp / PostOp / WardRound / Discharge** notes → stored in the new `video_urls` columns.
- [ ] Detail screen shows a video chip with a play icon.
- [ ] Tapping the video chip resolves a presigned URL and opens/plays it.
- [ ] Images still upload and display correctly.
- [ ] `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64` succeeds.
- [ ] `./gradlew :androidApp:assembleDebug` succeeds.

---

## Other open items

- [ ] **Manual standalone medical-record creation UI**
  - OPD consult images now create medical records automatically, but a dedicated screen to create a `medical_records` row and attach images manually is still deferred.

- [ ] **Android emulator MinIO endpoint**
  - When testing on the Android emulator, set `MINIO_PUBLIC_ENDPOINT=10.0.2.2:9100` in `backend/.env` so presigned URLs are reachable from the emulator.


---

# Consent Form Template Autofill (Approach A)

Status: Planned. Not started.

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

- [ ] **Make `GET /surgical-templates` hospital-wide and nurse-visible**
  - File: `backend/app/routers/surgical_templates.py`
  - Change dependency from `get_current_surgeon` to `get_current_nurse_or_surgeon` for the list endpoint only.
  - Resolve the current user's doctor record to obtain `hospital_id`.
  - Query templates where `doctor.hospital_id == current_user_hospital_id`.
  - Leave create/update/delete endpoints surgeon-only.

### Shared (KMP)

- [ ] **Add search + "Custom form" option to `TemplatePickerDialog`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/TemplatePickerDialog.kt`
  - Add an `OutlinedTextField` for search query.
  - Filter the list by `template.name.contains(query, ignoreCase = true)` or `template.procedure.contains(query, ignoreCase = true)`.
  - Add a top row "— Custom / Blank form —" that returns `null` (or a sentinel) so the caller can clear autofill.

- [ ] **Fetch surgical templates in `ConsentFormViewModel`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormViewModel.kt`
  - Inject `SurgicalTemplateRepository`.
  - Load templates on init and expose a `templates: StateFlow<List<SurgicalTemplateDto>>`.
  - Add a helper function `applyTemplate(template: SurgicalTemplateDto?)` that returns a bundle of prefilled field values (or `null` for custom/blank).

- [ ] **Wire template selector into `ConsentFormScreen`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/ipd/ConsentFormScreen.kt`
  - At the top of the form, add a card/row showing:
    - Selected template name, or
    - "Custom form" if no template selected.
  - Tapping it opens the searchable `TemplatePickerDialog`.
  - On template selection, set all form states using the mapping above.
  - On "Custom form" selection, reset only the fields that would have been autofilled (or leave them blank if already blank).
  - Keep all fields editable after autofill.

### Validation checklist

- [ ] Create a surgical template in the app (Surgeon dashboard → Surgical Templates).
- [ ] Open Admission → Add Consent Form.
- [ ] Tap template selector; type in search box and verify filtering by name/procedure.
- [ ] Select a template → `formType`, `procedure`, `anesthesia`, and `procedureDescription` are prefilled.
- [ ] Edit prefilled fields and submit → consent form creates successfully.
- [ ] Select "Custom form" → prefilled fields are cleared/blank.
- [ ] Nurse login can also see templates in the consent form screen.
- [ ] `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64` succeeds.


---

# Parent-Uploaded History (PDF + Images)

Status: Planned. Not started.

## Decisions

- The existing `documents` table and `/documents` endpoints will store parent-uploaded history.
- Doctor profile section name: **"Parent Uploaded History"**.
- Doctor sees **only** documents where `uploaded_by_role = "parent"`.
- Images render as a **thumbnail grid**; PDFs render as document chips.
- Parent can **delete** their uploaded history records after saving.

## Tasks

### Backend

- [ ] **Add document delete endpoint**
  - File: `backend/app/routers/documents.py`
  - Add `DELETE /documents/{document_id}`.
  - Verify the caller is either:
    - the parent of the linked patient (`patient.parent_phone == user.phone`), or
    - the treating doctor (`patient.doctor_id == doctor_id`).
  - Delete the `documents` row.
  - *(Optional future cleanup: also remove the linked `media` object from MinIO/Postgres.)*

### Shared (KMP)

- [ ] **Add delete call to `DocumentsRepository`**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/data/DocumentsRepository.kt`
  - Add `suspend fun deleteDocument(id: String): Result<Unit>`.

- [ ] **Add image upload to parent profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt`
  - Add an `ImageAttachmentPicker` for images alongside the existing `PdfAttachmentPicker`.
  - On "Save Records", create a `DocumentCreateRequest` for each uploaded image:
    - `type = "image"`
    - `category = "previous_health_record"`
    - `uploadedByRole = "parent"`
  - PDF creation stays unchanged (`type = "pdf"`).

- [ ] **Fix saved-document viewing in parent profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt`
  - Replace the raw `openUrl(document.url)` in `HealthRecordCard` with `MediaUrlChip(url = document.url)`.
  - `MediaUrlChip` handles both images and PDFs, resolves `/media/{id}` presigned URLs, and opens the correct viewer.

- [ ] **Allow parent to delete uploaded history**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileViewModel.kt`
  - Add `deleteDocument(documentId: String)` that calls `DocumentsRepository.deleteDocument` and reloads the profile.
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/parent/ParentProfileScreen.kt`
  - Add a delete button to `HealthRecordCard` (only when `uploadedByRole == "parent"`).

- [ ] **Load parent documents in doctor patient profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/surgeon/PatientProfileViewModel.kt`
  - Inject `DocumentsRepository`.
  - Load documents for the patient and expose only `uploadedByRole == "parent"`.

- [ ] **Add "Parent Uploaded History" section to doctor profile**
  - File: `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/surgeon/PatientProfileScreen.kt`
  - Add a new section titled **"Parent Uploaded History"**.
  - For each document:
    - If `type == "image"` → render `AuthenticatedUrlImage` in a thumbnail grid (e.g. `LazyRow` of thumbnails that open fullscreen on tap).
    - If `type == "pdf"` → render `MediaUrlChip`.
  - Show an empty message when there are no parent-uploaded documents.

### Validation checklist

- [ ] Parent: upload a PDF → saved → tap to view via presigned URL.
- [ ] Parent: upload an image → saved → tap to view via presigned URL.
- [ ] Parent: delete an uploaded document → document disappears and backend row is removed.
- [ ] Doctor: open patient profile → see "Parent Uploaded History" section with the PDF chip and image thumbnail.
- [ ] Doctor: tap PDF chip → opens/plays; tap image thumbnail → fullscreen viewer.
- [ ] `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64` succeeds.
