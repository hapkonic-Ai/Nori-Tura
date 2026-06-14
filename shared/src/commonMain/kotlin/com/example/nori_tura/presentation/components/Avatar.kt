package com.example.nori_tura.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = NorituraColors.PrimaryBlueLight,
    contentColor: Color = NorituraColors.PrimaryBlue
) {
    val initials = name.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}
