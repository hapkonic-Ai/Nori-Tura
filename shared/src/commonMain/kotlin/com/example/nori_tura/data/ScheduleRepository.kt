package com.example.nori_tura.data

import com.example.nori_tura.data.dto.OpdBookingRequest
import com.example.nori_tura.data.dto.OtBookingRequest
import com.example.nori_tura.data.dto.ScheduleSlotDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ScheduleRepository(
    private val client: HttpClient = ApiClient.client
) {

    suspend fun getOtSlots(date: String): Result<List<ScheduleSlotDto>> = safeApiCall {
        client.get("/schedule/ot?target_date=$date").body()
    }

    suspend fun getOpdSlots(date: String): Result<List<ScheduleSlotDto>> = safeApiCall {
        client.get("/schedule/opd?target_date=$date").body()
    }

    suspend fun bookOtSlot(request: OtBookingRequest): Result<ScheduleSlotDto> = safeApiCall {
        client.post("/schedule/ot") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun bookOpdSlot(request: OpdBookingRequest): Result<ScheduleSlotDto> = safeApiCall {
        client.post("/schedule/opd") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
