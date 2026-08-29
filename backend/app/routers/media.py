"""Media retrieval router.

Uploaded files are stored in a private MinIO bucket. This router proxies those
objects back to authenticated clients with the correct MIME type, and can also
issue short-lived presigned URLs for direct client loading.
"""

import mimetypes
from datetime import timedelta
from io import BytesIO
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.core.auth_deps import CurrentUser, get_current_user
from app.core.database import prisma
from app.core.storage import get_media_storage

router = APIRouter(prefix="/media", tags=["Media"])

PRESIGNED_URL_TTL_SECONDS = 15 * 60


def _guess_mime_type(filename: str) -> str:
    guessed, _ = mimetypes.guess_type(filename)
    return guessed or "application/octet-stream"


class MediaAccessResponse(BaseModel):
    url: str
    expires_in_seconds: int


async def _lookup_media(media_id: str):
    try:
        UUID(media_id, version=4)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid media id",
        )

    media = await prisma.media.find_unique(where={"id": media_id})
    if not media:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Media not found",
        )
    return media


@router.get("/{media_id}")
async def get_media(
    media_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    """Return the binary content of a stored media item."""
    media = await _lookup_media(media_id)
    content_type = media.mime_type or _guess_mime_type(media.filename)

    if media.bucket and media.object_key:
        storage = get_media_storage()
        raw_bytes = storage.get_object(media.object_key)
    elif media.data is not None:
        # Legacy fallback for rows not yet migrated to MinIO.
        raw_bytes = media.data.decode() if hasattr(media.data, "decode") else media.data
    else:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Media content not found",
        )

    return StreamingResponse(
        BytesIO(raw_bytes),
        media_type=content_type,
        headers={
            "Content-Disposition": f'inline; filename="{media.filename}"',
        },
    )


@router.post("/{media_id}/access", response_model=MediaAccessResponse)
async def get_media_access(
    media_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    """Return a short-lived presigned URL to download a media object.

    The app can load this URL directly with Coil / WebView / browser launcher
    without sending the JWT header.
    """
    media = await _lookup_media(media_id)
    if not media.bucket or not media.object_key:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Media content not found",
        )

    storage = get_media_storage()
    url = storage.presigned_get_url(
        object_key=media.object_key,
        expires=timedelta(seconds=PRESIGNED_URL_TTL_SECONDS),
    )

    return MediaAccessResponse(
        url=url,
        expires_in_seconds=PRESIGNED_URL_TTL_SECONDS,
    )
