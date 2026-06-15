package com.example.nori_tura.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthRepository(
    private val client: HttpClient = ApiClient.client,
    private val settings: Settings = Settings()
) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_ROLE = "auth_role"
    }

    suspend fun sendOtp(phone: String): Result<OtpResponse> = safeApiCall {
        client.post("/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(SendOtpRequest(phone))
        }.body()
    }

    suspend fun verifyOtp(phone: String, otp: String): Result<AuthResponse> = safeApiCall {
        client.post("/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(phone, otp))
        }.body()
    }

    suspend fun registerDoctor(request: RegisterDoctorRequest): Result<RegisterDoctorResponse> = safeApiCall {
        client.post("/auth/register-doctor") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getMe(): Result<MeResponse> = safeApiCall {
        client.get("/auth/me").body()
    }

    suspend fun registerFcm(fcmToken: String, platform: String): Result<Unit> = safeApiCall {
        client.post("/auth/register-fcm") {
            contentType(ContentType.Application.Json)
            setBody(RegisterFcmRequest(fcmToken, platform))
        }
    }

    fun saveToken(token: String) {
        settings[KEY_TOKEN] = token
    }

    fun getToken(): String? = settings[KEY_TOKEN]

    fun clearToken() {
        settings.remove(KEY_TOKEN)
    }

    fun saveRole(role: String) {
        settings[KEY_ROLE] = role
    }

    fun getRole(): String? = settings[KEY_ROLE]

    fun clearRole() {
        settings.remove(KEY_ROLE)
    }

    fun clearAll() {
        clearToken()
        clearRole()
    }
}
