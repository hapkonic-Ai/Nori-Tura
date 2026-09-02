package com.nonituracare.util

actual fun getCurrentDateString(): String =
    js("new Date().toISOString().slice(0,10)")
