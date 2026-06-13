package com.example.nori_tura.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MedicationCreateDto(
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String
)
