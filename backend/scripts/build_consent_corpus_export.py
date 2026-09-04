"""
Flatten the client's consent-form analysis corpus (Postgres tables `forms` +
`sections`, restored from consent_forms_ot.dump) into the JSON shape that
`seed_consent_content_templates.py` consumes.

The corpus has one spine family across 118 docx-derived forms (80 EN, 38 with
Hindi twins), validated with 687 automated checks (see ANALYSIS_NOTES.md /
FIELD_GAP_ANALYSIS.md from the client's own analysis pass). Every field this
script extracts was verified against the full corpus before being hardcoded
here — see the extraction rules below.

Extraction rules (verified against all 118 forms, both languages):
  - risks section: every form has exactly 3 bold blocks, in fixed order
    [General (skip — spine constant), Specific-to-this-operation (skip,
    header), Specific-to-my-child (skip — per-patient, not template)].
    Bullets between bold #2 and bold #3 -> possible_complications.
    The "General:" bold block itself is stored as risks (a single boilerplate
    sentence, present for the app's existing required "General Risks" field).
  - benefits section: exactly 1 bold block per form ("If not done: ...")
    -> consequences_of_no_treatment. The leading non-bold para -> benefits.
  - diagnosis section: first non-bold para -> diagnosis_plain_language.
    Bold blocks in fixed order: when has_site_side, [site/side,
    what-it-involves, contingencies] else [what-it-involves, contingencies].
  - alternatives section: single para -> alternatives.
  - conversion section (35/118 forms only): single/multi para -> conversion_consequences.
  - blood_consents section: block starting "May also be needed during
    recovery:" / "...जरूरत पड़ सकती है:" -> post_op_care (marker prefix
    verified present on all 118 forms). Block(s) from "Cost:" / "खर्च:"
    onward -> cost_conditional_note. Everything else in this section (blood
    transfusion, photography, specimen, DPDP boilerplate) is a spine constant
    already rendered by consent_base.html's layout, not per-template content.
  - anaesthesia, patient_info, extension, declaration_*, witness sections:
    spine constants per the corpus's own field-gap analysis — not extracted.

Run:
    cd backend
    python scripts/build_consent_corpus_export.py \\
        --db-url postgresql://postgres:scratch@localhost:5599/consent_forms \\
        --out scripts/consent_corpus_export.json
"""

import argparse
import json
import re
import sys
from pathlib import Path

import psycopg2
import psycopg2.extras


def _strip_label(text: str, max_label_len: int = 100) -> str:
    """Strip a leading 'Label: ' marker (English or Hindi) from a block's text.

    Every marker block in this corpus has the exact shape "Label: rest of
    text" with the colon immediately after the label. Only strips when a
    colon appears within the first `max_label_len` characters, so this never
    mangles a block that happens not to have a label.
    """
    idx = text.find(":")
    if 0 < idx < max_label_len:
        return text[idx + 1:].strip()
    return text.strip()


def _blocks(cur, section_id: int):
    cur.execute("SELECT blocks FROM sections WHERE id = %s", (section_id,))
    return cur.fetchone()[0]


def _section(cur, form_id: int, role: str):
    cur.execute(
        "SELECT id, plain_text, blocks FROM sections WHERE form_id = %s AND role = %s",
        (form_id, role),
    )
    row = cur.fetchone()
    if row is None:
        return None
    blocks = row["blocks"]
    if isinstance(blocks, str):
        blocks = json.loads(blocks)
    return row["id"], row["plain_text"], blocks


def _humanize(procedure: str) -> str:
    return " ".join(word.capitalize() for word in procedure.split("_"))


_SURGEON_LINE = ("surgeon:", "सर्जन:")


def _drop_surgeon_placeholder_line(blocks: list) -> list:
    """Drop the trailing 'Surgeon: Dr. ___ and team.' placeholder block.

    It always trails the diagnosis section as its own block and carries no
    template content — without dropping it, it would bleed into whichever
    field's extraction span runs to the end of the block list.
    """
    return [b for b in blocks if not b["text"].strip().lower().startswith(_SURGEON_LINE)]


