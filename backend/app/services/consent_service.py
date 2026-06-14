from datetime import datetime
from typing import Any, Dict, Optional

from jinja2 import Template


CONSENT_TEMPLATE = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Informed Consent Form</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.5; color: #333; }
    h1 { text-align: center; font-size: 22px; margin-bottom: 6px; }
    h2 { font-size: 16px; margin-top: 24px; border-bottom: 1px solid #ccc; padding-bottom: 4px; }
    .header { text-align: center; margin-bottom: 24px; }
    .section { margin-bottom: 12px; }
    .label { font-weight: bold; }
    .declaration { margin-top: 32px; }
    .signature { margin-top: 48px; }
    .footer { margin-top: 40px; font-size: 11px; color: #666; text-align: center; }
  </style>
</head>
<body>
  <div class="header">
    <h1>INFORMED CONSENT FORM</h1>
    <p>Pediatric Surgical Procedure</p>
  </div>

  <h2>Patient Details</h2>
  <div class="section"><span class="label">Patient Name:</span> {{ patient_name }}</div>
  <div class="section"><span class="label">Age / Gender:</span> {{ age }} / {{ gender }}</div>
  <div class="section"><span class="label">Parent/Guardian:</span> {{ parent_name }}</div>
  <div class="section"><span class="label">Parent Phone:</span> {{ parent_phone }}</div>

  <h2>Procedure</h2>
  <div class="section"><span class="label">Proposed Procedure:</span> {{ procedure }}</div>
  <div class="section"><span class="label">Anesthesia:</span> {{ anesthesia }}</div>
  <div class="section"><span class="label">Surgeon:</span> {{ surgeon_name }}</div>

  <h2>Risks</h2>
  <p>{{ risks }}</p>

  <h2>Benefits</h2>
  <p>{{ benefits }}</p>

  <h2>Alternatives</h2>
  <p>{{ alternatives }}</p>

  <h2>Post-operative Care</h2>
  <p>{{ post_op_care }}</p>

  <h2>Declaration</h2>
  <div class="declaration">
    <p>
      I, the undersigned parent/guardian of the above-named minor patient, have been explained the
      nature of the proposed procedure, its risks, benefits, alternatives, and post-operative care in
      a language I understand. I voluntarily give my consent for the procedure and anesthesia.
    </p>
  </div>

  <div class="signature">
    <div class="section"><span class="label">Parent/Guardian Signature:</span> ___________________________</div>
    <div class="section"><span class="label">Date:</span> ___________________________</div>
    <div class="section"><span class="label">Witness Name & Signature:</span> ___________________________</div>
  </div>

  <div class="footer">
    This form complies with MCI / NABH standards for informed consent for pediatric surgical procedures.
  </div>
</body>
</html>
"""


CONSENT_SIGNED_TEMPLATE = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Signed Informed Consent Form</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.5; color: #333; }
    h1 { text-align: center; font-size: 22px; margin-bottom: 6px; }
    h2 { font-size: 16px; margin-top: 24px; border-bottom: 1px solid #ccc; padding-bottom: 4px; }
    .header { text-align: center; margin-bottom: 24px; }
    .section { margin-bottom: 12px; }
    .label { font-weight: bold; }
    .declaration { margin-top: 32px; }
    .signature-block { margin-top: 32px; }
    .signature-img { max-width: 220px; max-height: 90px; border-bottom: 1px solid #333; margin-top: 4px; }
    .footer { margin-top: 40px; font-size: 11px; color: #666; text-align: center; }
    .signed-banner { background: #e6f4ea; border: 1px solid #34a853; color: #137333; padding: 10px; text-align: center; font-weight: bold; margin-bottom: 24px; }
  </style>
</head>
<body>
  <div class="header">
    <h1>SIGNED INFORMED CONSENT FORM</h1>
    <p>Pediatric Surgical Procedure</p>
  </div>

  <div class="signed-banner">
    This consent form was digitally signed on {{ signed_at }}.
  </div>

  <h2>Patient Details</h2>
  <div class="section"><span class="label">Patient Name:</span> {{ patient_name }}</div>
  <div class="section"><span class="label">Age / Gender:</span> {{ age }} / {{ gender }}</div>
  <div class="section"><span class="label">Parent/Guardian:</span> {{ parent_name }}</div>
  <div class="section"><span class="label">Parent Phone:</span> {{ parent_phone }}</div>

  <h2>Procedure</h2>
  <div class="section"><span class="label">Proposed Procedure:</span> {{ procedure }}</div>
  <div class="section"><span class="label">Anesthesia:</span> {{ anesthesia }}</div>
  <div class="section"><span class="label">Surgeon:</span> {{ surgeon_name }}</div>

  <h2>Risks</h2>
  <p>{{ risks }}</p>

  <h2>Benefits</h2>
  <p>{{ benefits }}</p>

  <h2>Alternatives</h2>
  <p>{{ alternatives }}</p>

  <h2>Post-operative Care</h2>
  <p>{{ post_op_care }}</p>

  <h2>Declaration</h2>
  <div class="declaration">
    <p>
      I, the undersigned parent/guardian of the above-named minor patient, have read and understood the
      information provided. I have had the opportunity to ask questions and all my questions have been
      answered satisfactorily. I voluntarily consent to the proposed surgical procedure and anesthesia.
    </p>
  </div>

  <div class="signature-block">
    <div class="section">
      <span class="label">Parent/Guardian Signature</span><br>
      <img class="signature-img" src="{{ parent_signature_url }}" alt="Parent signature">
    </div>
    {% if witness_name %}
    <div class="section" style="margin-top: 24px;">
      <span class="label">Witness: {{ witness_name }}</span><br>
      {% if witness_signature_url %}
      <img class="signature-img" src="{{ witness_signature_url }}" alt="Witness signature">
      {% endif %}
    </div>
    {% endif %}
  </div>

  <div class="footer">
    This signed form complies with MCI / NABH standards for informed consent for pediatric surgical procedures.
  </div>
</body>
</html>
"""


def generate_consent_pdf(form_data: Dict[str, Any]) -> bytes:
    """Generate a consent form PDF from structured form data.

    Uses WeasyPrint when system dependencies (Pango/GTK+) are available.
    Falls back to returning HTML bytes for local development without those libs.
    """
    html = Template(CONSENT_TEMPLATE).render(**form_data)

    try:
        from weasyprint import HTML
        return HTML(string=html).write_pdf()
    except Exception as exc:
        import logging
        logging.getLogger(__name__).warning(
            "WeasyPrint unavailable (%s); returning HTML fallback for consent PDF", exc
        )
        return html.encode("utf-8")


def generate_signed_consent_pdf(
    form_data: Dict[str, Any],
    parent_signature_url: str,
    witness_name: Optional[str] = None,
    witness_signature_url: Optional[str] = None,
    signed_at: Optional[str] = None,
) -> bytes:
    """Generate a signed consent PDF with embedded signature images."""
    context = dict(form_data)
    context.update(
        {
            "parent_signature_url": parent_signature_url,
            "witness_name": witness_name or "",
            "witness_signature_url": witness_signature_url or "",
            "signed_at": signed_at or datetime.now().isoformat(),
        }
    )
    html = Template(CONSENT_SIGNED_TEMPLATE).render(**context)

    try:
        from weasyprint import HTML
        return HTML(string=html).write_pdf()
    except Exception as exc:
        import logging
        logging.getLogger(__name__).warning(
            "WeasyPrint unavailable (%s); returning HTML fallback for signed consent PDF", exc
        )
        return html.encode("utf-8")
