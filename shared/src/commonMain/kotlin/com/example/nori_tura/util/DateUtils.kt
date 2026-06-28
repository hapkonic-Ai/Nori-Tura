package com.example.nori_tura.util

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }
}

private fun parseYMD(iso: String): Triple<Int, Int, Int> {
    val parts = iso.split("-")
    require(parts.size == 3) { "Invalid date: $iso" }
    return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

private fun formatYMD(year: Int, month: Int, day: Int): String {
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

/**
 * Convert a Gregorian date to days since 1970-01-01 (epoch days).
 * Uses the Julian day number formula for reliability.
 */
fun isoDateToEpochDays(iso: String): Int {
    val (year, month, day) = parseYMD(iso)
    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m = month + 12 * a - 3
    val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    return jdn - 2440588 // JDN of 1970-01-01
}

private fun epochDaysToJdn(epochDays: Int): Int = epochDays + 2440588

/**
 * Convert epoch days back to an ISO date string.
 */
fun epochDaysToIsoDate(epochDays: Int): String {
    val jdn = epochDaysToJdn(epochDays)
    val f = jdn + 1401 + (((4 * jdn + 274277) / 146097) * 3) / 4 - 38
    val e = 4 * f + 3
    val g = (e % 1461) / 4
    val h = 5 * g + 2
    val day = (h % 153) / 5 + 1
    val month = (h / 153 + 2) % 12 + 1
    val year = e / 1461 - 4716 + (14 - month) / 12
    return formatYMD(year, month, day)
}

fun addDaysToIsoDate(iso: String, days: Int): String {
    return epochDaysToIsoDate(isoDateToEpochDays(iso) + days)
}

/**
 * Returns the short weekday name for an ISO date assuming Monday = 0.
 */
fun isoDateDayOfWeek(iso: String): Int {
    return (isoDateToEpochDays(iso) + 4) % 7
}

fun isoDateDayName(iso: String): String {
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return names[isoDateDayOfWeek(iso)]
}

/**
 * Returns the ISO dates for the Monday-to-Sunday week containing [iso].
 */
fun weekContaining(iso: String): List<String> {
    val days = isoDateToEpochDays(iso)
    val mondayOffset = isoDateDayOfWeek(iso)
    val monday = days - mondayOffset
    return (0 until 7).map { epochDaysToIsoDate(monday + it) }
}

fun formatDisplayDate(iso: String): String {
    val (year, month, day) = parseYMD(iso)
    val monthName = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )[month - 1]
    return "$monthName $day"
}

fun formatDateTime(iso: String): String {
    val datePart = iso.takeWhile { it != 'T' }.takeIf { it.length == 10 } ?: return iso
    val timePart = iso.substringAfter('T', "")
    val time = timePart.take(5).ifBlank { "--:--" }
    val (year, month, day) = parseYMD(datePart)
    val monthName = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )[month - 1]
    return "$day $monthName $year, $time"
}
