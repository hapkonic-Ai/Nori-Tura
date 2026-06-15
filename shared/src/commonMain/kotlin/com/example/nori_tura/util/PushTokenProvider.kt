package com.example.nori_tura.util

expect object PushTokenProvider {
    /** Returns the FCM/APNs push token, or null if unavailable. */
    suspend fun getToken(): String?

    /** Returns the client platform identifier: android, ios, or web. */
    fun getPlatform(): String
}
