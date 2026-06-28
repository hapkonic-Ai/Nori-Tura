package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.AppointmentDto
import com.example.nori_tura.data.dto.ConsentAlertDto
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.formatDateTime

@Composable
fun SurgeonAlertsTab(
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = viewModel { AlertsViewModel() },
    onNavigateToConsent: (String) -> Unit = {},
    onNavigateToAppointment: (String) -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToAdmission: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Alerts",
                notificationCount = 0
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AlertsViewModel.UiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }
            is AlertsViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadAlerts() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AlertsViewModel.UiState.Success -> {
                AlertsList(
                    alerts = state.alerts,
                    onNavigateToConsent = onNavigateToConsent,
                    onNavigateToAppointment = onNavigateToAppointment,
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToAdmission = onNavigateToAdmission,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun AlertsList(
    alerts: com.example.nori_tura.data.dto.AlertsResponseDto,
    onNavigateToConsent: (String) -> Unit,
    onNavigateToAppointment: (String) -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToAdmission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            AlertSectionTitle("Pending Consents (${alerts.pendingConsents.size})")
        }
        if (alerts.pendingConsents.isEmpty()) {
            item { InlineAlertEmpty("No consents waiting for signature.") }
        } else {
            items(alerts.pendingConsents.size) { index ->
                ConsentAlertCard(
                    consent = alerts.pendingConsents[index],
                    onClick = { consentId -> onNavigateToConsent(consentId) }
                )
            }
        }

        item {
            AlertSectionTitle("Today's Appointments (${alerts.todayAppointments.size})")
        }
        if (alerts.todayAppointments.isEmpty()) {
            item { InlineAlertEmpty("No appointments scheduled for today.") }
        } else {
            items(alerts.todayAppointments.size) { index ->
                AppointmentAlertCard(
                    appointment = alerts.todayAppointments[index],
                    onClick = { appointmentId -> onNavigateToAppointment(appointmentId) }
                )
            }
        }

        item {
            AlertSectionTitle("Pending Reviews (${alerts.pendingReviews.size})")
        }
        if (alerts.pendingReviews.isEmpty()) {
            item { InlineAlertEmpty("No nurse entries waiting for review.") }
        } else {
            items(alerts.pendingReviews.size) { index ->
                ReviewAlertCard(
                    record = alerts.pendingReviews[index],
                    onClick = { recordId -> onNavigateToReview(recordId) }
                )
            }
        }

        item {
            AlertSectionTitle("Active Admissions (${alerts.activeAdmissions.size})")
        }
        if (alerts.activeAdmissions.isEmpty()) {
            item { InlineAlertEmpty("No currently admitted patients.") }
        } else {
            items(alerts.activeAdmissions.size) { index ->
                AdmissionAlertCard(
                    admission = alerts.activeAdmissions[index],
                    onClick = { admissionId -> onNavigateToAdmission(admissionId) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AlertSectionTitle(title: String) {
    Text(
        text = title,
        color = NorituraColors.TextPrimary,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun InlineAlertEmpty(message: String) {
    Text(
        text = message,
        color = NorituraColors.TextTertiary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ConsentAlertCard(
    consent: ConsentAlertDto,
    onClick: (String) -> Unit
) {
    AlertCard(onClick = { consent.id.let(onClick) }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Consent waiting for ${consent.patientName ?: "patient"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Procedure: ${consent.procedure ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            consent.generatedAt?.let {
                Text(
                    text = "Generated: ${formatDateTime(it)}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AppointmentAlertCard(
    appointment: AppointmentDto,
    onClick: (String) -> Unit
) {
    AlertCard(onClick = { appointment.id?.let(onClick) }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = appointment.patient?.name ?: "Patient",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "${appointment.visitType?.replaceFirstChar { it.uppercase() } ?: "Visit"} at ${appointment.slotDatetime?.substringAfter("T")?.take(5) ?: "--:--"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReviewAlertCard(
    record: OpdRecordDto,
    onClick: (String) -> Unit
) {
    AlertCard(onClick = { record.id?.let(onClick) }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Review needed: ${record.patient?.name ?: "Patient"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Complaint: ${record.chiefComplaint ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            record.createdAt?.let {
                Text(
                    text = "Recorded: ${formatDateTime(it)}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AdmissionAlertCard(
    admission: AdmissionDto,
    onClick: (String) -> Unit
) {
    AlertCard(onClick = { admission.id?.let(onClick) }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = admission.patient?.name ?: "Patient",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Status: ${admission.status?.replaceFirstChar { it.uppercase() } ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${admission.ward ?: "-"} • Bed ${admission.bedNo ?: "-"}",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AlertCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
