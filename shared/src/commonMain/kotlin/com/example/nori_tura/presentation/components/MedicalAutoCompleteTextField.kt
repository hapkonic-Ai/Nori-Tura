package com.example.nori_tura.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.example.nori_tura.data.MedicalTermRepository
import kotlinx.coroutines.delay

private const val DEBOUNCE_MS = 300L
private const val MIN_QUERY_LENGTH = 2
private const val SUGGESTION_LIMIT = 5

/**
 * A medical-domain autocomplete text field.
 *
 * - Debounces input by 300 ms.
 * - Queries the MedService `/autocomplete` endpoint with `semantic_expansion=false`.
 * - Shows the top suggestion as inline grey ghost text.
 * - Displays up to 5 suggestions in a dropdown anchored to the field.
 * - Replaces the full field value when a suggestion is selected.
 * - Fails silently on network/rate-limit errors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalAutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    repository: MedicalTermRepository = remember { MedicalTermRepository() }
) {
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var ignoreNextQuery by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val expanded = isFocused && suggestions.isNotEmpty() && !ignoreNextQuery

    val topSuggestion = suggestions.firstOrNull()
    val ghostColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val visualTransformation = remember(value, topSuggestion, ghostColor) {
        MedicalAutoCompleteVisualTransformation(
            query = value,
            suggestion = topSuggestion,
            ghostColor = ghostColor
        )
    }

    LaunchedEffect(value) {
        if (ignoreNextQuery) {
            ignoreNextQuery = false
            return@LaunchedEffect
        }

        val trimmed = value.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(DEBOUNCE_MS)

        repository.search(query = trimmed, limit = SUGGESTION_LIMIT)
            .onSuccess { terms ->
                suggestions = terms
            }
            .onFailure {
                suggestions = emptyList()
            }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { /* controlled by focus + suggestions */ },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                suggestions = emptyList()
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryEditable),
            label = label,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            trailingIcon = {
                if (suggestions.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { suggestions = emptyList() }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = highlightedSuggestion(
                                suggestion = suggestion,
                                query = value
                            )
                        )
                    },
                    onClick = {
                        ignoreNextQuery = true
                        onValueChange(suggestion)
                        suggestions = emptyList()
                    }
                )
            }
        }
    }
}

/**
 * Highlights the part of [suggestion] that matches the typed [query].
 */
@Composable
private fun highlightedSuggestion(suggestion: String, query: String): AnnotatedString {
    val prefixLength = suggestion.commonPrefixWith(query, ignoreCase = true).length
    val prefix = suggestion.take(prefixLength)
    val suffix = suggestion.drop(prefixLength)

    return buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(prefix)
        }
        append(suffix)
    }
}

/**
 * Visual transformation that renders the untyped portion of the top suggestion
 * as grey ghost text after the user's input.
 */
private class MedicalAutoCompleteVisualTransformation(
    private val query: String,
    private val suggestion: String?,
    private val ghostColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val ghost = if (suggestion.isNullOrBlank()) {
            ""
        } else {
            val commonPrefix = suggestion.commonPrefixWith(query, ignoreCase = true)
            if (commonPrefix.length == query.length && suggestion.length > query.length) {
                suggestion.substring(query.length)
            } else {
                ""
            }
        }

        val annotated = buildAnnotatedString {
            append(text)
            if (ghost.isNotEmpty()) {
                withStyle(style = SpanStyle(color = ghostColor)) {
                    append(ghost)
                }
            }
        }

        val typedLength = text.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset.coerceIn(0, typedLength)

            override fun transformedToOriginal(offset: Int): Int =
                offset.coerceIn(0, typedLength)
        }

        return TransformedText(annotated, mapping)
    }
}
