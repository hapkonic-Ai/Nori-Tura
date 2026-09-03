package com.nonituracare.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.SurgicalTemplateRepository
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateCreateRequest
import com.nonituracare.data.dto.SurgicalTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SurgicalTemplatesViewModel(
    private val repository: SurgicalTemplateRepository = SurgicalTemplateRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Global, admin-curated templates a doctor can start a new personal template from.
    private val _contentTemplates = MutableStateFlow<List<ContentTemplateDto>>(emptyList())
    val contentTemplates: StateFlow<List<ContentTemplateDto>> = _contentTemplates.asStateFlow()

    init {
        loadTemplates()
        loadContentTemplates()
    }

    fun loadTemplates() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listTemplates()
                .onSuccess { templates ->
                    _uiState.value = UiState.Success(templates)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load templates")
                }
        }
    }

    private fun loadContentTemplates() {
        viewModelScope.launch {
            repository.getContentTemplates()
                .onSuccess { _contentTemplates.value = it }
                .onFailure { /* optional — picker just won't offer a global option */ }
        }
    }

    fun createTemplate(request: SurgicalTemplateCreateRequest) {
        val current = (_uiState.value as? UiState.Success)?.templates ?: emptyList()
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.createTemplate(request)
                .onSuccess { template ->
                    _uiState.value = UiState.Success(current + template)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to create template")
                }
        }
    }

    fun updateTemplate(id: String, request: SurgicalTemplateUpdateRequest) {
        val current = (_uiState.value as? UiState.Success)?.templates ?: emptyList()
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.updateTemplate(id, request)
                .onSuccess { updated ->
                    _uiState.value = UiState.Success(current.map { if (it.id == id) updated else it })
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to update template")
                }
        }
    }

    fun deleteTemplate(id: String) {
        val current = (_uiState.value as? UiState.Success)?.templates ?: emptyList()
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.deleteTemplate(id)
                .onSuccess {
                    _uiState.value = UiState.Success(current.filter { it.id != id })
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to delete template")
                }
        }
    }

    fun resetError() {
        if (_uiState.value is UiState.Error) {
            loadTemplates()
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val templates: List<SurgicalTemplateDto>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
