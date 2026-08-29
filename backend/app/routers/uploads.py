"""Generic media upload router.

Files are stored in a private MinIO bucket. Metadata lives in Postgres, and the
returned URLs are relative paths like `/media/{id}` that the app resolves
against its configured base URL.
"""

import mimetypes
from typing import List, Literal
from uuid import uuid4

from fastapi import APIRouter, Depends, File, Form, UploadFile, status
from pydantic import BaseModel

from app.core.auth_deps import CurrentUser, get_current_user
from app.core.database import prisma
from app.core.storage import get_media_storage

router = APIRouter(prefix="/uploads", tags=["Uploads"])


class MediaItem(BaseModel):
    id: str
    url: str
    filename: str
    mime_type: str


class MediaUploadResponse(BaseModel):
    urls: List[MediaItem]


def _guess_mime_type(filename: str) -> str:
    guessed, _ = mimetypes.guess_type(filename)
    return guessed or "application/octet-stream"


@router.post("/media", response_model=MediaUploadResponse, status_code=status.HTTP_200_OK)
async def upload_media_files(
    files: List[UploadFile] = File(..., description="One or more image/video/PDF files"),
    resource_type: Literal["image", "video", "raw"] = Form("image"),
    folder: str = Form("nonitura"),
    user: CurrentUser = Depends(get_current_user),
):
    """Upload image, video or raw files (e.g. PDFs) and store them in MinIO."""
    storage = get_media_storage()
    items: List[MediaItem] = []

    for upload in files:
        content = await upload.read()
        if not content:
            continue

        filename = upload.filename or "upload"
        mime_type = upload.content_type
        if not mime_type or mime_type == "application/octet-stream":
            mime_type = _guess_mime_type(filename)
        media_id = str(uuid4())
        object_key = storage.object_key(media_id, filename)

        storage.upload_object(
            object_key=object_key,
            data=content,
            content_type=mime_type,
        )

        media = await prisma.media.create(
            data={
                "id": media_id,
                "filename": filename,
                "mime_type": mime_type,
                "bucket": storage.bucket,
                "object_key": object_key,
                "storage_backend": "minio",
                "created_by": getattr(user, "phone", None),
            }
        )

        items.append(
            MediaItem(
                id=media.id,
                url=f"/media/{media.id}",
                filename=filename,
                mime_type=mime_type,
            )
        )
        await upload.close()

    return MediaUploadResponse(urls=items)
