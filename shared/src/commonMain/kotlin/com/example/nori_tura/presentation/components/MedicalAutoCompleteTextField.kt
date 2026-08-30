package com.example.nori_tura.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.example.nori_tura.data.AutocompleteSelectionCache
import com.example.nori_tura.data.MedicalTermRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * - Records selections in a local per-field cache and boosts previously selected
 *   terms above server suggestions.
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
    fieldType: String? = null,
    repository: MedicalTermRepository = remember { MedicalTermRepository() },
    cache: AutocompleteSelectionCache = AutocompleteSelectionCache
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Keep internal state in sync when the parent resets/clears the field.
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var ignoreNextQuery by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val expanded = isFocused && suggestions.isNotEmpty() && !ignoreNextQuery

    val cursor = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
    val (tokenStart, tokenEnd, token) = remember(textFieldValue.text, cursor) {
        val (start, end) = tokenBounds(textFieldValue.text, cursor)
        Triple(start, end, textFieldValue.text.substring(start, end).trim())
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

    LaunchedEffect(textFieldValue.text, cursor) {
        if (ignoreNextQuery) {
            ignoreNextQuery = false
            return@LaunchedEffect
        }

        if (token.length < MIN_QUERY_LENGTH) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(DEBOUNCE_MS)

        repository.search(
            query = token,
            fieldTypes = fieldType ?: "all",
            limit = SUGGESTION_LIMIT
        )
            .onSuccess { terms ->
                val cachedTerms = if (!fieldType.isNullOrBlank()) {
                    cache.getSuggestions(token, fieldType, SUGGESTION_LIMIT)
                } else {
                    emptyList()
                }
                suggestions = mergeSuggestions(cachedTerms, terms, SUGGESTION_LIMIT)
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
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onValueChange(it.text)
                suggestions = emptyList()
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryEditable),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = textFieldValue.text,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    label = label,
                    trailingIcon = {
                        if (suggestions.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = OutlinedTextFieldDefaults.colors(),
                            shape = OutlinedTextFieldDefaults.shape
                        )
                    }
                )
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
                        val currentText = textFieldValue.text
                        val newText = currentText.replaceRange(tokenStart, tokenEnd, suggestion)
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newText.length)
                        )
                        onValueChange(newText)
                        suggestions = emptyList()
                        scope.launch {
                            cache.recordSelection(suggestion, fieldType)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Returns the start and end indices of the word/token around the cursor.
 */
private fun tokenBounds(text: String, cursor: Int): Pair<Int, Int> {
    if (cursor <= 0 || text.isEmpty()) return 0 to 0

    var start = (cursor - 1).coerceAtMost(text.lastIndex)
    while (start >= 0 && text[start] !in TOKEN_DELIMITERS) {
        start--
    }

    return (start + 1) to cursor.coerceAtMost(text.length)
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
 * Merge cached and server suggestions, keeping cached terms first and
 * de-duplicating by case-insensitive value.  The result is capped at [limit].
 */
private fun mergeSuggestions(
    cached: List<String>,
    server: List<String>,
    limit: Int
): List<String> {
    val merged = mutableListOf<String>()
    cached.forEach { if (merged.none { existing -> existing.equals(it, ignoreCase = true) }) merged.add(it) }
    server.forEach { if (merged.none { existing -> existing.equals(it, ignoreCase = true) }) merged.add(it) }
    return merged.take(limit)
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
