package com.example.nori_tura.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.PatientDto
import com.example.nori_tura.presentation.components.ActionCard
import com.example.nori_tura.presentation.components.Avatar
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.KpiTile
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.NorituraSurfaceCard
import com.example.nori_tura.presentation.components.SectionTitle
import com.example.nori_tura.presentation.components.StatusChip
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ParentHomeScreen(
    viewModel: ParentDashboardViewModel = viewModel { ParentDashboardViewModel() },
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    onNavigateToConsentView: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "PT",
                title = "SurgiCare",
                notificationCount = 0,
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = NorituraColors.TextSecondary
                        )
                    }
                }
            )
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
                    text = "Family Dashboard",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Track your child's surgery journey, appointments, and records.",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when (val state = uiState) {
                is ParentDashboardViewModel.UiState.Loading -> {
                    LoadingState(message = "Loading family dashboard...")
                }

                is ParentDashboardViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadDashboard() }
                    )
                }

                is ParentDashboardViewModel.UiState.Success -> {
                    val dashboard = state.dashboard

                    SectionTitle(title = "Summary")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiTile(
                            label = "Children",
                            value = dashboard.children.size.toString(),
                            icon = Icons.Default.ChildCare,
                            iconTint = NorituraColors.PrimaryBlue,
                            accentColor = NorituraColors.PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Upcoming Appts",
                            value = dashboard.upcomingAppointments.toString(),
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
                            label = "Active Admissions",
                            value = dashboard.activeAdmissions.toString(),
                            icon = Icons.Default.LocalHospital,
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
                            label = "Appointments",
                            icon = Icons.Default.CalendarMonth,
                            onClick = onNavigateToAppointments,
                            modifier = Modifier.weight(1f)
                        )
                        ActionCard(
                            label = "Records",
                            icon = Icons.AutoMirrored.Filled.List,
                            onClick = onNavigateToRecords,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            label = "Profile",
                            icon = Icons.Default.Person,
                            onClick = onNavigateToProfile,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SectionTitle(
                        title = "Your Children",
                        actionLabel = "Refresh",
                        onAction = { viewModel.loadDashboard() }
                    )

                    if (dashboard.children.isEmpty()) {
                        EmptyState(
                            title = "No children linked",
                            subtitle = "Contact your surgeon to link your phone number to a patient record."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            dashboard.children.forEach { child ->
                                ChildCard(child = child)
                            }
                        }
                    }

                    val pendingConsents = dashboard.admissions
                        .flatMap { it.consentForms }
                        .filter { it.status != "signed" }
                        .sortedByDescending { it.generatedAt }

                    if (pendingConsents.isNotEmpty()) {
                        SectionTitle(title = "Consent Forms Awaiting Signature")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            pendingConsents.forEach { consent ->
                                PendingConsentCard(
                                    consent = consent,
                                    onClick = { consent.id?.let(onNavigateToConsentView) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChildCard(child: PatientDto) {
    val status = child.ipdAdmissions?.lastOrNull()?.status ?: "Outpatient"
    val statusColor = when (status.lowercase()) {
        "pre-op" -> NorituraColors.PreOp
        "in-surgery", "in-operation" -> NorituraColors.InOt
        "recovery", "post-op" -> NorituraColors.PostOp
        "admitted" -> NorituraColors.Info
        "discharged" -> NorituraColors.TextTertiary
        else -> NorituraColors.Stable
    }

    NorituraSurfaceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(name = child.name ?: "?", size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name ?: "Unknown",
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${child.age ?: "-"} yrs • ${child.gender ?: "-"}${child.bloodGroup?.let { " • Blood: $it" } ?: ""}",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusChip(
                    label = status.replaceFirstChar { it.uppercase() },
                    color = statusColor
                )
            }

            if (!child.allergies.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = NorituraColors.Divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Allergies",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = child.allergies,
                    color = NorituraColors.Error,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun PendingConsentCard(
    consent: ConsentFormDto,
    onClick: () -> Unit
) {
    val patientName = consent.contentJson?.get("patient_name")?.toString()?.removeSurrounding("\"") ?: "Your Child"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consent.formType ?: "Consent Form",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Pending",
                    color = NorituraColors.Warning,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Patient: $patientName",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Generated: ${consent.generatedAt?.take(10) ?: "-"}",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
