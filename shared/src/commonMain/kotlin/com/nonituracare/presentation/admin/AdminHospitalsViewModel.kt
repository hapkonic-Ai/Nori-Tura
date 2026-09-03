package com.nonituracare.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AdminRepository
import com.nonituracare.data.dto.HospitalCreateRequest
import com.nonituracare.data.dto.HospitalRefDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminHospitalsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val hospitals: List<HospitalRefDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class CreateState {
        data object Idle : CreateState()
        data object Loading : CreateState()
        data object Success : CreateState()
        data class Error(val message: String) : CreateState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<CreateState>(CreateState.Idle)
    val createState: StateFlow<CreateState> = _createState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listHospitals()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load hospitals") }
        }
    }

    fun createHospital(request: HospitalCreateRequest) {
        _createState.value = CreateState.Loading
        viewModelScope.launch {
            repository.createHospital(request)
                .onSuccess {
                    _createState.value = CreateState.Success
                    load()
                }
                .onFailure { error ->
                    _createState.value = CreateState.Error(error.message ?: "Failed to create hospital")
                }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateState.Idle
    }
}
