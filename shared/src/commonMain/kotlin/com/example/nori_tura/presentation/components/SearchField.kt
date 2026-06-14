package com.example.nori_tura.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = NorituraColors.TextTertiary
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = NorituraColors.TextTertiary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Search
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = NorituraColors.Surface,
            unfocusedContainerColor = NorituraColors.Surface,
            focusedBorderColor = NorituraColors.Outline,
            unfocusedBorderColor = NorituraColors.Outline,
            focusedTextColor = NorituraColors.TextPrimary,
            unfocusedTextColor = NorituraColors.TextPrimary,
            focusedPlaceholderColor = NorituraColors.TextTertiary,
            unfocusedPlaceholderColor = NorituraColors.TextTertiary,
            focusedLeadingIconColor = NorituraColors.TextTertiary,
            unfocusedLeadingIconColor = NorituraColors.TextTertiary,
            cursorColor = NorituraColors.PrimaryBlue
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone Number",
    placeholder: String = "+91 98765 43210"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = NorituraColors.PrimaryBlue,
            unfocusedBorderColor = NorituraColors.Outline,
            focusedTextColor = NorituraColors.TextPrimary,
            unfocusedTextColor = NorituraColors.TextPrimary,
            focusedLabelColor = NorituraColors.PrimaryBlue,
            unfocusedLabelColor = NorituraColors.TextSecondary,
            cursorColor = NorituraColors.PrimaryBlue
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}
