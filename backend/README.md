# Noni Tura — Backend

FastAPI-based backend for the Noni Tura pediatric surgical care platform.

## Stack

- **Framework:** FastAPI 0.111.0
- **Python:** 3.11
- **ORM:** Prisma Client Python 0.13.1
- **Database:** PostgreSQL 16
- **PDF:** WeasyPrint 62.3 (with HTML fallback when system libs are missing)
- **AI:** OpenAI / Anthropic (fallback when no API key)

## Local Setup

```bash
cd backend
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Start PostgreSQL
docker compose up -d

# Copy environment variables
cp .env.example .env

# Generate Prisma client and push schema
prisma generate
prisma db push

# Run server
uvicorn app.main:app --reload
```

## Environment Variables

See `.env.example` for required variables:

- `DATABASE_URL`
- `JWT_SECRET`
- `OPENAI_API_KEY` / `ANTHROPIC_API_KEY`
- `META_WA_TOKEN` / `META_WA_PHONE_ID`
- `TWOFACTOR_API_KEY`
- `CLOUDINARY_URL`
- `FIREBASE_CREDENTIALS_JSON`

## API Overview

| Prefix | Description |
|--------|-------------|
| `/auth` | OTP login (surgeon / nurse / parent), FCM token registration |
| `/patients` | Patient CRUD with doctor-pool isolation |
| `/opd` | OPD records, medications, investigations |
| `/appointments` | Appointment booking and status updates |
| `/ipd` | Admissions, pre-op / intra-op / post-op notes, ward rounds, discharge |
| `/ai` | AI suggestive diagnosis with audit logging |
| `/consent` | MCI/NABH consent form generation and digital signing |
| `/nurses` | Nurse management (surgeon only) |
| `/documents` | Document record tracking |

## Role-Based Access

- **Surgeon:** Full access within their doctor pool.
- **Nurse:** Can add patients, OPD records, admissions (urgency only), appointments, ward rounds, post-op notes. Cannot make surgical decisions or sign discharge summaries.
- **Parent:** Can view only their own children's data and book appointments for them.

## Notes

- External integrations (WhatsApp, FCM, Cloudinary) are stubbed to log only until credentials are configured.
- WeasyPrint requires Pango/GTK+ system libraries. On macOS install them with `brew install pango gdk-pixbuf libffi`. Without them, consent forms fall back to HTML bytes.
- WeasyPrint 62.3 is pinned to `pydyf==0.10.0` because newer `pydyf` versions are incompatible and raise `'super' object has no attribute 'transform'`.
- AI diagnosis returns a structured fallback response when no LLM API key is configured.

