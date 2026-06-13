from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.core.database import connect_db, disconnect_db
from app.jobs import start_reminder_scheduler, shutdown_reminder_scheduler
from app.routers import auth, patients, opd, appointments, ipd, ai, consent, nurses, documents, surgical_templates, admin


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

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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


@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "noni-tura-api"}
