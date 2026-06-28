"""Generic media upload router.

Files are uploaded to Cloudinary by the backend so credentials never reach
the client. The returned secure URLs are then attached to records by the
calling code.
"""

from typing import List, Literal

from fastapi import APIRouter, Depends, File, Form, UploadFile, status
from pydantic import BaseModel

from app.core.auth_deps import CurrentUser, get_current_user
from app.services.cloudinary_service import upload_media

router = APIRouter(prefix="/uploads", tags=["Uploads"])


class MediaUploadResponse(BaseModel):
    urls: List[str]


@router.post("/media", response_model=MediaUploadResponse, status_code=status.HTTP_200_OK)
async def upload_media_files(
    files: List[UploadFile] = File(..., description="One or more image/video files"),
    resource_type: Literal["image", "video"] = Form("image"),
    folder: str = Form("nonitura"),
    user: CurrentUser = Depends(get_current_user),
):
    """Upload image or video files to Cloudinary and return their URLs."""
    urls: List[str] = []

    for upload in files:
        content = await upload.read()
        if not content:
            continue

        url = await upload_media(
            file_bytes=content,
            filename=upload.filename or "upload",
            resource_type=resource_type,
            folder=folder,
        )
        if url:
            urls.append(url)
        await upload.close()

    return MediaUploadResponse(urls=urls)
