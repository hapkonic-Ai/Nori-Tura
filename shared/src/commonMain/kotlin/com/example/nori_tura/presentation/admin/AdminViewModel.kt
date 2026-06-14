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

    data class Dashboard(
        val pendingDoctors: List<DoctorDto> = emptyList(),
        val allDoctors: List<DoctorDto> = emptyList()
    ) {
        val pendingCount: Int get() = pendingDoctors.size
        val totalCount: Int get() = allDoctors.size
        val activeCount: Int get() = allDoctors.count { it.isActive }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val dashboard: Dashboard) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val pendingResult = repository.listPendingDoctors()
            val allResult = repository.listDoctors()

            if (pendingResult.isFailure || allResult.isFailure) {
                _uiState.value = UiState.Error(pendingResult.exceptionOrNull()?.message
                    ?: allResult.exceptionOrNull()?.message
                    ?: "Failed to load dashboard")
                return@launch
            }

            _uiState.value = UiState.Success(
                Dashboard(
                    pendingDoctors = pendingResult.getOrNull() ?: emptyList(),
                    allDoctors = allResult.getOrNull() ?: emptyList()
                )
            )
        }
    }

    fun approveDoctor(id: String) {
        val current = (_uiState.value as? UiState.Success)?.dashboard ?: return
        viewModelScope.launch {
            repository.updateDoctorStatus(id, true)
                .onSuccess { approved ->
                    val updatedPending = current.pendingDoctors.filter { it.id != id }
                    val updatedAll = current.allDoctors.map {
                        if (it.id == approved.id) approved else it
                    }
                    _uiState.value = UiState.Success(
                        current.copy(
                            pendingDoctors = updatedPending,
                            allDoctors = updatedAll
                        )
                    )
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to approve doctor")
                }
        }
    }
}
