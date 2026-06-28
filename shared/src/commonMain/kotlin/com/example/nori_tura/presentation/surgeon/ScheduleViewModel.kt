package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.ScheduleRepository
import com.example.nori_tura.data.dto.OpdBookingRequest
import com.example.nori_tura.data.dto.OtBookingRequest
import com.example.nori_tura.data.dto.PatientDto
import com.example.nori_tura.data.dto.ScheduleSlotDto
import com.example.nori_tura.util.addDaysToIsoDate
import com.example.nori_tura.util.getCurrentDateString
import com.example.nori_tura.util.weekContaining
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    enum class Tab { OT, OPD }

    data class BookingForm(
        val patientId: String = "",
        val time: String = "09:00",
        val procedure: String = "",
        val urgency: String = "routine",
        val visitType: String = "opd"
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val selectedDate: String,
            val weekDays: List<String>,
            val selectedTab: Tab,
            val slots: List<ScheduleSlotDto>,
            val showBookingDialog: Boolean = false,
            val bookingForm: BookingForm = BookingForm(),
            val patients: List<PatientDto> = emptyList()
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val today: String = getCurrentDateString()

    init {
        val week = weekContaining(today)
        _uiState.value = UiState.Success(
            selectedDate = today,
            weekDays = week,
            selectedTab = Tab.OT,
            slots = emptyList()
        )
        loadSlots()
    }

    fun selectDate(date: String) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(
            selectedDate = date,
            weekDays = weekContaining(date),
            slots = emptyList()
        )
        loadSlots()
    }

    fun selectTab(tab: Tab) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(selectedTab = tab, slots = emptyList())
        loadSlots()
    }

    fun previousWeek() {
        val current = _uiState.value as? UiState.Success ?: return
        selectDate(addDaysToIsoDate(current.selectedDate, -7))
    }

    fun nextWeek() {
        val current = _uiState.value as? UiState.Success ?: return
        selectDate(addDaysToIsoDate(current.selectedDate, 7))
    }

    fun loadSlots() {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(slots = emptyList())
        viewModelScope.launch {
            val result = when (current.selectedTab) {
                Tab.OT -> repository.getOtSlots(current.selectedDate)
                Tab.OPD -> repository.getOpdSlots(current.selectedDate)
            }
            result
                .onSuccess { slots ->
                    val updated = _uiState.value as? UiState.Success ?: return@onSuccess
                    _uiState.value = updated.copy(slots = slots)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load schedule")
                }
        }
    }

    fun showBookingDialog() {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(showBookingDialog = true)
    }

    fun dismissBookingDialog() {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(showBookingDialog = false)
    }

    fun updateBookingForm(form: BookingForm) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(bookingForm = form)
    }

    fun bookSlot(onSuccess: () -> Unit = {}) {
        val current = _uiState.value as? UiState.Success ?: return
        val form = current.bookingForm
        if (form.patientId.isBlank()) return

        viewModelScope.launch {
            val result = when (current.selectedTab) {
                Tab.OT -> repository.bookOtSlot(
                    OtBookingRequest(
                        patientId = form.patientId,
                        date = current.selectedDate,
                        time = form.time,
                        procedure = form.procedure,
                        urgency = form.urgency
                    )
                )
                Tab.OPD -> repository.bookOpdSlot(
                    OpdBookingRequest(
                        patientId = form.patientId,
                        date = current.selectedDate,
                        time = form.time,
                        visitType = form.visitType
                    )
                )
            }
            result
                .onSuccess {
                    val updated = _uiState.value as? UiState.Success ?: return@onSuccess
                    _uiState.value = updated.copy(
                        showBookingDialog = false,
                        bookingForm = BookingForm()
                    )
                    loadSlots()
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Booking failed")
                }
        }
    }

    fun setPatients(patients: List<PatientDto>) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(patients = patients)
    }
}

