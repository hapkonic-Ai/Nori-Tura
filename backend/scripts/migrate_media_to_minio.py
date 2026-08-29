"""Backfill legacy media blobs from Postgres into MinIO.

Run from the backend directory with the virtual environment active:

    python scripts/migrate_media_to_minio.py

The script is idempotent: rows already migrated (bucket/object_key set) are skipped.
"""

import asyncio
import sys
from pathlib import Path

# Ensure the backend package is importable when running the script directly.
backend_root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_root))

from app.core.database import connect_db, disconnect_db, prisma
from app.core.storage import get_media_storage


async def main() -> None:
    await connect_db()
    storage = get_media_storage()

    try:
        rows = await prisma.media.find_many(
            where={
                "data": {"not": None},
                "bucket": None,
                "object_key": None,
            }
        )

        if not rows:
            print("No legacy media rows need migration.")
            return

        print(f"Migrating {len(rows)} media row(s) to MinIO bucket '{storage.bucket}'...")

        migrated = 0
        failed = 0

        for row in rows:
            try:
                raw_bytes = row.data.decode() if hasattr(row.data, "decode") else row.data
                object_key = storage.object_key(row.id, row.filename)

                storage.upload_object(
                    object_key=object_key,
                    data=raw_bytes,
                    content_type=row.mime_type,
                )

                await prisma.media.update(
                    where={"id": row.id},
                    data={
                        "bucket": storage.bucket,
                        "object_key": object_key,
                        "storage_backend": "minio",
                        "data": None,
                    },
                )

                migrated += 1
                print(f"  ✓ {row.id} -> {object_key}")
            except Exception as exc:
                failed += 1
                print(f"  ✗ {row.id}: {exc}")

        print(f"\nDone. Migrated: {migrated}, Failed: {failed}, Total: {len(rows)}")
    finally:
        await disconnect_db()


if __name__ == "__main__":
    asyncio.run(main())
