package com.example.nori_tura.presentation.ipd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.OutlinedTextField
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
import com.example.nori_tura.data.dto.ConsentFormCreateRequest
import com.example.nori_tura.data.dto.SurgicalTemplateDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.MedicalAutoCompleteTextField
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.TemplatePickerDialog
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ConsentFormScreen(
    admissionId: String,
    viewModel: ConsentFormViewModel = viewModel { ConsentFormViewModel() },
    onBack: () -> Unit,
    onConsentCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.templates.collectAsState()

    var selectedTemplate by remember { mutableStateOf<SurgicalTemplateDto?>(null) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    // Core clinical fields
    var formType by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var procedure by remember { mutableStateOf("") }
    var procedureDescription by remember { mutableStateOf("") }
    var anesthesia by remember { mutableStateOf("") }

    // Risks & recovery
    var risks by remember { mutableStateOf("") }
    var materialRisks by remember { mutableStateOf("") }
    var possibleComplications by remember { mutableStateOf("") }
    var benefits by remember { mutableStateOf("") }
    var alternatives by remember { mutableStateOf("") }
    var postOpCare by remember { mutableStateOf("") }
    var expectedRecovery by remember { mutableStateOf("") }

    // Hospital information
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var hospitalContact by remember { mutableStateOf("") }
    var hospitalRegNo by remember { mutableStateOf("") }

    // Doctor information
    var doctorQualification by remember { mutableStateOf("") }
    var doctorRegNo by remember { mutableStateOf("") }

    // Guardian
    var guardianRelationship by remember { mutableStateOf("") }

    // Specific consents
    var consentForAnesthesia by remember { mutableStateOf(true) }
    var consentForBloodProducts by remember { mutableStateOf(false) }
    var consentForPhotography by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ConsentFormViewModel.UiState.Success) {
            val consentId = (uiState as ConsentFormViewModel.UiState.Success).response.consentForm.id
            onConsentCreated(consentId)
            viewModel.resetState()
        }
    }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            templates = templates,
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                selectedTemplate = template
                val fields = viewModel.applyTemplate(template)
                formType = fields.formType
                procedure = fields.procedure
                anesthesia = fields.anesthesia
                procedureDescription = fields.procedureDescription
                risks = fields.risks
                materialRisks = fields.materialRisks
                possibleComplications = fields.possibleComplications
                benefits = fields.benefits
                alternatives = fields.alternatives
                postOpCare = fields.postOpCare
                expectedRecovery = fields.expectedRecovery
                showTemplatePicker = false
            }
        )
    }

    val isFormValid = formType.isNotBlank() && diagnosis.isNotBlank() &&
        procedure.isNotBlank() && anesthesia.isNotBlank() &&
        risks.isNotBlank() && benefits.isNotBlank() &&
        alternatives.isNotBlank() && postOpCare.isNotBlank()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Consent Form",
                onBack = onBack
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Admission: $admissionId",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )

            // ── Template Selector ───────────────────────────────────────
            ConsentTemplateSelectorCard(
                selectedTemplateName = selectedTemplate?.name,
                onClick = { showTemplatePicker = true }
            )

            // ── Core Info ──────────────────────────────────────────────
            ConsentSection(title = "Core Information") {
                ConsentField(
                    value = formType,
                    onValueChange = { formType = it },
                    label = "Form Type *",
                    placeholder = "Surgical Consent / Anaesthesia Consent"
                )
                ConsentField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = "Diagnosis *",
                    placeholder = "e.g. Appendicitis, Inguinal Hernia",
                    autocomplete = true
                )
                ConsentField(
                    value = procedure,
                    onValueChange = { procedure = it },
                    label = "Proposed Procedure *",
                    placeholder = "e.g. Laparoscopic Appendicectomy",
                    autocomplete = true
                )
                ConsentField(
                    value = procedureDescription,
                    onValueChange = { procedureDescription = it },
                    label = "Procedure Description",
                    placeholder = "Step-by-step or lay description for patient",
                    minLines = 3,
                    autocomplete = true
                )
                ConsentField(
                    value = anesthesia,
                    onValueChange = { anesthesia = it },
                    label = "Anaesthesia *",
                    placeholder = "e.g. General Anaesthesia, Spinal",
                    autocomplete = true
                )
            }

            // ── Risks & Outcomes ───────────────────────────────────────
            ConsentSection(title = "Risks, Benefits & Alternatives") {
                ConsentField(
                    value = risks,
                    onValueChange = { risks = it },
                    label = "General Risks *",
                    placeholder = "Common risks (bleeding, infection, etc.)",
                    minLines = 3,
                    autocomplete = true
                )
                ConsentField(
                    value = materialRisks,
                    onValueChange = { materialRisks = it },
                    label = "Material / Serious Risks",
                    placeholder = "Risks of particular significance for this patient",
                    minLines = 3,
                    autocomplete = true
                )
                ConsentField(
                    value = possibleComplications,
                    onValueChange = { possibleComplications = it },
                    label = "Possible Complications",
                    placeholder = "Procedure-specific complications",
                    minLines = 3,
                    autocomplete = true
                )
                ConsentField(
                    value = benefits,
                    onValueChange = { benefits = it },
                    label = "Benefits *",
                    placeholder = "Expected benefits of the procedure",
                    minLines = 2,
                    autocomplete = true
                )
                ConsentField(
                    value = alternatives,
                    onValueChange = { alternatives = it },
                    label = "Alternatives *",
                    placeholder = "Alternative treatment options including no treatment",
                    minLines = 2,
                    autocomplete = true
                )
            }

            // ── Recovery ───────────────────────────────────────────────
            ConsentSection(title = "Post-operative Care & Recovery") {
                ConsentField(
                    value = postOpCare,
                    onValueChange = { postOpCare = it },
                    label = "Post-operative Care Instructions *",
                    placeholder = "Wound care, activity restrictions, follow-up",
                    minLines = 3,
                    autocomplete = true
                )
                ConsentField(
                    value = expectedRecovery,
                    onValueChange = { expectedRecovery = it },
                    label = "Expected Recovery",
                    placeholder = "Estimated recovery timeline",
                    minLines = 2,
                    autocomplete = true
                )
            }

            // ── Hospital ───────────────────────────────────────────────
            ConsentSection(title = "Hospital / Facility Details") {
                ConsentField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = "Hospital Name",
                    placeholder = "Name of the facility"
                )
                ConsentField(
                    value = hospitalAddress,
                    onValueChange = { hospitalAddress = it },
                    label = "Hospital Address",
                    placeholder = "Full address"
                )
                ConsentField(
                    value = hospitalContact,
                    onValueChange = { hospitalContact = it },
                    label = "Hospital Contact",
                    placeholder = "Phone number"
                )
                ConsentField(
                    value = hospitalRegNo,
                    onValueChange = { hospitalRegNo = it },
                    label = "Hospital Registration No.",
                    placeholder = "e.g. MH/HOS/2024/00123"
                )
            }

            // ── Doctor ─────────────────────────────────────────────────
            ConsentSection(title = "Surgeon / Doctor Details") {
                ConsentField(
                    value = doctorQualification,
                    onValueChange = { doctorQualification = it },
                    label = "Qualification",
                    placeholder = "e.g. MS (Gen Surgery), MCh (Paed Surgery)"
                )
                ConsentField(
                    value = doctorRegNo,
                    onValueChange = { doctorRegNo = it },
                    label = "Medical Registration No.",
                    placeholder = "e.g. MCI-12345"
                )
            }

            // ── Guardian ───────────────────────────────────────────────
            ConsentSection(title = "Guardian / Representative") {
                ConsentField(
                    value = guardianRelationship,
                    onValueChange = { guardianRelationship = it },
                    label = "Relationship to Patient",
                    placeholder = "e.g. Father, Mother, Spouse, Self"
                )
            }

            // ── Consent Checkboxes ─────────────────────────────────────
            ConsentSection(title = "Specific Consents") {
                ConsentCheckbox(
                    text = "Consent for anaesthesia / sedation",
                    checked = consentForAnesthesia,
                    onCheckedChange = { consentForAnesthesia = it }
                )
                ConsentCheckbox(
                    text = "Consent for blood / blood product transfusion if required",
                    checked = consentForBloodProducts,
                    onCheckedChange = { consentForBloodProducts = it }
                )
                ConsentCheckbox(
                    text = "Consent for clinical photography / recording for treatment records",
                    checked = consentForPhotography,
                    onCheckedChange = { consentForPhotography = it }
                )
            }

            if (uiState is ConsentFormViewModel.UiState.Error) {
                Text(
                    text = (uiState as ConsentFormViewModel.UiState.Error).message,
                    color = NorituraColors.Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    viewModel.createConsentForm(
                        ConsentFormCreateRequest(
                            admissionId = admissionId,
                            formType = formType,
                            diagnosis = diagnosis,
                            procedure = procedure,
                            procedureDescription = procedureDescription.takeIf { it.isNotBlank() },
                            anesthesia = anesthesia,
                            risks = risks,
                            materialRisks = materialRisks.takeIf { it.isNotBlank() },
                            possibleComplications = possibleComplications.takeIf { it.isNotBlank() },
                            benefits = benefits,
                            alternatives = alternatives,
                            postOpCare = postOpCare,
                            expectedRecovery = expectedRecovery.takeIf { it.isNotBlank() },
                            hospitalName = hospitalName.takeIf { it.isNotBlank() },
                            hospitalAddress = hospitalAddress.takeIf { it.isNotBlank() },
                            hospitalContact = hospitalContact.takeIf { it.isNotBlank() },
                            hospitalRegistrationNumber = hospitalRegNo.takeIf { it.isNotBlank() },
                            doctorQualification = doctorQualification.takeIf { it.isNotBlank() },
                            doctorRegistrationNumber = doctorRegNo.takeIf { it.isNotBlank() },
                            guardianRelationship = guardianRelationship.takeIf { it.isNotBlank() },
                            consentForAnesthesia = consentForAnesthesia,
                            consentForBloodProducts = consentForBloodProducts,
                            consentForPhotography = consentForPhotography
                        )
                    )
                },
                enabled = uiState !is ConsentFormViewModel.UiState.Loading && isFormValid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState is ConsentFormViewModel.UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Generate Consent Form", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConsentTemplateSelectorCard(
    selectedTemplateName: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Template",
                    style = MaterialTheme.typography.labelMedium,
                    color = NorituraColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedTemplateName ?: "Custom form",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selectedTemplateName != null) NorituraColors.PrimaryBlue else NorituraColors.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Select template",
                tint = NorituraColors.TextTertiary
            )
        }
    }
}

@Composable
private fun ConsentSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NorituraColors.PrimaryBlue
            )
            content()
        }
    }
}

@Composable
private fun ConsentField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    minLines: Int = 1,
    autocomplete: Boolean = false,
    fieldType: String? = null
) {
    if (autocomplete) {
        MedicalAutoCompleteTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            maxLines = if (minLines > 1) minLines + 2 else 1,
            singleLine = minLines == 1,
            fieldType = fieldType
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder, color = NorituraColors.TextTertiary) }) else null,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            maxLines = if (minLines > 1) minLines + 2 else 1,
            singleLine = minLines == 1,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConsentCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
