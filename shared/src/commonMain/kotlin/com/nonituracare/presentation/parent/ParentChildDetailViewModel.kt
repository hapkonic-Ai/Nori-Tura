package com.nonituracare.presentation.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.DocumentsRepository
import com.nonituracare.data.SurgeonRepository
import com.nonituracare.data.dto.ConsentFormDto
import com.nonituracare.data.dto.DocumentCreateRequest
import com.nonituracare.data.dto.DocumentDto
import com.nonituracare.data.dto.DoctorDto
import com.nonituracare.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentChildDetailViewModel(
    private val patientId: String,
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository(),
    private val documentsRepository: DocumentsRepository = DocumentsRepository()
) : ViewModel() {

    data class ChildProfile(
        val child: PatientDto,
        val doctor: DoctorDto?,
        val consentForms: List<ConsentFormDto>,
        val documents: List<DocumentDto>
    )

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val profile: ChildProfile) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val patientResult = surgeonRepository.getPatientDetail(token, patientId)
            val patient = patientResult.getOrNull()
            if (patient == null) {
                _uiState.value = UiState.Error(patientResult.exceptionOrNull()?.message ?: "Failed to load child profile")
                return@launch
            }

            val admissionsResult = surgeonRepository.getAdmissions(token)
            val consentForms = (admissionsResult.getOrNull() ?: emptyList())
                .filter { it.patientId == patientId }
                .flatMap { it.consentForms ?: emptyList() }
                .sortedByDescending { it.generatedAt }

            val doctorResult = patient.doctorId?.let { surgeonRepository.getDoctor(token, it) }
            val documentsResult = documentsRepository.getPatientDocuments(patientId)

            _uiState.value = UiState.Success(
                ChildProfile(
                    child = patient,
                    doctor = doctorResult?.getOrNull(),
                    consentForms = consentForms,
                    documents = documentsResult.getOrNull() ?: emptyList()
                )
            )
        }
    }

    fun createDocument(request: DocumentCreateRequest) {
        viewModelScope.launch {
            documentsRepository.createDocument(request).onSuccess {
                load()
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            documentsRepository.deleteDocument(documentId).onSuccess {
                load()
            }
        }
    }
}
