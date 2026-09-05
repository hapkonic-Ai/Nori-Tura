package com.nonituracare.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AdminRepository
import com.nonituracare.data.dto.OtNoteTemplateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminOtNoteTemplatesViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val templates: List<OtNoteTemplateDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listOtNoteTemplates()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load OT note templates") }
        }
    }
}
