package com.example.nori_tura.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AdminRepository
import com.example.nori_tura.data.dto.DoctorDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadPendingDoctors()
    }

    fun loadPendingDoctors() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listPendingDoctors()
                .onSuccess { doctors ->
                    _uiState.value = UiState.Success(doctors)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load doctors")
                }
        }
    }

    fun approveDoctor(id: String) {
        val current = (_uiState.value as? UiState.Success)?.doctors ?: emptyList()
        viewModelScope.launch {
            repository.updateDoctorStatus(id, true)
                .onSuccess {
                    _uiState.value = UiState.Success(current.filter { it.id != id })
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to approve doctor")
                }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val doctors: List<DoctorDto>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
