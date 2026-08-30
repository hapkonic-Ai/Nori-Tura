"""No-code consent layout template renderer.

Converts a JSON block list + style settings into a Jinja2 HTML string that the
existing consent PDF pipeline can render.  This keeps the backend as the single
source of truth for the generated template while letting non-technical admins
compose layouts visually.
"""

import html
import json
import uuid
from typing import Any, Dict, List, Optional

DEFAULT_STYLES: Dict[str, Any] = {
    "page_size": "A4",
    "page_margins": "18mm 16mm 28mm 16mm",
    "primary_color": "#1a4d8f",
    "font_family": "DejaVu Sans, Arial, sans-serif",
    "font_size": "10.5pt",
    "line_height": "1.5",
    "section_spacing": "12px",
    "border_style": "solid",
}

# Variables available in consent_service consent context.
# The renderer emits Jinja2 placeholders using these exact names.
BLOCK_REGISTRY: Dict[str, Dict[str, Any]] = {
    "header": {
        "label": "Hospital Header",
        "icon": "🏥",
        "default_title": "Hospital Header",
        "description": "Hospital name, address, contact and registration.",
    },
    "title": {
        "label": "Document Title",
        "icon": "📄",
        "default_title": "Informed Consent for Pediatric Surgical Procedure",
        "description": "Main title and subtitle of the form.",
    },
    "metadata": {
        "label": "Consent Metadata",
        "icon": "🔖",
        "default_title": "Consent Metadata",
        "description": "Consent number, version, generated date, language, status.",
    },
    "patient_info": {
        "label": "Patient Information",
        "icon": "🧒",
        "default_title": "Patient Information",
        "description": "Patient name, UHID, age/gender, admission, department, ward.",
    },
    "guardian_info": {
        "label": "Guardian Information",
        "icon": "👨‍👩‍👧",
        "default_title": "Parent / Guardian Information",
        "description": "Parent name, relationship and mobile.",
    },
    "doctor_info": {
        "label": "Doctor Information",
        "icon": "👨‍⚕️",
        "default_title": "Treating Doctor Information",
        "description": "Doctor name, qualification, registration, department.",
    },
    "clinical_info": {
        "label": "Clinical Information",
        "icon": "🩺",
        "default_title": "Clinical Information",
        "description": "Diagnosis, procedure, description, anesthesia, benefits, risks, complications, alternatives, recovery.",
    },
    "consent_clauses": {
        "label": "Consent Clauses",
        "icon": "✅",
        "default_title": "Consent Clauses",
        "description": "Numbered informed-consent declarations.",
    },
    "doctor_declaration": {
        "label": "Doctor Declaration",
        "icon": "📝",
        "default_title": "Doctor Declaration",
        "description": "Doctor declaration box with name, registration and timestamp.",
    },
    "guardian_declaration": {
        "label": "Guardian Declaration",
        "icon": "✍️",
        "default_title": "Parent / Guardian Declaration",
        "description": "Parent/guardian declaration box.",
    },
    "signature_section": {
        "label": "Signature Section",
        "icon": "🖋️",
        "default_title": "Signatures",
        "description": "Parent, witness, doctor and date signature blocks.",
    },
    "custom_text": {
        "label": "Custom Text",
        "icon": "📌",
        "default_title": "Custom Section",
        "description": "Free-form text block with optional Jinja2 placeholders.",
    },
    "spacer": {
        "label": "Spacer",
        "icon": "↕️",
        "default_title": "Spacer",
        "description": "Empty vertical space.",
    },
    "page_break": {
        "label": "Page Break",
        "icon": "⤵️",
        "default_title": "Page Break",
        "description": "Force a new page.",
    },
}


