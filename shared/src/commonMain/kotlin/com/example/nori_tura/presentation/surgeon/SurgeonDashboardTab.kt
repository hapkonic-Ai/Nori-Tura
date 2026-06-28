package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.presentation.components.ActionCard
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.SectionTitle
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.getCurrentDateString

@Composable
fun SurgeonDashboardTab(
    modifier: Modifier = Modifier,
    viewModel: SurgeonDashboardViewModel = viewModel { SurgeonDashboardViewModel() },
    onNavigateToPatientList: () -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit,
    onNavigateToPatientProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "SurgiCare",
                notificationCount = 3
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                
                .background(NorituraColors.Background)
        ) {
            when (val state = uiState) {
            is SurgeonDashboardViewModel.UiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }
            is SurgeonDashboardViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadDashboard() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is SurgeonDashboardViewModel.UiState.Success -> {
                DashboardContent(
                    data = state.data,
                    onNavigateToPatientList = onNavigateToPatientList,
                    onNavigateToAddPatient = onNavigateToAddPatient,
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
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit,
    onNavigateToPatientProfile: (String) -> Unit
) {
    val today = getCurrentDateString()
    val todaysAppointments = data.appointments.filter {
        it.slotDatetime?.startsWith(today) == true
    }
    val surgeriesScheduled = data.appointments.count {
        it.visitType?.lowercase()?.contains("surgery") == true ||
                it.status?.lowercase()?.contains("scheduled") == true
    }
    val preOpCount = data.admissions.count { it.status?.lowercase() == "pre-op" }
    val inOtCount = data.admissions.count { it.status?.lowercase() == "in-surgery" }
    val postOpCount = data.admissions.count { it.status?.lowercase() == "recovery" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Welcome back, Doctor",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            SectionTitle(title = "Today's Summary")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    icon = Icons.Default.CalendarToday,
                    iconTint = NorituraColors.PrimaryBlue,
                    iconBackground = NorituraColors.PrimaryBlueLight,
                    label = "Today",
                    value = todaysAppointments.size.toString(),
                    unit = "APPOINTMENTS",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    icon = Icons.Default.MedicalServices,
                    iconTint = NorituraColors.AccentGreen,
                    iconBackground = NorituraColors.AccentGreenLight,
                    label = "Scheduled",
                    value = surgeriesScheduled.toString(),
                    unit = "SURGERIES",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionTitle(title = "Surgical Status")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    value = preOpCount.toString(),
                    label = "Pre-op",
                    subLabel = "Patients Ready",
                    color = NorituraColors.PreOp,
                    background = NorituraColors.PrimaryBlueLight,
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    value = inOtCount.toString(),
                    label = "In-Operation",
                    subLabel = "Active Cases",
                    color = NorituraColors.InOt,
                    background = NorituraColors.WarningLight,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            RecoveryCard(
                count = postOpCount,
                onClick = onNavigateToAdmissions
            )
        }

        item {
            SectionTitle(title = "Quick Actions")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    label = "Add Patient",
                    icon = Icons.Default.People,
                    onClick = onNavigateToAddPatient,
                    modifier = Modifier.weight(1f),
                    iconTint = NorituraColors.PrimaryBlue,
                    iconBackground = NorituraColors.PrimaryBlueLight
                )
                ActionCard(
                    label = "Surgical Templates",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    onClick = onNavigateToSurgicalTemplates,
                    modifier = Modifier.weight(1f),
                    iconTint = NorituraColors.AccentGreen,
                    iconBackground = NorituraColors.AccentGreenLight
                )
                ActionCard(
                    label = "Admissions",
                    icon = Icons.Default.MedicalServices,
                    onClick = onNavigateToAdmissions,
                    modifier = Modifier.weight(1f),
                    iconTint = NorituraColors.Warning,
                    iconBackground = NorituraColors.WarningLight
                )
            }
        }

        item {
            SectionTitle(title = "Today's Appointments", actionLabel = "View all", onAction = onNavigateToAppointments)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (todaysAppointments.isEmpty()) {
            item {
                InlineEmpty(title = "No appointments for today")
            }
        } else {
            items(todaysAppointments.size, key = { todaysAppointments[it].id ?: it }) { index ->
                val appointment = todaysAppointments[index]
                AppointmentRow(
                    appointment = appointment,
                    onClick = {
                        appointment.patientId?.let(onNavigateToPatientProfile)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = label,
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = value,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = unit,
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun StatusCard(
    value: String,
    label: String,
    subLabel: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = value,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subLabel,
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecoveryCard(
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NorituraColors.AccentGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = NorituraColors.AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Post-op Recovery",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "$count Patients Stable",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = NorituraColors.TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AppointmentRow(
    appointment: com.example.nori_tura.data.dto.AppointmentDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NorituraColors.PrimaryBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appointment.patient?.name?.take(1)?.uppercase() ?: "P",
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.patient?.name ?: "Patient",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${appointment.slotDatetime ?: "-"} • ${appointment.visitType ?: "-"}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = NorituraColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InlineEmpty(title: String) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
