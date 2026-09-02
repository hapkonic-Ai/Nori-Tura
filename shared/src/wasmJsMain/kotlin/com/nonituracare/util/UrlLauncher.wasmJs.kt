package com.nonituracare.util

actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}
