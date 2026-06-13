package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.PatientCreateRequest
import com.example.nori_tura.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddPatientViewModel(
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data class Success(val patient: PatientDto) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun createPatient(request: PatientCreateRequest) {
        val validationError = validate(request)
        if (validationError != null) {
            _uiState.value = UiState.Error(validationError)
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            surgeonRepository.createPatient(request)
                .onSuccess { patient ->
                    _uiState.value = UiState.Success(patient)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to create patient")
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    private fun validate(request: PatientCreateRequest): String? {
        if (request.name.isBlank()) return "Patient name is required"
        if (request.age < 0 || request.age > 150) return "Please enter a valid age"
        if (request.gender.isBlank()) return "Gender is required"
        if (request.parentName.isBlank()) return "Parent name is required"
        if (!request.parentPhone.startsWith("+91") || request.parentPhone.length != 13 ||
            request.parentPhone.drop(3).any { !it.isDigit() }
        ) {
            return "Parent phone must be +91 followed by 10 digits"
        }
        return null
    }
}
