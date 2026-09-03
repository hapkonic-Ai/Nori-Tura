package com.nonituracare.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AdminRepository
import com.nonituracare.data.dto.AdminSurgicalTemplateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminSurgicalTemplatesViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val templates: List<AdminSurgicalTemplateDto>) : UiState()
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
            repository.listSurgicalTemplates()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load surgical templates") }
        }
    }
}
