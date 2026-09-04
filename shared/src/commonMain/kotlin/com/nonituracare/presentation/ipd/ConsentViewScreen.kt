package com.nonituracare.presentation.ipd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.ConsentFormDto
import kotlinx.serialization.json.JsonObject
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.ui.theme.NorituraColors
import com.nonituracare.util.openUrl
import androidx.compose.foundation.Image
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.success_consent_generated
import org.jetbrains.compose.resources.painterResource

/**
 * Read-only view of a generated consent form.
 *
 * Consent forms are no longer signed inside the app: a nurse generates and
 * downloads the PDF here, prints it, and it gets signed by hand at the
 * hospital. This screen therefore never collects a signature or OTP — it
 * shows what was generated and offers a download/re-download in either
 * language. `status == "signed"` only ever appears on historical rows from
 * before this change and is shown as a read-only banner.
 */
@Composable
fun ConsentViewScreen(
    consentId: String,
    viewModel: ConsentViewViewModel = viewModel(key = consentId) { ConsentViewViewModel(consentId) },
    onBack: () -> Unit,
    topBarInitials: String = "DR",
    topBarTitle: String = "Consent Form"
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    LaunchedEffect(downloadState) {
        val ready = downloadState as? ConsentViewViewModel.DownloadState.Ready
        if (ready != null) {
            openUrl(ready.pdfUrl)
            viewModel.resetDownloadState()
        }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = topBarInitials,
                title = topBarTitle,
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        when (val state = uiState) {
            is ConsentViewViewModel.UiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }

            is ConsentViewViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadConsent() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is ConsentViewViewModel.UiState.Success -> {
                ConsentViewContent(
                    consent = state.consent,
                    downloadState = downloadState,
                    onDownload = { language -> viewModel.downloadPdf(language) }
                )
            }
        }
    }
}

@Composable
private fun ConsentViewContent(
    consent: ConsentFormDto,
    downloadState: ConsentViewViewModel.DownloadState,
    onDownload: (String?) -> Unit
) {
    val isHistoricallySigned = consent.status == "signed"
    val content = consent.contentJson
    var selectedLanguage by remember { mutableStateOf(consent.language ?: "English") }
    val isDownloading = downloadState is ConsentViewViewModel.DownloadState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = content.string("form_title") ?: "Informed Consent",
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                content.string("hospital_name")?.let {
                    Text(
                        text = it,
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider(color = NorituraColors.Divider)
                Text(
                    text = content.string("patient_name") ?: "Patient",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRowCompact(label = "Age", value = "${content.string("age") ?: "-"} yrs")
                    InfoRowCompact(label = "Gender", value = content.string("gender") ?: "-")
                }
                InfoRowCompact(label = "Parent / Guardian", value = content.string("parent_name") ?: "-")
                InfoRowCompact(label = "Parent Phone", value = content.string("parent_phone") ?: "-")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(label = consent.language ?: "English", tone = NorituraColors.PrimaryBlue)
                    if (isHistoricallySigned) {
                        StatusChip(label = "Signed (historical)", tone = NorituraColors.PostOp)
                    } else {
                        StatusChip(label = "Generated", tone = NorituraColors.PostOp)
                    }
                }
            }
        }

        ConsentDocumentSection(title = "Diagnosis & Procedure") {
            InfoRow(label = "Diagnosis", value = content.string("diagnosis") ?: "-")
            InfoRow(label = "Proposed Procedure", value = content.string("procedure") ?: "-")
            InfoRow(label = "Anesthesia Plan", value = content.string("anesthesia") ?: "-")
            InfoRow(label = "Surgeon", value = content.string("surgeon_name") ?: "-")
        }

        ConsentDocumentSection(title = "Risks, Benefits & Alternatives") {
            ParagraphBlock(label = "Risks", text = content.string("risks"))
            ParagraphBlock(label = "Benefits", text = content.string("benefits"))
            ParagraphBlock(label = "Alternatives", text = content.string("alternatives"))
            ParagraphBlock(label = "Post-operative Care", text = content.string("post_op_care"))
        }

        ConsentDocumentSection(title = "Specific Consents") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoRowCompact(label = "Anesthesia", value = content.string("consent_for_anesthesia") ?: "-")
                InfoRowCompact(label = "Blood Products", value = content.string("consent_for_blood_products") ?: "-")
            }
            InfoRowCompact(
                label = "Blood transfusion",
                value = consent.bloodTransfusionConsent ?: "Not applicable / not asked"
            )
            InfoRowCompact(
                label = "Photography",
                value = listOfNotNull(
                    "Medical record".takeIf { consent.photoConsentMedicalRecord },
                    "Teaching/audit".takeIf { consent.photoConsentDeidentifiedTeaching },
                    "Publication".takeIf { consent.photoConsentPublication }
                ).joinToString(", ").ifBlank { "None" }
            )
        }

        if (!isHistoricallySigned) {
            Text(
                text = "This form is printed and signed by hand at the hospital — it is not signed on the platform.",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Download language",
                style = MaterialTheme.typography.labelMedium,
                color = NorituraColors.TextSecondary
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("English", "Hindi").forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selectedLanguage == option,
                        onClick = { selectedLanguage = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(option)
                    }
                }
            }

            if (downloadState is ConsentViewViewModel.DownloadState.Error) {
                Text(
                    text = downloadState.message,
                    color = NorituraColors.PreOp,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { onDownload(selectedLanguage) },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Download PDF ($selectedLanguage)")
                }
            }

            consent.pdfUrl?.let { pdfUrl ->
                Image(
                    painter = painterResource(Res.drawable.success_consent_generated),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(110.dp)
                )
                OutlinedButton(
                    onClick = { openUrl(pdfUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Last Generated PDF")
                }
            }
        } else {
            SignedSuccessCard(consent = consent)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

private fun JsonObject?.string(key: String): String? {
    return this?.get(key)?.toString()?.removeSurrounding("\"")
}

@Composable
private fun ConsentDocumentSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            HorizontalDivider(color = NorituraColors.Divider)
            content()
        }
    }
}

@Composable
private fun ParagraphBlock(label: String, text: String?) {
    if (text.isNullOrBlank()) return
    Column {
        Text(
            text = label,
            color = NorituraColors.TextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = text,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun InfoRowCompact(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = NorituraColors.TextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun SignedSuccessCard(
    consent: ConsentFormDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.PrimaryBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Signed",
                    tint = NorituraColors.PostOp
                )
                Text(
                    text = "Consent signed electronically (historical record)",
                    color = NorituraColors.PostOp,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            consent.signedAt?.let {
                Text(
                    text = "Signed on ${it.take(10)}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            consent.witnessName?.let {
                Text(
                    text = "Witness: $it",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            consent.witnessMobile?.let {
                Text(
                    text = "Witness Phone: $it",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = consent.signedPdfUrl != null) {
                OutlinedButton(
                    onClick = { consent.signedPdfUrl?.let { openUrl(it) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Signed PDF")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = NorituraColors.TextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun StatusChip(label: String, tone: androidx.compose.ui.graphics.Color) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = label,
            color = tone,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
