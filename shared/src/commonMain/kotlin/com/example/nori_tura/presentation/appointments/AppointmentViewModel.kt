package com.example.nori_tura.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.AppointmentRequestRepository
import com.example.nori_tura.data.dto.AppointmentConfirmResponse
import com.example.nori_tura.data.dto.AppointmentRequestResponse
import com.example.nori_tura.data.dto.AvailableSlotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentViewModel(
    private val repository: AppointmentRequestRepository = AppointmentRequestRepository()
) : ViewModel() {

    sealed class RequestState {
        object Idle : RequestState()
        object Loading : RequestState()
        data class Success(val response: AppointmentRequestResponse) : RequestState()
        data class Error(val message: String) : RequestState()
    }

    sealed class ConfirmState {
        object Idle : ConfirmState()
        object Loading : ConfirmState()
        data class Success(val response: AppointmentConfirmResponse) : ConfirmState()
        data class Error(val message: String) : ConfirmState()
    }

    private val _requestState = MutableStateFlow<RequestState>(RequestState.Idle)
    val requestState: StateFlow<RequestState> = _requestState.asStateFlow()

    private val _confirmState = MutableStateFlow<ConfirmState>(ConfirmState.Idle)
    val confirmState: StateFlow<ConfirmState> = _confirmState.asStateFlow()

    private val _selectedSlot = MutableStateFlow<AvailableSlotDto?>(null)
    val selectedSlot: StateFlow<AvailableSlotDto?> = _selectedSlot.asStateFlow()

    fun requestAppointment(doctorId: String, reason: String?, urgency: String) {
        _requestState.value = RequestState.Loading
        viewModelScope.launch {
            repository.requestAppointment(doctorId, reason, urgency)
                .onSuccess { _requestState.value = RequestState.Success(it) }
                .onFailure { _requestState.value = RequestState.Error(it.message ?: "Failed") }
        }
    }

    fun selectSlot(slot: AvailableSlotDto) {
        _selectedSlot.value = slot
    }

    fun confirmAppointment(appointmentId: String, autoCreatePatient: Boolean, patientData: Map<String, String>?) {
        val slot = _selectedSlot.value ?: run {
            _confirmState.value = ConfirmState.Error("No slot selected")
            return
        }
        _confirmState.value = ConfirmState.Loading
        viewModelScope.launch {
            repository.confirmAppointment(appointmentId, slot.slot_datetime, autoCreatePatient, patientData)
                .onSuccess { _confirmState.value = ConfirmState.Success(it) }
                .onFailure { _confirmState.value = ConfirmState.Error(it.message ?: "Failed") }
        }
    }

    fun reset() {
        _requestState.value = RequestState.Idle
        _confirmState.value = ConfirmState.Idle
        _selectedSlot.value = null
    }
}
