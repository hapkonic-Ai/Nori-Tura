"""Backend tests for the follow-up tracking flow.

There is no appointment-booking system behind this: a follow-up is just an
OPD record with a suggested `follow_up_date`. It stays "pending" (and
therefore listed) whether its date is in the future (upcoming) or already
past (overdue, since nothing marked it attended) — the frontend does that
split by comparing dates; the backend just returns every pending one.

Run with:

    source .venv/bin/activate
    pytest backend/tests/test_follow_ups_flow.py -v
"""

from datetime import datetime, timedelta

import pytest


async def _create_opd_record(client, auth_headers, patient_id, follow_up_date=None):
    response = await client.post(
        f"/opd/patients/{patient_id}/records",
        json={
            "visit_type": "new",
            "complaint": "Fever",
            "examination": "Stable",
            "diagnosis": "Viral fever",
            "follow_up_date": follow_up_date.isoformat() if follow_up_date else None,
        },
        headers=auth_headers,
    )
    assert response.status_code == 201
    return response.json()


@pytest.mark.asyncio
async def test_follow_ups_list_includes_past_and_future_dates(client, auth_headers, test_patient):
    """Both overdue (past date) and upcoming (future date) follow-ups show up —
    there's no single-day window any more."""
    overdue = await _create_opd_record(client, auth_headers, test_patient.id, datetime.now() - timedelta(days=5))
    upcoming = await _create_opd_record(client, auth_headers, test_patient.id, datetime.now() + timedelta(days=10))
    await _create_opd_record(client, auth_headers, test_patient.id, follow_up_date=None)  # no follow-up — must not appear

    list_response = await client.get("/opd/follow-ups", headers=auth_headers)
    assert list_response.status_code == 200
    ids = {r["id"] for r in list_response.json()}
    assert overdue["id"] in ids
    assert upcoming["id"] in ids
    assert all(r["follow_up_status"] == "pending" for r in list_response.json())


@pytest.mark.asyncio
async def test_mark_attendance_removes_from_pending_list(client, auth_headers, test_patient):
    record = await _create_opd_record(client, auth_headers, test_patient.id, datetime.now() + timedelta(days=2))

    mark_response = await client.post(f"/opd/follow-ups/{record['id']}/attendance", headers=auth_headers)
    assert mark_response.status_code == 200
    body = mark_response.json()
    assert body["follow_up_status"] == "attended"
    assert body["follow_up_attended_at"] is not None

    list_response = await client.get("/opd/follow-ups", headers=auth_headers)
    ids = {r["id"] for r in list_response.json()}
    assert record["id"] not in ids


@pytest.mark.asyncio
async def test_mark_attendance_requires_ownership(client, auth_headers, test_patient):
    record = await _create_opd_record(client, auth_headers, test_patient.id, datetime.now())

    bad_response = await client.post("/opd/follow-ups/does-not-exist/attendance", headers=auth_headers)
    assert bad_response.status_code == 404
