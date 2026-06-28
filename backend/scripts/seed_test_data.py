"""Seed comprehensive test data for local exploration.

Run after `prisma db push` and `seed_superadmin.py`.
"""

import asyncio
import sys
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from prisma import Prisma

DOCTORS = [
    {
        "name": "Dr. Arjun Mehta",
        "phone": "+919876543210",
        "specialty": "Pediatric Surgery",
        "is_active": True,
        "hospital_name": "Test Hospital",
    },
    {
        "name": "Dr. Priya Sharma",
        "phone": "+919304155460",
        "specialty": "Pediatric Urology",
        "is_active": True,
        "hospital_name": "CMC",
    },
]

NURSES = [
    {"name": "Nurse Renu", "phone": "+919876543211", "doctor_phone": "+919876543210"},
    {"name": "Nurse Karthik", "phone": "+919876543212", "doctor_phone": "+919876543210"},
    {"name": "Nurse Anjali", "phone": "+919876543213", "doctor_phone": "+919304155460"},
    {"name": "Nurse David", "phone": "+919876543214", "doctor_phone": "+919304155460"},
]

PATIENTS = [
    {"name": "Aarav Patel", "age": 4, "gender": "male", "blood_group": "B+", "allergies": "None", "parent_name": "Raj Patel", "parent_phone": "+918000000001", "doctor_phone": "+919876543210"},
    {"name": "Diya Singh", "age": 7, "gender": "female", "blood_group": "O+", "allergies": "Penicillin", "parent_name": "Sunita Singh", "parent_phone": "+918000000002", "doctor_phone": "+919876543210"},
    {"name": "Vihaan Gupta", "age": 2, "gender": "male", "blood_group": "A+", "allergies": "None", "parent_name": "Pooja Gupta", "parent_phone": "+918000000003", "doctor_phone": "+919876543210"},
    {"name": "Isha Reddy", "age": 5, "gender": "female", "blood_group": "AB+", "allergies": "Dust", "parent_name": "Mohan Reddy", "parent_phone": "+918000000004", "doctor_phone": "+919304155460"},
    {"name": "Kabir Khan", "age": 8, "gender": "male", "blood_group": "B+", "allergies": "None", "parent_name": "Fatima Khan", "parent_phone": "+918000000005", "doctor_phone": "+919304155460"},
    {"name": "Ananya Iyer", "age": 3, "gender": "female", "blood_group": "O-", "allergies": "Latex", "parent_name": "Suresh Iyer", "parent_phone": "+918000000006", "doctor_phone": "+919304155460"},
]

OPD_RECORDS = [
    {"patient_idx": 0, "visit_type": "new", "complaint": "Abdominal pain and vomiting for 2 days", "examination": "Tenderness in right iliac fossa, low-grade fever", "diagnosis": "Acute appendicitis", "surgical_decision": "Open appendectomy", "planned_procedure": "Appendectomy", "advice": "NPO, IV fluids, monitor vitals", "medications": [{"name": "Paracetamol", "dose": "250mg", "frequency": "QID", "duration": "3 days"}], "investigations": [{"type": "CBC"}, {"type": "Ultrasound abdomen"}]},
    {"patient_idx": 1, "visit_type": "follow-up", "complaint": "Wound check after hernia repair", "examination": "Clean dry wound, no discharge", "diagnosis": "Post-op recovery", "surgical_decision": "Continue observation", "planned_procedure": None, "advice": "Return in 1 week", "medications": [{"name": "Syrup Ibuprofen", "dose": "5ml", "frequency": "TDS", "duration": "2 days"}], "investigations": []},
    {"patient_idx": 2, "visit_type": "new", "complaint": "Swelling in groin since 1 month", "examination": "Reducible right inguinal swelling", "diagnosis": "Right inguinal hernia", "surgical_decision": "Herniotomy", "planned_procedure": "Laparoscopic herniotomy", "advice": "Pre-op fasting instructions given", "medications": [], "investigations": [{"type": "Chest X-ray"}]},
    {"patient_idx": 3, "visit_type": "new", "complaint": "Burning urination and fever", "examination": "Crying on micturition, normal abdomen", "diagnosis": "Urinary tract infection", "surgical_decision": "Medical management", "planned_procedure": None, "advice": "Plenty of fluids, urine culture", "medications": [{"name": "Syrup Cefixime", "dose": "5ml", "frequency": "BD", "duration": "5 days"}], "investigations": [{"type": "Urine routine"}, {"type": "Urine culture"}]},
    {"patient_idx": 4, "visit_type": "follow-up", "complaint": "Follow up after hypospadias repair", "examination": "Catheter in situ, minimal ooze", "diagnosis": "Post-op hypospadias repair", "surgical_decision": "Continue catheter", "planned_procedure": None, "advice": "Catheter care explained", "medications": [{"name": "Syrup Paracetamol", "dose": "5ml", "frequency": "SOS", "duration": "3 days"}], "investigations": []},
    {"patient_idx": 5, "visit_type": "new", "complaint": "Constipation with abdominal distension", "examination": "Mild abdominal distension, active bowel sounds", "diagnosis": "Functional constipation", "surgical_decision": "Medical management", "planned_procedure": None, "advice": "High fiber diet, laxatives", "medications": [{"name": "Lactulose", "dose": "5ml", "frequency": "OD", "duration": "7 days"}], "investigations": [{"type": "X-ray abdomen"}]},
]

