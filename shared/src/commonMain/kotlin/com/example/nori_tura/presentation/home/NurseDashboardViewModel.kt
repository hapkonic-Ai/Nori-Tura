package com.example.nori_tura.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.util.getCurrentDateString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NurseDashboardViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    data class Metrics(
        val patientsAddedToday: Int = 0,
        val upcomingAppointments: Int = 0,
        val activeIpdAdmissions: Int = 0
    )

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val metrics: Metrics) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val today = getCurrentDateString()

            val patientsResult = surgeonRepository.getPatients(token)
            val appointmentsResult = surgeonRepository.getAppointments(token)
            val admissionsResult = surgeonRepository.getAdmissions(token)

            if (patientsResult.isFailure || appointmentsResult.isFailure || admissionsResult.isFailure) {
                _uiState.value = UiState.Error("Failed to load dashboard metrics")
                return@launch
            }

            val patients = patientsResult.getOrNull() ?: emptyList()
            val appointments = appointmentsResult.getOrNull() ?: emptyList()
            val admissions = admissionsResult.getOrNull() ?: emptyList()

            val metrics = Metrics(
                patientsAddedToday = patients.count { it.createdAt?.startsWith(today) == true },
                upcomingAppointments = appointments.count {
                    it.status == "scheduled" && (it.slotDatetime?.compareTo(today) ?: -1) >= 0
                },
                activeIpdAdmissions = admissions.count { it.status == "admitted" }
            )

            _uiState.value = UiState.Success(metrics)
        }
    }
}
