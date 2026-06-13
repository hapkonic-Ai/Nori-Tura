package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.PatientDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientListViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val patients: List<PatientDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allPatients = emptyList<PatientDto>()

    init {
        loadPatients()
    }

    fun loadPatients() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            surgeonRepository.getPatients(token)
                .onSuccess { patients ->
                    allPatients = patients
                    applyFilter()
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load patients")
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                val nameMatch = patient.name?.lowercase()?.contains(query) ?: false
                val phoneMatch = patient.parentPhone?.contains(query) ?: false
                nameMatch || phoneMatch
            }
        }
        _uiState.value = UiState.Success(filtered)
    }
}
