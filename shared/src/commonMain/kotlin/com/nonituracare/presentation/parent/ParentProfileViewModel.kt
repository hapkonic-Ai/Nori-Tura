package com.nonituracare.presentation.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.SurgeonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    data class Profile(
        val parentName: String?,
        val parentPhone: String?,
        val childrenCount: Int
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
            surgeonRepository.getPatients(token)
                .onSuccess { patients ->
                    val first = patients.firstOrNull()
                    _uiState.value = UiState.Success(
                        Profile(
                            parentName = first?.parentName,
                            parentPhone = first?.parentPhone,
                            childrenCount = patients.size
                        )
                    )
                }
                .onFailure {
                    _uiState.value = UiState.Error("Failed to load profile")
                }
        }
    }
}
