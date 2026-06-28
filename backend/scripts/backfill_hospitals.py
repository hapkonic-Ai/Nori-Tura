"""Backfill hospitals after the hospitals/media schema migration.

Run once after `prisma db push` to recreate hospitals from the previously
exported doctor/nurse `hospital` values and propagate them to patients,
records, admissions, documents and consent forms.
"""

import sys
from pathlib import Path

# Make the generated prisma client package importable when running this script
# from the scripts/ directory.
sys.path.insert(0, str(Path(__file__).parent.parent))

import asyncio
from prisma import Prisma

HOSPITAL_SEEDS = [
    {"name": "Test Hospital"},
    {"name": "CMC"},
]

# Mapping from phone -> hospital name as observed before the migration.
STAFF_HOSPITALS = {
    "+919876543210": "Test Hospital",
    "+919304155460": "CMC",
    "+919876543215": "Test Hospital",
    "+919876543216": "Test Hospital",
}


async def main() -> None:
    db = Prisma()
    await db.connect()

    # 1. Seed hospitals
    name_to_id: dict[str, str] = {}
    for seed in HOSPITAL_SEEDS:
        existing = await db.hospitals.find_first(where={"name": seed["name"]})
        if existing:
            name_to_id[seed["name"]] = existing.id
        else:
            created = await db.hospitals.create(data=seed)
            name_to_id[seed["name"]] = created.id
            print(f"Created hospital '{seed['name']}' -> {created.id}")

    # 2. Assign hospital_id to doctors and nurses
    for phone, hospital_name in STAFF_HOSPITALS.items():
        hospital_id = name_to_id[hospital_name]

        doctor = await db.doctors.find_first(where={"phone": phone})
        if doctor:
            await db.doctors.update(
                where={"id": doctor.id},
                data={"hospital_id": hospital_id},
            )
            print(f"Updated doctor {phone} -> {hospital_name}")

        nurse = await db.nurses.find_first(where={"phone": phone})
        if nurse:
            await db.nurses.update(
                where={"id": nurse.id},
                data={"hospital_id": hospital_id},
            )
            print(f"Updated nurse {phone} -> {hospital_name}")

    # 3. Propagate hospital to patients, OPD records, IPD admissions,
    #    documents and consent forms via their doctor.
    for phone, hospital_name in STAFF_HOSPITALS.items():
        hospital_id = name_to_id[hospital_name]
        hospital = await db.hospitals.find_unique(where={"id": hospital_id})
        if not hospital:
            continue
        denorm = {
            "hospital_id": hospital_id,
            "hospital_name": hospital.name,
            "hospital_logo_url": hospital.logo_url,
        }

        doctor = await db.doctors.find_first(where={"phone": phone})
        if not doctor:
            continue

        patients = await db.patients.find_many(where={"doctor_id": doctor.id})
        for patient in patients:
            await db.patients.update(
                where={"id": patient.id}, data={"hospital_id": hospital_id}
            )

        await db.opd_records.update_many(
            where={"doctor_id": doctor.id}, data=denorm
        )
        await db.ipd_admissions.update_many(
            where={"doctor_id": doctor.id}, data=denorm
        )
        await db.documents.update_many(
            where={"doctor_id": doctor.id},
            data={"hospital_id": hospital_id},
        )
        await db.consent_forms.update_many(
            where={"doctor_id": doctor.id},
            data={
                "hospital_id": hospital_id,
                "hospital_name": hospital.name,
                "hospital_logo_url": hospital.logo_url,
            },
        )
        print(f"Propagated {hospital_name} for doctor {phone}")

    await db.disconnect()
    print("Backfill complete.")


if __name__ == "__main__":
    asyncio.run(main())