def _split_bold_spans(blocks: list):
    """Split a block list into (header, content_blocks) spans at bold-block
    boundaries. The first span's header is None (text before the first bold
    block, if any). `content_blocks` holds every block following a header up
    to (not including) the next bold block — this is what makes wrapped
    paragraphs (a label's sentence split across two docx blocks) join back
    together instead of being silently truncated at the first block.
    """
    spans = []
    header = None
    content: list = []
    for b in blocks:
        if b.get("bold"):
            spans.append((header, content))
            header, content = b, []
        else:
            content.append(b)
    spans.append((header, content))
    return spans


def _span_prose(header, content) -> str:
    """Join a span's header + non-bullet content blocks into one string,
    with the header's leading label stripped."""
    parts = [header["text"]] if header else []
    parts.extend(b["text"] for b in content if b.get("type") != "bullet")
    return _strip_label(" ".join(p.strip() for p in parts if p.strip()).strip())


def _span_bullets(content) -> list:
    return [b["text"].strip() for b in content if b.get("type") == "bullet"]


def extract_language_fields(cur, form_row: dict) -> dict:
    """Extract the bilingual-mappable fields for one form (one language)."""
    form_id = form_row["id"]
    fields: dict = {}

    # ── diagnosis ────────────────────────────────────────────────────────
    diagnosis = _section(cur, form_id, "diagnosis")
    if diagnosis:
        _, _, raw_blocks = diagnosis
        blocks = _drop_surgeon_placeholder_line(raw_blocks)
        spans = _split_bold_spans(blocks)
        # spans[0] = (None, [diagnosis para, "Operation: ..." para, ...]) —
        # only the first block is the actual diagnosis text; "Operation:"
        # is already captured structurally via forms.operation/approach.
        if spans and spans[0][1]:
            fields["diagnosis_plain_language"] = _strip_label(spans[0][1][0]["text"])

        bold_spans = spans[1:]  # one span per bold header, in document order
        idx = 0
        if form_row["has_site_side"] and len(bold_spans) >= 3:
            fields["site_side_instruction"] = _strip_label(bold_spans[0][0]["text"])
            idx = 1
        if len(bold_spans) >= idx + 2:
            header, content = bold_spans[idx]
            fields["procedure_description"] = _span_prose(header, content)
            technique_options = _span_bullets(content)
            if technique_options:
                fields["technique_options"] = technique_options

            header, content = bold_spans[idx + 1]
            contingency_text = _span_prose(header, content)
            if contingency_text:
                fields["consented_contingencies"] = [contingency_text]

    # ── benefits (always exactly 3 blocks: intro / "If not done:" / generic
    #    "surgery cannot guarantee..." disclaimer — verified across all 118
    #    forms, no continuation wrapping occurs here) ─────────────────────
    benefits = _section(cur, form_id, "benefits")
    if benefits:
        _, _, blocks = benefits
        non_bold = [b for b in blocks if not b.get("bold")]
        bold = [b for b in blocks if b.get("bold")]
        if non_bold:
            fields["benefits"] = [non_bold[0]["text"].strip()]
        if bold:
            fields["consequences_of_no_treatment"] = _strip_label(bold[0]["text"])

    # ── alternatives ─────────────────────────────────────────────────────
    alternatives = _section(cur, form_id, "alternatives")
    if alternatives:
        _, plain_text, _ = alternatives
        fields["alternatives"] = [plain_text.strip()]

    # ── risks / possible_complications ──────────────────────────────────
    # Every risks section has exactly 3 bold blocks, in fixed order:
    # [General (spine constant), Specific-to-this-operation (header),
    # Specific-to-my-child (per-patient, not template)]. The "General:"
    # sentence sometimes wraps into a following non-bold block, which the
    # span model folds back in instead of truncating.
    risks = _section(cur, form_id, "risks")
    if risks:
        _, _, blocks = risks
        spans = _split_bold_spans(blocks)
        bold_spans = spans[1:]
        if len(bold_spans) == 3:
            general_header, general_content = bold_spans[0]
            fields["risks"] = [_span_prose(general_header, general_content)]
            _, complications_content = bold_spans[1]
            complications = [b["text"].strip() for b in complications_content]
            if complications:
                fields["possible_complications"] = complications

    # ── conversion (only on the 35 lap/thoracoscopic forms) ─────────────
    conversion = _section(cur, form_id, "conversion")
    if conversion:
        _, plain_text, _ = conversion
        fields["conversion_consequences"] = plain_text.strip()

    # ── blood_consents: post-op care + cost note ────────────────────────
    # These two are the only per-procedure content in this section — blood
    # transfusion, photography, specimen and DPDP paragraphs are identical
    # spine-constant boilerplate handled by consent_base.html's layout.
    blood = _section(cur, form_id, "blood_consents")
    if blood:
        _, _, blocks = blood
        post_op_markers = ("may also be needed during recovery:", "ठीक होने के दौरान")
        cost_markers = ("cost:", "खर्च:")
        boundary_markers = (
            "blood transfusion", "खून चढ़ाना",
            "photography", "फोटो / वीडियो",
            "specimen", "ऑपरेशन में निकाला गया नमूना",
        )

        def _matches(text: str, markers) -> bool:
            lowered = text.lower()
            return any(m in lowered or m in text for m in markers)

        post_op_parts, cost_parts = [], []
        state = None
        for b in blocks:
            text = b["text"].strip()
            if _matches(text, post_op_markers):
                state = "post_op"
            elif _matches(text, cost_markers):
                state = "cost"
            elif _matches(text, boundary_markers):
                state = None
            if state == "post_op":
                post_op_parts.append(text)
            elif state == "cost":
                cost_parts.append(text)
        if post_op_parts:
            fields["post_op_care"] = _strip_label(" ".join(post_op_parts))
        if cost_parts:
            fields["cost_conditional_note"] = " ".join(cost_parts)

    return fields


