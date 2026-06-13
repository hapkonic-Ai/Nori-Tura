package com.example.nori_tura.presentation.auth

import com.example.nori_tura.data.MeResponse

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class OtpSent(val phone: String) : AuthUiState
    data class RegistrationSubmitted(val message: String) : AuthUiState
    data class Authenticated(val role: String, val profile: MeResponse?) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
