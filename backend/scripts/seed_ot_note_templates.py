"""
Seed/update global `ot_note_templates` rows from the flattened OT-notes
corpus export.

Run after `prisma db push`:
  cd backend && python scripts/seed_ot_note_templates.py scripts/ot_notes_corpus_export.json

Idempotent — upserts by `procedure` (create if absent, update if present), so
it's safe to re-run if the corpus export is regenerated or corrected later.
All rows written here are global (`is_global=True`, `doctor_id=None`) —
doctors' own templates are created separately through the app.
"""

import asyncio
import json
import sys
from pathlib import Path
from typing import Any, Dict, List

sys.path.insert(0, str(Path(__file__).parent.parent))

from prisma import Prisma

FIELDS = [
    "name",
    "procedure",
    "approach",
    "source_reference",
    "anaesthesia",
    "preop_diagnosis",
    "postop_diagnosis",
    "operation_performed",
    "position_preparation",
    "incision_approach",
    "procedure_steps",
    "closure",
    "specimen",
    "implants",
    "drains",
    "estimated_blood_loss",
    "counts",
    "standard_complications",
    "postop_plan",
]


async def seed(records: List[Dict[str, Any]]) -> None:
    db = Prisma()
    await db.connect()

    created = updated = 0
    try:
        for record in records:
            procedure = record["procedure"]
            data = {field: record[field] for field in FIELDS if field in record}
            data["is_global"] = True
            data["doctor_id"] = None

            existing = await db.ot_note_templates.find_first(
                where={"procedure": procedure, "is_global": True}
            )
            if existing:
                await db.ot_note_templates.update(where={"id": existing.id}, data=data)
                updated += 1
            else:
                await db.ot_note_templates.create(data=data)
                created += 1

        print(f"Done. created={created} updated={updated}")
    finally:
        await db.disconnect()


def main() -> None:
    if len(sys.argv) != 2:
        print(f"Usage: python {Path(__file__).name} path/to/export.json")
        sys.exit(1)

    export_path = Path(sys.argv[1])
    records = json.loads(export_path.read_text(encoding="utf-8"))
    if not isinstance(records, list):
        print("Expected a JSON array of procedure records at the top level.")
        sys.exit(1)

    asyncio.run(seed(records))


if __name__ == "__main__":
    main()
