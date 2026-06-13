package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.AppointmentDto
import com.example.nori_tura.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SurgeonDashboardViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val data: DashboardData) : UiState()
        data class Error(val message: String) : UiState()
    }

    data class DashboardData(
        val patients: List<PatientDto> = emptyList(),
        val appointments: List<AppointmentDto> = emptyList(),
        val admissions: List<AdmissionDto> = emptyList()
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val patientsResult = surgeonRepository.getPatients(token)
            val appointmentsResult = surgeonRepository.getAppointments(token)
            val admissionsResult = surgeonRepository.getAdmissions(token)

            val firstError = listOfNotNull(
                patientsResult.exceptionOrNull(),
                appointmentsResult.exceptionOrNull(),
                admissionsResult.exceptionOrNull()
            ).firstOrNull()

            if (firstError != null) {
                _uiState.value = UiState.Error(firstError.message ?: "Failed to load dashboard")
            } else {
                _uiState.value = UiState.Success(
                    DashboardData(
                        patients = patientsResult.getOrNull() ?: emptyList(),
                        appointments = appointmentsResult.getOrNull() ?: emptyList(),
                        admissions = admissionsResult.getOrNull() ?: emptyList()
                    )
                )
            }
        }
    }
}
