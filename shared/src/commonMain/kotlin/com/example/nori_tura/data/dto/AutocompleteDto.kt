package com.example.nori_tura.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AutocompleteRequest(
    val query: String,
    val field_types: JsonElement? = null,
    val limit: Int = 5,
    val fuzzy: Boolean = true,
    val semantic_expansion: Boolean = false
)

@Serializable
data class AutocompleteResponse(
    val query: String = "",
    val field_types: JsonElement? = null,
    val results: List<AutocompleteResult> = emptyList(),
    val latency_ms: Double = 0.0,
    val cached: Boolean = false
)

@Serializable
data class AutocompleteResult(
    val term: String = "",
    val cui: String? = null,
    val tuis: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val match_type: String = "",
    val score: Double = 0.0
)
