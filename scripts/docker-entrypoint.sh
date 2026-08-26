#!/bin/sh
set -e

cd /app/backend

echo "==> Running Prisma db push..."
prisma db push

echo "==> Seeding default superadmin..."
python scripts/seed_superadmin.py

echo "==> Starting uvicorn..."
exec uvicorn app.main:app --host 0.0.0.0 --port 8000
