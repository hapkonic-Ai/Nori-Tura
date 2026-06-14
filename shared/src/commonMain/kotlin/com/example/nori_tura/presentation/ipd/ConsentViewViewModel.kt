package com.example.nori_tura.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.ConsentRepository
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.ConsentSignRequest
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

    fun signConsent(request: ConsentSignRequest) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.signConsentForm(consentId, request)
                .onSuccess { consent ->
                    _uiState.value = UiState.Success(consent)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to sign consent form")
                }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val consent: ConsentFormDto) : UiState()
        data class Error(val message: String) : UiState()
    }
}
