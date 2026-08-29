"""MinIO / S3-compatible object storage client.

The bucket is kept private; all access goes through the backend, so media
continues to be served via the authenticated `/media/{id}` endpoint.
"""

from datetime import timedelta
from functools import lru_cache
from io import BytesIO
from typing import Optional

from minio import Minio
from minio.error import S3Error

from app.core.config import get_settings


class MediaStorage:
    """Thin wrapper around MinIO for uploaded media blobs."""

    def __init__(self) -> None:
        settings = get_settings()
        self.bucket = settings.MINIO_BUCKET
        self._client = Minio(
            endpoint=settings.MINIO_ENDPOINT,
            access_key=settings.MINIO_ACCESS_KEY,
            secret_key=settings.MINIO_SECRET_KEY,
            secure=settings.MINIO_SECURE,
        )

    def _ensure_bucket(self) -> None:
        """Create the configured bucket if it does not already exist."""
        if not self._client.bucket_exists(self.bucket):
            self._client.make_bucket(self.bucket)

    def object_key(self, media_id: str, filename: str) -> str:
        """Return the MinIO object key for a media row."""
        # Keep keys flat but namespaced by media id to avoid collisions.
        safe_name = filename.replace("/", "_")
        return f"{media_id}/{safe_name}"

    def upload_object(
        self,
        object_key: str,
        data: bytes,
        content_type: str = "application/octet-stream",
        length: Optional[int] = None,
    ) -> None:
        """Upload bytes to MinIO."""
        self._ensure_bucket()
        self._client.put_object(
            bucket_name=self.bucket,
            object_name=object_key,
            data=BytesIO(data),
            length=length or len(data),
            content_type=content_type,
        )

    def get_object(self, object_key: str) -> bytes:
        """Download bytes from MinIO."""
        response = self._client.get_object(self.bucket, object_key)
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()

    def delete_object(self, object_key: str) -> None:
        """Remove an object from MinIO."""
        try:
            self._client.remove_object(self.bucket, object_key)
        except S3Error as exc:
            # Treat already-deleted objects as idempotent.
            if exc.code != "NoSuchKey":
                raise

    def presigned_get_url(self, object_key: str, expires: timedelta) -> str:
        """Return a temporary presigned URL to download an object."""
        url = self._client.presigned_get_object(
            bucket_name=self.bucket,
            object_name=object_key,
            expires=expires,
        )
        public_endpoint = get_settings().MINIO_PUBLIC_ENDPOINT
        if public_endpoint:
            # Replace the internal MinIO host with the public-facing endpoint
            # while preserving the path and query (i.e. the signed URL).
            from urllib.parse import urlsplit, urlunsplit

            scheme = "https" if get_settings().MINIO_SECURE else "http"
            public_netloc = public_endpoint.rstrip("/")
            parts = urlsplit(url)
            url = urlunsplit((scheme, public_netloc, parts.path, parts.query, parts.fragment))
        return url


@lru_cache()
def get_media_storage() -> MediaStorage:
    return MediaStorage()
