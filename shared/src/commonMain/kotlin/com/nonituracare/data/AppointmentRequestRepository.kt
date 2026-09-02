package com.nonituracare.data

import com.nonituracare.data.dto.AppointmentConfirmBody
import com.nonituracare.data.dto.AppointmentConfirmResponse
import com.nonituracare.data.dto.AppointmentRequestBody
import com.nonituracare.data.dto.AppointmentRequestResponse
import com.nonituracare.data.dto.AvailableSlotsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AppointmentRequestRepository(
    private val client: HttpClient = ApiClient.client
) {
    suspend fun requestAppointment(doctorId: String, reason: String?, urgency: String): Result<AppointmentRequestResponse> = safeApiCall {
        client.post("/appointments/request") {
            contentType(ContentType.Application.Json)
            setBody(AppointmentRequestBody(doctor_id = doctorId, reason = reason, urgency = urgency))
        }.body()
    }

    suspend fun getAvailableSlots(doctorId: String, days: Int = 14): Result<AvailableSlotsResponse> = safeApiCall {
        client.get("/appointments/available-slots") {
            parameter("doctor_id", doctorId)
            parameter("days", days)
        }.body()
    }

    suspend fun confirmAppointment(appointmentId: String, slotDatetime: String, autoCreatePatient: Boolean, patientData: Map<String, String>?): Result<AppointmentConfirmResponse> = safeApiCall {
        client.post("/appointments/$appointmentId/confirm") {
            contentType(ContentType.Application.Json)
            setBody(AppointmentConfirmBody(slot_datetime = slotDatetime, auto_create_patient = autoCreatePatient, patient_data = patientData))
        }.body()
    }
}
