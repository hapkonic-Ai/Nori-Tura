"""Backend tests for the consent form generate/download flow.

Consent forms are no longer signed in-app: a nurse/surgeon generates a
printable PDF (in English or Hindi) and downloads it; it is signed by hand
after printing. There is no OTP-signing endpoint any more.

Run with:

    source .venv/bin/activate
    pytest backend/tests/test_consent_flow.py -v
"""

import pytest


async def _create_consent(client, auth_headers, admission_id, procedure="Laparoscopic Appendectomy", **overrides):
    payload = {
        "admission_id": admission_id,
        "form_type": "Surgical Consent",
        "diagnosis": "Appendicitis",
        "procedure": procedure,
        "anesthesia": "General anesthesia",
        "risks": "Bleeding, infection, anesthesia complications",
        "benefits": "Removal of infected appendix, symptom relief",
        "alternatives": "Open appendectomy, antibiotic therapy",
        "post_op_care": "Pain management, wound care, follow-up in 1 week",
        "consent_for_anesthesia": True,
        "consent_for_blood_products": False,
        "consent_for_photo_medical_record": True,
    }
    payload.update(overrides)
    create_response = await client.post("/consent/forms", json=payload, headers=auth_headers)
    assert create_response.status_code == 201
    return create_response.json()


@pytest.mark.asyncio
async def test_create_consent_form_generates_pdf_immediately(client, auth_headers, test_admission):
    """Creating a consent form generates and 'downloads' the PDF in one step — no signing step follows."""
    body = await _create_consent(client, auth_headers, test_admission.id)
    consent_id = body["consent_form"]["id"]
    assert body["consent_form"]["status"] == "generated"
    assert body["consent_form"]["language"] == "English"
    assert body["consent_form"]["download_count"] == 1
    assert body["consent_form"]["downloaded_at"] is not None

    get_response = await client.get(f"/consent/forms/{consent_id}", headers=auth_headers)
    assert get_response.status_code == 200
    assert get_response.json()["status"] == "generated"


@pytest.mark.asyncio
async def test_create_consent_form_in_hindi(client, auth_headers, test_admission):
    """language="Hindi" is accepted and stored on the created form."""
    body = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy", language="Hindi")
    assert body["consent_form"]["language"] == "Hindi"


@pytest.mark.asyncio
async def test_create_consent_form_rejects_invalid_language(client, auth_headers, test_admission):
    create_response = await client.post(
        "/consent/forms",
        json={
            "admission_id": test_admission.id,
            "form_type": "Surgical Consent",
            "diagnosis": "Appendicitis",
            "procedure": "Herniotomy",
            "anesthesia": "General anesthesia",
            "risks": "Bleeding, infection",
            "benefits": "Symptom relief",
            "alternatives": "Antibiotic therapy",
            "post_op_care": "Wound care, follow-up",
            "language": "French",
        },
        headers=auth_headers,
    )
    assert create_response.status_code == 422


@pytest.mark.asyncio
async def test_download_consent_form_same_language_uses_cached_url(client, auth_headers, test_admission):
    body = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy")
    consent_id = body["consent_form"]["id"]

    download_response = await client.get(f"/consent/forms/{consent_id}/download", headers=auth_headers)
    assert download_response.status_code == 200
    assert "pdf_url" in download_response.json()

    get_response = await client.get(f"/consent/forms/{consent_id}", headers=auth_headers)
    assert get_response.json()["download_count"] == 2


@pytest.mark.asyncio
async def test_download_consent_form_rejects_invalid_language(client, auth_headers, test_admission):
    body = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy")
    consent_id = body["consent_form"]["id"]

    download_response = await client.get(
        f"/consent/forms/{consent_id}/download",
        params={"language": "French"},
        headers=auth_headers,
    )
    assert download_response.status_code == 400


@pytest.mark.asyncio
async def test_no_otp_signing_endpoints_exist(client, auth_headers, test_admission):
    """The removed e-sign endpoints must be gone (404), not just unauthorized."""
    body = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy")
    consent_id = body["consent_form"]["id"]

    for path in (
        f"/consent/forms/{consent_id}/request-otp",
        f"/consent/forms/{consent_id}/request-witness-otp",
        f"/consent/forms/{consent_id}/verify-otp",
    ):
        response = await client.post(path, json={}, headers=auth_headers)
        assert response.status_code == 404


@pytest.mark.asyncio
async def test_audit_trail_records_download_events(client, auth_headers, test_admission, test_doctor):
    body = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy")
    consent_id = body["consent_form"]["id"]

    await client.get(f"/consent/forms/{consent_id}/download", headers=auth_headers)

    audit_response = await client.get(f"/consent/forms/{consent_id}/audit", headers=auth_headers)
    assert audit_response.status_code == 200
    events = [e for e in audit_response.json()["events"] if e["event_type"] == "pdf_downloaded"]
    assert len(events) == 2  # one from creation, one from the explicit download above
    assert events[0]["actor_user_id"] == test_doctor.id
    assert events[0]["detail"]["language"] == "English"
