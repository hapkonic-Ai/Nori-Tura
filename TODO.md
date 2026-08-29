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


---

# Media Storage: Postgres Blobs → MinIO (Analysis)

Status: Feasible; effort estimate only. No code changes yet.

## Current state

- `backend/prisma/schema.prisma` has a `media` model that stores the uploaded blob in a `data Bytes` column.
- `backend/app/routers/uploads.py` (`POST /uploads/media`) accepts multipart files, base64-encodes them, writes a `media` row, and returns absolute URLs of the form `{base}/media/{media.id}`.
- `backend/app/routers/media.py` (`GET /media/{media_id}`) reads the blob from Postgres and serves it back. It requires authentication via `get_current_user`.
- The app uploads through `ApiClient.uploadMedia()` and stores the returned URLs as plain strings in fields like `image_urls`, `video_urls`, `prescription_image_urls`.
- UI display (`UrlImage`, `MediaUrlChip`, `Coil`) fetches the returned URL directly.
- Docker Compose only runs `postgres`; `.env.example` has no S3/MinIO settings.
- `requirements.txt` does not include `boto3` or `minio` Python SDK.

## Recommended architecture options

### Option A — Backend proxy (safest, smallest change)
Keep the existing `/media/{id}` contract and have the backend fetch objects from MinIO and stream them back. Uploads go to MinIO, but metadata stays in Postgres.

- **Backend changes**
  - Schema: replace `data Bytes` with `bucket String` and `object_key String` (or a single `storage_path String`). Keep `filename`, `mime_type`, `created_by`.
  - Upload router: write file bytes to MinIO (`bucket = nonitura-media`, `object_key = {uuid}/{filename}`), then create the `media` row with bucket/key.
  - Media router: fetch object from MinIO and return `StreamingResponse` (or `Response`) with the stored MIME type. Auth gate is unchanged.
  - Add MinIO settings: endpoint, access key, secret key, bucket name, secure flag, region.
  - Add `minio` or `boto3` to `requirements.txt`.
  - Add `minio` service to `docker-compose.yml`.

- **Frontend changes**
  - None. The returned `/media/{id}` URLs and display logic stay identical.

- **Migration**
  - Add new columns, export existing `media.data` rows to MinIO, back-fill `bucket`/`object_key`, then drop `data Bytes`.

### Option B — Direct MinIO URLs (more performant, larger change)
Upload to MinIO and return presigned or public URLs directly.

- **Backend changes**
  - Same schema change as Option A.
  - Upload router returns a presigned GET URL (e.g. valid for 1 hour) or a public URL if the bucket is public.
  - `/media/{id}` can still exist for long-term/permanent URLs.

- **Frontend changes**
  - Small if we keep the URL string opaque. Potentially need presigned-URL refresh logic for long-lived views.
  - Public MinIO URLs require CORS setup on the MinIO bucket.

- **Trade-off**
  - Reduces backend bandwidth but complicates auth and URL expiry handling.

## Security / auth decisions needed

1. **Bucket policy**: private bucket is safest; use presigned URLs or backend proxy.
2. **CORS**: if the web/Wasm client ever loads MinIO directly, configure `Access-Control-Allow-Origin` on the bucket.
3. **Access keys**: separate read/write keys; rotate them; store in `.env` only.
4. **File size / type limits**: currently implicit; should be enforced explicitly when moving to object storage.
5. **Patient-data isolation**: consider prefixing objects by `patient_id` or `created_by` if bucket is shared.

## Files that would change

### Backend
- `backend/prisma/schema.prisma` — `media` model migration.
- `backend/app/routers/uploads.py` — upload to MinIO.
- `backend/app/routers/media.py` — serve from MinIO (Option A) or redirect/presign (Option B).
- `backend/app/core/config.py` — MinIO env vars.
- `backend/app/core/storage.py` *(new)* — MinIO client wrapper.
- `backend/.env.example` — add MinIO credentials.
- `backend/docker-compose.yml` — add MinIO service + healthcheck.
- `backend/requirements.txt` — add `minio>=7.2.0` or `boto3>=1.34.0`.
- Any pytest tests that create `media` rows directly.

### Frontend
- **Option A**: no changes.
- **Option B**: `UrlImage` / `MediaUrlChip` may need refresh logic for presigned URLs; `ApiClient` contract stays the same.

