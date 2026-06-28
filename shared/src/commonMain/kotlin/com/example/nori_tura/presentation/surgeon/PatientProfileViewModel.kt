package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.IpdRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.AdmissionCreateRequest
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientProfileViewModel(
    private val patientId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository(),
    private val ipdRepository: IpdRepository = IpdRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val patient: PatientDto,
            val opdRecords: List<OpdRecordDto>
        ) : UiState()

        data class Error(val message: String) : UiState()
    }

    sealed class AdmitUiState {
        data object Idle : AdmitUiState()
        data object Loading : AdmitUiState()
        data object Success : AdmitUiState()
        data class Error(val message: String) : AdmitUiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _admitUiState = MutableStateFlow<AdmitUiState>(AdmitUiState.Idle)
    val admitUiState: StateFlow<AdmitUiState> = _admitUiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val patientResult = surgeonRepository.getPatientDetail(token, patientId)
            val opdResult = surgeonRepository.getOpdRecords(token, patientId)

            val firstError = listOfNotNull(
                patientResult.exceptionOrNull(),
                opdResult.exceptionOrNull()
            ).firstOrNull()

            if (firstError != null) {
                _uiState.value = UiState.Error(firstError.message ?: "Failed to load patient profile")
            } else {
                _uiState.value = UiState.Success(
                    patient = patientResult.getOrNull() ?: PatientDto(id = patientId),
                    opdRecords = opdResult.getOrNull() ?: emptyList()
                )
            }
        }
    }

    fun admitPatient(request: AdmissionCreateRequest) {
        val token = authRepository.getToken() ?: run {
            _admitUiState.value = AdmitUiState.Error("Not authenticated")
            return
        }

        _admitUiState.value = AdmitUiState.Loading
        viewModelScope.launch {
            val result = ipdRepository.createAdmission(request)
            if (result.isSuccess) {
                _admitUiState.value = AdmitUiState.Success
                loadProfile()
            } else {
                _admitUiState.value = AdmitUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to admit patient"
                )
            }
        }
    }

    fun resetAdmitState() {
        _admitUiState.value = AdmitUiState.Idle
    }
}
