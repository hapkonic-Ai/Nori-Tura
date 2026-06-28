package com.example.nori_tura.presentation.ipd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.DischargeSummaryCreateRequest
import com.example.nori_tura.data.dto.IntraOpNoteCreateRequest
import com.example.nori_tura.data.dto.PostOpNoteCreateRequest
import com.example.nori_tura.data.dto.PreOpNoteCreateRequest
import com.example.nori_tura.data.dto.SurgicalTemplateDto
import com.example.nori_tura.data.dto.WardRoundNoteCreateRequest
import com.example.nori_tura.presentation.components.TemplatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionDetailScreen(
    admissionId: String,
    viewModel: AdmissionDetailViewModel = viewModel(key = admissionId) { AdmissionDetailViewModel(admissionId) },
    onBack: () -> Unit,
    onNavigateToConsentForm: () -> Unit,
    onNavigateToConsentView: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AdmissionDetailViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AdmissionDetailViewModel.UiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admission Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AdmissionDetailViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AdmissionDetailViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load admission")
                        Button(onClick = { viewModel.loadAdmission() }) {
                            Text("Retry")
                        }
                    }
                }

                is AdmissionDetailViewModel.UiState.Success -> {
                    AdmissionDetailContent(
                        admission = state.admission,
                        templates = templates,
                        viewModel = viewModel,
                        onNavigateToConsentForm = onNavigateToConsentForm,
                        onNavigateToConsentView = onNavigateToConsentView
                    )
                }
            }
        }
    }
}

