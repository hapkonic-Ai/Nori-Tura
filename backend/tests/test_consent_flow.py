"""Backend tests for the consent form generate/view/sign flow.

Run with:

    source .venv/bin/activate
    pytest backend/tests/test_consent_flow.py -v
"""

import pytest


@pytest.mark.asyncio
async def test_create_and_sign_consent_form(client, auth_headers, test_admission):
    """Full surgeon flow: create a consent form, fetch it, then sign it."""

    # 1. Create a consent form for the admission.
    create_response = await client.post(
        "/consent/forms",
        json={
            "admission_id": test_admission.id,
            "form_type": "Surgical Consent",
            "diagnosis": "Appendicitis",
            "procedure": "Laparoscopic Appendectomy",
            "anesthesia": "General anesthesia",
            "risks": "Bleeding, infection, anesthesia complications",
            "benefits": "Removal of infected appendix, symptom relief",
            "alternatives": "Open appendectomy, antibiotic therapy",
            "post_op_care": "Pain management, wound care, follow-up in 1 week",
            "consent_for_anesthesia": True,
            "consent_for_blood_products": False,
            "consent_for_photography": True,
        },
        headers=auth_headers,
    )
    assert create_response.status_code == 201
    body = create_response.json()
    consent = body["consent_form"]
    consent_id = consent["id"]
    assert consent["status"] == "pending"
    assert consent["admission_id"] == test_admission.id
    assert consent["content_json"]["procedure"] == "Laparoscopic Appendectomy"
    assert consent["consent_number"].startswith("NT-CONSENT-")

    # 2. Fetch the consent form by id.
    get_response = await client.get(f"/consent/forms/{consent_id}", headers=auth_headers)
    assert get_response.status_code == 200
    fetched = get_response.json()
    assert fetched["id"] == consent_id
    assert fetched["status"] == "pending"

    # 3. Sign the consent form.
    sign_response = await client.post(
        f"/consent/forms/{consent_id}/sign",
        json={
            "parent_signature_url": "http://testserver/media/00000000-0000-0000-0000-000000000001",
            "witness_name": "Ravi Kumar",
            "witness_relationship": "Uncle",
            "witness_mobile": "+919876543211",
            "witness_signature_url": "http://testserver/media/00000000-0000-0000-0000-000000000002",
        },
        headers=auth_headers,
    )
    assert sign_response.status_code == 200
    signed = sign_response.json()
    assert signed["status"] == "signed"
    assert signed["parent_signature_url"] == "http://testserver/media/00000000-0000-0000-0000-000000000001"
    assert signed["witness_name"] == "Ravi Kumar"
    assert signed["signed_pdf_hash"] is not None

    # 4. Fetch again and confirm signed state.
    get_signed_response = await client.get(f"/consent/forms/{consent_id}", headers=auth_headers)
    assert get_signed_response.status_code == 200
    assert get_signed_response.json()["status"] == "signed"


@pytest.mark.asyncio
async def test_sign_already_signed_consent_fails(client, auth_headers, test_admission):
    """Signing a consent form twice should return 400."""
    create_response = await client.post(
        "/consent/forms",
        json={
            "admission_id": test_admission.id,
            "form_type": "Surgical Consent",
            "diagnosis": "Hernia",
            "procedure": "Herniotomy",
            "anesthesia": "General anesthesia",
            "risks": "Infection, recurrence",
            "benefits": "Definitive repair",
            "alternatives": "Observation",
            "post_op_care": "Rest",
        },
        headers=auth_headers,
    )
    consent_id = create_response.json()["consent_form"]["id"]

    await client.post(
        f"/consent/forms/{consent_id}/sign",
        json={
            "parent_signature_url": "http://testserver/media/00000000-0000-0000-0000-000000000001",
        },
        headers=auth_headers,
    )

    second_sign = await client.post(
        f"/consent/forms/{consent_id}/sign",
        json={
            "parent_signature_url": "http://testserver/media/00000000-0000-0000-0000-000000000001",
        },
        headers=auth_headers,
    )
    assert second_sign.status_code == 400
    assert "already signed" in second_sign.json()["detail"].lower()
