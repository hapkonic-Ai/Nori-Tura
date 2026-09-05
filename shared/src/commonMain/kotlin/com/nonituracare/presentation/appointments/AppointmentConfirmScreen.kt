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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.SectionTitle
import com.nonituracare.ui.theme.NorituraColors

private val GENDER_OPTIONS = listOf("Male", "Female", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentConfirmScreen(
    appointmentId: String,
    viewModel: AppointmentViewModel,
    onBack: () -> Unit,
    onConfirmed: () -> Unit
) {
    val confirmState by viewModel.confirmState.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()
    val requestState by viewModel.requestState.collectAsState()

    var autoCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var genderExpanded by remember { mutableStateOf(false) }

    val isLoading = confirmState is AppointmentViewModel.ConfirmState.Loading

    // Extract display info from selected slot
    val slotDate = selectedSlot?.slot_datetime?.take(10) ?: "-"
    val slotTime = selectedSlot?.slot_datetime?.let {
        if (it.length >= 16) it.substring(11, 16) else it
    } ?: "-"
    val surgeonName = selectedSlot?.surgeon_name ?: "-"

    // Derive urgency from requestState if available
    val urgencyLabel = (requestState as? AppointmentViewModel.RequestState.Success)
        ?.response?.urgency?.replaceFirstChar { it.uppercase() } ?: "Routine"

    LaunchedEffect(confirmState) {
        if (confirmState is AppointmentViewModel.ConfirmState.Success) {
            onConfirmed()
        }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                title = "Confirm Appointment",
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

            // Appointment summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Appointment Summary",
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    HorizontalDivider(color = NorituraColors.Divider)

                    AppointmentDetailRow(label = "Doctor", value = surgeonName)
                    AppointmentDetailRow(label = "Date", value = slotDate)
                    AppointmentDetailRow(label = "Time", value = slotTime)
                    AppointmentDetailRow(label = "Urgency", value = urgencyLabel)
                }
            }

            SectionTitle("Is this a new patient?")

            // Auto-create patient toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-create patient record",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = autoCreate,
                    onCheckedChange = { autoCreate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NorituraColors.Surface,
                        checkedTrackColor = NorituraColors.PrimaryBlue,
                        uncheckedThumbColor = NorituraColors.TextTertiary,
                        uncheckedTrackColor = NorituraColors.SurfaceVariant
                    )
                )
            }

            // Patient data fields shown when autoCreate is enabled
            if (autoCreate) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Patient Details",
                            color = NorituraColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NorituraColors.PrimaryBlue,
                                unfocusedBorderColor = NorituraColors.Outline,
                                focusedTextColor = NorituraColors.TextPrimary,
                                unfocusedTextColor = NorituraColors.TextPrimary,
                                focusedLabelColor = NorituraColors.PrimaryBlue,
                                unfocusedLabelColor = NorituraColors.TextTertiary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { c -> c.isDigit() } },
                            label = { Text("Age *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NorituraColors.PrimaryBlue,
                                unfocusedBorderColor = NorituraColors.Outline,
                                focusedTextColor = NorituraColors.TextPrimary,
                                unfocusedTextColor = NorituraColors.TextPrimary,
                                focusedLabelColor = NorituraColors.PrimaryBlue,
                                unfocusedLabelColor = NorituraColors.TextTertiary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NorituraColors.PrimaryBlue,
                                    unfocusedBorderColor = NorituraColors.Outline,
                                    focusedTextColor = NorituraColors.TextPrimary,
                                    unfocusedTextColor = NorituraColors.TextPrimary,
                                    focusedLabelColor = NorituraColors.PrimaryBlue,
                                    unfocusedLabelColor = NorituraColors.TextTertiary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false }
                            ) {
                                GENDER_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            gender = option
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.confirmAppointment(
                        appointmentId = appointmentId,
                        autoCreatePatient = autoCreate,
                        patientData = if (autoCreate) mapOf("name" to name, "age" to age, "gender" to gender) else null
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
                    text = "Confirm Appointment",
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

            if (confirmState is AppointmentViewModel.ConfirmState.Error) {
                val error = confirmState as AppointmentViewModel.ConfirmState.Error
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

@Composable
private fun AppointmentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
