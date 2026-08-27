"""Media retrieval router.

Uploaded files are stored in the database as binary blobs. This router serves
those blobs back to authenticated clients with the correct MIME type.
"""

import mimetypes
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import Response

from app.core.auth_deps import CurrentUser, get_current_user
from app.core.database import prisma

router = APIRouter(prefix="/media", tags=["Media"])


def _guess_mime_type(filename: str) -> str:
    guessed, _ = mimetypes.guess_type(filename)
    return guessed or "application/octet-stream"


@router.get("/{media_id}")
async def get_media(
    media_id: str,
    user: CurrentUser = Depends(get_current_user),
):
    """Return the binary content of a stored media item."""
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

    content_type = media.mime_type or _guess_mime_type(media.filename)
    raw_bytes = media.data.decode() if hasattr(media.data, "decode") else media.data
    return Response(
        content=raw_bytes,
        media_type=content_type,
        headers={
            "Content-Disposition": f'inline; filename="{media.filename}"',
        },
    )
