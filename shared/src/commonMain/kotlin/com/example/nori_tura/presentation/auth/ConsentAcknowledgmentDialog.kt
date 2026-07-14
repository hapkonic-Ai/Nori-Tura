package com.example.nori_tura.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

private val CONSENT_TEXT = """
NONI TURA SURGICAL CARE PLATFORM
Terms & Consent Agreement

By using this platform, you acknowledge:

1. MEDICAL INFORMATION STORAGE
Your medical information will be stored securely in compliance with Indian healthcare regulations.

2. DATA SCOPE
Your data is accessible only to your treating surgeon and assigned nursing staff.

3. ELECTRONIC RECORDS
You consent to electronic storage of medical records including OPD notes, surgical records, and imaging studies.

4. DATA DELETION
You can request data deletion at any time by contacting your surgeon or administrator.

5. PRIVACY COMMITMENT
Your data will not be shared with third parties without explicit consent, except as required by law.

Version: v1.0
Last Updated: 2026-07-14
""".trimIndent()

@Composable
fun ConsentAcknowledgmentDialog(
    onAcknowledged: suspend () -> Unit,
    onDismiss: () -> Unit = {}
) {
    var acknowledged by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Surface.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    Dialog(
        onDismissRequest = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = NorituraColors.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NorituraColors.PrimaryBlue)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Terms & Consent",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NorituraColors.Surface
                    )
                }

                // Scrollable consent text
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            CONSENT_TEXT,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acknowledged,
                        onCheckedChange = { acknowledged = it }
                    )
                    Text(
                        "I have read and understand the terms & consent",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Error message
                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Exit")
                    }

                    Button(
                        onClick = {
                            if (acknowledged) {
                                isLoading = true
                                // Note: In a real app, you would call the API here
                                // For now, just proceed
                                try {
                                    onAcknowledged()
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to acknowledge consent"
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = acknowledged && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NorituraColors.PrimaryBlue
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NorituraColors.Surface
                            )
                        } else {
                            Text("Continue")
                        }
                    }
                }
            }
        }
    }
}
