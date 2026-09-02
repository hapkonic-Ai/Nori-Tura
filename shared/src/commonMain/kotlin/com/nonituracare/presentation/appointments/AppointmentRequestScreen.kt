package com.nonituracare.presentation.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.SectionTitle
import com.nonituracare.ui.theme.NorituraColors

private val URGENCY_OPTIONS = listOf("routine", "urgent", "emergency")

@Composable
fun AppointmentRequestScreen(
    doctorId: String,
    doctorName: String,
    viewModel: AppointmentViewModel,
    onBack: () -> Unit,
    onRequested: (hospitalContact: String?) -> Unit
) {
    val requestState by viewModel.requestState.collectAsState()

    var reason by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("routine") }

    val isLoading = requestState is AppointmentViewModel.RequestState.Loading

    LaunchedEffect(requestState) {
        if (requestState is AppointmentViewModel.RequestState.Success) {
            val success = requestState as AppointmentViewModel.RequestState.Success
            onRequested(success.response.hospital_contact)
        }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                title = "Request Appointment",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle("Your Surgeon")

            Text(
                text = doctorName,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            SectionTitle("Reason for Visit (optional)")

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Describe the reason for your visit...",
                        color = NorituraColors.TextTertiary
                    )
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NorituraColors.PrimaryBlue,
                    unfocusedBorderColor = NorituraColors.Outline,
                    focusedTextColor = NorituraColors.TextPrimary,
                    unfocusedTextColor = NorituraColors.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            SectionTitle("Urgency")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                URGENCY_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = urgency == option,
                        onClick = { urgency = option },
                        label = {
                            Text(
                                text = option.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NorituraColors.PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = NorituraColors.SurfaceVariant,
                            labelColor = NorituraColors.TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.requestAppointment(
                        doctorId = doctorId,
                        reason = reason.ifBlank { null },
                        urgency = urgency
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NorituraColors.PrimaryBlue,
                    disabledContainerColor = NorituraColors.PrimaryBlue.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Submit Request",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NorituraColors.PrimaryBlue)
                }
            }

            if (requestState is AppointmentViewModel.RequestState.Error) {
                val error = requestState as AppointmentViewModel.RequestState.Error
                Text(
                    text = error.message,
                    color = NorituraColors.Error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