@Composable
private fun AdmissionDetailContent(
    admission: AdmissionDto,
    templates: List<SurgicalTemplateDto>,
    viewModel: AdmissionDetailViewModel,
    onNavigateToConsentForm: () -> Unit,
    onNavigateToConsentView: (String) -> Unit
) {
    var showPreOp by remember { mutableStateOf(false) }
    var showIntraOp by remember { mutableStateOf(false) }
    var showPostOp by remember { mutableStateOf(false) }
    var showWardRound by remember { mutableStateOf(false) }
    var showDischarge by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = admission.patient?.name ?: "Patient",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Status: ${admission.status ?: "-"} | Urgency: ${admission.urgency ?: "-"}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Ward: ${admission.ward ?: "-"} | Bed: ${admission.bedNo ?: "-"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("Pre-Op Notes")
        for (note in admission.preOpNotes ?: emptyList()) {
            NoteCard {
                Text("Procedure: ${note.procedure}", fontWeight = FontWeight.SemiBold)
                note.approach?.let { Text("Approach: $it") }
                note.anaesthesia?.let { Text("Anaesthesia: $it") }
                if (note.investigations.isNotEmpty()) {
                    Text("Investigations: ${note.investigations.joinToString()}")
                }
                note.riskLevel?.let { Text("Risk: $it") }
            }
        }
        OutlinedButton(onClick = { showPreOp = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Pre-Op Note")
        }

        SectionTitle("Intra-Op Notes")
        for (note in admission.intraOpNotes ?: emptyList()) {
            NoteCard {
                Text("Procedure: ${note.procedureDone}", fontWeight = FontWeight.SemiBold)
                note.findings?.let { Text("Findings: $it") }
                note.technique?.let { Text("Technique: $it") }
                note.complications?.let { Text("Complications: $it") }
                note.bloodLoss?.let { Text("Blood Loss: $it") }
            }
        }
        OutlinedButton(onClick = { showIntraOp = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Intra-Op Note")
        }

        SectionTitle("Post-Op Notes")
        for (note in admission.postOpNotes ?: emptyList()) {
            NoteCard {
                Text("Day ${note.dayNumber}: ${note.condition}", fontWeight = FontWeight.SemiBold)
                Text("Vitals: ${note.vitalsJson.entries.joinToString { "${it.key}=${it.value}" }}")
                note.woundStatus?.let { Text("Wound: $it") }
                note.painScore?.let { Text("Pain: $it/10") }
                note.diet?.let { Text("Diet: $it") }
            }
        }
        OutlinedButton(onClick = { showPostOp = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Post-Op Note")
        }

        SectionTitle("Ward Round Notes")
        for (note in admission.wardRoundNotes ?: emptyList()) {
            NoteCard {
                Text("SOAP", fontWeight = FontWeight.SemiBold)
                note.subjective?.let { Text("S: $it") }
                note.objective?.let { Text("O: $it") }
                note.assessment?.let { Text("A: $it") }
                note.plan?.let { Text("P: $it") }
                if (note.readyForDischarge) {
                    Text("Ready for discharge", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        OutlinedButton(onClick = { showWardRound = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Ward Round Note")
        }

        SectionTitle("Discharge Summary")
        (admission.dischargeSummaries ?: emptyList()).firstOrNull()?.let { summary ->
            NoteCard {
                Text("Condition: ${summary.conditionAtDischarge}", fontWeight = FontWeight.SemiBold)
                Text("Procedure: ${summary.procedureSummary}")
                summary.followUpDate?.let { Text("Follow-up: $it") }
                summary.redFlags?.let { Text("Red Flags: $it") }
            }
        }
        if (admission.dischargeSummaries.isNullOrEmpty()) {
            OutlinedButton(onClick = { showDischarge = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Discharge Patient")
            }
        }

        SectionTitle("Consent Forms")
        for (consent in admission.consentForms ?: emptyList()) {
            ConsentListCard(
                consent = consent,
                onClick = { consent.id.let(onNavigateToConsentView) }
            )
        }
        OutlinedButton(onClick = onNavigateToConsentForm, modifier = Modifier.fillMaxWidth()) {
            Text("Add Consent Form")
        }
    }

    if (showPreOp) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPreOp = false }) {
            PreOpForm(
                templates = templates,
                onDismiss = { showPreOp = false },
                onSave = { request ->
                    viewModel.createPreOpNote(request)
                    showPreOp = false
                }
            )
        }
    }

    if (showIntraOp) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showIntraOp = false }) {
            IntraOpForm(
                templates = templates,
                onDismiss = { showIntraOp = false },
                onSave = { request ->
                    viewModel.createIntraOpNote(request)
                    showIntraOp = false
                }
            )
        }
    }

    if (showPostOp) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPostOp = false }) {
            PostOpForm(
                onDismiss = { showPostOp = false },
                onSave = { request ->
                    viewModel.createPostOpNote(request)
                    showPostOp = false
                }
            )
        }
    }

    if (showWardRound) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showWardRound = false }) {
            WardRoundForm(
                onDismiss = { showWardRound = false },
                onSave = { request ->
                    viewModel.createWardRoundNote(request)
                    showWardRound = false
                }
            )
        }
    }

    if (showDischarge) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDischarge = false }) {
            DischargeForm(
                onDismiss = { showDischarge = false },
                onSave = { request ->
                    viewModel.createDischargeSummary(request)
                    showDischarge = false
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun NoteCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun ConsentListCard(
    consent: com.example.nori_tura.data.dto.ConsentFormDto,
    onClick: () -> Unit
) {
    val statusColor = if (consent.status == "signed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consent.formType,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = consent.status?.replaceFirstChar { it.uppercase() } ?: "Pending",
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generated: ${consent.generatedAt?.take(10) ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreOpForm(
    templates: List<SurgicalTemplateDto>,
    onDismiss: () -> Unit,
    onSave: (PreOpNoteCreateRequest) -> Unit
) {
    var procedure by remember { mutableStateOf("") }
    var approach by remember { mutableStateOf("") }
    var anaesthesia by remember { mutableStateOf("") }
    var investigations by remember { mutableStateOf("") }
    var riskLevel by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var showTemplatePicker by remember { mutableStateOf(false) }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            templates = templates,
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                procedure = template.procedure
                approach = template.approach ?: approach
                anaesthesia = template.anaesthesia.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: anaesthesia
                investigations = (investigations.split(",").map { it.trim() }.filter { it.isNotBlank() } +
                    template.investigations).distinct().joinToString(", ")
                riskLevel = template.riskLevel ?: riskLevel
                instructions = template.specialInstructions ?: instructions
                showTemplatePicker = false
            }
        )
    }

    FormCard(title = "Pre-Op Note") {
        OutlinedButton(
            onClick = { showTemplatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply from Template")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FormTextField(procedure, { procedure = it }, "Procedure *")
        FormTextField(approach, { approach = it }, "Approach")
        FormTextField(anaesthesia, { anaesthesia = it }, "Anaesthesia")
        FormTextField(investigations, { investigations = it }, "Investigations (comma separated)")
        FormTextField(riskLevel, { riskLevel = it }, "Risk Level")
        FormTextField(instructions, { instructions = it }, "Special Instructions")
        FormButtons(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    PreOpNoteCreateRequest(
                        procedure = procedure,
                        approach = approach.takeIf { it.isNotBlank() },
                        anaesthesia = anaesthesia.takeIf { it.isNotBlank() },
                        investigations = investigations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        riskLevel = riskLevel.takeIf { it.isNotBlank() },
                        specialInstructions = instructions.takeIf { it.isNotBlank() }
                    )
                )
            },
            enabled = procedure.isNotBlank()
        )
    }
}

@Composable
private fun IntraOpForm(
    templates: List<SurgicalTemplateDto>,
    onDismiss: () -> Unit,
    onSave: (IntraOpNoteCreateRequest) -> Unit
) {
    var procedure by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var technique by remember { mutableStateOf("") }
    var complications by remember { mutableStateOf("") }
    var bloodLoss by remember { mutableStateOf("") }
    var otStart by remember { mutableStateOf("") }
    var otEnd by remember { mutableStateOf("") }
    var showTemplatePicker by remember { mutableStateOf(false) }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            templates = templates,
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                procedure = template.procedure
                technique = template.technique ?: technique
                showTemplatePicker = false
            }
        )
    }

    FormCard(title = "Intra-Op Note") {
        OutlinedButton(
            onClick = { showTemplatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply from Template")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FormTextField(procedure, { procedure = it }, "Procedure Done *")
        FormTextField(findings, { findings = it }, "Findings")
        FormTextField(technique, { technique = it }, "Technique")
        FormTextField(complications, { complications = it }, "Complications")
        FormTextField(bloodLoss, { bloodLoss = it }, "Blood Loss")
        FormTextField(otStart, { otStart = it }, "OT Start (ISO)")
        FormTextField(otEnd, { otEnd = it }, "OT End (ISO)")
        FormButtons(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    IntraOpNoteCreateRequest(
                        procedureDone = procedure,
                        findings = findings.takeIf { it.isNotBlank() },
                        technique = technique.takeIf { it.isNotBlank() },
                        complications = complications.takeIf { it.isNotBlank() },
                        bloodLoss = bloodLoss.takeIf { it.isNotBlank() },
                        otStart = otStart.takeIf { it.isNotBlank() },
                        otEnd = otEnd.takeIf { it.isNotBlank() }
                    )
                )
            },
            enabled = procedure.isNotBlank()
        )
    }
}