IPD_ADMISSIONS = [
    {"patient_idx": 0, "urgency": "emergency", "bed_no": "ICU-01", "ward": "PICU", "status": "admitted"},
    {"patient_idx": 2, "urgency": "elective", "bed_no": "W2-12", "ward": "Pediatric Ward", "status": "admitted"},
]

APPOINTMENTS = [
    {"patient_idx": 1, "visit_type": "follow-up", "procedure": "Wound review", "urgency": "routine", "days_offset": 2},
    {"patient_idx": 3, "visit_type": "new", "procedure": None, "urgency": "routine", "days_offset": 1},
    {"patient_idx": 4, "visit_type": "follow-up", "procedure": "Catheter removal", "urgency": "semi-urgent", "days_offset": 3},
]


async def main() -> None:
    db = Prisma()
    await db.connect()

    hospitals = await db.hospitals.find_many()
    hospital_by_name = {h.name: h.id for h in hospitals}

    # 1. Seed doctors
    doctor_by_phone: dict[str, str] = {}
    for d in DOCTORS:
        existing = await db.doctors.find_first(where={"phone": d["phone"]})
        if existing:
            doctor_by_phone[d["phone"]] = existing.id
            print(f"Doctor already exists: {d['name']} ({d['phone']})")
        else:
            hospital_id = hospital_by_name.get(d["hospital_name"])
            created = await db.doctors.create(
                data={
                    "name": d["name"],
                    "phone": d["phone"],
                    "specialty": d["specialty"],
                    "is_active": d["is_active"],
                    "hospital_id": hospital_id,
                }
            )
            doctor_by_phone[d["phone"]] = created.id
            print(f"Created doctor: {created.name} ({created.phone})")

    # 2. Seed nurses
    for n in NURSES:
        existing = await db.nurses.find_first(where={"phone": n["phone"]})
        if existing:
            print(f"Nurse already exists: {n['name']} ({n['phone']})")
            continue
        doctor_id = doctor_by_phone[n["doctor_phone"]]
        doctor = await db.doctors.find_unique(where={"id": doctor_id})
        created = await db.nurses.create(
            data={
                "name": n["name"],
                "phone": n["phone"],
                "doctor_id": doctor_id,
                "hospital_id": doctor.hospital_id if doctor else None,
            }
        )
        print(f"Created nurse: {created.name} ({created.phone})")

    # 3. Seed patients
    patient_ids: list[str] = []
    for p in PATIENTS:
        existing = await db.patients.find_first(where={"parent_phone": p["parent_phone"]})
        if existing:
            patient_ids.append(existing.id)
            print(f"Patient already exists: {p['name']}")
            continue
        doctor_id = doctor_by_phone[p["doctor_phone"]]
        doctor = await db.doctors.find_unique(where={"id": doctor_id})
        created = await db.patients.create(
            data={
                "name": p["name"],
                "age": p["age"],
                "gender": p["gender"],
                "blood_group": p["blood_group"],
                "allergies": p["allergies"],
                "parent_name": p["parent_name"],
                "parent_phone": p["parent_phone"],
                "doctor_id": doctor_id,
                "hospital_id": doctor.hospital_id if doctor else None,
            }
        )
        patient_ids.append(created.id)
        print(f"Created patient: {created.name} ({created.parent_phone})")

    # 4. Seed OPD records with medications and investigations
    opd_ids: list[str] = []
    for record in OPD_RECORDS:
        patient_id = patient_ids[record["patient_idx"]]
        patient = await db.patients.find_unique(where={"id": patient_id})
        doctor_id = patient.doctor_id
        doctor = await db.doctors.find_unique(where={"id": doctor_id})
        hospital_id = doctor.hospital_id if doctor else None
        hospital_name = None
        hospital_logo_url = None
        if hospital_id:
            hospital = await db.hospitals.find_unique(where={"id": hospital_id})
            hospital_name = hospital.name if hospital else None
            hospital_logo_url = hospital.logo_url if hospital else None

        created = await db.opd_records.create(
            data={
                "patient_id": patient_id,
                "doctor_id": doctor_id,
                "hospital_id": hospital_id,
                "hospital_name": hospital_name,
                "hospital_logo_url": hospital_logo_url,
                "visit_type": record["visit_type"],
                "complaint": record["complaint"],
                "examination": record["examination"],
                "diagnosis": record["diagnosis"],
                "surgical_decision": record["surgical_decision"],
                "planned_procedure": record["planned_procedure"],
                "advice": record["advice"],
            }
        )
        opd_ids.append(created.id)
        print(f"Created OPD record for {patient.name}: {created.diagnosis}")

        for med in record["medications"]:
            await db.medications.create(
                data={
                    "opd_record_id": created.id,
                    "name": med["name"],
                    "dose": med["dose"],
                    "frequency": med["frequency"],
                    "duration": med["duration"],
                }
            )

        for inv in record["investigations"]:
            await db.investigations.create(
                data={
                    "opd_record_id": created.id,
                    "type": inv["type"],
                    "status": "pending",
                }
            )

    # 5. Seed IPD admissions
    for adm in IPD_ADMISSIONS:
        patient_id = patient_ids[adm["patient_idx"]]
        patient = await db.patients.find_unique(where={"id": patient_id})
        doctor_id = patient.doctor_id
        doctor = await db.doctors.find_unique(where={"id": doctor_id})
        hospital_id = doctor.hospital_id if doctor else None
        hospital_name = None
        hospital_logo_url = None
        if hospital_id:
            hospital = await db.hospitals.find_unique(where={"id": hospital_id})
            hospital_name = hospital.name if hospital else None
            hospital_logo_url = hospital.logo_url if hospital else None

        created = await db.ipd_admissions.create(
            data={
                "patient_id": patient_id,
                "doctor_id": doctor_id,
                "hospital_id": hospital_id,
                "hospital_name": hospital_name,
                "hospital_logo_url": hospital_logo_url,
                "bed_no": adm["bed_no"],
                "ward": adm["ward"],
                "urgency": adm["urgency"],
                "status": adm["status"],
            }
        )
        print(f"Created IPD admission for {patient.name}: {created.ward} / {created.bed_no}")

    # 6. Seed appointments
    for appt in APPOINTMENTS:
        patient_id = patient_ids[appt["patient_idx"]]
        patient = await db.patients.find_unique(where={"id": patient_id})
        doctor_id = patient.doctor_id
        slot = datetime.utcnow() + timedelta(days=appt["days_offset"])
        created = await db.appointments.create(
            data={
                "patient_id": patient_id,
                "doctor_id": doctor_id,
                "slot_datetime": slot,
                "visit_type": appt["visit_type"],
                "procedure": appt["procedure"],
                "urgency": appt["urgency"],
                "status": "booked",
                "booked_by": "parent",
            }
        )
        print(f"Created appointment for {patient.name}: {created.visit_type} at {created.slot_datetime.isoformat()}")

    await db.disconnect()
    print("\nTest data seeding complete.")


if __name__ == "__main__":
    asyncio.run(main())
