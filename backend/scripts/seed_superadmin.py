import asyncio
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from prisma import Prisma


async def main():
    prisma = Prisma()
    await prisma.connect()

    phone = os.getenv("SUPERADMIN_PHONE", "+919999999999")
    name = os.getenv("SUPERADMIN_NAME", "Super Admin")

    existing = await prisma.admins.find_first(where={"phone": phone})
    if existing:
        print(f"Superadmin already exists: {existing.id}")
    else:
        admin = await prisma.admins.create(
            data={
                "name": name,
                "phone": phone,
                "role": "superadmin",
                "is_active": True,
            }
        )
        print(f"Created superadmin: {admin.id} ({admin.phone})")

    await prisma.disconnect()


if __name__ == "__main__":
    asyncio.run(main())
