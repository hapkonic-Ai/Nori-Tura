package com.example.nori_tura.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.MeResponse
import com.example.nori_tura.data.RegisterDoctorRequest
import com.example.nori_tura.util.PushTokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun sendOtp(phone: String) {
        if (!isValidPhone(phone)) {
            _uiState.value = AuthUiState.Error("Please enter a valid Indian phone number (+91 followed by 10 digits).")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.sendOtp(phone)
                .onSuccess {
                    _uiState.value = AuthUiState.OtpSent(phone)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to send OTP")
                }
        }
    }

    fun verifyOtp(phone: String, otp: String) {
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("Please enter a valid 6-digit OTP.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.verifyOtp(phone, otp)
                .onSuccess { response ->
                    repository.saveToken(response.access_token)
                    repository.saveRole(response.role)
                    registerFcmToken()
                    _uiState.value = AuthUiState.Authenticated(response.role, profile = null)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to verify OTP")
                }
        }
    }

    fun registerDoctor(name: String, phone: String, hospital: String, specialty: String) {
        if (!isValidPhone(phone)) {
            _uiState.value = AuthUiState.Error("Please enter a valid Indian phone number (+91 followed by 10 digits).")
            return
        }
        if (name.isBlank() || hospital.isBlank() || specialty.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.registerDoctor(
                RegisterDoctorRequest(
                    name = name,
                    phone = phone,
                    hospital = hospital,
                    specialty = specialty
                )
            )
                .onSuccess { response ->
                    _uiState.value = AuthUiState.RegistrationSubmitted(
                        response.message ?: "Registration submitted. Wait for admin approval."
                    )
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to register")
                }
        }
    }

    fun checkAuthStatus() {
        val token = repository.getToken() ?: return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.getMe()
                .onSuccess { me ->
                    val role = me.role ?: repository.getRole()
                    if (role != null) {
                        registerFcmToken()
                        _uiState.value = AuthUiState.Authenticated(role, me)
                    } else {
                        repository.clearAll()
                        _uiState.value = AuthUiState.Idle
                    }
                }
                .onFailure {
                    repository.clearAll()
                    _uiState.value = AuthUiState.Idle
                }
        }
    }

    fun resetError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun logout() {
        repository.clearAll()
        _uiState.value = AuthUiState.Idle
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.startsWith("+91") &&
                phone.length == 13 &&
                phone.drop(3).all { it.isDigit() }
    }

    private fun registerFcmToken() {
        viewModelScope.launch {
            val token = PushTokenProvider.getToken() ?: return@launch
            repository.registerFcm(token, PushTokenProvider.getPlatform())
        }
    }
}
