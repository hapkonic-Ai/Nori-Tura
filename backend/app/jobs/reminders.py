import logging
from datetime import date, datetime, timedelta

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from app.core.database import prisma
from app.services import whatsapp_service

logger = logging.getLogger(__name__)

_scheduler: AsyncIOScheduler | None = None


async def send_follow_up_reminders() -> None:
    """Send WhatsApp reminders for follow-ups scheduled for tomorrow."""
    tomorrow = date.today() + timedelta(days=1)
    start_of_day = datetime.combine(tomorrow, datetime.min.time())
    end_of_day = datetime.combine(tomorrow, datetime.max.time())

    records = await prisma.opd_records.find_many(
        where={
            "follow_up_date": {
                "gte": start_of_day,
                "lte": end_of_day,
            },
            "reminder_sent": False,
        },
        include={
            "patient": True,
            "doctor": True,
        },
    )

    logger.info("Found %d follow-up record(s) to remind", len(records))

    for record in records:
        patient = record.patient
        doctor = record.doctor
        if not patient or not doctor:
            continue

        parent_phone = patient.parent_phone
        if not parent_phone:
            logger.warning(
                "No parent phone for patient %s; skipping reminder", patient.id
            )
            continue

        try:
            result = await whatsapp_service.send_follow_up_reminder(
                to=parent_phone,
                patient_name=patient.name,
                doctor_name=doctor.name,
                follow_up_date=tomorrow.isoformat(),
            )
            logger.info("Reminder sent to %s: %s", parent_phone, result)

            await prisma.opd_records.update(
                where={"id": record.id},
                data={"reminder_sent": True},
            )
        except Exception as e:
            logger.exception("Failed to send reminder for record %s: %s", record.id, e)


def start_reminder_scheduler() -> AsyncIOScheduler:
    """Start the daily reminder scheduler."""
    global _scheduler
    if _scheduler is not None:
        return _scheduler

    _scheduler = AsyncIOScheduler(timezone="Asia/Kolkata")
    _scheduler.add_job(
        send_follow_up_reminders,
        trigger=CronTrigger(hour=9, minute=0),
        id="follow_up_reminders",
        replace_existing=True,
    )
    _scheduler.start()
    logger.info("Follow-up reminder scheduler started")
    return _scheduler


def shutdown_reminder_scheduler() -> None:
    """Shutdown the reminder scheduler."""
    global _scheduler
    if _scheduler is not None:
        _scheduler.shutdown()
        _scheduler = None
        logger.info("Follow-up reminder scheduler shutdown")
