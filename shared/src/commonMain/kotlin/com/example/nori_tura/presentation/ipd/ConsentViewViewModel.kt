package com.example.nori_tura.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.ConsentRepository
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.ConsentOtpVerifyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsentViewViewModel(
    private val consentId: String,
    private val repository: ConsentRepository = ConsentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    init {
        loadConsent()
    }

    fun loadConsent() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getConsentForm(consentId)
                .onSuccess { consent ->
                    _uiState.value = UiState.Success(consent)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load consent form")
                }
        }
    }

    fun requestOtp() {
        _otpState.value = OtpState.Sending
        viewModelScope.launch {
            repository.requestConsentOtp(consentId)
                .onSuccess { response ->
                    _otpState.value = OtpState.Sent(
                        phone = response.phone,
                        devOtp = response.devOtp
                    )
                }
                .onFailure { error ->
                    _otpState.value = OtpState.Error(error.message ?: "Failed to send OTP")
                }
        }
    }

    fun verifyOtp(request: ConsentOtpVerifyRequest) {
        _otpState.value = OtpState.Verifying
        viewModelScope.launch {
            repository.verifyConsentOtp(consentId, request)
                .onSuccess { consent ->
                    _otpState.value = OtpState.Idle
                    _uiState.value = UiState.Success(consent)
                }
                .onFailure { error ->
                    _otpState.value = OtpState.Error(error.message ?: "Failed to verify OTP")
                }
        }
    }

    fun resetOtpError() {
        if (_otpState.value is OtpState.Error) {
            _otpState.value = OtpState.Idle
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val consent: ConsentFormDto) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class OtpState {
        object Idle : OtpState()
        object Sending : OtpState()
        data class Sent(val phone: String, val devOtp: String?) : OtpState()
        object Verifying : OtpState()
        data class Error(val message: String) : OtpState()
    }
}
