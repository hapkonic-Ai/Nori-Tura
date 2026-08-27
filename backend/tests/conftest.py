"""Shared test fixtures.

All tests run against a dedicated PostgreSQL test database (``nonitura_test``).
The schema is applied automatically with ``prisma db push`` before the test
session starts. Each test gets a fresh database connection so Prisma stays on
the same event loop as the test.
"""

import os
import subprocess
import sys
from unittest.mock import AsyncMock
from uuid import uuid4

import httpx
import pytest

# Point Prisma at the test database *before* any app code is imported.
TEST_DATABASE_URL = "postgresql://nonitura:nonitura123@localhost:5432/nonitura_test"
os.environ["DATABASE_URL"] = TEST_DATABASE_URL


def _ensure_test_schema():
    """Apply the current Prisma schema to the test database."""
    result = subprocess.run(
        [sys.executable, "-m", "prisma", "db", "push", "--skip-generate", "--accept-data-loss"],
        cwd=os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        capture_output=True,
        text=True,
        env={**os.environ, "DATABASE_URL": TEST_DATABASE_URL},
    )
    if result.returncode != 0:
        raise RuntimeError(f"Failed to push test schema:\n{result.stderr}\n{result.stdout}")


_ensure_test_schema()

# Import the app and patch its lifespan DB hooks so the app does not connect
# on its own; tests manage the connection per-test below.
from app.core.database import prisma  # noqa: E402
from app.core.security import create_access_token  # noqa: E402
from app.main import app  # noqa: E402

import app.main as _main_module  # noqa: E402
_main_module.connect_db = AsyncMock()
_main_module.disconnect_db = AsyncMock()


# Tables to truncate between tests for isolation.
_TRUNCATED_TABLES = [
    "media",
    "pre_op_notes",
    "intra_op_notes",
    "post_op_notes",
    "ward_round_notes",
    "discharge_summaries",
    "consent_forms",
    "whatsapp_logs",
    "appointments",
    "medical_record_images",
    "medical_records",
    "medications",
    "investigations",
    "opd_records",
    "ipd_admissions",
    "patients",
    "nurses",
    "doctors",
    "documents",
    "surgical_templates",
    "otp_sessions",
    "consent_acknowledgments",
    "admins",
    "hospitals",
]


@pytest.fixture
async def client():
    """Yield an async HTTP client backed by the FastAPI app.

    Connects to the test DB, truncates all data tables, then disconnects after
    the test so each test has its own fresh Prisma connection on the same loop.
    """
    await prisma.connect()
    await prisma.execute_raw(
        "TRUNCATE TABLE " + ", ".join(_TRUNCATED_TABLES) + " CASCADE;"
    )
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://testserver") as c:
        yield c
    await prisma.disconnect()


@pytest.fixture
async def test_doctor(client):
    """Create a verified doctor in the test database."""
    doctor = await prisma.doctors.create(
        data={
            "name": "Dr. Test",
            "phone": "+919876543210",
            "specialty": "General Surgery",
            "is_active": True,
        }
    )
    return doctor


@pytest.fixture
def auth_headers(test_doctor):
    """Return Authorization headers for the test doctor."""
    token = create_access_token(
        {
            "phone": test_doctor.phone,
            "role": "surgeon",
            "doctor_id": test_doctor.id,
        }
    )
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
async def test_patient(client, test_doctor):
    """Create a patient owned by the test doctor."""
    patient = await prisma.patients.create(
        data={
            "doctor_id": test_doctor.id,
            "name": "Test Patient",
            "age": 30,
            "gender": "male",
            "blood_group": "O+",
            "parent_name": "Parent",
            "parent_phone": "+919999999999",
        }
    )
    return patient


@pytest.fixture
async def test_admission(client, test_doctor, test_patient):
    """Create an active admission for the test patient."""
    admission = await prisma.ipd_admissions.create(
        data={
            "patient_id": test_patient.id,
            "doctor_id": test_doctor.id,
            "urgency": "elective",
            "ward": "1",
            "bed_no": "1",
            "status": "admitted",
        }
    )
    return admission
