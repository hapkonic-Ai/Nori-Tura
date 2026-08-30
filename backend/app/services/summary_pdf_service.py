"""Patient overall medical summary PDF generation service."""

import logging
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List

from jinja2 import Template

from app.core.database import prisma
from app.utils.pdf_integrity import compute_sha256, format_truncated_hash

logger = logging.getLogger(__name__)

_TEMPLATES_DIR = Path(__file__).resolve().parent.parent / "templates" / "summary"


def _load_template(name: str) -> Template:
    template_path = _TEMPLATES_DIR / name
    try:
        return Template(template_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        logger.error("Summary template not found: %s", template_path)
        raise RuntimeError(f"Summary template not found: {template_path}") from exc


def _render_pdf(html: str) -> bytes:
    try:
        from weasyprint import HTML
        return HTML(string=html).write_pdf()
    except Exception as exc:
        logger.warning("WeasyPrint unavailable (%s); trying wkhtmltopdf fallback", exc)

    try:
        import pdfkit
        import platform

        config = None
        if platform.system() == "Windows":
            candidates = [
                r"C:\Program Files\wkhtmltopdf\bin\wkhtmltopdf.exe",
                r"C:\Program Files (x86)\wkhtmltopdf\bin\wkhtmltopdf.exe",
            ]
            for path in candidates:
                if Path(path).exists():
                    config = pdfkit.configuration(wkhtmltopdf=path)
                    break

        return pdfkit.from_string(html, False, configuration=config)
    except Exception as exc:
        logger.warning("wkhtmltopdf fallback failed (%s); returning HTML fallback", exc)

    return html.encode("utf-8")


def _format_datetime(value: Any) -> str:
    if value is None:
        return "—"
    if isinstance(value, datetime):
        return value.strftime("%d %b %Y %H:%M")
    return str(value)


async def generate_patient_summary_pdf(patient_id: str) -> bytes:
    patient = await prisma.patients.find_first(
        where={"id": patient_id},
        include={"doctor": True, "hospital": True},
    )
    if not patient:
        raise ValueError("Patient not found")

    admissions = await prisma.ipd_admissions.find_many(
        where={"patient_id": patient_id},
        order={"admitted_at": "desc"},
        include={
            "pre_op_notes": True,
            "intra_op_notes": True,
            "post_op_notes": True,
            "ward_round_notes": True,
            "discharge_summaries": True,
            "consent_forms": {"order_by": {"generated_at": "desc"}},
            "hospital": True,
            "doctor": True,
        },
    )

    opd_records = await prisma.opd_records.find_many(
        where={"patient_id": patient_id},
        order={"created_at": "desc"},
        include={"medications": True, "investigations": True, "doctor": True, "hospital": True},
    )

    documents = await prisma.documents.find_many(
        where={"patient_id": patient_id},
        order={"uploaded_at": "desc"},
    )

    medical_records = await prisma.medical_records.find_many(
        where={"patient_id": patient_id},
        order={"created_at": "desc"},
        include={"images": True},
    )

    total_admissions = len(admissions)
    total_opd_visits = len(opd_records)
    active_statuses = {"admitted", "pre-op", "in-surgery", "recovery"}
    active_admission = any(getattr(a, "status", None) in active_statuses for a in admissions)

    all_consents = [cf for a in admissions for cf in (a.consent_forms or [])]
    total_consents = len(all_consents)
    signed_consents = sum(1 for cf in all_consents if getattr(cf, "signed_at", None) is not None)

    context = {
        "generated_at": datetime.now().isoformat(),
        "patient": {
            "id": patient.id,
            "name": patient.name,
            "age": patient.age,
            "gender": patient.gender,
            "blood_group": patient.blood_group or "—",
            "allergies": patient.allergies or "—",
            "parent_name": patient.parent_name,
            "parent_phone": patient.parent_phone,
        },
        "doctor": patient.doctor,
        "hospital": patient.hospital,
        "summary": {
            "total_opd_visits": total_opd_visits,
            "total_admissions": total_admissions,
            "active_admission": active_admission,
            "total_consents": total_consents,
            "signed_consents": signed_consents,
            "pending_consents": total_consents - signed_consents,
            "total_documents": len(documents),
            "total_medical_records": len(medical_records),
        },
        "opd_records": opd_records,
        "admissions": admissions,
        "documents": documents,
        "medical_records": medical_records,
        "format_datetime": _format_datetime,
    }

    template = _load_template("patient_summary.html")
    html = template.render(**context)
    pdf_bytes = _render_pdf(html)
    return pdf_bytes
