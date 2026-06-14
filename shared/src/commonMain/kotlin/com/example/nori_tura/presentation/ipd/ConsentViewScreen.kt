package com.example.nori_tura.presentation.ipd

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.ConsentSignRequest
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.SignaturePad
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.encodeSignatureToPngBase64
import com.example.nori_tura.util.openUrl

@Composable
fun ConsentViewScreen(
    consentId: String,
    viewModel: ConsentViewViewModel = viewModel(key = consentId) { ConsentViewViewModel(consentId) },
    onBack: () -> Unit,
    topBarInitials: String = "DR",
    topBarTitle: String = "Consent Form"
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = topBarInitials,
                title = topBarTitle,
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) { _ ->
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
                    onSign = { request ->
                        viewModel.signConsent(request)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConsentViewContent(
    consent: com.example.nori_tura.data.dto.ConsentFormDto,
    onSign: (ConsentSignRequest) -> Unit
) {
    val isSigned = consent.status == "signed"
    val content = consent.contentJson

    var witnessName by remember { mutableStateOf(consent.witnessName ?: "") }
    val parentSignaturePaths = remember { mutableStateListOf<List<Offset>>() }
    var parentSignatureDataUrl by remember { mutableStateOf("") }
    val witnessSignaturePaths = remember { mutableStateListOf<List<Offset>>() }
    var witnessSignatureDataUrl by remember { mutableStateOf("") }
    var acknowledged by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = content?.get("patient_name")?.toString()?.removeSurrounding("\"") ?: "Patient",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                StatusChip(label = if (isSigned) "Signed" else "Pending", isSigned = isSigned)

                InfoRow(label = "Form Type", value = consent.formType)
                InfoRow(label = "Procedure", value = content?.get("procedure")?.toString()?.removeSurrounding("\"") ?: "-")
                InfoRow(label = "Anesthesia", value = content?.get("anesthesia")?.toString()?.removeSurrounding("\"") ?: "-")
                InfoRow(
                    label = "Generated",
                    value = consent.generatedAt?.take(10) ?: "-"
                )
            }
        }

        if (!isSigned) {
            consent.pdfUrl?.let { pdfUrl ->
                OutlinedButton(
                    onClick = { openUrl(pdfUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Generated PDF")
                }
            }

            Text(
                text = "Parent / Guardian Signature",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            SignaturePad(
                modifier = Modifier.fillMaxWidth(),
                onPathsChange = { paths ->
                    parentSignaturePaths.clear()
                    parentSignaturePaths.addAll(paths)
                    parentSignatureDataUrl = if (paths.isNotEmpty()) {
                        encodeSignatureToPngBase64(paths, 800, 300)
                    } else {
                        ""
                    }
                }
            )

            Text(
                text = "Witness Signature (optional)",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            SignaturePad(
                modifier = Modifier.fillMaxWidth(),
                onPathsChange = { paths ->
                    witnessSignaturePaths.clear()
                    witnessSignaturePaths.addAll(paths)
                    witnessSignatureDataUrl = if (paths.isNotEmpty()) {
                        encodeSignatureToPngBase64(paths, 800, 300)
                    } else {
                        ""
                    }
                }
            )

            OutlinedTextField(
                value = witnessName,
                onValueChange = { witnessName = it },
                label = { Text("Witness Name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acknowledged,
                    onCheckedChange = { acknowledged = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NorituraColors.PrimaryBlue,
                        uncheckedColor = NorituraColors.Outline
                    )
                )
                Text(
                    text = "I have read and understood the information above and voluntarily consent to the procedure.",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Button(
                onClick = {
                    if (parentSignatureDataUrl.isNotBlank()) {
                        onSign(
                            ConsentSignRequest(
                                parentSignatureUrl = parentSignatureDataUrl,
                                witnessName = witnessName.takeIf { it.isNotBlank() },
                                witnessSignatureUrl = witnessSignatureDataUrl.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = parentSignatureDataUrl.isNotBlank() && acknowledged,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Consent")
            }
        } else {
            SignedSuccessCard(consent = consent)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SignedSuccessCard(
    consent: com.example.nori_tura.data.dto.ConsentFormDto
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
                    text = "Consent signed successfully",
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
private fun StatusChip(label: String, isSigned: Boolean) {
    val color = if (isSigned) NorituraColors.PostOp else NorituraColors.PreOp
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
