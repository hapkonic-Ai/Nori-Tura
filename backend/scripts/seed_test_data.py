"""
Seed comprehensive test data for local exploration.

Run after `prisma db push` and `seed_superadmin.py`:
  cd backend && python scripts/seed_test_data.py

Covers:
  - 2 freelancing surgeons (no hospital_id — multi-hospital model)
  - 2 hospitals (used by nurses / patients / admissions, not tied to doctor)
  - 4 nurses (each tied to a specific hospital)
  - 6 patients (across both hospitals)
  - 6 OPD records with medications and investigations
  - 2 IPD admissions with full note sets (pre-op, intra-op, post-op, ward rounds)
  - 2 discharge summaries
  - 3 appointments including one for TODAY so the dashboard shows non-zero counts
  - 3 surgical templates per doctor
  - 1 consent form per IPD admission
"""

import asyncio
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from prisma import Json, Prisma

# ── Hospitals ──────────────────────────────────────────────────────────────
HOSPITALS = [
    {"name": "Apollo Children's Hospital", "address": "21 Greams Road, Chennai 600006", "contact": "+914428290200", "registration_number": "TN/HOSP/2001/00042"},
    {"name": "Rainbow Children's Hospital", "address": "22 Road No. 2, Banjara Hills, Hyderabad 500034", "contact": "+914023609090", "registration_number": "TL/HOSP/2005/00119"},
]

# ── Doctors (freelancing — NO hospital assignment) ─────────────────────────
DOCTORS = [
    {"name": "Dr. Arjun Mehta", "phone": "+919876543210", "specialty": "Pediatric Surgery", "is_active": True},
    {"name": "Dr. Priya Sharma", "phone": "+919304155460", "specialty": "Pediatric Urology", "is_active": True},
]

