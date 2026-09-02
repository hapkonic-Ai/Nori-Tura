"""Backend tests for the consent form generate/view/OTP-sign flow.

Run with:

    source .venv/bin/activate
    pytest backend/tests/test_consent_flow.py -v
"""

import pytest


async def _create_consent(client, auth_headers, admission_id, procedure="Laparoscopic Appendectomy"):
    create_response = await client.post(
        "/consent/forms",
        json={
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
            "consent_for_photography": True,
        },
        headers=auth_headers,
    )
    assert create_response.status_code == 201
    return create_response.json()["consent_form"]["id"]


@pytest.mark.asyncio
async def test_create_and_sign_consent_form_with_otp(client, auth_headers, test_admission):
    """Full surgeon flow: create a consent form, request parent OTP, verify and sign."""

    # 1. Create a consent form for the admission.
    consent_id = await _create_consent(client, auth_headers, test_admission.id)

    get_response = await client.get(f"/consent/forms/{consent_id}", headers=auth_headers)
    assert get_response.status_code == 200
    assert get_response.json()["status"] == "pending"

    # 2. Request OTP for the patient's parent phone (+919999999999 fixture).
    otp_response = await client.post(
        f"/consent/forms/{consent_id}/request-otp",
        headers=auth_headers,
    )
    assert otp_response.status_code == 200
    otp_body = otp_response.json()
    assert otp_body["phone"].startswith("+91")
    otp = otp_body["dev_otp"]
    assert len(otp) == 6

    # 3. Wrong OTP must be rejected.
    wrong = await client.post(
        f"/consent/forms/{consent_id}/verify-otp",
        json={"otp": "000000" if otp != "000000" else "111111"},
        headers=auth_headers,
    )
    assert wrong.status_code == 401

    # 4. Verify with the correct OTP and optional witness details.
    sign_response = await client.post(
        f"/consent/forms/{consent_id}/verify-otp",
        json={
            "otp": otp,
            "witness_name": "Ravi Kumar",
            "witness_relationship": "Uncle",
            "witness_mobile": "+919876543211",
        },
        headers=auth_headers,
    )
    assert sign_response.status_code == 200
    signed = sign_response.json()
    assert signed["status"] == "signed"
    assert signed["parent_auth_method"] == "otp"
    assert signed["parent_auth_phone"].startswith("+91")
    assert signed["witness_name"] == "Ravi Kumar"
    assert signed["witness_relationship"] == "Uncle"
    assert signed["witness_mobile"] == "+919876543211"
    assert signed["signed_pdf_hash"] is not None

    # 5. Fetch again and confirm signed state.
    get_signed_response = await client.get(f"/consent/forms/{consent_id}", headers=auth_headers)
    assert get_signed_response.status_code == 200
    assert get_signed_response.json()["status"] == "signed"


@pytest.mark.asyncio
async def test_sign_without_otp_fails(client, auth_headers, test_admission):
    """Verifying with no OTP session requested must return 401."""
    consent_id = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy")

    sign_response = await client.post(
        f"/consent/forms/{consent_id}/verify-otp",
        json={"otp": "123456"},
        headers=auth_headers,
    )
    assert sign_response.status_code == 401


@pytest.mark.asyncio
async def test_sign_already_signed_consent_fails(client, auth_headers, test_admission):
    """Signing a consent form twice should return 400."""
    consent_id = await _create_consent(client, auth_headers, test_admission.id, procedure="Herniotomy")

    otp = (
        await client.post(f"/consent/forms/{consent_id}/request-otp", headers=auth_headers)
    ).json()["dev_otp"]

    await client.post(
        f"/consent/forms/{consent_id}/verify-otp",
        json={"otp": otp},
        headers=auth_headers,
    )

    second_sign = await client.post(
        f"/consent/forms/{consent_id}/verify-otp",
        json={"otp": otp},
        headers=auth_headers,
    )
    assert second_sign.status_code == 400
    assert "already signed" in second_sign.json()["detail"].lower()
