"""PDF document integrity utilities using SHA-256 hashing."""

import hashlib
from typing import Optional


def compute_sha256(data: bytes) -> str:
    """Return the lower-case hex SHA-256 digest of the given bytes."""
    return hashlib.sha256(data).hexdigest()


def format_truncated_hash(
    hash_value: str,
    parts: int = 4,
    chars_per_part: int = 4,
) -> str:
    """Format a hash as a truncated, human-readable string.

    Example:
        >>> format_truncated_hash("a3f829d18c7e...")
        'A3F8-29D1-8C7E-...'
    """
    if not hash_value:
        return "NOT AVAILABLE"

    chunks = []
    start = 0
    for _ in range(parts):
        chunk = hash_value[start : start + chars_per_part].upper()
        if not chunk:
            break
        chunks.append(chunk)
        start += chars_per_part

    suffix = "..." if len(hash_value) > start else ""
    return "-".join(chunks) + suffix


def verify_pdf_hash(pdf_bytes: bytes, expected_hash: Optional[str]) -> bool:
    """Verify that the SHA-256 hash of pdf_bytes matches expected_hash."""
    if not expected_hash:
        return False
    return compute_sha256(pdf_bytes) == expected_hash.lower()
