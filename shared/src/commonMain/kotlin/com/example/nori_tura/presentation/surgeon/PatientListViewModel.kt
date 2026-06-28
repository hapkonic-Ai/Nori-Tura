package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.PatientDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class PatientListViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val patients: List<PatientDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _diagnosisQuery = MutableStateFlow("")
    val diagnosisQuery: StateFlow<String> = _diagnosisQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _selectedHospital = MutableStateFlow<String?>(null)
    val selectedHospital: StateFlow<String?> = _selectedHospital.asStateFlow()

    private val _availableHospitals = MutableStateFlow<List<String>>(emptyList())
    val availableHospitals: StateFlow<List<String>> = _availableHospitals.asStateFlow()

    init {
        _searchQuery
            .debounce(300)
            .onEach { fetchPatients() }
            .launchIn(viewModelScope)

        _diagnosisQuery
            .debounce(300)
            .onEach { fetchPatients() }
            .launchIn(viewModelScope)

        _selectedStatus
            .onEach { fetchPatients() }
            .launchIn(viewModelScope)

        _selectedHospital
            .onEach { applyFilters() }
            .launchIn(viewModelScope)

        fetchPatients()
    }

    fun fetchPatients() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            surgeonRepository.getPatients(
                token = token,
                search = _searchQuery.value.takeIf { it.isNotBlank() },
                diagnosis = _diagnosisQuery.value.takeIf { it.isNotBlank() },
                status = _selectedStatus.value
            )
                .onSuccess { patients ->
                    _availableHospitals.value = patients.mapNotNull { it.hospitalName }.distinct().sorted()
                    applyFilters(patients)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load patients")
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDiagnosisQueryChange(query: String) {
        _diagnosisQuery.value = query
    }

    fun onStatusFilterSelected(status: String?) {
        _selectedStatus.value = status
    }

    fun onHospitalSelected(hospital: String?) {
        _selectedHospital.value = hospital
    }

    private fun applyFilters(patients: List<PatientDto>? = null) {
        val source = patients ?: (uiState.value as? UiState.Success)?.patients ?: return
        val filtered = if (_selectedHospital.value == null) {
            source
        } else {
            source.filter { it.hospitalName == _selectedHospital.value }
        }
        _uiState.value = UiState.Success(filtered)
    }
}
