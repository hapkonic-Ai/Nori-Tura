package com.nonituracare.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.FollowUpRepository
import com.nonituracare.data.dto.OpdRecordDto
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

    // Tracks in-flight "mark attended" calls per record so the card can show
    // a spinner on just that row instead of reloading the whole screen.
    private val _markingIds = MutableStateFlow<Set<String>>(emptySet())
    val markingIds: StateFlow<Set<String>> = _markingIds.asStateFlow()

    init {
        loadFollowUps()
    }

    fun loadFollowUps() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listFollowUps()
                .onSuccess { records ->
                    _uiState.value = UiState.Success(records)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load follow-ups")
                }
        }
    }

    fun markAttended(recordId: String) {
        val current = (_uiState.value as? UiState.Success)?.records ?: return
        _markingIds.value = _markingIds.value + recordId
        viewModelScope.launch {
            repository.markAttended(recordId)
                .onSuccess {
                    _uiState.value = UiState.Success(current.filter { it.id != recordId })
                }
                .onFailure { /* leave the card as-is; the spinner clears below and the button is tappable again */ }
            _markingIds.value = _markingIds.value - recordId
        }
    }
}
