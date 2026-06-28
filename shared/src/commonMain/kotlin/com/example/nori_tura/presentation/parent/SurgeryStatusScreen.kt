package com.example.nori_tura.presentation.parent

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.presentation.components.Avatar
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.LongPressCardPreview
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors

private val STAGES = listOf(
    "Admitted" to "Patient admitted to hospital",
    "Pre-op" to "Pre-operative assessment complete",
    "In Surgery" to "Surgery in progress",
    "Recovery" to "Post-operative recovery",
    "Discharged" to "Discharged from hospital"
)

@Composable
fun SurgeryStatusScreen(
    admissionId: String,
    onBack: () -> Unit,
    onNavigateToConsentView: (String) -> Unit = {},
    viewModel: SurgeryStatusViewModel = viewModel(key = admissionId) { SurgeryStatusViewModel(admissionId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "PT",
                title = "Surgery Status",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is SurgeryStatusViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is SurgeryStatusViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadAdmission() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is SurgeryStatusViewModel.UiState.Success -> {
                    SurgeryStatusContent(
                        admission = state.admission,
                        onNavigateToConsentView = onNavigateToConsentView
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SurgeryStatusContent(
    admission: AdmissionDto,
    onNavigateToConsentView: (String) -> Unit
) {
    val patient = admission.patient
    val doctor = admission.doctor
    val currentStageIndex = stageIndex(admission.status)

    PatientHeader(admission = admission)

    Spacer(modifier = Modifier.height(20.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Surgery Timeline",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            STAGES.forEachIndexed { index, (title, subtitle) ->
                TimelineItem(
                    title = title,
                    subtitle = subtitle,
                    stageIndex = index,
                    currentStageIndex = currentStageIndex,
                    isLast = index == STAGES.lastIndex
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    DoctorCard(admission = admission)

    Spacer(modifier = Modifier.height(16.dp))

    val pendingConsent = (admission.consentForms ?: emptyList()).firstOrNull { it.status != "signed" }
    if (pendingConsent != null) {
        Button(
            onClick = { pendingConsent.id?.let(onNavigateToConsentView) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.Warning),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Pending Consent")
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { /* TODO: WhatsApp share stub */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Share")
        }
        OutlinedButton(
            onClick = { /* TODO: dial hospital */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Call")
        }
    }
}

@Composable
private fun PatientHeader(admission: AdmissionDto) {
    val patient = admission.patient
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Avatar(
                name = patient?.name ?: "?",
                size = 56.dp,
                backgroundColor = NorituraColors.Surface,
                contentColor = NorituraColors.PrimaryBlue
            )
            Column {
                Text(
                    text = patient?.name ?: "Unknown",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${patient?.age ?: "-"} yrs • ${patient?.gender ?: "-"}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Ward ${admission.ward ?: "-"} • Bed ${admission.bedNo ?: "-"}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DoctorCard(admission: AdmissionDto) {
    val doctor = admission.doctor
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Doctor Preview"
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NorituraColors.PrimaryBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = NorituraColors.PrimaryBlue
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Treating Surgeon",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = doctor?.name ?: "-",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = doctor?.hospitalName ?: "-",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    }
}

@Composable
private fun TimelineItem(
    title: String,
    subtitle: String,
    stageIndex: Int,
    currentStageIndex: Int,
    isLast: Boolean
) {
    val isCompleted = stageIndex < currentStageIndex
    val isCurrent = stageIndex == currentStageIndex
    val color = when {
        isCompleted -> NorituraColors.PostOp
        isCurrent -> NorituraColors.Warning
        else -> NorituraColors.TextTertiary
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else if (isCurrent) {
                    PulsingDot()
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NorituraColors.TextTertiary.copy(alpha = 0.5f))
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(
                            if (isCompleted) NorituraColors.PostOp
                            else NorituraColors.Divider
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.padding(bottom = if (isLast) 0.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = if (isCurrent) NorituraColors.TextPrimary else color,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color.White)
            .size((10 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private fun stageIndex(status: String?): Int {
    return when (status?.lowercase()) {
        "admitted" -> 0
        "pre-op" -> 1
        "in-surgery", "in_operation" -> 2
        "recovery", "post-op" -> 3
        "discharged" -> 4
        else -> 0
    }
}
