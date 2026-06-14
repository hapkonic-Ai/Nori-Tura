package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun SurgeonFollowUpsTab(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Follow-ups - Coming Soon",
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
