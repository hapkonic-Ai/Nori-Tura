"""Backend tests for media upload/retrieval, OPD records, and IPD notes.

These tests hit the real FastAPI endpoints backed by a PostgreSQL test database.
Run with:

    source .venv/bin/activate
    pytest backend/tests/test_media_and_records.py -v
"""

import io

import pytest


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
    assert "/media/" in body["urls"][0]

    media_id = body["urls"][0].split("/media/")[-1]
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
