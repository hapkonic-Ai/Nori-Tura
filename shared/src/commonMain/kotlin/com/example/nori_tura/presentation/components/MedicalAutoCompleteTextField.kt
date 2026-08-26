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
private const val TOKEN_DELIMITERS = " \t\n,;:.!?()[]{}\"'`"

/**
 * A medical-domain autocomplete text field.
 *
 * - Debounces input by 300 ms.
 * - Queries the MedService `/autocomplete` endpoint with `semantic_expansion=false`.
 * - Shows the top suggestion as inline grey ghost text for the current word/token.
 * - Displays up to 5 suggestions in a dropdown anchored to the field.
 * - Replaces only the current word/token when a suggestion is selected and moves
 *   the cursor to the end of the field so the user can keep typing the sentence.
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

    val (tokenStart, tokenEnd, token) = remember(value) {
        val (start, end) = lastTokenBounds(value)
        Triple(start, end, value.substring(start, end).trim())
    }

    val topSuggestion = suggestions.firstOrNull()
    val ghostColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val visualTransformation = remember(tokenStart, tokenEnd, token, topSuggestion, ghostColor) {
        TokenGhostVisualTransformation(
            tokenStart = tokenStart,
            tokenEnd = tokenEnd,
            token = token,
            suggestion = topSuggestion,
            ghostColor = ghostColor
        )
    }

    LaunchedEffect(value) {
        if (ignoreNextQuery) {
            ignoreNextQuery = false
            return@LaunchedEffect
        }

        if (token.length < MIN_QUERY_LENGTH) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(DEBOUNCE_MS)

        repository.search(query = token, limit = SUGGESTION_LIMIT)
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
                                query = token
                            )
                        )
                    },
                    onClick = {
                        ignoreNextQuery = true
                        val newText = value.replaceRange(tokenStart, tokenEnd, suggestion)
                        onValueChange(newText)
                        suggestions = emptyList()
                    }
                )
            }
        }
    }
}

/**
 * Returns the start and end indices of the last word/token in [text].
 */
private fun lastTokenBounds(text: String): Pair<Int, Int> {
    if (text.isEmpty()) return 0 to 0

    var start = text.lastIndex
    while (start >= 0 && text[start] !in TOKEN_DELIMITERS) {
        start--
    }

    return (start + 1) to text.length
}

/**
 * Highlights the part of [suggestion] that matches the typed [query] token.
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
 * as grey ghost text after the current token.
 */
private class TokenGhostVisualTransformation(
    private val tokenStart: Int,
    private val tokenEnd: Int,
    private val token: String,
    private val suggestion: String?,
    private val ghostColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val ghost = if (token.isEmpty() || suggestion.isNullOrBlank()) {
            ""
        } else {
            val commonPrefix = suggestion.commonPrefixWith(token, ignoreCase = true)
            if (commonPrefix.length == token.length && suggestion.length > token.length) {
                suggestion.substring(token.length)
            } else {
                ""
            }
        }

        val safeTokenEnd = tokenEnd.coerceIn(0, text.length)
        val annotated = buildAnnotatedString {
            append(text.subSequence(0, safeTokenEnd))
            if (ghost.isNotEmpty()) {
                withStyle(style = SpanStyle(color = ghostColor)) {
                    append(ghost)
                }
            }
            append(text.subSequence(safeTokenEnd, text.length))
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= safeTokenEnd) offset else offset + ghost.length

            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= safeTokenEnd) offset else (offset - ghost.length).coerceAtLeast(safeTokenEnd)
        }

        return TransformedText(annotated, mapping)
    }
}
