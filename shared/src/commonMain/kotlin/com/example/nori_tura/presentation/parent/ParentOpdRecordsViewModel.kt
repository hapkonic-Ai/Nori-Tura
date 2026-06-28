package com.example.nori_tura.presentation.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.OpdRecordDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentOpdRecordsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val surgeonRepository: SurgeonRepository = SurgeonRepository()
) : ViewModel() {

    enum class Filter { ALL, SURGERY_RECOMMENDED, FOLLOW_UP, ROUTINE }

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val records: List<OpdRecordDto>,
            val filter: Filter = Filter.ALL
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        val token = authRepository.getToken() ?: run {
            _uiState.value = UiState.Error("Not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val patientsResult = surgeonRepository.getPatients(token)
            if (patientsResult.isFailure) {
                _uiState.value = UiState.Error("Failed to load children")
                return@launch
            }

            val patients = patientsResult.getOrNull() ?: emptyList()
            val allRecords = mutableListOf<OpdRecordDto>()
            for (patient in patients) {
                val id = patient.id ?: continue
                surgeonRepository.getOpdRecords(token, id)
                    .onSuccess { records ->
                        allRecords.addAll(records.map { it.copy(patient = patient) })
                    }
            }
            allRecords.sortByDescending { it.createdAt }
            _uiState.value = UiState.Success(records = allRecords)
        }
    }

    fun setFilter(filter: Filter) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(filter = filter)
    }
}

fun OpdRecordDto.matchesFilter(filter: ParentOpdRecordsViewModel.Filter): Boolean {
    return when (filter) {
        ParentOpdRecordsViewModel.Filter.ALL -> true
        ParentOpdRecordsViewModel.Filter.SURGERY_RECOMMENDED ->
            surgicalDecision?.lowercase()?.contains("recommended") == true ||
                    surgicalDecision?.lowercase()?.contains("surgery") == true
        ParentOpdRecordsViewModel.Filter.FOLLOW_UP ->
            !followUpDate.isNullOrBlank()
        ParentOpdRecordsViewModel.Filter.ROUTINE ->
            surgicalDecision.isNullOrBlank() && followUpDate.isNullOrBlank()
    }
}
