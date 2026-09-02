package com.nonituracare.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.DocumentsRepository
import com.nonituracare.data.IpdRepository
import com.nonituracare.data.SurgeonRepository
import com.nonituracare.data.dto.AdmissionCreateRequest
import com.nonituracare.data.dto.DocumentDto
import com.nonituracare.data.dto.OpdRecordDto
import com.nonituracare.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientProfileViewModel(
    private val patientId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository(),
    private val ipdRepository: IpdRepository = IpdRepository(),
    private val documentsRepository: DocumentsRepository = DocumentsRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val patient: PatientDto,
            val opdRecords: List<OpdRecordDto>,
            val parentDocuments: List<DocumentDto>
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
            val documentsResult = documentsRepository.getPatientDocuments(patientId)

            val firstError = listOfNotNull(
                patientResult.exceptionOrNull(),
                opdResult.exceptionOrNull(),
                documentsResult.exceptionOrNull()
            ).firstOrNull()

            if (firstError != null) {
                _uiState.value = UiState.Error(firstError.message ?: "Failed to load patient profile")
            } else {
                val parentDocuments = documentsResult.getOrNull()
                    ?.filter { it.uploadedByRole == "parent" }
                    ?: emptyList()
                _uiState.value = UiState.Success(
                    patient = patientResult.getOrNull() ?: PatientDto(id = patientId),
                    opdRecords = opdResult.getOrNull() ?: emptyList(),
                    parentDocuments = parentDocuments
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