### Infrastructure
- New local MinIO container (suggest `nonitura-minio` on ports `9000:9000` and `9001:9001`).
- Production: dedicated MinIO/S3 bucket, TLS, lifecycle policy, backup strategy.

## Migration path (zero-downtime-ish)

1. Add `bucket`, `object_key`, `storage_backend` columns to `media` (nullable).
2. Deploy MinIO client code that writes new uploads to MinIO.
3. Backfill old rows: read `data`, write to MinIO, set `bucket`/`object_key`.
4. Update `/media/{id}` to serve from MinIO when `object_key` is set, else fall back to `data`.
5. Once all rows are migrated, drop `data Bytes`.

## Rough effort estimate

| Task | Effort |
|---|---|
| Add MinIO to Docker Compose + env | 30 min |
| Add MinIO settings + client wrapper | 1–2 hrs |
| Update upload router | 2–3 hrs |
| Update media router (proxy) | 2–3 hrs |
| Prisma migration + backfill script | 3–4 hrs |
| Tests + local validation | 2–3 hrs |
| Frontend changes (Option A) | 0 |
| **Total (Option A)** | **~1–1.5 dev days** |
| **Total (Option B with presigned URLs)** | **+1–2 dev days** |

## Bottom line

Yes, the media layer can be converted to MinIO without major architectural rework. **Option A (backend proxy) is recommended first** because it keeps the existing `/media/{id}` contract, leaves the mobile/web clients untouched, and preserves the current authentication model. Option B can be considered later if backend egress cost or latency becomes a concern.


---

# Media Storage: Postgres Blobs → MinIO (Implementation Complete)

Status: **Implemented and validated locally.**

## What was done

1. **Infrastructure**
   - Added `nonitura-minio` service to `backend/docker-compose.yml` on host ports `9100` (API) and `9101` (console).
   - Updated `backend/.env.example` and local `.env` with MinIO settings.
   - Changed local Postgres mapping to `5433:5432` to match the already-running container.

2. **Backend config & dependencies**
   - Added `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`, `MINIO_SECURE` to `backend/app/core/config.py`.
   - Added `minio==7.2.7` to `backend/requirements.txt` and installed it in the venv.

3. **Storage client**
   - Created `backend/app/core/storage.py` with `MediaStorage` wrapper:
     - Auto-creates the configured bucket.
     - `upload_object`, `get_object`, `delete_object`, `object_key` helpers.

4. **Prisma schema & migration**
   - Replaced `media.data Bytes` with `bucket String`, `object_key String`, `storage_backend String`.
   - Pushed schema to the local database and regenerated the Prisma client.

5. **Upload / retrieval routers**
   - Updated `backend/app/routers/uploads.py` to write files to MinIO and store metadata in Postgres.
   - Updated `backend/app/routers/media.py` to proxy objects from MinIO through `StreamingResponse`, keeping the JWT auth gate.

6. **Backfill**
   - Created `backend/scripts/migrate_media_to_minio.py`.
   - Ran it: no legacy rows needed migration.

7. **End-to-end validation**
   - Started backend on `http://127.0.0.1:8001`.
   - Authenticated as existing doctor `+919876543210`.
   - Uploaded a test PNG via `POST /uploads/media` → returned `http://127.0.0.1:8001/media/{id}`.
   - Retrieved the image via `GET /media/{id}` → `200 OK`, `content-type: image/png`.
   - Verified the DB row: `bucket=nonitura-media`, `object_key={id}/test.png`, `storage_backend=minio`, `data=NULL`.
   - Cleaned up test objects from both MinIO and Postgres.

## Known issues

- The existing pytest suite (`tests/test_media_and_records.py`) fails with `RuntimeError: ... is bound to a different event loop`. This is a pre-existing test-fixture / event-loop issue unrelated to the MinIO migration; the manual end-to-end test passes.
- The MinIO healthcheck initially used `curl`, which is not present in the MinIO image. Updated to a `bash` TCP check.

## Files changed

- `backend/docker-compose.yml`
- `backend/.env.example`
- `backend/.env`
- `backend/app/core/config.py`
- `backend/requirements.txt`
- `backend/prisma/schema.prisma`
- `backend/app/routers/uploads.py`
- `backend/app/routers/media.py`
- `backend/app/core/storage.py` *(new)*
- `backend/scripts/migrate_media_to_minio.py` *(new)*
- `backend/tests/conftest.py` *(port updated to 5433)*

