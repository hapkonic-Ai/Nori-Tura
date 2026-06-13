package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.AppointmentDto
import com.example.nori_tura.util.getCurrentDateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgeonDashboardScreen(
    viewModel: SurgeonDashboardViewModel = viewModel { SurgeonDashboardViewModel() },
    onNavigateToPatientList: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit,
    onNavigateToPatientProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Surgeon Dashboard") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SurgeonDashboardViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is SurgeonDashboardViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadDashboard() }) {
                            Text("Retry")
                        }
                    }
                }

                is SurgeonDashboardViewModel.UiState.Success -> {
                    DashboardContent(
                        data = state.data,
                        onNavigateToPatientList = onNavigateToPatientList,
                        onNavigateToAppointments = onNavigateToAppointments,
                        onNavigateToSurgicalTemplates = onNavigateToSurgicalTemplates,
                        onNavigateToAdmissions = onNavigateToAdmissions,
                        onNavigateToPatientProfile = onNavigateToPatientProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: SurgeonDashboardViewModel.DashboardData,
    onNavigateToPatientList: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit,
    onNavigateToPatientProfile: (String) -> Unit
) {
    val today = getCurrentDateString()
    val todaysAppointments = data.appointments.filter {
        it.slotDatetime?.startsWith(today) == true
    }
    val activeAdmissions = data.admissions.filter {
        it.status?.lowercase() in setOf("admitted", "pre-op", "in-surgery", "recovery")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Welcome back, Doctor",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Patients",
                    count = data.patients.size.toString()
                )
                StatCard(
                    title = "Today's Appointments",
                    count = todaysAppointments.size.toString()
                )
                StatCard(
                    title = "Active Admissions",
                    count = activeAdmissions.size.toString()
                )
            }
        }

        item {
            Text(
                text = "Today's Appointments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (todaysAppointments.isEmpty()) {
            item {
                Text(
                    text = "No appointments scheduled for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(todaysAppointments, key = { it.id ?: it.hashCode() }) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onClick = {
                        appointment.patientId?.let(onNavigateToPatientProfile)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToPatientList,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Patients")
                }
                OutlinedButton(
                    onClick = onNavigateToAppointments,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Appointments")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToSurgicalTemplates,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Surgical Templates")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToAdmissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("IPD Admissions")
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, count: String) {
    Card(
        modifier = Modifier.width(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: AppointmentDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = appointment.patient?.name ?: "Patient",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Time: ${appointment.slotDatetime ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Visit: ${appointment.visitType ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${appointment.status ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
