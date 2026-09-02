"""Helpers for consent template management."""

from pathlib import Path

from app.core.database import prisma

_TEMPLATES_DIR = Path(__file__).resolve().parent.parent / "templates" / "consents"


async def ensure_default_layout_templates() -> None:
    """Seed/sync the built-in consent layout templates from the filesystem.

    The built-in "base" and "signed" templates always track the code shipped in
    templates/consents so that PDF rendering fixes (e.g. the OTP attestation
    block) take effect for forms referencing them. Custom templates created
    via the admin UI use other names and are left untouched.
    """
    for name, filename in [("base", "consent_base.html"), ("signed", "consent_signed.html")]:
        template_path = _TEMPLATES_DIR / filename
        if not template_path.exists():
            continue

        html = template_path.read_text(encoding="utf-8")
        existing = await prisma.consent_layout_templates.find_first(where={"name": name})
        if existing:
            if existing.html != html:
                await prisma.consent_layout_templates.update(
                    where={"id": existing.id},
                    data={"html": html},
                )
            continue

        await prisma.consent_layout_templates.create(
            data={
                "name": name,
                "html": html,
                "is_default": name == "base",
            }
        )
