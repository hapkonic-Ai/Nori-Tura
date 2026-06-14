package com.example.nori_tura.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.ConsentRepository
import com.example.nori_tura.data.ConsentFormResponse
import com.example.nori_tura.data.dto.ConsentFormCreateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsentFormViewModel(
    private val repository: ConsentRepository = ConsentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun createConsentForm(request: ConsentFormCreateRequest) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.createConsentForm(request)
                .onSuccess { response ->
                    _uiState.value = UiState.Success(response)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to create consent form")
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val response: ConsentFormResponse) : UiState()
        data class Error(val message: String) : UiState()
    }
}