def _merge_styles(styles: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    merged = dict(DEFAULT_STYLES)
    if styles:
        merged.update(styles)
    return merged


def _esc(text: str) -> str:
    """Escape text that is meant as literal HTML content (not Jinja2)."""
    return html.escape(str(text))


def _jinja(var_name: str) -> str:
    """Emit a Jinja2 placeholder that will be rendered by the PDF engine."""
    return "{{ " + var_name + " | default('—', true) }}"


def _maybe(var_name: str, inner: str) -> str:
    """Wrap content in a Jinja2 if-block so it only renders when the variable is truthy."""
    return "{% if " + var_name + " %}" + inner + "{% endif %}"


def _css_from_styles(styles: Dict[str, Any]) -> str:
    primary = styles.get("primary_color", "#1a4d8f")
    font_family = styles.get("font_family", "DejaVu Sans, Arial, sans-serif")
    font_size = styles.get("font_size", "10.5pt")
    line_height = styles.get("line_height", "1.5")
    section_spacing = styles.get("section_spacing", "12px")
    border_style = styles.get("border_style", "solid")
    page_size = styles.get("page_size", "A4")
    page_margins = styles.get("page_margins", "18mm 16mm 28mm 16mm")

    return f"""
    @page {{
      size: {page_size};
      margin: {page_margins};
      @top-center {{
        content: element(letterhead);
        width: 100%;
      }}
      @bottom-center {{
        content: element(footer);
        width: 100%;
      }}
      @bottom-right {{
        content: "Page " counter(page) " of " counter(pages);
        font-size: 9px;
        color: #555;
      }}
    }}

    * {{ box-sizing: border-box; }}

    body {{
      font-family: {_esc(font_family)};
      font-size: {font_size};
      line-height: {line_height};
      color: #222;
      margin: 0;
      padding: 0;
    }}

    #letterhead {{
      position: running(letterhead);
      border-bottom: 2px {border_style} {_esc(primary)};
      padding-bottom: 8px;
      margin-bottom: 8px;
    }}

    .hospital-name {{
      font-size: 16pt;
      font-weight: bold;
      color: {_esc(primary)};
      text-transform: uppercase;
    }}

    .hospital-address, .hospital-contact, .hospital-reg {{
      font-size: 8.5pt;
      color: #444;
    }}

    #footer {{
      position: running(footer);
      border-top: 1px {border_style} #bbb;
      padding-top: 6px;
      font-size: 8pt;
      color: #555;
    }}

    .footer-row {{
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }}

    .footer-left {{ width: 70%; }}
    .footer-right {{ width: 28%; text-align: right; }}
    .footer-qr {{ max-width: 70px; max-height: 70px; }}
    .hash-line {{ font-family: monospace; font-size: 8pt; color: #333; margin-top: 2px; }}
    .compliance-note {{ font-size: 7.5pt; color: #666; margin-top: 4px; }}

    .document-title {{
      text-align: center;
      margin: {section_spacing} 0 10px 0;
    }}

    .document-title h1 {{
      font-size: 15pt;
      margin: 0;
      color: {_esc(primary)};
      text-transform: uppercase;
    }}

    .document-title .subtitle {{
      font-size: 10pt;
      color: #555;
    }}

    .metadata-bar {{
      background: #f5f7fa;
      border: 1px {border_style} #d0d7de;
      border-radius: 4px;
      padding: 8px 10px;
      margin-bottom: {section_spacing};
      font-size: 9pt;
    }}

    .metadata-row {{
      display: flex;
      flex-wrap: wrap;
      gap: 8px 24px;
    }}

    .metadata-item {{ flex: 1 1 30%; min-width: 140px; }}
    .metadata-label {{ font-weight: bold; color: #333; }}

    h2 {{
      font-size: 11.5pt;
      color: {_esc(primary)};
      border-bottom: 1px {border_style} {_esc(primary)};
      padding-bottom: 3px;
      margin-top: 16px;
      margin-bottom: 8px;
    }}

    .info-grid {{ display: table; width: 100%; margin-bottom: 8px; }}
    .info-row {{ display: table-row; }}
    .info-label, .info-value {{
      display: table-cell;
      padding: 4px 6px;
      vertical-align: top;
    }}
    .info-label {{ width: 35%; font-weight: bold; color: #333; }}
    .info-value {{ width: 65%; }}

    .clinical-section p {{
      margin: 6px 0;
      text-align: justify;
    }}

    .numbered-clauses {{
      counter-reset: clause;
      list-style: none;
      padding-left: 0;
      margin: 8px 0;
    }}

    .numbered-clauses li {{
      position: relative;
      padding-left: 28px;
      margin-bottom: 8px;
      text-align: justify;
    }}

    .numbered-clauses li::before {{
      counter-increment: clause;
      content: counter(clause) ".";
      position: absolute;
      left: 0;
      font-weight: bold;
      color: {_esc(primary)};
    }}

    .declaration-box {{
      background: #f9fafb;
      border-left: 4px {border_style} {_esc(primary)};
      padding: 10px 12px;
      margin: {section_spacing} 0;
    }}

    .declaration-text {{ font-style: italic; margin: 0 0 8px 0; }}
    .declaration-meta {{ font-size: 9.5pt; color: #444; }}

    .signature-section {{ margin-top: 24px; page-break-inside: avoid; }}
    .signature-grid {{ display: table; width: 100%; margin-top: 14px; }}
    .signature-block {{
      display: table-cell;
      width: 48%;
      vertical-align: bottom;
      padding-right: 12px;
    }}
    .signature-line {{
      border-top: 1px {border_style} #333;
      margin-top: 40px;
      padding-top: 4px;
      font-size: 9.5pt;
    }}
    .signature-img {{
      max-width: 220px;
      max-height: 70px;
      border-bottom: 1px {border_style} #333;
      margin-bottom: 4px;
    }}

    .text-center {{ text-align: center; }}
    .small {{ font-size: 9pt; }}
    .muted {{ color: #555; }}
    .page-break {{ page-break-before: always; }}
    .avoid-break {{ page-break-inside: avoid; }}
    .spacer {{ height: {section_spacing}; }}
"""


def _render_header_block(block: Dict[str, Any]) -> str:
    hospital_name = _jinja("hospital_name")
    address_html = _maybe("hospital_address", '<div class="hospital-address">' + _jinja("hospital_address") + "</div>")
    contact = _maybe(
        "hospital_contact",
        '<span class="hospital-contact">Contact: ' + _jinja("hospital_contact") + "</span>",
    )
    reg = _maybe(
        "hospital_registration_number",
        '<span class="hospital-reg">Reg. No.: ' + _jinja("hospital_registration_number") + "</span>",
    )
    separator = "{% if hospital_contact and hospital_registration_number %} | {% endif %}"
    contact_line = _maybe(
        "hospital_contact or hospital_registration_number",
        "<div>" + contact + separator + reg + "</div>",
    )
    return f"""
  <div id="letterhead">
    <div class="hospital-name">{hospital_name}</div>
    {address_html}
    {contact_line}
  </div>
"""


def _render_footer_block() -> str:
    hash_html = _jinja("pdf_hash_truncated")
    consent_id_html = _maybe("consent_id", "Consent ID: " + _jinja("consent_id"))
    qr_html = _maybe(
        "qr_code_url",
        '<img class="footer-qr" src="{{ qr_code_url }}" alt="Verification QR">',
    )
    return f"""
  <div id="footer">
    <div class="footer-row">
      <div class="footer-left">
        <div><strong>Document Verification Hash:</strong> <span class="hash-line">{hash_html}</span></div>
        <div class="compliance-note">
          This consent form is prepared in accordance with NMC and NABH standards for informed consent.
          {consent_id_html}
        </div>
      </div>
      <div class="footer-right">
        {qr_html}
      </div>
    </div>
  </div>
"""


def _render_title_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Informed Consent for Pediatric Surgical Procedure"
    return f"""
  <div class="document-title">
    <h1>{_esc(title)}</h1>
    <div class="subtitle">{_jinja('form_type')}</div>
  </div>
"""


def _render_metadata_block(block: Dict[str, Any]) -> str:
    return """
  <div class="metadata-bar">
    <div class="metadata-row">
      <div class="metadata-item"><span class="metadata-label">Consent No.:</span> """ + _jinja("consent_number") + """</div>
      <div class="metadata-item"><span class="metadata-label">Version:</span> """ + _jinja("version") + """</div>
      <div class="metadata-item"><span class="metadata-label">Generated:</span> """ + _jinja("generated_at") + """</div>
      <div class="metadata-item"><span class="metadata-label">Language:</span> """ + _jinja("language") + """</div>
      <div class="metadata-item"><span class="metadata-label">Status:</span> """ + _jinja("status") + """</div>
    </div>
  </div>
"""


def _render_patient_info_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Patient Information"
    return f"""
  <h2>{_esc(title)}</h2>
  <div class="info-grid">
    <div class="info-row"><div class="info-label">Name</div><div class="info-value">{_jinja('patient_name')}</div></div>
    <div class="info-row"><div class="info-label">UHID / MRN</div><div class="info-value">{_jinja('patient_uhid')}</div></div>
    <div class="info-row"><div class="info-label">Age / Gender</div><div class="info-value">{_jinja('age')} / {_jinja('gender')}</div></div>
    <div class="info-row"><div class="info-label">Admission Number</div><div class="info-value">{_jinja('admission_number')}</div></div>
    <div class="info-row"><div class="info-label">Department</div><div class="info-value">{_jinja('department')}</div></div>
    <div class="info-row"><div class="info-label">Ward / Room</div><div class="info-value">{_jinja('ward_room')}</div></div>
  </div>
"""


def _render_guardian_info_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Parent / Guardian Information"
    return f"""
  <h2>{_esc(title)}</h2>
  <div class="info-grid">
    <div class="info-row"><div class="info-label">Name</div><div class="info-value">{_jinja('parent_name')}</div></div>
    <div class="info-row"><div class="info-label">Relationship to Patient</div><div class="info-value">{_jinja('guardian_relationship')}</div></div>
    <div class="info-row"><div class="info-label">Mobile Number</div><div class="info-value">{_jinja('parent_phone')}</div></div>
  </div>
"""


def _render_doctor_info_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Treating Doctor Information"
    return f"""
  <h2>{_esc(title)}</h2>
  <div class="info-grid">
    <div class="info-row"><div class="info-label">Doctor Name</div><div class="info-value">{_jinja('surgeon_name')}</div></div>
    <div class="info-row"><div class="info-label">Qualification</div><div class="info-value">{_jinja('doctor_qualification')}</div></div>
    <div class="info-row"><div class="info-label">Registration Number</div><div class="info-value">{_jinja('doctor_registration_number')}</div></div>
    <div class="info-row"><div class="info-label">Department</div><div class="info-value">{_jinja('department')}</div></div>
  </div>
"""


def _render_clinical_info_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Clinical Information"
    return f"""
  <h2>{_esc(title)}</h2>
  <div class="clinical-section">
    <p><strong>Diagnosis:</strong> {_jinja('diagnosis')}</p>
    <p><strong>Proposed Procedure:</strong> {_jinja('procedure')}</p>
    <p><strong>Procedure Description:</strong> {_jinja('procedure_description')}</p>
    <p><strong>Anesthesia:</strong> {_jinja('anesthesia')}</p>
    <p><strong>Expected Benefits:</strong> {_jinja('benefits')}</p>
    <p><strong>Material Risks:</strong> {_jinja('material_risks')}</p>
    <p><strong>Possible Complications:</strong> {_jinja('possible_complications')}</p>
    <p><strong>Alternative Treatment Options:</strong> {_jinja('alternatives')}</p>
    <p><strong>Consequences of Refusing Treatment:</strong> {_jinja('refusal_consequences')}</p>
    <p><strong>Expected Recovery:</strong> {_jinja('expected_recovery')}</p>
  </div>
"""


def _render_consent_clauses_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Consent Clauses"
    return f"""
  <h2>{_esc(title)}</h2>
  <ol class="numbered-clauses">
    <li>I confirm that the proposed procedure, expected benefits, material risks, possible complications, available alternatives, and consequences of refusing treatment have been explained to me in <strong>{_jinja('language')}</strong>, a language I understand.</li>
    <li>I have had the opportunity to ask questions, and all my questions have been answered to my satisfaction.</li>
    <li>I understand that no guarantee or assurance has been made regarding the outcome of the procedure.</li>
    <li>I consent to the administration of anesthesia and other reasonably required measures during the course of treatment.</li>
    <li>{{% if consent_for_blood_products == "Yes" %}}I consent to the transfusion of blood or blood products if deemed medically necessary.{{% endif %}}</li>
    <li>{{% if consent_for_photography == "Yes" %}}I consent to the capture and use of photographs / videos for medical records, education, or quality assurance as permitted by hospital policy.{{% endif %}}</li>
    <li>I understand that I have the right to withdraw consent at any time before or during the procedure without affecting future care.</li>
    <li>I have been informed of the hospital's privacy and confidentiality practices regarding my child's personal and medical information.</li>
  </ol>
"""


def _render_doctor_declaration_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Doctor Declaration"
    return f"""
  <h2>{_esc(title)}</h2>
  <div class="declaration-box avoid-break">
    <p class="declaration-text">
      "I have personally explained the diagnosis, proposed procedure, expected benefits, risks, possible complications, available alternatives, and consequences of refusing treatment to the parent/guardian in a language they understand."
    </p>
    <div class="declaration-meta">
      <div><strong>Doctor Name:</strong> {_jinja('surgeon_name')}</div>
      <div><strong>Registration Number:</strong> {_jinja('doctor_registration_number')}</div>
      <div><strong>Date / Time:</strong> {_jinja('doctor_declaration_timestamp')}</div>
    </div>
  </div>
"""


def _render_guardian_declaration_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Parent / Guardian Declaration"
    return f"""
  <h2>{_esc(title)}</h2>
  <div class="declaration-box avoid-break">
    <p class="declaration-text">
      "I confirm that the procedure, benefits, risks, alternatives, and consequences of refusal have been explained to me in a language that I understand. I have had the opportunity to ask questions and all my questions have been answered satisfactorily."
    </p>
    <div class="declaration-meta">
      <div><strong>Parent / Guardian:</strong> {_jinja('parent_name')}</div>
      <div><strong>Relationship:</strong> {_jinja('guardian_relationship')}</div>
      <div><strong>Mobile:</strong> {_jinja('parent_phone')}</div>
    </div>
  </div>
"""


def _render_signature_section_block(block: Dict[str, Any]) -> str:
    title = block.get("title") or "Signatures"
    witness_present = _maybe(
        "witness_name",
        '<span class="small muted">{{ witness_name }}{% if witness_relationship %}, {{ witness_relationship }}{% endif %}{% if witness_mobile %}, {{ witness_mobile }}{% endif %}</span>',
    )
    witness_absent = _maybe(
        "not witness_name",
        '<span class="small muted">Name, Relationship, Mobile</span>',
    )
    return f"""
  <div class="signature-section">
    <h2>{_esc(title)}</h2>
    <div class="signature-grid">
      <div class="signature-block">
        <div class="signature-line">
          <strong>Parent / Guardian Signature</strong><br>
          <span class="small muted">{_jinja('parent_name')}</span>
        </div>
      </div>
      <div class="signature-block">
        <div class="signature-line">
          <strong>Witness Signature</strong><br>
          {witness_present}
          {witness_absent}
        </div>
      </div>
    </div>
    <div class="signature-grid" style="margin-top: 18px;">
      <div class="signature-block">
        <div class="signature-line">
          <strong>Treating Doctor Signature</strong><br>
          <span class="small muted">{_jinja('surgeon_name')}</span>
        </div>
      </div>
      <div class="signature-block">
        <div class="signature-line">
          <strong>Date / Time</strong><br>
          <span class="small muted">{_jinja('generated_at')}</span>
        </div>
      </div>
    </div>
  </div>
"""


def _render_custom_text_block(block: Dict[str, Any]) -> str:
    content = block.get("content") or ""
    # Custom text is trusted admin content and may contain Jinja2/HTML placeholders.
    return f"""
  <div class="custom-text-block">
    {content}
  </div>
"""


def _render_spacer_block(block: Dict[str, Any]) -> str:
    height = block.get("height", "12px")
    return f'<div class="spacer" style="height: {_esc(height)};"></div>\n'


def _render_page_break_block(block: Dict[str, Any]) -> str:
    return '<div class="page-break"></div>\n'


def _render_block(block: Dict[str, Any]) -> str:
    block_type = block.get("type")
    if not block_type:
        return ""

    visible = block.get("visible", True)
    if not visible:
        return ""

    renderers = {
        "header": _render_header_block,
        "footer": lambda b: _render_footer_block(),
        "title": _render_title_block,
        "metadata": _render_metadata_block,
        "patient_info": _render_patient_info_block,
        "guardian_info": _render_guardian_info_block,
        "doctor_info": _render_doctor_info_block,
        "clinical_info": _render_clinical_info_block,
        "consent_clauses": _render_consent_clauses_block,
        "doctor_declaration": _render_doctor_declaration_block,
        "guardian_declaration": _render_guardian_declaration_block,
        "signature_section": _render_signature_section_block,
        "custom_text": _render_custom_text_block,
        "spacer": _render_spacer_block,
        "page_break": _render_page_break_block,
    }

    renderer = renderers.get(block_type)
    if not renderer:
        return f"<!-- Unknown block type: {_esc(block_type)} -->\n"

    return renderer(block)


def render_layout(
    blocks: Optional[List[Dict[str, Any]]] = None,
    styles: Optional[Dict[str, Any]] = None,
    form_data: Optional[Dict[str, Any]] = None,
) -> str:
    """Render a full Jinja2 HTML template string from blocks and styles.

    If ``form_data`` is supplied, the template is rendered immediately with that
    data and the resulting plain HTML is returned.  Otherwise a Jinja2 template
    string is returned for later rendering by the consent PDF service.
    """
    styles = _merge_styles(styles)
    blocks = blocks or []

    body_parts: List[str] = []
    for block in blocks:
        body_parts.append(_render_block(block))

    css = _css_from_styles(styles)
    html_template = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Informed Consent Form</title>
  <style>
    {css}
  </style>
</head>
<body>
{ "".join(body_parts) }
</body>
</html>
"""

    if form_data is not None:
        from jinja2 import Template
        tpl = Template(html_template)
        return tpl.render(**form_data)

    return html_template


def create_default_blocks() -> List[Dict[str, Any]]:
    """Return a sensible default block layout matching the existing base template."""
    block_types = [
        "header",
        "title",
        "metadata",
        "patient_info",
        "guardian_info",
        "doctor_info",
        "clinical_info",
        "consent_clauses",
        "doctor_declaration",
        "guardian_declaration",
        "signature_section",
        "footer",
    ]
    blocks: List[Dict[str, Any]] = []
    for bt in block_types:
        meta = BLOCK_REGISTRY.get(bt, {})
        blocks.append({
            "id": str(uuid.uuid4()),
            "type": bt,
            "title": meta.get("default_title", bt.replace("_", " ").title()),
            "visible": True,
        })
    return blocks


def validate_blocks(blocks: Any) -> List[Dict[str, Any]]:
    """Normalize and lightly validate a blocks payload."""
    if blocks is None:
        return create_default_blocks()
    if isinstance(blocks, str):
        blocks = json.loads(blocks)
    if not isinstance(blocks, list):
        raise ValueError("blocks_json must be a list of blocks")
    normalized: List[Dict[str, Any]] = []
    for block in blocks:
        if not isinstance(block, dict):
            continue
        block_type = block.get("type")
        if not block_type:
            continue
        normalized.append({
            "id": block.get("id") or str(uuid.uuid4()),
            "type": block_type,
            "title": block.get("title") or BLOCK_REGISTRY.get(block_type, {}).get("default_title", ""),
            "visible": bool(block.get("visible", True)),
            "content": block.get("content", ""),
            "height": block.get("height", "12px"),
            "align": block.get("align", "left"),
            "columns": block.get("columns", 1),
        })
    return normalized
