package com.nonituracare.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.SurgeonRepository
import com.nonituracare.data.dto.AdmissionDto
import com.nonituracare.data.dto.AppointmentDto
import com.nonituracare.data.dto.PatientDto
import com.nonituracare.presentation.components.HospitalOption
import com.nonituracare.util.getCurrentDateString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentDashboardViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    data class Dashboard(
        val allChildren: List<PatientDto> = emptyList(),
        val allAppointments: List<AppointmentDto> = emptyList(),
        val allAdmissions: List<AdmissionDto> = emptyList(),
        val selectedHospitalId: String? = null
    ) {
        // A child registered at more than one hospital shows up as one row per
        // hospital (each a separate `patients` record) — group by hospital so the
        // switcher lists each hospital once.
        val hospitals: List<HospitalOption>
            get() = allChildren
                .mapNotNull { child -> child.hospitalId?.let { HospitalOption(it, child.hospitalName ?: "Hospital") } }
                .distinctBy { it.id }

        val children: List<PatientDto>
            get() = if (selectedHospitalId == null) allChildren
                    else allChildren.filter { it.hospitalId == selectedHospitalId }

        val appointments: List<AppointmentDto>
            get() {
                if (selectedHospitalId == null) return allAppointments
                val childIds = children.mapNotNull { it.id }.toSet()
                return allAppointments.filter { it.patientId in childIds }
            }

        val admissions: List<AdmissionDto>
            get() = if (selectedHospitalId == null) allAdmissions
                    else allAdmissions.filter { it.hospitalId == selectedHospitalId }

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

            val children = patientsResult.getOrNull() ?: emptyList()
            val hospitalIds = children.mapNotNull { it.hospitalId }.toSet()
            val savedSelection = authRepository.getSelectedHospitalId()
            val selectedHospitalId = savedSelection?.takeIf { it in hospitalIds }
                ?: children.firstOrNull { it.hospitalId != null }?.hospitalId

            _uiState.value = UiState.Success(
                Dashboard(
                    allChildren = children,
                    allAppointments = appointmentsResult.getOrNull() ?: emptyList(),
                    allAdmissions = admissionsResult.getOrNull() ?: emptyList(),
                    selectedHospitalId = selectedHospitalId
                )
            )
        }
    }

    fun selectHospital(hospitalId: String) {
        authRepository.saveSelectedHospitalId(hospitalId)
        val current = (_uiState.value as? UiState.Success)?.dashboard ?: return
        _uiState.value = UiState.Success(current.copy(selectedHospitalId = hospitalId))
    }
}
