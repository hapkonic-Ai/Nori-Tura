# syntax=docker/dockerfile:1
FROM python:3.11-slim-bookworm

# Install system dependencies required by WeasyPrint and general runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libpango-1.0-0 \
        libharfbuzz0b \
        libpangoft2-1.0-0 \
        libgdk-pixbuf-2.0-0 \
        libffi-dev \
        libatomic1 \
        ca-certificates \
        shared-mime-info \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app/backend

# Install Python dependencies first (better layer caching)
COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend source
COPY backend/ .

# Generate Prisma client at build time
RUN prisma generate

# Expose FastAPI port
EXPOSE 8000

# Deployment entrypoint: push schema, seed superadmin, start uvicorn
COPY scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
