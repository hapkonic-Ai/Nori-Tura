import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from contextlib import asynccontextmanager

from app.core.database import connect_db, disconnect_db
from app.jobs import start_reminder_scheduler, shutdown_reminder_scheduler
from app.routers import auth, patients, opd, appointments, ipd, ai, consent, nurses, documents, surgical_templates, admin, doctors, schedule, alerts, uploads, media, medical_records, reports


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_db()
    start_reminder_scheduler()
    yield
    shutdown_reminder_scheduler()
    await disconnect_db()


app = FastAPI(
    title="Noni Tura API",
    description="Backend API for Noni Tura Surgical Care Platform",
    version="0.1.0",
    lifespan=lifespan
)

ALLOWED_ORIGINS = [
    "https://app.noritura.in",
    "https://surgeons.noritura.in",
]
render_url = os.getenv("RENDER_EXTERNAL_URL")
if render_url:
    ALLOWED_ORIGINS.append(render_url)
if os.getenv("ENVIRONMENT", "development") == "development":
    ALLOWED_ORIGINS += [
        "http://localhost:3000",
        "http://localhost:8080",
        "http://localhost:8001",
        "http://10.0.2.2:8000",  # Android emulator
        "http://10.0.2.2:8001",  # Android emulator (local backend)
    ]

# Allow additional origins from environment (e.g. for custom deployments)
_extra_origins = os.getenv("EXTRA_CORS_ORIGINS", "")
if _extra_origins:
    ALLOWED_ORIGINS.extend([origin.strip() for origin in _extra_origins.split(",") if origin.strip()])

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["Content-Type", "Authorization"],
    max_age=600,
)


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        if os.getenv("ENVIRONMENT") == "production":
            response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        return response

app.add_middleware(SecurityHeadersMiddleware)

app.include_router(auth.router)
app.include_router(patients.router)
app.include_router(opd.router)
app.include_router(appointments.router)
app.include_router(ipd.router)
app.include_router(ai.router)
app.include_router(consent.router)
app.include_router(nurses.router)
app.include_router(documents.router)
app.include_router(surgical_templates.router)
app.include_router(admin.router)
app.include_router(doctors.router)
app.include_router(schedule.router)
app.include_router(alerts.router)
app.include_router(uploads.router)
app.include_router(media.router)
app.include_router(medical_records.router)
app.include_router(reports.router)


@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "noni-tura-api"}
