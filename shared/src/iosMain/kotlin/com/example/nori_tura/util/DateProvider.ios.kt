package com.example.nori_tura.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun getCurrentDateString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(NSDate())
}
