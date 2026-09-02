package com.nonituracare.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AlertsRepository
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.dto.AlertsResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val alertsRepository: AlertsRepository = AlertsRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val alerts: AlertsResponseDto) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            alertsRepository.getAlerts()
                .onSuccess { alerts ->
                    _uiState.value = UiState.Success(alerts)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load alerts")
                }
        }
    }
}
