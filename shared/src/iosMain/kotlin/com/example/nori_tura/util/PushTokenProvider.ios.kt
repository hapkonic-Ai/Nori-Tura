package com.example.nori_tura.util

actual object PushTokenProvider {
    actual suspend fun getToken(): String? = null
    actual fun getPlatform(): String = "ios"
}
