package com.nonituracare.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.nonituracare.data.dto.AdmissionDto
import com.nonituracare.data.dto.AppointmentDto
import com.nonituracare.data.dto.ConsentFormDto
import com.nonituracare.data.dto.PatientDto
import com.nonituracare.presentation.components.ActionCard
import com.nonituracare.presentation.components.Avatar
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.HospitalSwitcher
import com.nonituracare.presentation.components.KpiTile
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.LongPressCardPreview
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.NorituraSurfaceCard
import com.nonituracare.presentation.components.ParentBottomNav
import com.nonituracare.presentation.components.SectionTitle
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.dashboard_greeting_banner
import noritura.shared.generated.resources.empty_patients
import org.jetbrains.compose.resources.painterResource

@Composable
fun ParentHomeScreen(
    viewModel: ParentDashboardViewModel = viewModel { ParentDashboardViewModel() },
    onNavigateToRecords: () -> Unit = {},
    onNavigateToSurgeryStatus: (String) -> Unit = {},
    onNavigateToConsentView: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBookAppointment: (doctorId: String, doctorName: String) -> Unit = { _, _ -> },
    onNavigateToChildDetail: (patientId: String) -> Unit = {},
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
        },
        bottomBar = {
            ParentBottomNav(
                selectedRoute = "home",
                onHome = { },
                onRecords = onNavigateToRecords,
                onProfile = onNavigateToProfile
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            com.nonituracare.presentation.components.DashboardGreetingBanner(
                title = "Family Dashboard",
                subtitle = "Track your child's surgery journey, appointments, and records."
            )

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

                    if (dashboard.hospitals.isNotEmpty()) {
                        HospitalSwitcher(
                            hospitals = dashboard.hospitals,
                            selectedHospitalId = dashboard.selectedHospitalId,
                            onSelect = { viewModel.selectHospital(it.id) }
                        )
                    }

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

                    dashboard.activeAdmission?.let { admission ->
                        SectionTitle(title = "Live Surgery Status")
                        SurgeryStatusCard(
                            admission = admission,
                            onClick = { admission.id?.let(onNavigateToSurgeryStatus) }
                        )
                    }

                    dashboard.nextAppointment?.let { appointment ->
                        SectionTitle(title = "Next Appointment")
                        NextAppointmentCard(appointment = appointment)
                    }

                    SectionTitle(title = "Quick Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            label = "Records",
                            icon = Icons.AutoMirrored.Filled.List,
                            onClick = onNavigateToRecords,
                            modifier = Modifier.weight(1f)
                        )
                        ActionCard(
                            label = "Book Appt",
                            icon = Icons.Default.CalendarMonth,
                            onClick = {
                                val doctor = dashboard.admissions.firstOrNull()?.doctor
                                onNavigateToBookAppointment(
                                    doctor?.id ?: "",
                                    doctor?.name ?: "Your Surgeon"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
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
                            subtitle = "Contact your surgeon to link your phone number to a patient record.",
                            illustration = Res.drawable.empty_patients
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            dashboard.children.forEach { child ->
                                ChildCard(
                                    child = child,
                                    onClick = { child.id?.let(onNavigateToChildDetail) }
                                )
                            }
                        }
                    }

                    val pendingConsents = dashboard.admissions
                        .flatMap { it.consentForms ?: emptyList() }
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
private fun SurgeryStatusCard(
    admission: AdmissionDto,
    onClick: () -> Unit
) {
    val status = admission.status ?: "Admitted"
    val statusColor = when (status.lowercase()) {
        "pre-op" -> NorituraColors.PreOp
        "in-surgery", "in-operation" -> NorituraColors.InOt
        "recovery", "post-op" -> NorituraColors.PostOp
        "admitted" -> NorituraColors.Info
        else -> NorituraColors.Stable
    }

    val procedure = admission.procedure
        ?: admission.consentForms?.firstOrNull()?.contentJson?.get("procedure")?.toString()?.removeSurrounding("\"")
        ?: "Procedure to be confirmed"

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        previewTitle = "Surgery Status Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = admission.patient?.name ?: "Your Child",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                StatusChip(
                    label = status.replaceFirstChar { it.uppercase() },
                    color = statusColor,
                    showDot = true
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = procedure,
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Surgeon: ${admission.doctor?.name ?: "-"}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!admission.ward.isNullOrBlank() || !admission.bedNo.isNullOrBlank()) {
                    Text(
                        text = "${admission.ward ?: "-"} • Bed ${admission.bedNo ?: "-"}",
                        color = NorituraColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "Tap to view full timeline →",
                color = NorituraColors.PrimaryBlue,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
    }
}

@Composable
private fun NextAppointmentCard(
    appointment: AppointmentDto
) {
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Appointment Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NorituraColors.PrimaryBlueLight)
                    .padding(12.dp)
            ) {
                Text(
                    text = appointment.slotDatetime?.take(10) ?: "-",
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.slotDatetime?.timePart() ?: "--:--",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${appointment.visitType?.replaceFirstChar { it.uppercase() } ?: "Visit"} with ${appointment.patient?.name ?: "-"}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    }
}

private fun String.timePart(): String? {
    val index = indexOf('T')
    if (index == -1) return null
    return substring(index + 1).take(5)
}

@Composable
private fun ChildCard(child: PatientDto, onClick: () -> Unit = {}) {
    val status = child.ipdAdmissions?.lastOrNull()?.status ?: "Outpatient"
    val statusColor = when (status.lowercase()) {
        "pre-op" -> NorituraColors.PreOp
        "in-surgery", "in-operation" -> NorituraColors.InOt
        "recovery", "post-op" -> NorituraColors.PostOp
        "admitted" -> NorituraColors.Info
        "discharged" -> NorituraColors.TextTertiary
        else -> NorituraColors.Stable
    }

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        previewTitle = "Child Preview"
    ) {
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

            val allergies = child.allergies?.trim()
            if (!allergies.isNullOrBlank() &&
                !allergies.equals("None", true) &&
                !allergies.equals("No", true) &&
                !allergies.equals("N/A", true)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = NorituraColors.Divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Allergies",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = allergies,
                    color = NorituraColors.Error,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
            }
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

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        previewTitle = "Consent Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
}
