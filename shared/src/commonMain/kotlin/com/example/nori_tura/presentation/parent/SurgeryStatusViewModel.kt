package com.example.nori_tura.presentation.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.AdmissionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SurgeryStatusViewModel(
    private val admissionId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val repository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val admission: AdmissionDto) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadAdmission()
    }

    fun loadAdmission() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getAdmission(token, admissionId)
                .onSuccess { admission ->
                    _uiState.value = UiState.Success(admission)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load surgery status")
                }
        }
    }
}
