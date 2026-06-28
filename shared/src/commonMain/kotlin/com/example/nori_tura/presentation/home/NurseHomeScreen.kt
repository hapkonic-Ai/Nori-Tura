package com.example.nori_tura.presentation.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.presentation.components.ActionCard
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.KpiTile
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.NurseBottomNav
import com.example.nori_tura.presentation.components.SectionTitle
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun NurseHomeScreen(
    viewModel: NurseDashboardViewModel = viewModel { NurseDashboardViewModel() },
    onNavigateToPatientList: () -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "NR",
                title = "SurgiCare",
                notificationCount = 0,
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = NorituraColors.TextPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NurseBottomNav(
                selectedRoute = "home",
                onHome = { },
                onPatients = onNavigateToPatientList,
                onAppointments = onNavigateToAppointments
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPatient,
                containerColor = NorituraColors.PrimaryBlue,
                contentColor = NorituraColors.Surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Patient")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Welcome back, Nurse",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Manage patients, appointments, and daily tasks for your supervising surgeon.",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when (val state = uiState) {
                is NurseDashboardViewModel.UiState.Loading -> {
                    LoadingState(message = "Loading dashboard...")
                }

                is NurseDashboardViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadMetrics() }
                    )
                }

                is NurseDashboardViewModel.UiState.Success -> {
                    val metrics = state.metrics

                    SectionTitle(title = "Today's Summary")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiTile(
                            label = "Patients Today",
                            value = metrics.patientsAddedToday.toString(),
                            icon = Icons.Default.Person,
                            iconTint = NorituraColors.PrimaryBlue,
                            accentColor = NorituraColors.PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Upcoming Appts",
                            value = metrics.upcomingAppointments.toString(),
                            icon = Icons.Default.DateRange,
                            iconTint = NorituraColors.AccentGreen,
                            accentColor = NorituraColors.AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiTile(
                            label = "Active IPD",
                            value = metrics.activeIpdAdmissions.toString(),
                            icon = Icons.Default.MedicalServices,
                            iconTint = NorituraColors.Warning,
                            accentColor = NorituraColors.Warning,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SectionTitle(title = "Quick Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            label = "Patient List",
                            icon = Icons.AutoMirrored.Filled.List,
                            onClick = onNavigateToPatientList,
                            modifier = Modifier.weight(1f)
                        )
                        ActionCard(
                            label = "Appointments",
                            icon = Icons.Default.CalendarMonth,
                            onClick = onNavigateToAppointments,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

        }
    }
}
