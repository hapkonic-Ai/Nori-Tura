"""Cloudinary upload helpers for media, PDFs and other files."""

from typing import Literal, Optional
from uuid import uuid4

from app.core.config import get_settings

settings = get_settings()

ResourceType = Literal["image", "video", "raw"]


def _is_configured() -> bool:
    return bool(
        settings.CLOUDINARY_CLOUD_NAME
        and settings.CLOUDINARY_API_KEY
        and settings.CLOUDINARY_API_SECRET
    )


def _configure_cloudinary() -> None:
    import cloudinary

    cloudinary.config(
        cloud_name=settings.CLOUDINARY_CLOUD_NAME,
        api_key=settings.CLOUDINARY_API_KEY,
        api_secret=settings.CLOUDINARY_API_SECRET,
    )


def generate_public_id(prefix: str = "nonitura") -> str:
    """Generate a unique public_id for Cloudinary."""
    return f"{prefix}/{uuid4().hex}"


async def upload_media(
    file_bytes: bytes,
    filename: str,
    resource_type: ResourceType = "image",
    folder: str = "nonitura",
) -> Optional[str]:
    """Upload a single file to Cloudinary and return its secure URL."""
    if not _is_configured():
        print(f"[Cloudinary stub] Would upload {filename} ({len(file_bytes)} bytes) as {resource_type}")
        return None

    _configure_cloudinary()
    import cloudinary.uploader

    public_id = f"{folder}/{uuid4().hex}"
    try:
        result = cloudinary.uploader.upload(
            file_bytes,
            resource_type=resource_type,
            public_id=public_id,
            filename=filename,
        )
        return result.get("secure_url")
    except Exception as exc:
        print(f"Cloudinary upload error for {filename}: {exc}")
        return None


async def upload_pdf(pdf_bytes: bytes, filename: str, folder: str = "nonitura/consents") -> Optional[str]:
    """Upload a PDF as a raw resource. Kept for backward compatibility with consent PDFs."""
    return await upload_media(
        pdf_bytes,
        filename,
        resource_type="raw",
        folder=folder,
    )
