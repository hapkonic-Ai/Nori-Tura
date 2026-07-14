package com.example.nori_tura.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun getCurrentDateString(): String {
    return Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()
}
