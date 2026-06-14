package com.example.nori_tura.util

actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}
