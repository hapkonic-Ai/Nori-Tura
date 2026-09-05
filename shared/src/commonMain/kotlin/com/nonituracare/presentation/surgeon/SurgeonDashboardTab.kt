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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.presentation.components.ActionCard
import com.nonituracare.presentation.components.AnimatedCountText
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyStateGlyph
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.MiniSegmentBar
import com.nonituracare.presentation.components.PatientFlowStepper
import com.nonituracare.presentation.components.RadialProgressRing
import com.nonituracare.presentation.components.SectionTitle
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.surgical_pipeline_banner

@Composable
fun SurgeonDashboardTab(
    modifier: Modifier = Modifier,
    viewModel: SurgeonDashboardViewModel = viewModel { SurgeonDashboardViewModel() },
    onNavigateToAddPatient: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToOtNoteTemplates: () -> Unit = {},
    onNavigateToAdmissions: () -> Unit,
    onOpenAlerts: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "SurgiCare",
                onNotificationClick = onOpenAlerts
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
                    onNavigateToAddPatient = onNavigateToAddPatient,
                    onNavigateToSurgicalTemplates = onNavigateToSurgicalTemplates,
                    onNavigateToOtNoteTemplates = onNavigateToOtNoteTemplates,
                    onNavigateToAdmissions = onNavigateToAdmissions
                )
            }
        }
        }
    }
}

@Composable
private fun DashboardContent(
    data: SurgeonDashboardViewModel.DashboardData,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToOtNoteTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit
) {
    val activeAdmissionStatuses = setOf("admitted", "pre-op", "in-surgery", "recovery")
    val activeAdmissionsCount = data.admissions.count { it.status?.lowercase() in activeAdmissionStatuses }
    val preOpCount = data.admissions.count { it.status?.lowercase() == "pre-op" }
    val inOtCount = data.admissions.count { it.status?.lowercase() == "in-surgery" }
    val postOpCount = data.admissions.count { it.status?.lowercase() == "recovery" }
    val activePatientIds = data.admissions
        .filter { it.status?.lowercase() in activeAdmissionStatuses }
        .mapNotNull { it.patientId }
        .toSet()
    val patientsUnderCareCount = data.patients.count { it.id in activePatientIds }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            com.nonituracare.presentation.components.DashboardGreetingBanner(
                title = "Welcome back, Doctor",
                subtitle = "Here's what's happening with your patients today."
            )
        }

        item {
            EnterAnimated {
                SectionTitle(title = "Overview")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        icon = Icons.Default.People,
                        iconTint = NorituraColors.PrimaryBlue,
                        iconBackground = NorituraColors.PrimaryBlueLight,
                        label = "Total",
                        count = data.patients.size,
                        unit = "PATIENTS",
                        emptyCaption = "No patients yet",
                        segments = listOf(
                            patientsUnderCareCount.toFloat() to NorituraColors.PrimaryBlue,
                            (data.patients.size - patientsUnderCareCount).toFloat() to NorituraColors.PrimaryBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        icon = Icons.Default.LocalHospital,
                        iconTint = NorituraColors.AccentGreen,
                        iconBackground = NorituraColors.AccentGreenLight,
                        label = "Active",
                        count = activeAdmissionsCount,
                        unit = "ADMISSIONS",
                        emptyCaption = "No active admissions today",
                        segments = listOf(
                            preOpCount.toFloat() to NorituraColors.PreOp,
                            inOtCount.toFloat() to NorituraColors.InOt,
                            postOpCount.toFloat() to NorituraColors.PostOp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            EnterAnimated {
                SectionTitle(title = "Surgical Status")
                Spacer(modifier = Modifier.height(8.dp))
                SurgicalFlowCard(
                    preOp = preOpCount,
                    inOt = inOtCount,
                    postOp = postOpCount,
                    onClick = onNavigateToAdmissions
                )
            }
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
                    icon = Icons.Default.LocalHospital,
                    onClick = onNavigateToAdmissions,
                    modifier = Modifier.weight(1f),
                    iconTint = NorituraColors.Warning,
                    iconBackground = NorituraColors.WarningLight
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    label = "OT Note Templates",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = onNavigateToOtNoteTemplates,
                    modifier = Modifier.weight(1f),
                    iconTint = NorituraColors.PrimaryBlue,
                    iconBackground = NorituraColors.PrimaryBlueLight
                )
                Box(modifier = Modifier.weight(2f))
            }
        }

    }
}

/** A soft radial-gauge card: icon + label header, a headline count with a
 * quiet progress ring behind it, and a thin segmented breakdown bar beneath.
 * Falls back to a muted empty-state glyph when the count is genuinely zero,
 * instead of rendering a bare "0" in a wall of whitespace. */
@Composable
private fun SummaryCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    label: String,
    count: Int,
    unit: String,
    emptyCaption: String,
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(iconBackground.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
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
                            .size(34.dp)
                            .clip(CircleShape)
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

                if (count == 0) {
                    EmptyStateGlyph(caption = emptyCaption)
                } else {
                    RadialProgressRing(
                        progress = (count / 20f).coerceIn(0.08f, 1f),
                        color = iconTint,
                        size = 56.dp
                    ) {
                        AnimatedCountText(
                            value = count,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = NorituraColors.TextPrimary
                        )
                    }
                    Text(
                        text = unit,
                        color = NorituraColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    MiniSegmentBar(segments = segments)
                }
            }
        }
    }
}

/** Horizontal patient-flow pipeline card: Pre-op -> In-Operation -> Recovery.
 * This is the mental model a surgeon actually works from, so it replaces two
 * disconnected number boxes plus a separate recovery row with one pipeline. */
@Composable
private fun SurgicalFlowCard(
    preOp: Int,
    inOt: Int,
    postOp: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (preOp == 0 && inOt == 0 && postOp == 0) {
                EmptyStateGlyph(
                    caption = "No patients in the surgical pipeline right now",
                    icon = Icons.Default.LocalHospital,
                    tint = NorituraColors.AccentLavender
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = org.jetbrains.compose.resources.painterResource(
                        Res.drawable.surgical_pipeline_banner
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                PatientFlowStepper(
                    preOp = preOp,
                    inOt = inOt,
                    postOp = postOp,
                    preOpColor = NorituraColors.PreOp,
                    inOtColor = NorituraColors.InOt,
                    postOpColor = NorituraColors.PostOp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "View admission pipeline",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = NorituraColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** A quiet fade + rise-in for a dashboard section, so the screen feels like
 * it's loading in rather than snapping into place all at once. */
@Composable
private fun EnterAnimated(content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(400)) +
            slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 6 })
    ) {
        Column { content() }
    }
}

