"""
Seed/update `consent_content_templates` from a flattened EN/HI export.

Run after `prisma db push`:
  cd backend && python scripts/seed_consent_content_templates.py path/to/export.json

This does NOT connect to the analysis staging database (the `consent_pg`
Postgres container used to validate the client's 118-form corpus) — that is
a separate one-off step, expected to flatten `consent_pg.forms` + `sections`
into the JSON shape below and hand it to this script. This script's only job
is: read that JSON, upsert `consent_content_templates` rows from it.

Input file shape — a JSON array, one object per procedure:

{
  "name": "string, template display name",
  "procedure": "string, matches consent_content_templates.procedure (used as the upsert key)",
  "approach": "open | laparoscopic | thoracoscopic | endoscopic/natural",
  "en": {
    "procedure_description": "...", "risks": ["...", "..."], "benefits": ["..."],
    "alternatives": ["..."], "possible_complications": ["..."], "material_risks": "...",
    "post_op_care": "...", "expected_recovery": "...", "anaesthesia": ["..."],
    "diagnosis_plain_language": "...", "consequences_of_no_treatment": "...",
    "consented_contingencies": ["...", "..."], "cost_conditional_note": "...",
    "specimen_handling_statement": "...", "site_side_instruction": "...",
    "conversion_consequences": "...", "technique_options": ["...", "..."],
    "lifelong_follow_up_note": "...", "pre_op_prep": "..."
  },
  "hi": { "...same keys, or {} / omitted if no Hindi twin exists yet...": "..." },
  "cost_category": "minor | intermediate | major | complex",
  "is_lateralizable": true,
  "conversion_to_open_possible": false,
  "staging": {"stage_number": 1, "total_stages": 2},
  "lifelong_follow_up_flag": false,
  "statutory_reference": "Digital Personal Data Protection Act, 2023"
}

`sections.role` (from the consent_pg analysis DB) maps into `en`/`hi` keys as:
  diagnosis -> diagnosis_plain_language (+ consequences_of_no_treatment, split out)
  benefits  -> benefits (+ consequences_of_no_treatment where separately called out)
  alternatives -> alternatives
  risks     -> risks (+ possible_complications, per the corpus's two risk tiers)
  conversion -> conversion_consequences
  anaesthesia -> anaesthesia
The exact split is a one-off mapping decision made by whoever writes the
consent_pg -> export.json flattening step; this script does not re-derive it.

Behavior:
  - Upserts by `procedure` (find_first by procedure, create if absent, else update).
  - Always writes `_en` fields from `record["en"]`.
  - If `record["hi"]` is present and non-empty: writes `_hi` fields, sets
    hi_content_status="seeded".
  - If `record["hi"]` is absent/empty: leaves `_hi` fields untouched, sets
    hi_content_status="missing" (unless already "seeded"/"reviewed" from a
    prior run, in which case scores are not downgraded).
  - Idempotent — safe to re-run once more translations arrive later; a
    second pass with `hi` populated upgrades a procedure from "missing" to
    "seeded" without touching English content.
  - Prints a summary: created / updated / hi_seeded / hi_missing counts.
"""

import asyncio
import json
import sys
from pathlib import Path
from typing import Any, Dict, List

sys.path.insert(0, str(Path(__file__).parent.parent))

from prisma import Prisma

TEXT_FIELDS = [
    "procedure_description",
    "material_risks",
    "post_op_care",
    "expected_recovery",
    "diagnosis_plain_language",
    "consequences_of_no_treatment",
    "cost_conditional_note",
    "specimen_handling_statement",
    "site_side_instruction",
    "conversion_consequences",
    "lifelong_follow_up_note",
    "pre_op_prep",
]
LIST_FIELDS = [
    "anaesthesia",
    "risks",
    "benefits",
    "alternatives",
    "possible_complications",
    "consented_contingencies",
    "technique_options",
]


def _language_columns(lang_data: Dict[str, Any], suffix: str) -> Dict[str, Any]:
    """Map a flat en/hi sub-object into `{field}{suffix}` column names."""
    columns: Dict[str, Any] = {}
    for field in TEXT_FIELDS:
        if field in lang_data and lang_data[field]:
            columns[f"{field}{suffix}"] = lang_data[field]
    for field in LIST_FIELDS:
        if field in lang_data and lang_data[field]:
            columns[f"{field}{suffix}"] = list(lang_data[field])
    return columns


async def seed(records: List[Dict[str, Any]]) -> None:
    db = Prisma()
    await db.connect()

    created = updated = hi_seeded = hi_missing = 0

    try:
        for record in records:
            procedure = record["procedure"]
            en_data = record.get("en") or {}
            hi_data = record.get("hi") or {}

            data: Dict[str, Any] = {
                "name": record.get("name", procedure),
                "procedure": procedure,
                "approach": record.get("approach"),
                "cost_category": record.get("cost_category"),
                "is_lateralizable": bool(record.get("is_lateralizable", False)),
                "conversion_to_open_possible": bool(record.get("conversion_to_open_possible", False)),
                "lifelong_follow_up_flag": bool(record.get("lifelong_follow_up_flag", False)),
                "statutory_reference": record.get("statutory_reference") or "Digital Personal Data Protection Act, 2023",
            }
            staging = record.get("staging") or {}
            if staging.get("stage_number") and staging.get("total_stages"):
                data["staging_stage_number"] = staging["stage_number"]
                data["staging_total_stages"] = staging["total_stages"]

            data.update(_language_columns(en_data, "_en"))

            existing = await db.consent_content_templates.find_first(where={"procedure": procedure})

            if hi_data:
                data.update(_language_columns(hi_data, "_hi"))
                data["hi_content_status"] = "seeded"
                hi_seeded += 1
            elif not existing or existing.hi_content_status == "missing":
                data["hi_content_status"] = "missing"
                hi_missing += 1

            if existing:
                await db.consent_content_templates.update(where={"id": existing.id}, data=data)
                updated += 1
            else:
                data.setdefault("hi_content_status", "missing")
                await db.consent_content_templates.create(data=data)
                created += 1

        print(
            f"Done. created={created} updated={updated} "
            f"hi_seeded_this_run={hi_seeded} hi_missing_this_run={hi_missing}"
        )
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
