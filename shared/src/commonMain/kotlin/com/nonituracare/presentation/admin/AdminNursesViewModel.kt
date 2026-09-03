package com.nonituracare.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AdminRepository
import com.nonituracare.data.dto.DoctorDto
import com.nonituracare.data.dto.HospitalRefDto
import com.nonituracare.data.dto.NurseCreateRequest
import com.nonituracare.data.dto.NurseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminNursesViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    data class Directory(
        val nurses: List<NurseDto> = emptyList(),
        val doctors: List<DoctorDto> = emptyList(),
        val hospitals: List<HospitalRefDto> = emptyList()
    )

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val directory: Directory) : UiState()
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
            val nursesResult = repository.listNurses()
            val doctorsResult = repository.listDoctors()
            val hospitalsResult = repository.listHospitals()

            if (nursesResult.isFailure) {
                _uiState.value = UiState.Error(nursesResult.exceptionOrNull()?.message ?: "Failed to load nurses")
                return@launch
            }

            _uiState.value = UiState.Success(
                Directory(
                    nurses = nursesResult.getOrNull() ?: emptyList(),
                    // Only doctors who are approved can actually be assigned a nurse.
                    doctors = (doctorsResult.getOrNull() ?: emptyList()).filter { it.isActive },
                    hospitals = hospitalsResult.getOrNull() ?: emptyList()
                )
            )
        }
    }

    fun createNurse(request: NurseCreateRequest) {
        _createState.value = CreateState.Loading
        viewModelScope.launch {
            repository.createNurse(request)
                .onSuccess {
                    _createState.value = CreateState.Success
                    load()
                }
                .onFailure { error ->
                    _createState.value = CreateState.Error(error.message ?: "Failed to create nurse login")
                }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateState.Idle
    }

    fun toggleNurseActive(nurse: NurseDto) {
        val id = nurse.id ?: return
        viewModelScope.launch {
            repository.updateNurseStatus(id, !nurse.isActive).onSuccess { load() }
        }
    }
}
