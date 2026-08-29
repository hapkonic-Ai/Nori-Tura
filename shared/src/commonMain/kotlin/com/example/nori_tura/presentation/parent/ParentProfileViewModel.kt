package com.example.nori_tura.presentation.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.DocumentsRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.DocumentCreateRequest
import com.example.nori_tura.data.dto.DocumentDto
import com.example.nori_tura.data.dto.DoctorDto
import com.example.nori_tura.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository(),
    private val documentsRepository: DocumentsRepository = DocumentsRepository()
) : ViewModel() {

    data class Profile(
        val child: PatientDto? = null,
        val doctor: DoctorDto? = null,
        val consentForms: List<ConsentFormDto> = emptyList(),
        val documents: List<DocumentDto> = emptyList()
    )

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val profile: Profile) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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
            val patientsResult = surgeonRepository.getPatients(token)
            val admissionsResult = surgeonRepository.getAdmissions(token)

            if (patientsResult.isFailure || admissionsResult.isFailure) {
                _uiState.value = UiState.Error("Failed to load profile")
                return@launch
            }

            val child = patientsResult.getOrNull()?.firstOrNull()
            val admissions = admissionsResult.getOrNull() ?: emptyList()
            val consentForms = admissions
                .flatMap { it.consentForms ?: emptyList() }
                .sortedByDescending { it.generatedAt }

            val doctorResult = child?.doctorId?.let {
                surgeonRepository.getDoctor(token, it)
            }

            val documentsResult = child?.id?.let {
                documentsRepository.getPatientDocuments(it)
            }

            _uiState.value = UiState.Success(
                Profile(
                    child = child,
                    doctor = doctorResult?.getOrNull(),
                    consentForms = consentForms,
                    documents = documentsResult?.getOrNull() ?: emptyList()
                )
            )
        }
    }

    fun createDocument(request: DocumentCreateRequest) {
        viewModelScope.launch {
            documentsRepository.createDocument(request).onSuccess {
                loadProfile()
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            documentsRepository.deleteDocument(documentId).onSuccess {
                loadProfile()
            }
        }
    }
}