@Composable
private fun PostOpForm(
    onDismiss: () -> Unit,
    onSave: (PostOpNoteCreateRequest) -> Unit
) {
    var day by remember { mutableStateOf("1") }
    var condition by remember { mutableStateOf("") }
    var vitals by remember { mutableStateOf("") }
    var wound by remember { mutableStateOf("") }
    var pain by remember { mutableStateOf("") }
    var diet by remember { mutableStateOf("") }

    FormCard(title = "Post-Op Note") {
        FormTextField(day, { day = it }, "Day Number *")
        FormTextField(condition, { condition = it }, "Condition *")
        FormTextField(vitals, { vitals = it }, "Vitals (key=value, comma)")
        FormTextField(wound, { wound = it }, "Wound Status")
        FormTextField(pain, { pain = it }, "Pain Score (0-10)")
        FormTextField(diet, { diet = it }, "Diet")
        FormButtons(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    PostOpNoteCreateRequest(
                        dayNumber = day.toIntOrNull() ?: 1,
                        condition = condition,
                        vitalsJson = parseKeyValue(vitals),
                        woundStatus = wound.takeIf { it.isNotBlank() },
                        painScore = pain.toIntOrNull(),
                        diet = diet.takeIf { it.isNotBlank() }
                    )
                )
            },
            enabled = condition.isNotBlank() && day.toIntOrNull() != null
        )
    }
}