def build_export(db_url: str) -> list:
    conn = psycopg2.connect(db_url)
    conn.autocommit = True
    records = []

    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute("SELECT * FROM forms WHERE language = 'EN' ORDER BY procedure")
        en_forms = cur.fetchall()

        for en_form in en_forms:
            procedure = en_form["procedure"]
            en_fields = extract_language_fields(cur, en_form)

            cur.execute(
                "SELECT * FROM forms WHERE procedure = %s AND language = 'HI'",
                (procedure,),
            )
            hi_form = cur.fetchone()
            hi_fields = extract_language_fields(cur, hi_form) if hi_form else {}

            record = {
                "name": _humanize(procedure),
                "procedure": procedure,
                "approach": en_form["approach_kind"],
                "en": en_fields,
                "hi": hi_fields,
                "cost_category": en_form["cost_category"],
                "is_lateralizable": bool(en_form["has_site_side"]),
                "conversion_to_open_possible": bool(en_form["has_conversion_section"]),
                "lifelong_follow_up_flag": False,
                "statutory_reference": "Digital Personal Data Protection Act, 2023",
            }
            records.append(record)

    conn.close()
    return records


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db-url", required=True, help="Postgres URL for the restored analysis corpus DB")
    parser.add_argument("--out", required=True, help="Output JSON file path")
    args = parser.parse_args()

    records = build_export(args.db_url)
    out_path = Path(args.out)
    out_path.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    hi_count = sum(1 for r in records if r["hi"])
    print(f"Wrote {len(records)} procedure records to {out_path} ({hi_count} with Hindi content, {len(records) - hi_count} English-only)")


if __name__ == "__main__":
    main()
