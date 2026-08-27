"""Generic media upload router.

Files are stored in the database as binary blobs so uploaded images and
documents are always accessible from the app, even when third-party services
like Cloudinary are not configured. The returned URLs point to the local
`/media/{id}` endpoint, which serves the blob back with the correct MIME type.
"""

import mimetypes
from typing import List, Literal
from urllib.parse import urljoin
from uuid import uuid4

from fastapi import APIRouter, Depends, File, Form, Request, UploadFile, status
from pydantic import BaseModel

from app.core.auth_deps import CurrentUser, get_current_user
from app.core.database import prisma
from prisma import Base64

router = APIRouter(prefix="/uploads", tags=["Uploads"])


class MediaUploadResponse(BaseModel):
    urls: List[str]


class MediaItem(BaseModel):
    id: str
    url: str
    filename: str
    mime_type: str


def _guess_mime_type(filename: str) -> str:
    guessed, _ = mimetypes.guess_type(filename)
    return guessed or "application/octet-stream"


def _base_url(request: Request) -> str:
    """Return the public base URL for building absolute media URLs."""
    # FastAPI's request.base_url ends with a trailing slash.
    return str(request.base_url).rstrip("/")


@router.post("/media", response_model=MediaUploadResponse, status_code=status.HTTP_200_OK)
async def upload_media_files(
    request: Request,
    files: List[UploadFile] = File(..., description="One or more image/video/PDF files"),
    resource_type: Literal["image", "video", "raw"] = Form("image"),
    folder: str = Form("nonitura"),
    user: CurrentUser = Depends(get_current_user),
):
    """Upload image, video or raw files (e.g. PDFs) and store them in the DB."""
    base = _base_url(request)
    urls: List[str] = []

    for upload in files:
        content = await upload.read()
        if not content:
            continue

        filename = upload.filename or "upload"
        mime_type = upload.content_type or _guess_mime_type(filename)

        media = await prisma.media.create(
            data={
                "id": str(uuid4()),
                "filename": filename,
                "mime_type": mime_type,
                "data": Base64.encode(content),
                "created_by": getattr(user, "phone", None),
            }
        )

        media_url = urljoin(f"{base}/", f"media/{media.id}")
        urls.append(media_url)
        await upload.close()

    return MediaUploadResponse(urls=urls)
