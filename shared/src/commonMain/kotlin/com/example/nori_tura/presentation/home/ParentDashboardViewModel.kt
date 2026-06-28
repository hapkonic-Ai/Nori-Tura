package com.example.nori_tura.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.AppointmentDto
import com.example.nori_tura.data.dto.PatientDto
import com.example.nori_tura.util.getCurrentDateString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentDashboardViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    data class Dashboard(
        val children: List<PatientDto> = emptyList(),
        val appointments: List<AppointmentDto> = emptyList(),
        val admissions: List<AdmissionDto> = emptyList()
    ) {
        val upcomingAppointments: Int
            get() = appointments.count {
                it.status == "scheduled" && (it.slotDatetime?.compareTo(getCurrentDateString()) ?: -1) >= 0
            }
        val activeAdmissions: Int
            get() = admissions.count { it.status == "admitted" || it.status == "pre-op" || it.status == "in-surgery" || it.status == "recovery" }

        val activeAdmission: AdmissionDto?
            get() = admissions.firstOrNull {
                it.status == "admitted" || it.status == "pre-op" || it.status == "in-surgery" || it.status == "recovery"
            }

        val nextAppointment: AppointmentDto?
            get() = appointments
                .filter { (it.status == "scheduled" || it.status == "booked") && (it.slotDatetime?.compareTo(getCurrentDateString()) ?: -1) >= 0 }
                .minByOrNull { it.slotDatetime ?: "" }
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
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val patientsResult = surgeonRepository.getPatients(token)
            val appointmentsResult = surgeonRepository.getAppointments(token)
            val admissionsResult = surgeonRepository.getAdmissions(token)

            if (patientsResult.isFailure || appointmentsResult.isFailure || admissionsResult.isFailure) {
                _uiState.value = UiState.Error("Failed to load family dashboard")
                return@launch
            }

            _uiState.value = UiState.Success(
                Dashboard(
                    children = patientsResult.getOrNull() ?: emptyList(),
                    appointments = appointmentsResult.getOrNull() ?: emptyList(),
                    admissions = admissionsResult.getOrNull() ?: emptyList()
                )
            )
        }
    }
}
