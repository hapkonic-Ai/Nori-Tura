package com.example.nori_tura.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WhatsAppPreviewDto(
    val phone: String,
    val body: String,
    @SerialName("can_send_whatsapp") val canSendWhatsApp: Boolean,
    @SerialName("can_send_sms") val canSendSms: Boolean
)

@Serializable
data class SendMessageRequest(
    val channel: String,
    val message: String? = null
)

@Serializable
data class SendMessageResponse(
    val status: String,
    val channel: String,
    @SerialName("message_body") val messageBody: String
)
