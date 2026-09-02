package com.nonituracare.util

import kotlin.js.Date

actual fun getCurrentDateString(): String = Date().toISOString().take(10)
