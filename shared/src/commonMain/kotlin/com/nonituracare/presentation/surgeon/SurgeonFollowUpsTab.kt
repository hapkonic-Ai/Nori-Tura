package com.nonituracare.presentation.surgeon

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.OpdRecordDto
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.LongPressCardPreview
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.empty_followups

/**
 * There's no appointment-booking system behind this screen. A follow-up is
 * just an OPD record carrying a doctor-suggested `follow_up_date`; it stays
 * "pending" until someone taps "Mark Attended" here. A pending record whose
 * date has already passed is shown as overdue — that's the "they didn't come
 * back" signal, derived purely from comparing dates client-side, not a
 * separate stored state.
 */
@Composable
fun SurgeonFollowUpsTab(
    modifier: Modifier = Modifier,
    viewModel: FollowUpsViewModel = viewModel { FollowUpsViewModel() },
    onNavigateToPreview: (String) -> Unit,
    onOpenAlerts: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val markingIds by viewModel.markingIds.collectAsState()

    NorituraScaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Follow-ups",
                notificationCount = 0,
                onNotificationClick = onOpenAlerts
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is FollowUpsViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is FollowUpsViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadFollowUps() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FollowUpsViewModel.UiState.Success -> {
                    if (state.records.isEmpty()) {
                        EmptyState(
                            title = "All caught up",
                            subtitle = "No pending follow-ups — nothing overdue and nothing scheduled ahead.",
                            icon = Icons.Default.EventAvailable,
                            modifier = Modifier.fillMaxSize(),
                            illustration = Res.drawable.empty_followups
                        )
                    } else {
                        val today = androidx.compose.runtime.remember { com.nonituracare.util.getCurrentDateString() }
                        val (overdue, upcoming) = state.records.partition { record ->
                            val d = record.followUpDate?.take(10)
                            d != null && d < today
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (overdue.isNotEmpty()) {
                                item { FollowUpSectionHeader(title = "Overdue (${overdue.size})", color = NorituraColors.Error) }
                                items(overdue, key = { it.id ?: it.hashCode() }) { record ->
                                    FollowUpCard(
                                        record = record,
                                        overdue = true,
                                        isMarking = markingIds.contains(record.id),
                                        onPreviewClick = { record.id?.let(onNavigateToPreview) },
                                        onMarkAttended = { record.id?.let(viewModel::markAttended) }
                                    )
                                }
                            }
                            if (upcoming.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(if (overdue.isNotEmpty()) 8.dp else 0.dp))
                                    FollowUpSectionHeader(title = "Upcoming (${upcoming.size})", color = NorituraColors.PrimaryBlue)
                                }
                                items(upcoming, key = { it.id ?: it.hashCode() }) { record ->
                                    FollowUpCard(
                                        record = record,
                                        overdue = false,
                                        isMarking = markingIds.contains(record.id),
                                        onPreviewClick = { record.id?.let(onNavigateToPreview) },
                                        onMarkAttended = { record.id?.let(viewModel::markAttended) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpSectionHeader(title: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = title,
            color = color,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun FollowUpCard(
    record: OpdRecordDto,
    overdue: Boolean,
    isMarking: Boolean,
    onPreviewClick: () -> Unit,
    onMarkAttended: () -> Unit
) {
    val patient = record.patient
    val doctor = record.doctor
    val reminderSent = record.reminderSent
    val dateText = record.followUpDate?.take(10) ?: "Not set"
    val isFollowUpVisit = record.visitType?.lowercase() == "follow_up"

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Follow-up Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (overdue) NorituraColors.ErrorLight else NorituraColors.Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = patient?.name ?: "Unknown",
                            color = NorituraColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        StatusChip(
                            label = if (isFollowUpVisit) "Follow-up Visit" else "New Visit",
                            color = if (isFollowUpVisit) NorituraColors.AccentLavender else NorituraColors.PrimaryBlue,
                            showDot = false
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        StatusChip(
                            label = if (overdue) "Overdue" else if (reminderSent) "Reminder Sent" else "Upcoming",
                            color = if (overdue) NorituraColors.Error else if (reminderSent) NorituraColors.PostOp else NorituraColors.Warning,
                            showDot = true
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = NorituraColors.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Follow-up: $dateText",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Parent: ${patient?.parentPhone ?: "-"}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Doctor: ${doctor?.name ?: "-"}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkAttended,
                        enabled = !isMarking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isMarking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(0.dp))
                            Text(" Attended", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Button(
                        onClick = onPreviewClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(" Send", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
