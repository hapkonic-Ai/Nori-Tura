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
