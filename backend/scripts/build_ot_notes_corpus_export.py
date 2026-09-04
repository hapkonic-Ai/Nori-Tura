"""
Flatten the client's operative-note corpus (Postgres tables `ot_forms` +
`ot_sections`, restored from consent_forms_ot.dump — the same dump used for
the consent-forms corpus) into the JSON shape `seed_ot_note_templates.py`
consumes.

The corpus has one skeleton across 79 docx-derived operative-note templates
(Spitz & Coran, Operative Pediatric Surgery 7E), verified 79/79 present for
every slot except `incision_approach` (54/79 — intentionally absent for
endoscopic/stoma/perineal-access procedures with no surgical incision).

Extraction rules (verified against the full corpus before being hardcoded
here):
  - Single-concept slots (anaesthesia, preop_diagnosis, postop_diagnosis,
    operation_performed, position_preparation, incision_approach, closure,
    postop_plan) -> use the section's `plain_text` directly. Several of these
    (postop_plan especially, up to 7 blocks) span multiple docx paragraphs,
    so reading only the first block (as an earlier pass of the consent-forms
    extraction mistakenly did before being fixed) would truncate them;
    `plain_text` is already the full reconstructed section text.
  - `procedure_steps` slot -> ordered list of `type='step'` blocks, in
    document order (5-17 steps per file, median 10). This preserves the
    corpus's [EDIT]/[VAR] placeholder markers verbatim, per the confirmed
    freeform-text approach (the doctor edits/replaces each line directly).
  - `operative_details` slot -> six bullets, always in this order, always
    present (100/100 coverage verified): "Specimen:", "Implants:",
    "Drains:", "Estimated blood loss:", "Counts:", "Complications:". Split
    by prefix match into specimen/implants/drains/estimated_blood_loss/
    counts/standard_complications.
  - `header_meta` slot -> only the "operation_id: ... · source: ..." citation
    line is kept, as `source_reference`. The letterhead placeholder, DRAFT
    disclaimer, and patient/date blank lines are never carried through as
    content (per OT_PLACEHOLDERS_CAUTION.md: placeholder/provenance lines
    are not document content).
  - `findings` and `team_table` slots are per-surgery, not template content
    — never extracted (an agent must not invent findings; the roster is
    filled in per operation, not templated).

Run:
    cd backend
    python scripts/build_ot_notes_corpus_export.py \\
        --db-url postgresql://postgres:scratch@localhost:5599/consent_forms \\
        --out scripts/ot_notes_corpus_export.json
"""

import argparse
import json
from pathlib import Path

import psycopg2
import psycopg2.extras

SINGLE_FIELDS = [
    "anaesthesia",
    "preop_diagnosis",
    "postop_diagnosis",
    "operation_performed",
    "position_preparation",
    "incision_approach",
    "closure",
    "postop_plan",
]

OPERATIVE_DETAIL_MARKERS = [
    ("specimen", "Specimen"),
    ("implants", "Implants"),
    ("drains", "Drains"),
    ("estimated_blood_loss", "Estimated blood loss"),
    ("counts", "Counts"),
    ("standard_complications", "Complications"),
]


def _humanize(operation_id: str) -> str:
    return " ".join(word.capitalize() for word in operation_id.split("_"))


def _section_text(cur, form_id: int, slot: str):
    cur.execute(
        "SELECT plain_text FROM ot_sections WHERE form_id = %s AND slot = %s",
        (form_id, slot),
    )
    row = cur.fetchone()
    return row["plain_text"].strip() if row and row["plain_text"] else None


def _procedure_steps(cur, form_id: int):
    cur.execute(
        "SELECT blocks FROM ot_sections WHERE form_id = %s AND slot = 'procedure_steps'",
        (form_id,),
    )
    row = cur.fetchone()
    if not row or not row["blocks"]:
        return []
    blocks = row["blocks"]
    if isinstance(blocks, str):
        blocks = json.loads(blocks)
    return [b["text"].strip() for b in blocks if b.get("type") == "step" and b.get("text")]


def _operative_details(cur, form_id: int) -> dict:
    cur.execute(
        "SELECT blocks FROM ot_sections WHERE form_id = %s AND slot = 'operative_details'",
        (form_id,),
    )
    row = cur.fetchone()
    if not row or not row["blocks"]:
        return {}
    blocks = row["blocks"]
    if isinstance(blocks, str):
        blocks = json.loads(blocks)

    fields = {}
    for b in blocks:
        text = (b.get("text") or "").strip()
        for field_name, marker in OPERATIVE_DETAIL_MARKERS:
            if text.startswith(marker):
                fields[field_name] = text
                break
    return fields


def _source_reference(cur, form_id: int):
    cur.execute(
        "SELECT plain_text FROM ot_sections WHERE form_id = %s AND slot = 'header_meta'",
        (form_id,),
    )
    row = cur.fetchone()
    if not row or not row["plain_text"]:
        return None
    for line in row["plain_text"].splitlines():
        line = line.strip()
        if line.startswith("operation_id:"):
            return line
    return None


def build_export(db_url: str) -> list:
    conn = psycopg2.connect(db_url)
    conn.autocommit = True
    records = []

    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute("SELECT * FROM ot_forms ORDER BY operation_id")
        forms = cur.fetchall()

        for form in forms:
            form_id = form["id"]
            operation_id = form["operation_id"]

            record = {
                "name": _humanize(operation_id),
                "procedure": operation_id,
                "source_reference": _source_reference(cur, form_id),
                "procedure_steps": _procedure_steps(cur, form_id),
            }
            for field in SINGLE_FIELDS:
                value = _section_text(cur, form_id, field)
                if value:
                    record[field] = value
            record.update(_operative_details(cur, form_id))

            records.append(record)

    conn.close()
    return records


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db-url", required=True, help="Postgres URL for the restored OT-notes corpus DB")
    parser.add_argument("--out", required=True, help="Output JSON file path")
    args = parser.parse_args()

    records = build_export(args.db_url)
    out_path = Path(args.out)
    out_path.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(records)} OT note template records to {out_path}")


if __name__ == "__main__":
    main()
