package com.example.nori_tura.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = NorituraColors.PrimaryBlue,
    showDot: Boolean = true,
    containerColor: Color? = null
) {
    val bg = containerColor ?: color.copy(alpha = 0.12f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun StatusIndicator(
    modifier: Modifier = Modifier,
    color: Color = NorituraColors.AccentGreen,
    size: androidx.compose.ui.unit.Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