## Next steps for production

- Replace local MinIO container with a production MinIO/S3 endpoint.
- Use strong, rotated credentials and restrict bucket access.
- Set up bucket lifecycle policies and backups.
- Fix the pytest event-loop issue and re-enable automated media tests.


---

# Authenticated Image Loading Fix — Implementation Complete

Status: **Implemented and validated locally.**

## What was done

1. **Backend contract**
   - `POST /uploads/media` now returns relative `/media/{id}` paths (no absolute URLs).
   - `GET /media/{id}` continues to proxy the authenticated binary object.
   - `POST /media/{id}/access` returns a short-lived MinIO presigned URL:
     ```json
     { "url": "...", "expires_in_seconds": 900 }
     ```
   - Added optional `MINIO_PUBLIC_ENDPOINT` setting so presigned URLs can point at a
     client-reachable host (e.g. `10.0.2.2:9100` for the Android emulator) while the
     backend still uses the internal `MINIO_ENDPOINT`.
   - Added Android emulator origins to the CORS allow-list (`http://10.0.2.2:8001`,
     `http://localhost:8001`).

2. **Backend persistence**
   - `POST /opd/patients/{id}/records` accepts `medical_images: List[MedicalImageCreate]`.
   - After the OPD record is created, the backend auto-creates a linked `medical_records`
     row with title `OPD Consult Images — YYYY-MM-DD` and writes each image to
     `medical_record_images` with the chosen category/label/description.

3. **Shared (KMP) image loading**
   - Created `MediaAccessRepository`:
     - Exchanges `/media/{id}` paths for presigned URLs.
     - Caches URLs until 2 minutes before expiry.
     - Falls back to public/data URLs unchanged.
   - Created authenticated image components:
     - `AuthenticatedUrlImage`
     - `AuthenticatedUrlImageRow`
   - Updated `MediaUrlChip` to resolve presigned URLs before opening the fullscreen
     viewer or the platform URL launcher.
   - Updated `ImageAttachmentPicker` and `MedicalImagePicker` to preview newly-uploaded
     relative media through the authenticated loader.
   - Updated `isImageUrl()` to treat `/media/{id}` as an image path.

4. **Detail screens swapped to authenticated loaders**
   - `OpdRecordDetailScreen`
   - `ParentConsultDetailScreen`
   - `AdmissionDetailScreen`
   - `MedicalRecordDetailScreen`
   - `ConsentViewScreen` (signature thumbnails)

5. **OPD consult sends medical images**
   - Added `MedicalImageCreateDto`.
   - Extended `OpdRecordCreateRequest` with `medical_images`.
   - `OpdConsultScreen` maps the `MedicalImagePicker` state into the request body,
     setting `uploaded_by_role` to `"nurse"` when `isNurse == true`.

## Validation performed

- Backend startup: `uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload`.
- Doctor login via `/auth/send-otp` + `/auth/verify-otp`.
- Upload PNG via `POST /uploads/media` → returned `/media/{id}`.
- `GET /media/{id}` → `200 OK`, `content-type: image/png`.
- `POST /media/{id}/access` → working presigned URL.
- Created OPD record with `medical_images` → verified linked `medical_records` row and
  `medical_record_images` entries in Postgres.
- `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid` → BUILD SUCCESSFUL.
- `./gradlew :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL.
- `./gradlew :androidApp:assembleDebug` → BUILD SUCCESSFUL.

## Known limitations / next steps

- **Local MinIO public endpoint**: out of the box `MINIO_PUBLIC_ENDPOINT` is empty, so
  presigned URLs use `localhost:9100`. This works for iOS Simulator and web. When testing
  on the Android emulator, set `MINIO_PUBLIC_ENDPOINT=10.0.2.2:9100` in `backend/.env`
  (MinIO must also be reachable on the host at `0.0.0.0:9100`, which it already is).
- **Manual medical-record creation UI** (Phase 3.5 from earlier planning) is deferred;
  OPD images now create records automatically, but a standalone “create medical record”
  screen remains future work.
- **Video classification in picker**: relative `/media/{id}` paths are treated as images by
  `isImageUrl()`. For IPD note forms that allow video, the picker separates image/video
  launchers but the returned URL has no extension. If videos become common, add a type
  hint to the upload response or track media kind separately.
