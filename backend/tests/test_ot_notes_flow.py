"""Backend tests for the OT (operative) notes flow — the unified replacement
for pre-op/intra-op/post-op notes.

Run with:

    source .venv/bin/activate
    pytest backend/tests/test_ot_notes_flow.py -v
"""

import pytest


@pytest.mark.asyncio
async def test_create_and_fetch_ot_note(client, auth_headers, test_admission):
    create_response = await client.post(
        f"/ipd/admissions/{test_admission.id}/ot-notes",
        json={
            "procedure": "Laparoscopic Appendectomy",
            "approach": "laparoscopic",
            "anaesthesia": "General anaesthesia",
            "preop_diagnosis": "Acute appendicitis",
            "procedure_steps": ["Step 1: create pneumoperitoneum", "Step 2: identify appendix", "Step 3: divide mesoappendix"],
            "findings": "Inflamed appendix, no perforation",
            "team_members": [{"role": "Surgeon", "name": "Dr. Mehta"}, {"role": "Anaesthetist", "name": "Dr. Rao"}],
            "status": "submitted",
        },
        headers=auth_headers,
    )
    assert create_response.status_code == 201
    note = create_response.json()
    assert note["procedure"] == "Laparoscopic Appendectomy"
    assert note["procedure_steps"] == [
        "Step 1: create pneumoperitoneum",
        "Step 2: identify appendix",
        "Step 3: divide mesoappendix",
    ]
    assert note["status"] == "submitted"
    assert note["team_members"] == [
        {"role": "Surgeon", "name": "Dr. Mehta"},
        {"role": "Anaesthetist", "name": "Dr. Rao"},
    ]

    get_response = await client.get(
        f"/ipd/admissions/{test_admission.id}/ot-notes/{note['id']}",
        headers=auth_headers,
    )
    assert get_response.status_code == 200
    assert get_response.json()["findings"] == "Inflamed appendix, no perforation"

    admission_response = await client.get(f"/ipd/admissions/{test_admission.id}", headers=auth_headers)
    assert admission_response.status_code == 200
    assert len(admission_response.json()["ot_notes"]) == 1


@pytest.mark.asyncio
async def test_update_ot_note_and_add_media(client, auth_headers, test_admission):
    create_response = await client.post(
        f"/ipd/admissions/{test_admission.id}/ot-notes",
        json={"procedure": "Herniotomy", "procedure_steps": ["Step 1"]},
        headers=auth_headers,
    )
    note_id = create_response.json()["id"]
    assert create_response.json()["status"] == "draft"

    update_response = await client.patch(
        f"/ipd/admissions/{test_admission.id}/ot-notes/{note_id}",
        json={"status": "submitted", "closure": "Layered closure with absorbable sutures"},
        headers=auth_headers,
    )
    assert update_response.status_code == 200
    assert update_response.json()["status"] == "submitted"
    assert update_response.json()["closure"] == "Layered closure with absorbable sutures"

    media_response = await client.post(
        f"/ipd/admissions/{test_admission.id}/ot-notes/{note_id}/media",
        json={"url": "/media/abc123", "media_type": "image", "label": "Post-op wound"},
        headers=auth_headers,
    )
    assert media_response.status_code == 201
    body = media_response.json()
    assert body["image_urls"] == ["/media/abc123"]
    assert body["media_items"][0]["label"] == "Post-op wound"


@pytest.mark.asyncio
async def test_ot_note_template_crud_and_visibility(client, auth_headers, test_admission):
    create_response = await client.post(
        "/ot-note-templates",
        json={
            "name": "My Appendectomy Template",
            "procedure": "Appendectomy",
            "procedure_steps": ["Step 1", "Step 2"],
        },
        headers=auth_headers,
    )
    assert create_response.status_code == 201
    template = create_response.json()
    assert template["is_global"] is False

    list_response = await client.get("/ot-note-templates", headers=auth_headers)
    assert list_response.status_code == 200
    names = [t["name"] for t in list_response.json()]
    assert "My Appendectomy Template" in names

    update_response = await client.patch(
        f"/ot-note-templates/{template['id']}",
        json={"closure": "Standard layered closure"},
        headers=auth_headers,
    )
    assert update_response.status_code == 200
    assert update_response.json()["closure"] == "Standard layered closure"

    delete_response = await client.delete(f"/ot-note-templates/{template['id']}", headers=auth_headers)
    assert delete_response.status_code == 204

    get_after_delete = await client.get(f"/ot-note-templates/{template['id']}", headers=auth_headers)
    assert get_after_delete.status_code == 404
