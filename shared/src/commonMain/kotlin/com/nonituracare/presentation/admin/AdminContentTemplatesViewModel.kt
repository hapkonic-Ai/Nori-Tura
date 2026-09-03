package com.nonituracare.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AdminRepository
import com.nonituracare.data.dto.ContentTemplateCreateRequest
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.ContentTemplateUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminContentTemplatesViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val templates: List<ContentTemplateDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class SaveState {
        data object Idle : SaveState()
        data object Loading : SaveState()
        data object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listContentTemplates()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load content templates") }
        }
    }

    fun createTemplate(request: ContentTemplateCreateRequest) {
        _saveState.value = SaveState.Loading
        viewModelScope.launch {
            repository.createContentTemplate(request)
                .onSuccess {
                    _saveState.value = SaveState.Success
                    load()
                }
                .onFailure { error ->
                    _saveState.value = SaveState.Error(error.message ?: "Failed to create template")
                }
        }
    }

    fun updateTemplate(id: String, request: ContentTemplateUpdateRequest) {
        _saveState.value = SaveState.Loading
        viewModelScope.launch {
            repository.updateContentTemplate(id, request)
                .onSuccess {
                    _saveState.value = SaveState.Success
                    load()
                }
                .onFailure { error ->
                    _saveState.value = SaveState.Error(error.message ?: "Failed to update template")
                }
        }
    }

    fun toggleActive(template: ContentTemplateDto) {
        viewModelScope.launch {
            repository.updateContentTemplate(
                template.id,
                ContentTemplateUpdateRequest(isActive = !template.isActive)
            ).onSuccess { load() }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteContentTemplate(id).onSuccess { load() }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
