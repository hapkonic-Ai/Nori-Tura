package com.example.nori_tura.util

import kotlinx.browser.window

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}
