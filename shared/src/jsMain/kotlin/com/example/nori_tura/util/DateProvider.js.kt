package com.example.nori_tura.util

import kotlin.js.Date

actual fun getCurrentDateString(): String = Date().toISOString().take(10)
