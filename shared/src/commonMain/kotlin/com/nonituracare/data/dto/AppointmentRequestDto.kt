package com.nonituracare.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvailableSlotDto(
    val slot_id: String,
    val slot_datetime: String,  // ISO string
    val surgeon_name: String,
    val is_available: Boolean
)

@Serializable
data class AppointmentRequestBody(
    val doctor_id: String,
    val reason: String? = null,
    val urgency: String = "routine"
)

@Serializable
data class AppointmentRequestResponse(
    val id: String,
    val doctor_id: String,
    val appointment_type: String,
    val urgency: String? = null,
    val status: String,
    val notes: String? = null,
    val hospital_contact: String? = null,
    val available_slots: List<AvailableSlotDto> = emptyList()
)

@Serializable
data class AppointmentConfirmBody(
    val slot_datetime: String,  // ISO string
    val auto_create_patient: Boolean = false,
    val patient_data: Map<String, String>? = null
)

@Serializable
data class AppointmentConfirmResponse(
    val id: String,
    val patient_id: String? = null,
    val doctor_id: String,
    val slot_datetime: String,
    val is_confirmed: Boolean,
    val status: String,
    val auto_created_patient: Boolean = false
)

@Serializable
data class AvailableSlotsResponse(
    val slots: List<AvailableSlotDto>
)
