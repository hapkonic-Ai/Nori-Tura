package com.nonituracare.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.IpdRepository
import com.nonituracare.data.SurgeonRepository
import com.nonituracare.data.dto.AdmissionCreateRequest
import com.nonituracare.data.dto.AdmissionDto
import com.nonituracare.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdmissionsListViewModel(
    private val repository: IpdRepository = IpdRepository(),
    private val patientsRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _patients = MutableStateFlow<List<PatientDto>>(emptyList())
    val patients: StateFlow<List<PatientDto>> = _patients.asStateFlow()

    init {
        loadAdmissions()
        loadPatients()
    }

    private fun loadPatients() {
        viewModelScope.launch {
            patientsRepository.getPatients("")
                .onSuccess { _patients.value = it }
        }
    }

    fun loadAdmissions() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listAdmissions()
                .onSuccess { admissions ->
                    _uiState.value = UiState.Success(admissions)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load admissions")
                }
        }
    }

    fun createAdmission(request: AdmissionCreateRequest) {
        val current = (_uiState.value as? UiState.Success)?.admissions ?: emptyList()
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.createAdmission(request)
                .onSuccess { admission ->
                    _uiState.value = UiState.Success(current + admission)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to admit patient")
                }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val admissions: List<AdmissionDto>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
