package com.nonituracare.presentation.medicalrecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.MedicalRecordDto
import com.nonituracare.data.MedicalRecordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalRecordsViewModel(
    private val patientId: String,
    private val repository: MedicalRecordsRepository = MedicalRecordsRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val records: List<MedicalRecordDto>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _allRecords = mutableListOf<MedicalRecordDto>()

    init {
        loadRecords()
    }

    fun loadRecords() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.listMedicalRecords(patientId)
                .onSuccess { records ->
                    _allRecords.clear()
                    _allRecords.addAll(records)
                    applyFilter()
                }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load records") }
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        applyFilter()
    }

    private fun applyFilter() {
        // MedicalRecordDto is a container (no category field); show all records
        // The UI handles visual filtering on the category chips via record images
        _uiState.value = UiState.Success(_allRecords.toList())
    }
}
