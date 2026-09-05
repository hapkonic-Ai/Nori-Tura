package com.nonituracare.presentation.surgeon

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.AlertsResponseDto
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.ui.theme.NorituraColors
import com.nonituracare.util.formatDateTime
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.empty_alerts

/**
 * One flat, chronological-feeling activity feed (icon + title + subtitle +
 * time, no boxed cards or raw "(count)" section headers) — the same shape as
 * a normal app notification list, instead of four separately-labeled
 * dashboards stacked on top of each other.
 */
@Composable
fun SurgeonAlertsTab(
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = viewModel { AlertsViewModel() },
    onBack: (() -> Unit)? = null,
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
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
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
                AlertsFeed(
                    alerts = state.alerts,
                    onNavigateToConsent = onNavigateToConsent,
                    onNavigateToAppointment = onNavigateToAppointment,
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToAdmission = onNavigateToAdmission
                )
            }
        }
    }
}

private data class FeedRow(
    val id: String,
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val timestamp: String?,
    val onClick: () -> Unit
)

@Composable
private fun AlertsFeed(
    alerts: AlertsResponseDto,
    onNavigateToConsent: (String) -> Unit,
    onNavigateToAppointment: (String) -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToAdmission: (String) -> Unit
) {
    val rows = buildList {
        alerts.pendingConsents.forEach { consent ->
            add(
                FeedRow(
                    id = "consent-${consent.id}",
                    icon = Icons.Default.Description,
                    iconTint = NorituraColors.PrimaryBlue,
                    title = "Consent waiting for ${consent.patientName ?: "patient"}",
                    subtitle = consent.procedure ?: "Procedure not set",
                    timestamp = consent.generatedAt?.let { formatDateTime(it) },
                    onClick = { consent.id.let(onNavigateToConsent) }
                )
            )
        }
        alerts.todayAppointments.forEach { appointment ->
            add(
                FeedRow(
                    id = "appt-${appointment.id}",
                    icon = Icons.Default.CalendarMonth,
                    iconTint = NorituraColors.AccentGreen,
                    title = appointment.patient?.name ?: "Patient",
                    subtitle = "${appointment.visitType?.replaceFirstChar { it.uppercase() } ?: "Visit"} today at " +
                        (appointment.slotDatetime?.substringAfter("T")?.take(5) ?: "--:--"),
                    timestamp = null,
                    onClick = { appointment.id?.let(onNavigateToAppointment) }
                )
            )
        }
        alerts.pendingReviews.forEach { record ->
            add(
                FeedRow(
                    id = "review-${record.id}",
                    icon = Icons.Default.RateReview,
                    iconTint = NorituraColors.Warning,
                    title = "Review needed: ${record.patient?.name ?: "Patient"}",
                    subtitle = record.chiefComplaint ?: "No complaint noted",
                    timestamp = record.createdAt?.let { formatDateTime(it) },
                    onClick = { record.id?.let(onNavigateToReview) }
                )
            )
        }
        alerts.activeAdmissions.forEach { admission ->
            add(
                FeedRow(
                    id = "admission-${admission.id}",
                    icon = Icons.Default.LocalHospital,
                    iconTint = NorituraColors.AccentLavender,
                    title = admission.patient?.name ?: "Patient",
                    subtitle = "${admission.status?.replaceFirstChar { it.uppercase() } ?: "Admitted"} · " +
                        "${admission.ward ?: "Ward -"} · Bed ${admission.bedNo ?: "-"}",
                    timestamp = null,
                    onClick = { admission.id?.let(onNavigateToAdmission) }
                )
            )
        }
    }

    if (rows.isEmpty()) {
        EmptyState(
            title = "No alerts",
            subtitle = "You're all caught up — nothing needs your attention right now.",
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background),
            illustration = Res.drawable.empty_alerts
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        items(rows, key = { it.id }) { row ->
            FeedRowItem(row = row)
            HorizontalDivider(color = NorituraColors.Divider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun FeedRowItem(row: FeedRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = row.onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(row.iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = row.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = row.subtitle,
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        row.timestamp?.let {
            Text(
                text = it,
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
