package com.example.nori_tura.presentation.medicalrecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.MedicalRecordDetailDto
import com.example.nori_tura.data.MedicalRecordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalRecordDetailViewModel(
    private val recordId: String,
    private val repository: MedicalRecordsRepository = MedicalRecordsRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val record: MedicalRecordDetailDto) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getMedicalRecordDetail(recordId)
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load record") }
        }
    }
}
