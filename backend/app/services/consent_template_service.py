"""Helpers for consent template management."""

from pathlib import Path

from app.core.database import prisma

_TEMPLATES_DIR = Path(__file__).resolve().parent.parent / "templates" / "consents"


async def ensure_default_layout_templates() -> None:
    """Seed the default consent layout templates from the filesystem if none exist."""
    for name, filename in [("base", "consent_base.html"), ("signed", "consent_signed.html")]:
        existing = await prisma.consent_layout_templates.find_first(where={"name": name})
        if existing:
            continue

        template_path = _TEMPLATES_DIR / filename
        if not template_path.exists():
            continue

        html = template_path.read_text(encoding="utf-8")
        await prisma.consent_layout_templates.create(
            data={
                "name": name,
                "html": html,
                "is_default": name == "base",
            }
        )