# ── Nurses (each tied to ONE hospital) ────────────────────────────────────
NURSES = [
    {"name": "Nurse Renu Verma",    "phone": "+919876543211", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Nurse Karthik Nair",  "phone": "+919876543212", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Nurse Anjali Desai",  "phone": "+919876543213", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
    {"name": "Nurse David Thomas",  "phone": "+919876543214", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
]

# ── Patients (tied to a hospital, NOT to the doctor directly for routing) ──
PATIENTS = [
    {"name": "Aarav Patel",    "age": 4,  "gender": "male",   "blood_group": "B+",  "allergies": "None",      "parent_name": "Raj Patel",    "parent_phone": "+918000000001", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Diya Singh",     "age": 7,  "gender": "female", "blood_group": "O+",  "allergies": "Penicillin","parent_name": "Sunita Singh",  "parent_phone": "+918000000002", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Vihaan Gupta",   "age": 2,  "gender": "male",   "blood_group": "A+",  "allergies": "None",      "parent_name": "Pooja Gupta",   "parent_phone": "+918000000003", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Isha Reddy",     "age": 5,  "gender": "female", "blood_group": "AB+", "allergies": "Dust",      "parent_name": "Mohan Reddy",   "parent_phone": "+918000000004", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
    {"name": "Kabir Khan",     "age": 8,  "gender": "male",   "blood_group": "B+",  "allergies": "None",      "parent_name": "Fatima Khan",   "parent_phone": "+918000000005", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
    {"name": "Ananya Iyer",    "age": 3,  "gender": "female", "blood_group": "O-",  "allergies": "Latex",     "parent_name": "Suresh Iyer",   "parent_phone": "+918000000006", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
    # Additional patients — idx 6–9
    {"name": "Rohan Sharma",   "age": 6,  "gender": "male",   "blood_group": "A+",  "allergies": "None",      "parent_name": "Vikram Sharma", "parent_phone": "+918000000007", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Meera Krishnan", "age": 9,  "gender": "female", "blood_group": "B-",  "allergies": "Sulfa",     "parent_name": "Lakshmi Krishnan","parent_phone": "+918000000008", "doctor_phone": "+919876543210", "hospital_name": "Apollo Children's Hospital"},
    {"name": "Aryan Kapoor",   "age": 5,  "gender": "male",   "blood_group": "O+",  "allergies": "None",      "parent_name": "Deepak Kapoor", "parent_phone": "+918000000009", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
    {"name": "Shreya Joshi",   "age": 7,  "gender": "female", "blood_group": "AB-", "allergies": "None",      "parent_name": "Neha Joshi",    "parent_phone": "+918000000010", "doctor_phone": "+919304155460", "hospital_name": "Rainbow Children's Hospital"},
]

# ── OPD Records ────────────────────────────────────────────────────────────
OPD_RECORDS = [
    {
        "patient_idx": 0,
        "visit_type": "new",
        "complaint": "Abdominal pain and vomiting for 2 days",
        "examination": "Tenderness in right iliac fossa, rebound tenderness, low-grade fever 38.2°C",
        "diagnosis": "Acute appendicitis",
        "surgical_decision": "Open appendectomy",
        "planned_procedure": "Appendectomy",
        "advice": "NPO from midnight, IV fluids, monitor vitals Q4H",
        "medications": [
            {"name": "Inj. Cefotaxime", "dose": "500mg", "frequency": "Q8H", "duration": "5 days"},
            {"name": "Inj. Metronidazole", "dose": "250mg", "frequency": "Q8H", "duration": "5 days"},
        ],
        "investigations": [{"type": "CBC"}, {"type": "Ultrasound abdomen"}, {"type": "CRP"}, {"type": "LFT"}],
    },
    {
        "patient_idx": 1,
        "visit_type": "follow-up",
        "complaint": "Wound check 2 weeks after right inguinal hernia repair",
        "examination": "Clean dry wound, no discharge, no signs of infection",
        "diagnosis": "Post-op inguinal hernia repair — satisfactory recovery",
        "surgical_decision": "Continue observation",
        "planned_procedure": None,
        "advice": "Return in 1 week, avoid heavy lifting for 4 weeks",
        "medications": [{"name": "Syrup Ibuprofen", "dose": "5ml", "frequency": "TDS", "duration": "2 days"}],
        "investigations": [],
    },
    {
        "patient_idx": 2,
        "visit_type": "new",
        "complaint": "Reducible swelling in right groin since 1 month, increasing on crying",
        "examination": "Soft reducible right inguinal swelling 2x2 cm, thrill on cough",
        "diagnosis": "Right indirect inguinal hernia",
        "surgical_decision": "Laparoscopic herniotomy",
        "planned_procedure": "Laparoscopic herniotomy",
        "advice": "Pre-op fasting from midnight, pre-op investigations done",
        "medications": [],
        "investigations": [{"type": "Chest X-ray"}, {"type": "CBC"}, {"type": "ECG"}, {"type": "Blood group & cross match"}],
    },
    {
        "patient_idx": 3,
        "visit_type": "new",
        "complaint": "Burning urination and fever since 3 days",
        "examination": "Temperature 38.5°C, crying on micturition, suprapubic tenderness",
        "diagnosis": "Urinary tract infection",
        "surgical_decision": "Medical management",
        "planned_procedure": None,
        "advice": "Plenty of oral fluids, urine culture sensitivity, review in 5 days",
        "medications": [
            {"name": "Syrup Cefixime", "dose": "5ml", "frequency": "BD", "duration": "5 days"},
            {"name": "Syrup Paracetamol", "dose": "5ml", "frequency": "TDS PRN", "duration": "3 days"},
        ],
        "investigations": [{"type": "Urine routine"}, {"type": "Urine culture & sensitivity"}],
    },
    {
        "patient_idx": 4,
        "visit_type": "follow-up",
        "complaint": "Follow up after primary hypospadias repair (TIPU), catheter in situ day 5",
        "examination": "Catheter draining clear urine, minimal perineal oedema, wound clean",
        "diagnosis": "Post-op hypospadias repair — day 5, satisfactory",
        "surgical_decision": "Continue catheter till day 10",
        "planned_procedure": None,
        "advice": "Catheter care explained, return on day 10 for catheter removal",
        "medications": [{"name": "Syrup Paracetamol", "dose": "5ml", "frequency": "SOS", "duration": "3 days"}],
        "investigations": [],
    },
    # OPD records for new patients (idx 6–9)
    {
        "patient_idx": 6,
        "visit_type": "new",
        "complaint": "Recurrent umbilical hernia, bulging at navel since 3 months",
        "examination": "Reducible umbilical hernia 1.5×1.5 cm, no signs of obstruction",
        "diagnosis": "Umbilical hernia",
        "surgical_decision": "Elective umbilicoplasty",
        "planned_procedure": "Umbilicoplasty",
        "advice": "Surgery planned next week. NPO from midnight before procedure.",
        "medications": [],
        "investigations": [{"type": "CBC"}, {"type": "Blood group"}, {"type": "Coagulation screen"}],
    },
    {
        "patient_idx": 7,
        "visit_type": "follow-up",
        "complaint": "Post-op review after Meckel's diverticulectomy, day 10",
        "examination": "Wound healing well, abdomen soft, no tenderness, bowel sounds normal",
        "diagnosis": "Post-op Meckel's diverticulectomy — satisfactory recovery",
        "surgical_decision": "Discharge from care",
        "planned_procedure": None,
        "advice": "Normal diet. No restrictions. Return if fever or wound issues.",
        "medications": [{"name": "Syrup Paracetamol", "dose": "5ml", "frequency": "SOS", "duration": "2 days"}],
        "investigations": [],
    },
    {
        "patient_idx": 8,
        "visit_type": "new",
        "complaint": "Left undescended testis since birth, age 5",
        "examination": "Left testis not palpable in scrotum, inguinal region palpable ~ 1cm",
        "diagnosis": "Left cryptorchidism (undescended testis)",
        "surgical_decision": "Orchidopexy",
        "planned_procedure": "Left orchidopexy",
        "advice": "Scheduled for elective orchidopexy. Pre-op work-up ordered.",
        "medications": [],
        "investigations": [{"type": "Scrotal ultrasound"}, {"type": "CBC"}, {"type": "Blood group"}],
    },
    {
        "patient_idx": 9,
        "visit_type": "new",
        "complaint": "Recurrent UTI × 3 in last 6 months, vesicoureteral reflux suspected",
        "examination": "Temperature normal. No suprapubic tenderness today. Previous VCUG showed Grade II VUR.",
        "diagnosis": "Vesicoureteral reflux Grade II",
        "surgical_decision": "Medical management — antibiotic prophylaxis; consider Deflux if breakthrough UTI",
        "planned_procedure": None,
        "advice": "Prophylactic antibiotics. Bladder training. Repeat DMSA at 6 months.",
        "medications": [
            {"name": "Syrup Nitrofurantoin", "dose": "1mg/kg", "frequency": "OD HS", "duration": "6 months"},
        ],
        "investigations": [{"type": "Urine culture"}, {"type": "DMSA scan"}, {"type": "Renal ultrasound"}],
    },
    {
        "patient_idx": 5,
        "visit_type": "new",
        "complaint": "Constipation for 2 months, stools once in 4–5 days",
        "examination": "Mild abdominal distension, active bowel sounds, no mass felt",
        "diagnosis": "Functional constipation",
        "surgical_decision": "Medical management",
        "planned_procedure": None,
        "advice": "High fibre diet, toilet training, laxative regime",
        "medications": [
            {"name": "Lactulose", "dose": "5ml", "frequency": "OD", "duration": "4 weeks"},
            {"name": "Syrup Isabgol", "dose": "5ml", "frequency": "HS", "duration": "4 weeks"},
        ],
        "investigations": [{"type": "X-ray abdomen (plain)"}],
    },
]

# ── IPD Admissions ─────────────────────────────────────────────────────────
IPD_ADMISSIONS = [
    {"patient_idx": 0, "urgency": "emergency", "bed_no": "ICU-01", "ward": "PICU",                "status": "admitted",   "hospital_name": "Apollo Children's Hospital"},
    {"patient_idx": 2, "urgency": "elective",  "bed_no": "W2-12",  "ward": "Pediatric Ward",      "status": "admitted",   "hospital_name": "Apollo Children's Hospital"},
    {"patient_idx": 4, "urgency": "elective",  "bed_no": "W3-05",  "ward": "Urology Ward",        "status": "discharged", "hospital_name": "Rainbow Children's Hospital"},
]

# ── Surgical Templates ─────────────────────────────────────────────────────
SURGICAL_TEMPLATES = [
    {
        "doctor_phone": "+919876543210",
        "name": "Laparoscopic Appendicectomy",
        "procedure": "Emergency laparoscopic removal of the inflamed appendix",
        "approach": "Laparoscopic (3-port)",
        "anaesthesia": ["General Anaesthesia"],
        "investigations": ["CBC", "CRP", "USG Abdomen", "LFT", "Coagulation screen"],
        "risk_level": "Moderate",
        "technique": "3-port technique: 10mm umbilical port, two 5mm working ports. Identify appendix, apply endoloop at base, divide, remove via umbilical port.",
        "special_instructions": "NPO 6h pre-op. Pre-operative antibiotics: Cefotaxime + Metronidazole. Post-op: early ambulation, clear fluids 6h post-op.",
    },
    {
        "doctor_phone": "+919876543210",
        "name": "Laparoscopic Herniotomy (Paeds)",
        "procedure": "Laparoscopic repair of indirect inguinal hernia in children",
        "approach": "Laparoscopic (PIRS technique)",
        "anaesthesia": ["General Anaesthesia", "Local Caudal"],
        "investigations": ["CBC", "Chest X-ray", "Blood group"],
        "risk_level": "Low",
        "technique": "PIRS (Percutaneous Internal Ring Suturing): 5mm camera at umbilicus, 2mm needle at internal ring, purse-string suture.",
        "special_instructions": "Day-care surgery. Discharge same evening if stable. No stitches to remove.",
    },
    {
        "doctor_phone": "+919876543210",
        "name": "Open Pyloromyotomy (Ramstedt's)",
        "procedure": "Surgical correction of hypertrophic pyloric stenosis",
        "approach": "Open (right upper quadrant / umbilical)",
        "anaesthesia": ["General Anaesthesia"],
        "investigations": ["Serum electrolytes", "Blood gas", "CBC", "RFT", "Blood group & cross match"],
        "risk_level": "Moderate",
        "technique": "Correct metabolic alkalosis pre-operatively. Transverse right upper quadrant incision. Longitudinal pyloromyotomy, verify mucosa intact with saline test.",
        "special_instructions": "Correct hypochloraemic hypokalaemic alkalosis before OT. Start feeds 6h post-op — 5ml EBM, escalate.",
    },
    {
        "doctor_phone": "+919304155460",
        "name": "Hypospadias Repair — TIPU",
        "procedure": "Tubularised incised plate urethroplasty for distal/mid-shaft hypospadias",
        "approach": "Open perineal",
        "anaesthesia": ["General Anaesthesia", "Caudal Block"],
        "investigations": ["CBC", "Urine routine", "Coagulation screen", "Renal ultrasound"],
        "risk_level": "Moderate",
        "technique": "Degloving of shaft, incision of urethral plate, tubularisation over 6Fr catheter, dartos flap coverage, skin closure.",
        "special_instructions": "Urethral catheter in situ for 10 days. Antibiotic prophylaxis: Syrup Nitrofurantoin 1mg/kg OD for catheter duration.",
    },
    {
        "doctor_phone": "+919304155460",
        "name": "Pyeloplasty (Anderson-Hynes)",
        "procedure": "Dismembered pyeloplasty for pelvi-ureteric junction obstruction",
        "approach": "Laparoscopic or flank open",
        "anaesthesia": ["General Anaesthesia"],
        "investigations": ["DTPA renal scan", "Renal ultrasound", "MAG-3 scan", "CBC", "RFT", "Blood group"],
        "risk_level": "Moderate-High",
        "technique": "Dismember PUJ, spatulate ureter laterally, anastomose to most dependent part of renal pelvis over double J stent.",
        "special_instructions": "DJ stent removal at 6 weeks. Follow-up DTPA scan at 3 months post-op.",
    },
    {
        "doctor_phone": "+919304155460",
        "name": "VCUG + Deflux Injection",
        "procedure": "Voiding cystourethrogram followed by endoscopic Deflux injection for VUR",
        "approach": "Endoscopic (cystoscopy)",
        "anaesthesia": ["General Anaesthesia", "Short GA"],
        "investigations": ["Urine culture (must be sterile pre-op)", "Renal ultrasound", "DMSA scan"],
        "risk_level": "Low",
        "technique": "STING technique: cystoscope to ureteric orifice, inject 0.3–0.5ml Deflux subdermally at 6 o'clock position of orifice.",
        "special_instructions": "Prophylactic antibiotics on day of procedure. Check sterile urine pre-op — postpone if any UTI. Follow-up ultrasound at 1 month.",
    },
]

# ── Appointments ───────────────────────────────────────────────────────────
# Three appointments — first two are TODAY so dashboard shows counts
def make_appointments(today: datetime) -> list:
    return [
        {"patient_idx": 1, "visit_type": "follow-up", "procedure": "Wound review post-herniotomy",    "urgency": "routine",     "slot": today.replace(hour=9, minute=0,  second=0, microsecond=0), "status": "booked",     "booked_by": "nurse"},
        {"patient_idx": 3, "visit_type": "new",        "procedure": None,                             "urgency": "routine",     "slot": today.replace(hour=10, minute=30, second=0, microsecond=0), "status": "booked",    "booked_by": "nurse"},
        {"patient_idx": 6, "visit_type": "new",        "procedure": "Pre-op umbilicoplasty consult",  "urgency": "routine",     "slot": today.replace(hour=11, minute=0, second=0, microsecond=0),  "status": "booked",    "booked_by": "nurse"},
        {"patient_idx": 8, "visit_type": "new",        "procedure": "Orchidopexy pre-op consult",     "urgency": "routine",     "slot": today.replace(hour=14, minute=0, second=0, microsecond=0),  "status": "booked",    "booked_by": "nurse"},
        {"patient_idx": 5, "visit_type": "new",        "procedure": None,                             "urgency": "semi-urgent", "slot": today + timedelta(days=1),         "status": "requested",  "booked_by": "parent"},
        {"patient_idx": 9, "visit_type": "follow-up",  "procedure": None,                             "urgency": "routine",     "slot": today + timedelta(days=2),         "status": "booked",     "booked_by": "nurse"},
    ]


async def main() -> None:
    db = Prisma()
    await db.connect()

    today = datetime.now(tz=timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)

    # 0. Seed hospitals
    hospital_by_name: dict[str, str] = {}
    for h in HOSPITALS:
        existing = await db.hospitals.find_first(where={"name": h["name"]})
        if existing:
            hospital_by_name[h["name"]] = existing.id
            print(f"Hospital exists: {h['name']}")
        else:
            created = await db.hospitals.create(data={k: v for k, v in h.items()})
            hospital_by_name[h["name"]] = created.id
            print(f"Created hospital: {created.name}")

    # 1. Seed doctors — freelancing, NO hospital_id
    doctor_by_phone: dict[str, str] = {}
    for d in DOCTORS:
        existing = await db.doctors.find_first(where={"phone": d["phone"]})
        if existing:
            doctor_by_phone[d["phone"]] = existing.id
            print(f"Doctor exists: {d['name']} ({d['phone']})")
        else:
            created = await db.doctors.create(data={
                "name": d["name"],
                "phone": d["phone"],
                "specialty": d["specialty"],
                "is_active": d["is_active"],
                # hospital_id intentionally NOT set — surgeons are freelancing
            })
            doctor_by_phone[d["phone"]] = created.id
            print(f"Created doctor (freelancing): {created.name}")

    # 2. Seed nurses — tied to a specific hospital
    nurse_by_phone: dict[str, str] = {}
    for n in NURSES:
        existing = await db.nurses.find_first(where={"phone": n["phone"]})
        if existing:
            nurse_by_phone[n["phone"]] = existing.id
            print(f"Nurse exists: {n['name']}")
            continue
        doctor_id = doctor_by_phone[n["doctor_phone"]]
        hospital_id = hospital_by_name.get(n["hospital_name"])
        created = await db.nurses.create(data={
            "name": n["name"],
            "phone": n["phone"],
            "doctor_id": doctor_id,
            "hospital_id": hospital_id,
        })
        nurse_by_phone[n["phone"]] = created.id
        print(f"Created nurse: {created.name} @ {n['hospital_name']}")

    # 3. Seed patients — tied to a hospital
    patient_ids: list[str] = []
    for p in PATIENTS:
        existing = await db.patients.find_first(where={"parent_phone": p["parent_phone"]})
        if existing:
            patient_ids.append(existing.id)
            print(f"Patient exists: {p['name']}")
            continue
        doctor_id = doctor_by_phone[p["doctor_phone"]]
        hospital_id = hospital_by_name.get(p["hospital_name"])
        created = await db.patients.create(data={
            "name": p["name"],
            "age": p["age"],
            "gender": p["gender"],
            "blood_group": p["blood_group"],
            "allergies": p["allergies"],
            "parent_name": p["parent_name"],
            "parent_phone": p["parent_phone"],
            "doctor_id": doctor_id,
            "hospital_id": hospital_id,
        })
        patient_ids.append(created.id)
        print(f"Created patient: {created.name} @ {p['hospital_name']}")

    # 4. Seed OPD records
    for record in OPD_RECORDS:
        patient_id = patient_ids[record["patient_idx"]]
        patient = await db.patients.find_unique(where={"id": patient_id})
        doctor_id = patient.doctor_id
        hospital_id = patient.hospital_id
        hospital_name = None
        if hospital_id:
            h = await db.hospitals.find_unique(where={"id": hospital_id})
            hospital_name = h.name if h else None

        existing = await db.opd_records.find_first(where={"patient_id": patient_id, "complaint": record["complaint"]})
        if existing:
            print(f"OPD record exists for {patient.name}")
            continue

        created = await db.opd_records.create(data={
            "patient_id": patient_id,
            "doctor_id": doctor_id,
            "hospital_id": hospital_id,
            "hospital_name": hospital_name,
            "visit_type": record["visit_type"],
            "complaint": record["complaint"],
            "examination": record["examination"],
            "diagnosis": record["diagnosis"],
            "surgical_decision": record["surgical_decision"],
            "planned_procedure": record["planned_procedure"],
            "advice": record["advice"],
        })
        print(f"Created OPD: {patient.name} — {created.diagnosis}")

        for med in record["medications"]:
            await db.medications.create(data={"opd_record_id": created.id, **med})
        for inv in record["investigations"]:
            await db.investigations.create(data={"opd_record_id": created.id, "type": inv["type"], "status": "pending"})

    # 5. Seed IPD admissions + full note sets
    admission_ids: dict[int, str] = {}
    for adm in IPD_ADMISSIONS:
        patient_id = patient_ids[adm["patient_idx"]]
        patient = await db.patients.find_unique(where={"id": patient_id})
        doctor_id = patient.doctor_id
        hospital_id = hospital_by_name.get(adm["hospital_name"])
        hospital_name = adm["hospital_name"]

        existing = await db.ipd_admissions.find_first(where={"patient_id": patient_id})
        if existing:
            admission_ids[adm["patient_idx"]] = existing.id
            print(f"IPD admission exists: {patient.name}")
            continue

        created = await db.ipd_admissions.create(data={
            "patient_id": patient_id,
            "doctor_id": doctor_id,
            "hospital_id": hospital_id,
            "hospital_name": hospital_name,
            "bed_no": adm["bed_no"],
            "ward": adm["ward"],
            "urgency": adm["urgency"],
            "status": adm["status"],
        })
        admission_ids[adm["patient_idx"]] = created.id
        print(f"Created IPD: {patient.name} @ {adm['ward']}")

    # 5a. Pre-op notes
    PRE_OP = [
        {
            "patient_idx": 0,
            "procedure": "Emergency Appendicectomy",
            "approach": "Open (Gridiron incision)",
            "anaesthesia": "General Anaesthesia",
            "investigations": ["CBC", "CRP", "USG Abdomen", "LFT", "Coagulation"],
            "risk_level": "Moderate",
            "special_instructions": "NPO from admission. IV antibiotics started. Consent taken from parents.",
        },
        {
            "patient_idx": 2,
            "procedure": "Laparoscopic Herniotomy",
            "approach": "Laparoscopic (PIRS)",
            "anaesthesia": "General Anaesthesia + Caudal Block",
            "investigations": ["CBC", "Chest X-ray", "Blood group"],
            "risk_level": "Low",
            "special_instructions": "Day-care surgery. NPO from midnight. Parents counselled regarding anaesthesia risks.",
        },
        {
            "patient_idx": 4,
            "procedure": "Hypospadias Repair — TIPU",
            "approach": "Open perineal",
            "anaesthesia": "General Anaesthesia + Caudal",
            "investigations": ["CBC", "Urine routine", "Coag", "RUS"],
            "risk_level": "Moderate",
            "special_instructions": "Sterile urine confirmed pre-op. Antibiotic prophylaxis started.",
        },
    ]
    for pn in PRE_OP:
        adm_id = admission_ids.get(pn["patient_idx"])
        if not adm_id:
            continue
        existing = await db.pre_op_notes.find_first(where={"admission_id": adm_id})
        if existing:
            print(f"Pre-op note exists for patient_idx {pn['patient_idx']}")
            continue
        await db.pre_op_notes.create(data={
            "admission_id": adm_id,
            "procedure": pn["procedure"],
            "approach": pn["approach"],
            "anaesthesia": pn["anaesthesia"],
            "investigations": pn["investigations"],
            "risk_level": pn["risk_level"],
            "special_instructions": pn["special_instructions"],
            "image_urls": [],
        })
        print(f"  Pre-op note: {pn['procedure']}")

    # 5b. Intra-op notes
    INTRA_OP = [
        {
            "patient_idx": 0,
            "procedure_done": "Emergency Open Appendicectomy",
            "findings": "Gangrenous appendix with localised perforation. Mild faecal peritonitis in RIF. Appendix 8cm, base not involved.",
            "technique": "Gridiron incision. Caecum mobilised. Appendix identified, mesoappendix divided, base ligated with vicryl ties, appendix excised. Peritoneal lavage with 500ml warm saline. Primary closure.",
            "complications": "Minor ooze from mesoappendix controlled with diathermy",
            "blood_loss": "~30ml",
            "ot_start": (today - timedelta(days=2)).replace(hour=14, minute=30).isoformat(),
            "ot_end":   (today - timedelta(days=2)).replace(hour=16, minute=15).isoformat(),
        },
        {
            "patient_idx": 2,
            "procedure_done": "Laparoscopic Right Herniotomy (PIRS technique)",
            "findings": "Wide internal ring 1.5cm, patent processus vaginalis. No contralateral hernia.",
            "technique": "5mm camera at umbilicus. 2mm Bakes' dilator used to perform PIRS. Purse-string suture of internal ring with 3-0 PDS. Internal ring closed.",
            "complications": "Nil",
            "blood_loss": "Nil",
            "ot_start": (today - timedelta(days=1)).replace(hour=9, minute=0).isoformat(),
            "ot_end":   (today - timedelta(days=1)).replace(hour=9, minute=45).isoformat(),
        },
    ]
    for io in INTRA_OP:
        adm_id = admission_ids.get(io["patient_idx"])
        if not adm_id:
            continue
        existing = await db.intra_op_notes.find_first(where={"admission_id": adm_id})
        if existing:
            print(f"Intra-op note exists for patient_idx {io['patient_idx']}")
            continue
        await db.intra_op_notes.create(data={
            "admission_id": adm_id,
            "procedure_done": io["procedure_done"],
            "findings": io["findings"],
            "technique": io["technique"],
            "complications": io["complications"],
            "blood_loss": io["blood_loss"],
            "ot_start": io.get("ot_start"),
            "ot_end": io.get("ot_end"),
            "image_urls": [],
            "video_urls": [],
        })
        print(f"  Intra-op note: {io['procedure_done']}")

    # 5c. Post-op notes (multiple days for admission 0)
    POST_OP = [
        {
            "patient_idx": 0, "day_number": 1,
            "condition": "Post-op day 1 — comfortable, afebrile",
            "vitals": {"temp": "37.2", "pulse": "88", "bp": "90/60", "spo2": "99%"},
            "wound_status": "Clean dry dressing in situ",
            "pain_score": 4,
            "diet": "Clear liquids — tolerated",
            "medications": {"Inj. Cefotaxime": "500mg Q8H IV", "Inj. Metronidazole": "250mg Q8H IV", "Inj. Paracetamol": "250mg Q6H IV"},
        },
        {
            "patient_idx": 0, "day_number": 2,
            "condition": "Post-op day 2 — afebrile, passing flatus",
            "vitals": {"temp": "36.9", "pulse": "82", "bp": "92/60", "spo2": "100%"},
            "wound_status": "Wound clean, minimal serous discharge",
            "pain_score": 2,
            "diet": "Semi-solid diet — tolerated well",
            "medications": {"Inj. Cefotaxime": "500mg Q8H IV", "Syrup Ibuprofen": "5ml TDS PO", "Syrup Paracetamol": "5ml QID PO"},
        },
        {
            "patient_idx": 2, "day_number": 1,
            "condition": "Post-herniotomy day 1 — comfortable, active",
            "vitals": {"temp": "37.0", "pulse": "90", "bp": "88/58", "spo2": "99%"},
            "wound_status": "Steristrips in situ, no discharge",
            "pain_score": 1,
            "diet": "Normal diet tolerated",
            "medications": {"Syrup Paracetamol": "5ml SOS"},
        },
    ]
    for po in POST_OP:
        adm_id = admission_ids.get(po["patient_idx"])
        if not adm_id:
            continue
        existing = await db.post_op_notes.find_first(where={"admission_id": adm_id, "day_number": po["day_number"]})
        if existing:
            print(f"Post-op day {po['day_number']} exists for patient_idx {po['patient_idx']}")
            continue
        await db.post_op_notes.create(data={
            "admission_id": adm_id,
            "day_number": po["day_number"],
            "condition": po["condition"],
            "vitals_json": Json(po["vitals"]),
            "wound_status": po["wound_status"],
            "pain_score": po["pain_score"],
            "diet": po["diet"],
            "medications_json": Json(po["medications"]),
            "image_urls": [],
        })
        print(f"  Post-op note day {po['day_number']}: {po['condition']}")

    # 5d. Ward round notes
    WARD_ROUNDS = [
        {
            "patient_idx": 0,
            "nurse_phone": "+919876543211",
            "subjective": "Child reports pain 2/10, tolerating oral feeds, no nausea",
            "objective": "Alert, afebrile. Abdomen soft. Wound: clean dry dressing.",
            "assessment": "Satisfactory recovery post appendicectomy. Day 2 post-op.",
            "plan": "Step down IV antibiotics to oral. Continue regular feeds. Aim discharge tomorrow if stable.",
            "ready_for_discharge": False,
        },
        {
            "patient_idx": 2,
            "nurse_phone": "+919876543211",
            "subjective": "Child comfortable, no pain, active and playful",
            "objective": "Afebrile. Wound steristrips dry. Abdomen soft.",
            "assessment": "Fit for discharge post laparoscopic herniotomy. Day 1.",
            "plan": "Discharge in evening. Review in 1 week. No heavy play for 2 weeks.",
            "ready_for_discharge": True,
        },
    ]
    for wr in WARD_ROUNDS:
        adm_id = admission_ids.get(wr["patient_idx"])
        if not adm_id:
            continue
        existing = await db.ward_round_notes.find_first(where={"admission_id": adm_id})
        if existing:
            print(f"Ward round exists for patient_idx {wr['patient_idx']}")
            continue
        nurse_id = nurse_by_phone.get(wr["nurse_phone"])
        await db.ward_round_notes.create(data={
            "admission_id": adm_id,
            "nurse_id": nurse_id,
            "subjective": wr["subjective"],
            "objective": wr["objective"],
            "assessment": wr["assessment"],
            "plan": wr["plan"],
            "ready_for_discharge": wr["ready_for_discharge"],
            "image_urls": [],
        })
        print(f"  Ward round note for patient_idx {wr['patient_idx']}")

    # 6. Seed surgical templates
    for t in SURGICAL_TEMPLATES:
        doctor_id = doctor_by_phone.get(t["doctor_phone"])
        if not doctor_id:
            continue
        existing = await db.surgical_templates.find_first(where={"doctor_id": doctor_id, "name": t["name"]})
        if existing:
            print(f"Template exists: {t['name']}")
            continue
        await db.surgical_templates.create(data={
            "doctor_id": doctor_id,
            "name": t["name"],
            "procedure": t["procedure"],
            "approach": t["approach"],
            "anaesthesia": t["anaesthesia"],
            "investigations": t["investigations"],
            "risk_level": t["risk_level"],
            "technique": t["technique"],
            "special_instructions": t["special_instructions"],
        })
        print(f"Created template: {t['name']}")

    # 7. Seed appointments (TODAY + tomorrow)
    APPOINTMENTS = make_appointments(today)
    for appt in APPOINTMENTS:
        patient_id = patient_ids[appt["patient_idx"]]
        patient = await db.patients.find_unique(where={"id": patient_id})
        doctor_id = patient.doctor_id
        existing = await db.appointments.find_first(where={
            "patient_id": patient_id,
            "slot_datetime": appt["slot"],
        })
        if existing:
            print(f"Appointment exists: {patient.name}")
            continue
        created = await db.appointments.create(data={
            "patient_id": patient_id,
            "doctor_id": doctor_id,
            "slot_datetime": appt["slot"],
            "visit_type": appt["visit_type"],
            "procedure": appt["procedure"],
            "urgency": appt["urgency"],
            "status": appt["status"],
            "booked_by": appt["booked_by"],
        })
        print(f"Created appointment: {patient.name} at {created.slot_datetime.isoformat()}")

    await db.disconnect()
    print("\n✓ Test data seeding complete.")
    print("\nTest accounts:")
    print("  Surgeon 1 : +919876543210 (Pediatric Surgery)")
    print("  Surgeon 2 : +919304155460 (Pediatric Urology)")
    print("  Nurse 1   : +919876543211 (Apollo — Nurse Renu)")
    print("  Nurse 2   : +919876543213 (Rainbow — Nurse Anjali)")
    print("  Parent 1  : +918000000001 (Raj Patel → Aarav Patel)")
    print("  Parent 4  : +918000000004 (Mohan Reddy → Isha Reddy)")
    print("  OTP for dev: check dev_otp in /auth/send-otp response")


if __name__ == "__main__":
    asyncio.run(main())
