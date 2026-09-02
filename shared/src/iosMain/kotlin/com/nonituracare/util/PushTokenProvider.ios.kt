package com.nonituracare.util

actual object PushTokenProvider {
    actual suspend fun getToken(): String? = null
    actual fun getPlatform(): String = "ios"
}
