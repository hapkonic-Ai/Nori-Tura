# Changelog — 27 August 2026

This release focuses on making uploaded media viewable inside the app, removing reliance on third-party image hosting, fixing the pre-op note unique-constraint bug, and ensuring the iOS app points to the production backend by default.

## Backend

### Media storage (replaces Cloudinary stub)
- Added a `media` model to `backend/prisma/schema.prisma` that stores uploaded files as binary blobs in PostgreSQL.
- Rewrote `backend/app/routers/uploads.py` so `POST /uploads/media` persists files in the DB and returns absolute `/media/{id}` URLs.
- Added `backend/app/routers/media.py` with an authenticated `GET /media/{media_id}` endpoint that serves the stored blob with the correct `Content-Type` and `Content-Disposition: inline`.
- Registered the new media router in `backend/app/main.py`.

### Pre-op notes fix
- Removed the implicit unique constraint on `pre_op_notes.admission_id` in `backend/prisma/schema.prisma` so multiple pre-op notes can be created per admission.
- Updated `backend/app/routers/ipd.py` `create_pre_op_note` to use `prisma.pre_op_notes.create(...)` instead of `upsert`.

### Tests
- Added `pytest==8.2.2` and `pytest-asyncio==0.23.7` to `backend/requirements.txt`.
- Added `backend/pytest.ini` with `pythonpath = .` and `asyncio_mode = auto`.
- Added `backend/tests/conftest.py` with fixtures for an isolated PostgreSQL test database, an async HTTP client, and a verified test doctor/patient/admission.
- Added `backend/tests/test_media_and_records.py` covering:
  - Image upload, DB persistence, and retrieval via `/media/{id}`.
  - `404` for missing media and `400` for malformed media IDs.
  - OPD records that include `prescription_image_urls`.
  - Creating multiple pre-op notes for the same admission.

## Shared / UI

### Full-screen image viewer
- Added `shared/src/commonMain/kotlin/com/example/nori_tura/presentation/components/UrlImage.kt` with:
  - `UrlImage` — clickable image thumbnail that opens a full-screen viewer.
  - `FullscreenImageViewer` — pinch-to-zoom dialog with a close button.
  - `UrlImageRow` — horizontal row of clickable thumbnails.
- Updated all existing image consumers to use `UrlImage`/`UrlImageRow` so they automatically get the inline viewer:
  - OPD record detail (`OpdRecordDetailScreen.kt`)
  - Parent consult detail (`ParentConsultDetailScreen.kt`)
  - Admission detail / pre-op/intra-op/post-op/ward-round notes (`AdmissionDetailScreen.kt`)
  - Consent signature images (`ConsentViewScreen.kt`)
  - Medical image picker (`MedicalImagePicker.kt`)
  - Image attachment picker (`ImageAttachmentPicker.kt`)
- Updated `MedicalRecordDetailScreen.kt` image cards to display the actual image thumbnail (via `UrlImage`) instead of a generic placeholder that opened the browser.
- Updated `MediaUrlChip.kt` so image attachments open the full-screen viewer; non-image files (PDFs, videos) still open with the platform URL launcher.

### OPD record refresh
- Added `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` in `PatientProfileScreen.kt` to reload the patient profile (and OPD records list) when the user returns from `OpdConsultScreen`, removing the need to navigate away and back.

## iOS configuration
- Changed `iosApp/iosApp/Info.plist` `BASE_URL` from `http://127.0.0.1:8000` to `https://nori-tura.onrender.com`.
  - This fixes the issue where the iOS simulator could not reach a backend running on the Mac, because `127.0.0.1` inside the simulator points to the simulator itself.
  - To test against a local backend on the iOS simulator, temporarily set `BASE_URL` to the Mac's LAN IP (e.g. `http://192.168.1.x:8000`).

## Documentation
- Updated `ENV_SETUP.md` with a "Backend URL Configuration" section documenting:
  - Production fallback: `https://nori-tura.onrender.com`
  - Android debug/release URL locations in `shared/build.gradle.kts`
  - iOS URL location in `iosApp/iosApp/Info.plist`
  - How to temporarily point iOS at a local Mac backend.

## Verification

### Backend tests
```bash
cd backend
source .venv/bin/activate
pytest tests/test_media_and_records.py -v
```
Expected result: `5 passed`.

### Shared module compile
```bash
./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 --no-daemon
```
Expected result: `BUILD SUCCESSFUL`.

### Local backend smoke test
```bash
curl http://127.0.0.1:8000/health
```
Expected result: `{"status":"ok","service":"noni-tura-api"}`.

## Deployment notes
1. Restart the backend so the updated Prisma schema and new routers are loaded.
2. If the database already contains data, run `prisma db push --accept-data-loss` (or a Prisma migration) to apply the schema change that removes the unique constraint on `pre_op_notes.admission_id`.
3. Rebuild and reinstall the iOS app so the new `BASE_URL` is bundled.
