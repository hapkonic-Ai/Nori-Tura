package com.nonituracare.presentation.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.DoctorDto
import com.nonituracare.data.dto.HospitalRefDto
import com.nonituracare.data.dto.NurseCreateRequest
import com.nonituracare.data.dto.NurseDto
import com.nonituracare.presentation.components.Avatar
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.NorituraSurfaceCard
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.empty_patients

@Composable
fun AdminNursesScreen(
    viewModel: AdminNursesViewModel = viewModel { AdminNursesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "NU",
                title = "Nurses",
                onBack = onBack,
                notificationCount = 0
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Nurse Login")
            }
        }
    ) {
        when (val state = uiState) {
            is AdminNursesViewModel.UiState.Loading -> {
                LoadingState(message = "Loading nurses...", modifier = Modifier.fillMaxSize())
            }

            is AdminNursesViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is AdminNursesViewModel.UiState.Success -> {
                val directory = state.directory

                if (directory.nurses.isEmpty()) {
                    EmptyState(
                        title = "No nurse logins yet",
                        subtitle = "Tap + to create one for a hospital and doctor.",
                        modifier = Modifier.fillMaxSize(),
                        illustration = Res.drawable.empty_patients
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(directory.nurses, key = { it.id ?: it.phone.orEmpty() }) { nurse ->
                            NurseCard(
                                nurse = nurse,
                                onToggleActive = { viewModel.toggleNurseActive(nurse) }
                            )
                        }
                    }
                }

                if (showCreateDialog) {
                    Dialog(onDismissRequest = {
                        showCreateDialog = false
                        viewModel.resetCreateState()
                    }) {
                        CreateNurseDialog(
                            doctors = directory.doctors,
                            hospitals = directory.hospitals,
                            createState = viewModel.createState.collectAsState().value,
                            onDismiss = {
                                showCreateDialog = false
                                viewModel.resetCreateState()
                            },
                            onCreate = { request -> viewModel.createNurse(request) }
                        )
                    }

                    val createState = viewModel.createState.collectAsState().value
                    if (createState is AdminNursesViewModel.CreateState.Success) {
                        showCreateDialog = false
                        viewModel.resetCreateState()
                    }
                }
            }
        }
    }
}

@Composable
private fun NurseCard(
    nurse: NurseDto,
    onToggleActive: () -> Unit
) {
    NorituraSurfaceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(name = nurse.name ?: "?", size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nurse.name ?: "Unnamed",
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = nurse.phone ?: "-",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusChip(
                    label = if (nurse.isActive) "Active" else "Inactive",
                    color = if (nurse.isActive) NorituraColors.AccentGreen else NorituraColors.TextTertiary
                )
                Switch(checked = nurse.isActive, onCheckedChange = { onToggleActive() })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Dr. ${nurse.doctor?.name ?: "Unassigned"} · ${nurse.hospital?.name ?: "No hospital"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNurseDialog(
    doctors: List<DoctorDto>,
    hospitals: List<HospitalRefDto>,
    createState: AdminNursesViewModel.CreateState,
    onDismiss: () -> Unit,
    onCreate: (NurseCreateRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedDoctor by remember { mutableStateOf<DoctorDto?>(null) }
    var selectedHospital by remember { mutableStateOf<HospitalRefDto?>(null) }
    var doctorExpanded by remember { mutableStateOf(false) }
    var hospitalExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Create Nurse Login", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number *") },
                placeholder = { Text("+919876543210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = doctorExpanded,
                onExpandedChange = { doctorExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDoctor?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Doctor *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = doctorExpanded,
                    onDismissRequest = { doctorExpanded = false }
                ) {
                    doctors.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text("${doctor.name} · ${doctor.specialty ?: ""}") },
                            onClick = {
                                selectedDoctor = doctor
                                doctorExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = hospitalExpanded,
                onExpandedChange = { hospitalExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedHospital?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hospital *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hospitalExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = hospitalExpanded,
                    onDismissRequest = { hospitalExpanded = false }
                ) {
                    hospitals.forEach { hospital ->
                        DropdownMenuItem(
                            text = { Text(hospital.name ?: "") },
                            onClick = {
                                selectedHospital = hospital
                                hospitalExpanded = false
                            }
                        )
                    }
                }
            }

            if (createState is AdminNursesViewModel.CreateState.Error) {
                Text(
                    text = createState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val doctorId = selectedDoctor?.id ?: return@Button
                        onCreate(
                            NurseCreateRequest(
                                name = name.trim(),
                                phone = phone.trim(),
                                doctorId = doctorId,
                                hospitalId = selectedHospital?.id
                            )
                        )
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank() && selectedDoctor != null &&
                        selectedHospital != null && createState !is AdminNursesViewModel.CreateState.Loading
                ) {
                    Text("Create")
                }
            }
        }
    }
}
