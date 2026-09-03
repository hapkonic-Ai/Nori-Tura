from typing import Optional

from app.core.database import prisma


async def resolve_or_create_hospital(
    hospital_id: Optional[str], hospital_name: Optional[str]
) -> Optional[str]:
    """Return a hospital_id, creating the hospital by name if it doesn't exist yet."""
    if hospital_id:
        existing = await prisma.hospitals.find_first(where={"id": hospital_id})
        if existing:
            return existing.id

    if hospital_name and hospital_name.strip():
        name = hospital_name.strip()
        existing = await prisma.hospitals.find_first(
            where={"name": {"equals": name, "mode": "insensitive"}}
        )
        if existing:
            return existing.id
        new_hospital = await prisma.hospitals.create(data={"name": name})
        return new_hospital.id

    return None
