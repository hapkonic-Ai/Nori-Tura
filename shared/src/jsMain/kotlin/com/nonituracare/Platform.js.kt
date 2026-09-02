package com.nonituracare

class JsPlatform : Platform {
    override val name: String =
        js("navigator.userAgent") as? String ?: "Unknown JS"
}

actual fun getPlatform(): Platform = JsPlatform()
