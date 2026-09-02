package com.nonituracare.data

import kotlinx.serialization.Serializable

/**
 * A single autocomplete selection recorded for a specific field type.
 *
 * @param term The exact term the user selected from the dropdown.
 * @param fieldType Normalised field type (e.g. "procedure", "diagnosis", "all").
 * @param selectedAtMillis Epoch millis of the most recent selection.
 * @param count Total number of times this term has been selected for this field.
 */
@Serializable
data class AutocompleteSelection(
    val term: String,
    val fieldType: String,
    val selectedAtMillis: Long,
    val count: Int
)