@Composable
private fun WardRoundForm(
    onDismiss: () -> Unit,
    onSave: (WardRoundNoteCreateRequest) -> Unit
) {
    var subjective by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var assessment by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }

    FormCard(title = "Ward Round Note") {
        FormTextField(subjective, { subjective = it }, "Subjective")
        FormTextField(objective, { objective = it }, "Objective")
        FormTextField(assessment, { assessment = it }, "Assessment")
        FormTextField(plan, { plan = it }, "Plan")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ready for discharge")
            Spacer(modifier = Modifier.width(8.dp))
            // Simple toggle via button
            Button(onClick = { ready = !ready }) {
                Text(if (ready) "Yes" else "No")
            }
        }
        FormButtons(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    WardRoundNoteCreateRequest(
                        subjective = subjective.takeIf { it.isNotBlank() },
                        objective = objective.takeIf { it.isNotBlank() },
                        assessment = assessment.takeIf { it.isNotBlank() },
                        plan = plan.takeIf { it.isNotBlank() },
                        readyForDischarge = ready
                    )
                )
            },
            enabled = true
        )
    }
}

@Composable
private fun DischargeForm(
    onDismiss: () -> Unit,
    onSave: (DischargeSummaryCreateRequest) -> Unit
) {
    var condition by remember { mutableStateOf("") }
    var procedureSummary by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var woundCare by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var diet by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }
    var redFlags by remember { mutableStateOf("") }

    FormCard(title = "Discharge Summary") {
        FormTextField(condition, { condition = it }, "Condition at Discharge *")
        FormTextField(procedureSummary, { procedureSummary = it }, "Procedure Summary *")
        FormTextField(medications, { medications = it }, "Medications (key=value, comma)")
        FormTextField(woundCare, { woundCare = it }, "Wound Care")
        FormTextField(activity, { activity = it }, "Activity Restrictions")
        FormTextField(diet, { diet = it }, "Diet Instructions")
        FormTextField(followUp, { followUp = it }, "Follow-up Date (ISO)")
        FormTextField(redFlags, { redFlags = it }, "Red Flags")
        FormButtons(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    DischargeSummaryCreateRequest(
                        conditionAtDischarge = condition,
                        procedureSummary = procedureSummary,
                        dischargeMedicationsJson = parseKeyValue(medications),
                        woundCare = woundCare.takeIf { it.isNotBlank() },
                        activityRestrictions = activity.takeIf { it.isNotBlank() },
                        dietInstructions = diet.takeIf { it.isNotBlank() },
                        followUpDate = followUp.takeIf { it.isNotBlank() },
                        redFlags = redFlags.takeIf { it.isNotBlank() }
                    )
                )
            },
            enabled = condition.isNotBlank() && procedureSummary.isNotBlank()
        )
    }
}

@Composable
private fun FormCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FormButtons(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
        Button(onClick = onSave, enabled = enabled) {
            Text("Save")
        }
    }
}

private fun parseKeyValue(input: String): Map<String, String?> {
    if (input.isBlank()) return emptyMap()
    return input.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() && "=" in it }
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key.trim() to value.trim().takeIf { v -> v.isNotBlank() }
        }
}
