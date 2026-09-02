package com.nonituracare.presentation.ipd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.dto.AdmissionCreateRequest
import com.nonituracare.data.dto.AdmissionDto
import com.nonituracare.data.dto.PatientDto
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.LongPressCardPreview
import com.nonituracare.ui.theme.NorituraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionsListScreen(
    viewModel: AdmissionsListViewModel = viewModel { AdmissionsListViewModel() },
    onBack: () -> Unit,
    onAdmissionClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val patients by viewModel.patients.collectAsState()
    var showAdmitDialog by remember { mutableStateOf(false) }
    val allowWardBed = remember { AuthRepository().getRole()?.lowercase() != "nurse" }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AdmissionsListViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AdmissionsListViewModel.UiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "IPD Admissions",
                onBack = onBack,
                notificationCount = 0
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdmitDialog = true },
                containerColor = NorituraColors.PrimaryBlue,
                contentColor = NorituraColors.Surface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Admit patient")
            }
        },
        containerColor = NorituraColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AdmissionsListViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }

                is AdmissionsListViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadAdmissions() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is AdmissionsListViewModel.UiState.Success -> {
                    if (state.admissions.isEmpty()) {
                        EmptyState(
                            title = "No admissions yet",
                            subtitle = "Tap + to admit a patient",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.admissions) { admission ->
                                AdmissionCard(
                                    admission = admission,
                                    onClick = { admission.id?.let(onAdmissionClick) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdmitDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdmitDialog = false }) {
            AdmitPatientDialog(
                patients = patients,
                allowWardBed = allowWardBed,
                onDismiss = { showAdmitDialog = false },
                onAdmit = { request ->
                    viewModel.createAdmission(request)
                    showAdmitDialog = false
                }
            )
        }
    }
}

@Composable
private fun AdmissionCard(
    admission: AdmissionDto,
    onClick: () -> Unit
) {
    val statusColor = when (admission.status?.lowercase()) {
        "pre-op" -> NorituraColors.PreOp
        "in-surgery" -> NorituraColors.InOt
        "recovery" -> NorituraColors.PostOp
        "discharged" -> NorituraColors.TextTertiary
        else -> NorituraColors.PrimaryBlue
    }
    val urgencyColor = when (admission.urgency?.lowercase()) {
        "emergency" -> MaterialTheme.colorScheme.error
        "urgent" -> NorituraColors.Warning
        else -> NorituraColors.TextTertiary
    }

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        previewTitle = "Admission Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = admission.patient?.name ?: "Unknown Patient",
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = (admission.status ?: "—").uppercase(),
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Ward ${admission.ward ?: "—"} · Bed ${admission.bedNo ?: "—"}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = admission.urgency?.replaceFirstChar { it.uppercase() } ?: "-",
                        color = urgencyColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = admission.admittedAt?.take(10) ?: "-",
                        color = NorituraColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmitPatientDialog(
    patients: List<PatientDto>,
    patient: PatientDto? = null,
    error: String? = null,
    allowWardBed: Boolean = true,
    onDismiss: () -> Unit,
    onAdmit: (AdmissionCreateRequest) -> Unit
) {
    var selectedPatient by remember { mutableStateOf<PatientDto?>(patient) }
    var expanded by remember { mutableStateOf(false) }
    var urgency by remember { mutableStateOf("elective") }
    var urgencyExpanded by remember { mutableStateOf(false) }
    var bedNo by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }

    val urgencyOptions = listOf("elective", "urgent", "emergency")

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (patient != null) "Admit ${patient.name ?: "Patient"}" else "Admit Patient",
                style = MaterialTheme.typography.headlineSmall
            )

            if (patient == null) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Patient *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patients.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name ?: "") },
                                onClick = {
                                    selectedPatient = p
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ExposedDropdownMenuBox(
                expanded = urgencyExpanded,
                onExpandedChange = { urgencyExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = urgency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Urgency *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = urgencyExpanded,
                    onDismissRequest = { urgencyExpanded = false }
                ) {
                    urgencyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                urgency = option
                                urgencyExpanded = false
                            }
                        )
                    }
                }
            }

            if (allowWardBed) {
                OutlinedTextField(
                    value = ward,
                    onValueChange = { ward = it },
                    label = { Text("Ward") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bedNo,
                    onValueChange = { bedNo = it },
                    label = { Text("Bed No") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        selectedPatient?.id?.let { patientId ->
                            onAdmit(
                                AdmissionCreateRequest(
                                    patientId = patientId,
                                    urgency = urgency,
                                    bedNo = if (allowWardBed) bedNo.takeIf { it.isNotBlank() } else null,
                                    ward = if (allowWardBed) ward.takeIf { it.isNotBlank() } else null
                                )
                            )
                        }
                    },
                    enabled = selectedPatient?.id?.isNotBlank() == true
                ) {
                    Text("Admit")
                }
            }
        }
    }
}
