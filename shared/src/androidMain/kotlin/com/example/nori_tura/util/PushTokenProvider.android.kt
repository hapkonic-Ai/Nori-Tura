package com.example.nori_tura.util

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

actual object PushTokenProvider {
    actual suspend fun getToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }

    actual fun getPlatform(): String = "android"
}
