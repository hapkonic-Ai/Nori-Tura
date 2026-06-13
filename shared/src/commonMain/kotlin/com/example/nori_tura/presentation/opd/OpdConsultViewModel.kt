package com.example.nori_tura.presentation.opd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.AiDiagnosisRequest
import com.example.nori_tura.data.dto.AiDiagnosisResponse
import com.example.nori_tura.data.dto.OpdRecordCreateRequest
import com.example.nori_tura.data.dto.OpdRecordDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpdConsultViewModel(
    private val patientId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data object Idle : UiState()
        data class Success(val record: OpdRecordDto) : UiState()
        data class Error(val message: String) : UiState()
        data class AiSuggestions(val response: AiDiagnosisResponse) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _patientName = MutableStateFlow<String?>(null)
    val patientName: StateFlow<String?> = _patientName.asStateFlow()

    private val _patientAge = MutableStateFlow<Int?>(null)
    val patientAge: StateFlow<Int?> = _patientAge.asStateFlow()

    private val _patientGender = MutableStateFlow<String?>(null)
    val patientGender: StateFlow<String?> = _patientGender.asStateFlow()

    init {
        loadPatient()
    }

    fun loadPatient() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            surgeonRepository.getPatientDetail(token, patientId)
                .onSuccess { patient ->
                    _patientName.value = patient.name
                    _patientAge.value = patient.age
                    _patientGender.value = patient.gender
                    _uiState.value = UiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load patient details")
                }
        }
    }

    fun requestAiSuggestions(complaint: String, examination: String) {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val request = AiDiagnosisRequest(
                patientId = patientId,
                complaint = complaint,
                examination = examination,
                age = _patientAge.value,
                gender = _patientGender.value
            )
            surgeonRepository.suggestDiagnosis(token, request)
                .onSuccess { response ->
                    _uiState.value = UiState.AiSuggestions(response)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to get AI suggestions")
                }
        }
    }

    fun saveOpdRecord(request: OpdRecordCreateRequest) {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            surgeonRepository.createOpdRecord(token, patientId, request)
                .onSuccess { record ->
                    _uiState.value = UiState.Success(record)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to save OPD record")
                }
        }
    }

    fun resetError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }

    fun clearAiSuggestions() {
        if (_uiState.value is UiState.AiSuggestions) {
            _uiState.value = UiState.Idle
        }
    }
}
