package com.example.nori_tura.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.IpdRepository
import com.example.nori_tura.data.SurgicalTemplateRepository
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.DischargeSummaryCreateRequest
import com.example.nori_tura.data.dto.IntraOpNoteCreateRequest
import com.example.nori_tura.data.dto.PostOpNoteCreateRequest
import com.example.nori_tura.data.dto.PreOpNoteCreateRequest
import com.example.nori_tura.data.dto.SurgicalTemplateDto
import com.example.nori_tura.data.dto.WardRoundNoteCreateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdmissionDetailViewModel(
    private val admissionId: String,
    private val repository: IpdRepository = IpdRepository(),
    private val templateRepository: SurgicalTemplateRepository = SurgicalTemplateRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _templates = MutableStateFlow<List<SurgicalTemplateDto>>(emptyList())
    val templates: StateFlow<List<SurgicalTemplateDto>> = _templates.asStateFlow()

    init {
        loadAdmission()
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            templateRepository.listTemplates()
                .onSuccess { _templates.value = it }
                .onFailure { _templates.value = emptyList() }
        }
    }

    fun loadAdmission() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getAdmission(admissionId)
                .onSuccess { admission ->
                    _uiState.value = UiState.Success(admission)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load admission")
                }
        }
    }

    fun createPreOpNote(request: PreOpNoteCreateRequest) {
        viewModelScope.launch {
            repository.createPreOpNote(admissionId, request)
                .onSuccess { loadAdmission() }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to save pre-op note")
                }
        }
    }

    fun createIntraOpNote(request: IntraOpNoteCreateRequest) {
        viewModelScope.launch {
            repository.createIntraOpNote(admissionId, request)
                .onSuccess { loadAdmission() }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to save intra-op note")
                }
        }
    }

    fun createPostOpNote(request: PostOpNoteCreateRequest) {
        viewModelScope.launch {
            repository.createPostOpNote(admissionId, request)
                .onSuccess { loadAdmission() }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to save post-op note")
                }
        }
    }

    fun createWardRoundNote(request: WardRoundNoteCreateRequest) {
        viewModelScope.launch {
            repository.createWardRoundNote(admissionId, request)
                .onSuccess { loadAdmission() }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to save ward round note")
                }
        }
    }

    fun createDischargeSummary(request: DischargeSummaryCreateRequest) {
        viewModelScope.launch {
            repository.createDischargeSummary(admissionId, request)
                .onSuccess { loadAdmission() }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to discharge patient")
                }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val admission: AdmissionDto) : UiState()
        data class Error(val message: String) : UiState()
    }
}
