"""Consent form PDF generation service.

Generates unsigned and signed informed-consent PDFs from Jinja2 HTML templates
using WeasyPrint. Computes SHA-256 document hashes for integrity verification
and embeds QR codes for quick verification lookups.
"""

import logging
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

from jinja2 import Template

from app.utils.pdf_integrity import compute_sha256, format_truncated_hash
from app.utils.qr_generator import generate_consent_qr_data_uri

logger = logging.getLogger(__name__)

_TEMPLATES_DIR = Path(__file__).resolve().parent.parent / "templates" / "consents"


def _load_template(name: str) -> Template:
    """Load a Jinja2 template from the templates/consents directory."""
    template_path = _TEMPLATES_DIR / name
    try:
        return Template(template_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        logger.error("Consent template not found: %s", template_path)
        raise RuntimeError(f"Consent template not found: {template_path}") from exc


def _render_pdf(html: str) -> bytes:
    """Render HTML to PDF using WeasyPrint, falling back to raw HTML bytes."""
    try:
        from weasyprint import HTML
        return HTML(string=html).write_pdf()
    except Exception as exc:
        logger.warning(
            "WeasyPrint unavailable (%s); returning HTML fallback for consent PDF", exc
        )
        return html.encode("utf-8")


def _build_common_context(form_data: Dict[str, Any]) -> Dict[str, Any]:
    """Build the base template context from form_data.

    Ensures all template variables have a value so the PDF renders cleanly even
    when optional fields are omitted by older clients.
    """
    now = datetime.now().isoformat()
    context = dict(form_data)

    # Core identifiers and metadata
    context.setdefault("consent_id", "")
    context.setdefault("consent_number", "")
    context.setdefault("version", "v2.1")
    context.setdefault("status", "Pending")
    context.setdefault("generated_at", now)
    context.setdefault("signed_at", "")
    context.setdefault("language", "English")
    context.setdefault("form_type", "Surgical Consent")

    # Hospital
    context.setdefault("hospital_name", "Hospital Name")
    context.setdefault("hospital_address", "")
    context.setdefault("hospital_contact", "")
    context.setdefault("hospital_registration_number", "")

    # Patient
    context.setdefault("patient_name", "—")
    context.setdefault("patient_uhid", "—")
    context.setdefault("age", "—")
    context.setdefault("gender", "—")
    context.setdefault("admission_number", "—")
    context.setdefault("department", "Pediatric Surgery")
    context.setdefault("ward_room", "—")

    # Guardian
    context.setdefault("parent_name", "—")
    context.setdefault("guardian_relationship", "Parent / Guardian")
    context.setdefault("parent_phone", "—")

    # Doctor
    context.setdefault("surgeon_name", "—")
    context.setdefault("doctor_qualification", "—")
    context.setdefault("doctor_registration_number", "—")

    # Clinical
    context.setdefault("diagnosis", "—")
    context.setdefault("procedure", "—")
    context.setdefault("procedure_description", "")
    context.setdefault("anesthesia", "—")
    context.setdefault("benefits", "—")
    context.setdefault("risks", "—")
    context.setdefault("material_risks", context.get("risks", "—"))
    context.setdefault("possible_complications", "—")
    context.setdefault("alternatives", "—")
    context.setdefault(
        "refusal_consequences",
        "The treating doctor will explain the consequences of refusal, which may include worsening of the patient's condition or other serious outcomes.",
    )
    context.setdefault("expected_recovery", "")
    context.setdefault("consent_for_anesthesia", "Yes")
    context.setdefault("consent_for_blood_products", "No")
    context.setdefault("consent_for_photography", "No")

    # Doctor declaration timestamp
    context.setdefault("doctor_declaration_timestamp", context.get("generated_at", now))

    return context


def _build_unsigned_context(form_data: Dict[str, Any]) -> Dict[str, Any]:
    """Build context for the unsigned consent template."""
    context = _build_common_context(form_data)

    # QR code for unsigned form uses consent id + patient id (no signed timestamp)
    qr_url = generate_consent_qr_data_uri(
        consent_id=context.get("consent_id") or "",
        patient_id=context.get("patient_id") or "",
        signed_at=None,
    )
    context["qr_code_url"] = qr_url or ""

    # Hash placeholder for unsigned form
    context.setdefault("pdf_hash_truncated", "PENDING SIGNATURE")

    return context


def _build_signed_context(
    form_data: Dict[str, Any],
    parent_signature_url: str,
    witness_name: Optional[str] = None,
    witness_relationship: Optional[str] = None,
    witness_mobile: Optional[str] = None,
    witness_signature_url: Optional[str] = None,
    signed_at: Optional[str] = None,
) -> Dict[str, Any]:
    """Build context for the signed consent template."""
    context = _build_common_context(form_data)
    context["status"] = "Signed"
    context["parent_signature_url"] = parent_signature_url
    context["witness_name"] = witness_name or ""
    context["witness_relationship"] = witness_relationship or ""
    context["witness_mobile"] = witness_mobile or ""
    context["witness_signature_url"] = witness_signature_url or ""
    context["signed_at"] = signed_at or datetime.now().isoformat()

    # QR code for signed form includes the signed timestamp
    qr_url = generate_consent_qr_data_uri(
        consent_id=context.get("consent_id") or "",
        patient_id=context.get("patient_id") or "",
        signed_at=context["signed_at"],
    )
    context["qr_code_url"] = qr_url or ""

    return context


def _compute_document_hash(html: str) -> str:
    """Compute a SHA-256 hash of the rendered HTML document.

    The hash is computed from the HTML content (before it is converted to PDF) so
    that the same hash can be printed in the PDF footer without generating the
    PDF twice. This provides a fast, stable document reference that can be
    re-verified by re-rendering the consent data and comparing HTML hashes.
    """
    return compute_sha256(html.encode("utf-8"))


def generate_consent_pdf(form_data: Dict[str, Any]) -> Dict[str, Any]:
    """Generate an unsigned consent form PDF.

    Returns a dict with:
        - pdf_bytes: the generated PDF bytes
        - html: the rendered HTML string
        - pdf_hash: SHA-256 hex digest of the rendered HTML document
        - pdf_hash_truncated: human-readable truncated hash
    """
    context = _build_unsigned_context(form_data)
    template = _load_template("consent_base.html")

    # First render: compute content hash from the HTML.
    first_html = template.render(**context)
    content_hash = _compute_document_hash(first_html)

    # Second render: embed the truncated hash into the footer and generate PDF once.
    context["pdf_hash_truncated"] = format_truncated_hash(content_hash)
    final_html = template.render(**context)
    final_pdf_bytes = _render_pdf(final_html)

    return {
        "pdf_bytes": final_pdf_bytes,
        "html": final_html,
        "pdf_hash": content_hash,
        "pdf_hash_truncated": context["pdf_hash_truncated"],
    }


def generate_signed_consent_pdf(
    form_data: Dict[str, Any],
    parent_signature_url: str,
    witness_name: Optional[str] = None,
    witness_relationship: Optional[str] = None,
    witness_mobile: Optional[str] = None,
    witness_signature_url: Optional[str] = None,
    signed_at: Optional[str] = None,
) -> Dict[str, Any]:
    """Generate a signed consent form PDF with embedded signature images.

    Returns a dict with:
        - pdf_bytes: the generated PDF bytes
        - html: the rendered HTML string
        - pdf_hash: SHA-256 hex digest of the rendered HTML document
        - pdf_hash_truncated: human-readable truncated hash
    """
    context = _build_signed_context(
        form_data=form_data,
        parent_signature_url=parent_signature_url,
        witness_name=witness_name,
        witness_relationship=witness_relationship,
        witness_mobile=witness_mobile,
        witness_signature_url=witness_signature_url,
        signed_at=signed_at,
    )
    template = _load_template("consent_signed.html")

    # First render: compute content hash from the HTML.
    first_html = template.render(**context)
    content_hash = _compute_document_hash(first_html)

    # Second render: embed the truncated hash into the footer and generate PDF once.
    context["pdf_hash_truncated"] = format_truncated_hash(content_hash)
    final_html = template.render(**context)
    final_pdf_bytes = _render_pdf(final_html)

    return {
        "pdf_bytes": final_pdf_bytes,
        "html": final_html,
        "pdf_hash": content_hash,
        "pdf_hash_truncated": context["pdf_hash_truncated"],
    }


def render_consent_html_preview(form_data: Dict[str, Any], signed: bool = False) -> str:
    """Render consent HTML for preview/debugging without generating a PDF."""
    if signed:
        context = _build_signed_context(
            form_data=form_data,
            parent_signature_url=form_data.get("parent_signature_url", ""),
            witness_name=form_data.get("witness_name"),
            witness_relationship=form_data.get("witness_relationship"),
            witness_mobile=form_data.get("witness_mobile"),
            witness_signature_url=form_data.get("witness_signature_url"),
            signed_at=form_data.get("signed_at"),
        )
        template = _load_template("consent_signed.html")
    else:
        context = _build_unsigned_context(form_data)
        template = _load_template("consent_base.html")

    return template.render(**context)
