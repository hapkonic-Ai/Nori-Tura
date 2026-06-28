package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.FollowUpRepository
import com.example.nori_tura.data.dto.OpdRecordDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FollowUpsViewModel(
    private val repository: FollowUpRepository = FollowUpRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val records: List<OpdRecordDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadFollowUps()
    }

    fun loadFollowUps(date: String? = null) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listFollowUps(date)
                .onSuccess { records ->
                    _uiState.value = UiState.Success(records)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load follow-ups")
                }
        }
    }
}
