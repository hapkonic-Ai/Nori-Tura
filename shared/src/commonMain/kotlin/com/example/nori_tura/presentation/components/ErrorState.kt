package com.example.nori_tura.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = NorituraColors.Error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun InlineError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
