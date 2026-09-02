"""Backend tests for media upload/retrieval, OPD records, and IPD notes.

These tests hit the real FastAPI endpoints backed by a PostgreSQL test database.
Run with:

    source .venv/bin/activate
    pytest backend/tests/test_media_and_records.py -v
"""

import io

import pytest

from app.core.database import prisma


@pytest.mark.asyncio
async def test_upload_image_stores_media_and_returns_url(client, auth_headers):
    """POST /uploads/media should save the file and return a /media/{id} URL."""
    response = await client.post(
        "/uploads/media",
        data={"resource_type": "image", "folder": "nonitura"},
        files={"files": ("test.jpg", io.BytesIO(b"fake-image-bytes"), "image/jpeg")},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert len(body["urls"]) == 1
    item = body["urls"][0]
    assert "/media/" in item["url"]
    assert item["mime_type"] == "image/jpeg"
    assert item["filename"] == "test.jpg"
    assert item["id"]

    media_id = item["id"]
    get_response = await client.get(f"/media/{media_id}", headers=auth_headers)
    assert get_response.status_code == 200
    assert get_response.content == b"fake-image-bytes"
    assert get_response.headers["content-type"] == "image/jpeg"


@pytest.mark.asyncio
async def test_get_media_not_found(client, auth_headers):
    """GET /media/{id} should return 404 for unknown ids."""
    response = await client.get("/media/00000000-0000-0000-0000-000000000000", headers=auth_headers)
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_get_media_invalid_id(client, auth_headers):
    """GET /media/{id} should return 400 for malformed ids."""
    response = await client.get("/media/not-a-uuid", headers=auth_headers)
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_create_opd_record_with_prescription_images(client, auth_headers, test_patient):
    """POST /opd/patients/{id}/records should accept and return prescription_image_urls."""
    image_urls = ["http://testserver/media/12345678-1234-1234-1234-123456789abc"]

    response = await client.post(
        f"/opd/patients/{test_patient.id}/records",
        json={
            "visit_type": "new",
            "complaint": "Headache",
            "examination": "Normal",
            "diagnosis": "Migraine",
            "prescription_image_urls": image_urls,
        },
        headers=auth_headers,
    )

    assert response.status_code == 201
    body = response.json()
    assert body["prescription_image_urls"] == image_urls

    # Verify the stored record can be retrieved with the images attached.
    record_id = body["id"]
    get_response = await client.get(f"/opd/records/{record_id}", headers=auth_headers)
    assert get_response.status_code == 200
    assert get_response.json()["prescription_image_urls"] == image_urls


@pytest.mark.asyncio
async def test_create_multiple_pre_op_notes_allowed(client, auth_headers, test_admission):
    """POST /ipd/admissions/{id}/pre-op should allow more than one note per admission."""
    created = []
    for i in range(2):
        response = await client.post(
            f"/ipd/admissions/{test_admission.id}/pre-op",
            json={
                "procedure": f"Procedure {i + 1}",
                "image_urls": ["http://testserver/media/12345678-1234-1234-1234-123456789abc"],
            },
            headers=auth_headers,
        )
        assert response.status_code == 201
        created.append(response.json()["id"])

    assert len(created) == 2
    assert created[0] != created[1]

    # Both notes should appear on the admission detail.
    admission_response = await client.get(
        f"/ipd/admissions/{test_admission.id}", headers=auth_headers
    )
    assert admission_response.status_code == 200
    pre_op = admission_response.json().get("pre_op_notes", [])
    assert len(pre_op) == 2
    assert {note["procedure"] for note in pre_op} == {"Procedure 1", "Procedure 2"}


@pytest.mark.asyncio
async def test_upload_video_returns_mime_type_and_url(client, auth_headers):
    """POST /uploads/media should return mime_type for video uploads."""
    response = await client.post(
        "/uploads/media",
        data={"resource_type": "video", "folder": "nonitura"},
        files={"files": ("test.mp4", io.BytesIO(b"fake-video-bytes"), "video/mp4")},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert len(body["urls"]) == 1
    item = body["urls"][0]
    assert item["mime_type"] == "video/mp4"
    assert item["filename"] == "test.mp4"
    assert "/media/" in item["url"]


@pytest.mark.asyncio
async def test_create_pre_op_note_with_video_urls(client, auth_headers, test_admission):
    """POST /ipd/admissions/{id}/pre-op should persist video_urls."""
    response = await client.post(
        f"/ipd/admissions/{test_admission.id}/pre-op",
        json={
            "procedure": "Lap Appendectomy",
            "image_urls": ["http://testserver/media/12345678-1234-1234-1234-123456789abc"],
            "video_urls": ["http://testserver/media/abcdef12-1234-1234-1234-123456789abc"],
        },
        headers=auth_headers,
    )
    assert response.status_code == 201
    body = response.json()
    assert body["video_urls"] == ["http://testserver/media/abcdef12-1234-1234-1234-123456789abc"]
    assert body["image_urls"] == ["http://testserver/media/12345678-1234-1234-1234-123456789abc"]


@pytest.mark.asyncio
async def test_delete_document_by_doctor(client, auth_headers, test_patient):
    """DELETE /documents/{id} should allow the treating doctor to delete."""
    document = await prisma.documents.create(
        data={
            "patient_id": test_patient.id,
            "doctor_id": test_patient.doctor_id,
            "name": "record.pdf",
            "url": "/media/12345678-1234-1234-1234-123456789abc",
            "type": "pdf",
            "category": "previous_health_record",
            "uploaded_by_role": "parent",
        }
    )

    response = await client.delete(f"/documents/{document.id}", headers=auth_headers)
    assert response.status_code == 204

    deleted = await prisma.documents.find_first(where={"id": document.id})
    assert deleted is None


@pytest.mark.asyncio
async def test_delete_document_by_parent(client, test_patient):
    """DELETE /documents/{id} should allow the patient's parent to delete."""
    from app.core.security import create_access_token

    document = await prisma.documents.create(
        data={
            "patient_id": test_patient.id,
            "doctor_id": test_patient.doctor_id,
            "name": "record.jpg",
            "url": "/media/12345678-1234-1234-1234-123456789abc",
            "type": "image",
            "category": "previous_health_record",
            "uploaded_by_role": "parent",
        }
    )

    parent_headers = {
        "Authorization": f"Bearer {create_access_token({'phone': test_patient.parent_phone, 'role': 'patient_parent'})}"
    }

    response = await client.delete(f"/documents/{document.id}", headers=parent_headers)
    assert response.status_code == 204

    deleted = await prisma.documents.find_first(where={"id": document.id})
    assert deleted is None


@pytest.mark.asyncio
async def test_delete_document_forbidden_for_other_doctor(client, test_patient):
    """DELETE /documents/{id} should reject a doctor who does not own the patient."""
    from app.core.security import create_access_token

    other_doctor = await prisma.doctors.create(
        data={
            "name": "Dr. Other",
            "phone": "+919876543211",
            "specialty": "General Surgery",
            "is_active": True,
        }
    )
    document = await prisma.documents.create(
        data={
            "patient_id": test_patient.id,
            "doctor_id": test_patient.doctor_id,
            "name": "record.pdf",
            "url": "/media/12345678-1234-1234-1234-123456789abc",
            "type": "pdf",
            "category": "previous_health_record",
            "uploaded_by_role": "parent",
        }
    )

    other_headers = {
        "Authorization": f"Bearer {create_access_token({'phone': other_doctor.phone, 'role': 'surgeon', 'doctor_id': other_doctor.id})}"
    }

    response = await client.delete(f"/documents/{document.id}", headers=other_headers)
    assert response.status_code == 403

    remaining = await prisma.documents.find_first(where={"id": document.id})
    assert remaining is not None
