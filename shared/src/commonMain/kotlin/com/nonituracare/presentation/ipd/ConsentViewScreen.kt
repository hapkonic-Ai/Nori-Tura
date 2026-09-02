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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.AuthRepository
import com.nonituracare.data.dto.ConsentOtpVerifyRequest
import kotlinx.serialization.json.JsonObject
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.ui.theme.NorituraColors
import com.nonituracare.util.openUrl

@Composable
fun ConsentViewScreen(
    consentId: String,
    viewModel: ConsentViewViewModel = viewModel(key = consentId) { ConsentViewViewModel(consentId) },
    onBack: () -> Unit,
    topBarInitials: String = "DR",
    topBarTitle: String = "Consent Form"
) {
    val uiState by viewModel.uiState.collectAsState()
    val otpState by viewModel.otpState.collectAsState()
    val witnessOtpState by viewModel.witnessOtpState.collectAsState()

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
                    otpState = otpState,
                    witnessOtpState = witnessOtpState,
                    onRequestOtp = { viewModel.requestOtp() },
                    onRequestWitnessOtp = { witnessMobile ->
                        viewModel.requestWitnessOtp(witnessMobile)
                    },
                    onResetWitnessOtp = { viewModel.resetWitnessOtp() },
                    onVerifyOtp = { request ->
                        viewModel.verifyOtp(request)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConsentViewContent(
    consent: com.nonituracare.data.dto.ConsentFormDto,
    otpState: ConsentViewViewModel.OtpState,
    witnessOtpState: ConsentViewViewModel.OtpState,
    onRequestOtp: () -> Unit,
    onRequestWitnessOtp: (String) -> Unit,
    onResetWitnessOtp: () -> Unit,
    onVerifyOtp: (ConsentOtpVerifyRequest) -> Unit
) {
    val isSigned = consent.status == "signed"
    val content = consent.contentJson

    var otp by remember { mutableStateOf("") }
    var witnessOtp by remember { mutableStateOf("") }
    var witnessOn by remember { mutableStateOf(false) }
    var witnessName by remember { mutableStateOf(consent.witnessName ?: "") }
    var witnessRelationship by remember { mutableStateOf(consent.witnessRelationship ?: "") }
    var witnessMobile by remember { mutableStateOf(consent.witnessMobile ?: "") }
    var acknowledged by remember { mutableStateOf(false) }
    var signerAttested by remember { mutableStateOf(false) }
    val witnessPhoneValid = witnessMobile.matches(Regex("^\\+91[0-9]{10}$"))
    // The handover notice is an instruction to hospital staff only; a parent
    // viewing this screen on their own device must not see it.
    val isStaff = remember {
        AuthRepository().getRole()?.lowercase() in listOf("surgeon", "nurse")
    }

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
                StatusChip(label = if (isSigned) "Signed" else "Pending", isSigned = isSigned)
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

        ConsentDocumentSection(title = "Rights & Additional Consents") {
            ParagraphBlock(label = "Refusal Consequences", text = content.string("refusal_consequences"))
            ParagraphBlock(label = "Right to Withdraw", text = content.string("right_to_withdraw"))
            ParagraphBlock(label = "Privacy", text = content.string("privacy_statement"))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoRowCompact(label = "Anesthesia", value = content.string("consent_for_anesthesia") ?: "-")
                InfoRowCompact(label = "Blood Products", value = content.string("consent_for_blood_products") ?: "-")
                InfoRowCompact(label = "Photography", value = content.string("consent_for_photography") ?: "-")
            }
        }

        ConsentDocumentSection(title = "Declarations") {
            ParagraphBlock(label = "Parent / Guardian Declaration", text = content.string("parent_guardian_declaration"))
            ParagraphBlock(label = "Doctor Declaration", text = content.string("doctor_declaration"))
            ParagraphBlock(label = "Statutory Reference", text = content.string("statutory_reference"))
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
                text = "Parent Verification",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = "An OTP will be sent to the parent / guardian's registered phone number. Signing happens only after the parent shares the OTP.",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            when (val state = otpState) {
                is ConsentViewViewModel.OtpState.Sent -> {
                    Text(
                        text = "OTP sent to ${state.phone}",
                        color = NorituraColors.PostOp,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    state.devOtp?.let {
                        Text(
                            text = "Dev OTP: $it",
                            color = NorituraColors.TextTertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isStaff) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NorituraColors.PrimaryBlueLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "Hand the device to the parent / patient to review the summary and enter their OTP.",
                                color = NorituraColors.PrimaryBlue,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                is ConsentViewViewModel.OtpState.Error -> {
                    Text(
                        text = state.message,
                        color = NorituraColors.PreOp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }

            OutlinedButton(
                onClick = onRequestOtp,
                enabled = otpState !is ConsentViewViewModel.OtpState.Sending &&
                    otpState !is ConsentViewViewModel.OtpState.Verifying,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (otpState is ConsentViewViewModel.OtpState.Sent) "Resend OTP"
                    else if (otpState is ConsentViewViewModel.OtpState.Sending) "Sending OTP…"
                    else "Send OTP"
                )
            }

            OutlinedTextField(
                value = otp,
                onValueChange = { input ->
                    if (input.length <= 6 && input.all { it.isDigit() }) otp = input
                },
                label = { Text("6-digit OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add witness (optional)",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Switch(
                    checked = witnessOn,
                    onCheckedChange = { checked ->
                        witnessOn = checked
                        if (!checked) {
                            witnessName = ""
                            witnessRelationship = ""
                            witnessMobile = ""
                            witnessOtp = ""
                            onResetWitnessOtp()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NorituraColors.Surface,
                        checkedTrackColor = NorituraColors.PrimaryBlue,
                        uncheckedThumbColor = NorituraColors.TextTertiary
                    )
                )
            }

            AnimatedVisibility(visible = witnessOn) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = witnessName,
                        onValueChange = { witnessName = it },
                        label = { Text("Witness Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = witnessRelationship,
                        onValueChange = { witnessRelationship = it },
                        label = { Text("Relationship to Patient") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = witnessMobile,
                        onValueChange = {
                            if (it != witnessMobile) {
                                witnessOtp = ""
                                onResetWitnessOtp()
                            }
                            witnessMobile = it
                        },
                        label = { Text("Witness Phone (+91…)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!witnessPhoneValid) {
                        Text(
                            text = "Enter a valid +91 mobile number to verify the witness",
                            color = NorituraColors.TextTertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        when (val wState = witnessOtpState) {
                            is ConsentViewViewModel.OtpState.Sent -> {
                                Text(
                                    text = "Witness OTP sent to ${wState.phone}",
                                    color = NorituraColors.PostOp,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                wState.devOtp?.let {
                                    Text(
                                        text = "Dev OTP: $it",
                                        color = NorituraColors.TextTertiary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            is ConsentViewViewModel.OtpState.Error -> {
                                Text(
                                    text = wState.message,
                                    color = NorituraColors.PreOp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            else -> {}
                        }

                        OutlinedButton(
                            onClick = { onRequestWitnessOtp(witnessMobile) },
                            enabled = witnessOtpState !is ConsentViewViewModel.OtpState.Sending &&
                                witnessOtpState !is ConsentViewViewModel.OtpState.Verifying,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (witnessOtpState is ConsentViewViewModel.OtpState.Sent) "Resend Witness OTP"
                                else if (witnessOtpState is ConsentViewViewModel.OtpState.Sending) "Sending OTP…"
                                else "Send OTP to Witness"
                            )
                        }

                        OutlinedTextField(
                            value = witnessOtp,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) witnessOtp = input
                            },
                            label = { Text("Witness 6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = signerAttested,
                    onCheckedChange = { signerAttested = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NorituraColors.PrimaryBlue,
                        uncheckedColor = NorituraColors.Outline
                    )
                )
                Text(
                    text = "I have read the consent terms and am entering the OTP myself.",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Button(
                onClick = {
                    onVerifyOtp(
                        ConsentOtpVerifyRequest(
                            otp = otp,
                            witnessName = witnessName.takeIf { witnessOn && it.isNotBlank() },
                            witnessRelationship = witnessRelationship.takeIf { witnessOn && it.isNotBlank() },
                            witnessMobile = witnessMobile.takeIf { witnessOn && witnessPhoneValid },
                            witnessOtp = witnessOtp.takeIf { witnessOn && it.length == 6 },
                            signerAttested = signerAttested
                        )
                    )
                },
                enabled = otp.length == 6 &&
                    acknowledged &&
                    signerAttested &&
                    (!witnessOn || (witnessName.isNotBlank() && witnessPhoneValid && witnessOtp.length == 6)) &&
                    (otpState is ConsentViewViewModel.OtpState.Sent ||
                        otpState is ConsentViewViewModel.OtpState.Error) &&
                    otpState !is ConsentViewViewModel.OtpState.Verifying,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (otpState is ConsentViewViewModel.OtpState.Verifying) "Verifying…"
                    else "Verify OTP & Sign Consent"
                )
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
    consent: com.nonituracare.data.dto.ConsentFormDto
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
