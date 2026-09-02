package com.nonituracare.presentation.auth

import com.nonituracare.data.MeResponse

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class OtpSent(val phone: String, val devOtp: String? = null) : AuthUiState
    data class RegistrationSubmitted(val message: String) : AuthUiState
    data class Authenticated(val role: String, val profile: MeResponse?) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
