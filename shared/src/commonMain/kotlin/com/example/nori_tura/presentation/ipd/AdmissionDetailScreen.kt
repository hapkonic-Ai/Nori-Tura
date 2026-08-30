package com.example.nori_tura.presentation.ipd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.nori_tura.data.UploadedMedia
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.DischargeSummaryCreateRequest
import com.example.nori_tura.data.dto.IntraOpNoteCreateRequest
import com.example.nori_tura.data.dto.PostOpNoteCreateRequest
import com.example.nori_tura.data.dto.PreOpNoteCreateRequest
import com.example.nori_tura.data.dto.SurgicalTemplateDto
import com.example.nori_tura.data.dto.WardRoundNoteCreateRequest
import com.example.nori_tura.presentation.components.AuthenticatedUrlImageRow
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ImageAttachmentPicker
import com.example.nori_tura.presentation.components.MediaUrlChipGrid
import com.example.nori_tura.presentation.components.MedicalAutoCompleteTextField
import com.example.nori_tura.presentation.components.TemplatePickerDialog
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.openUrl
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface

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
            BrandTopBar(
                initials = "DR",
                title = "Admission Details",
                onBack = onBack,
                notificationCount = 0
            )
        },
        containerColor = NorituraColors.Background,
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

    val statusColor = when (admission.status?.lowercase()) {
        "pre-op" -> NorituraColors.PreOp
        "in-surgery" -> NorituraColors.InOt
        "recovery" -> NorituraColors.PostOp
        "discharged" -> NorituraColors.TextTertiary
        else -> NorituraColors.PrimaryBlue
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Patient info header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = statusColor.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = admission.patient?.name ?: "Patient",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = NorituraColors.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = (admission.status ?: "—").uppercase(),
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = admission.urgency?.replaceFirstChar { it.uppercase() } ?: "-",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Ward: ${admission.ward ?: "-"} · Bed: ${admission.bedNo ?: "-"}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SectionTitle("Pre-Op Notes")
        for (note in admission.preOpNotes ?: emptyList()) {
            NoteCard(imageUrls = note.imageUrls, videoUrls = note.videoUrls) {
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
            NoteCard(imageUrls = note.imageUrls, videoUrls = note.videoUrls) {
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
            NoteCard(imageUrls = note.imageUrls, videoUrls = note.videoUrls) {
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
            NoteCard(imageUrls = note.imageUrls, videoUrls = note.videoUrls) {
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
            NoteCard(imageUrls = summary.imageUrls, videoUrls = summary.videoUrls) {
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

    val fullScreenDialogProps = DialogProperties(usePlatformDefaultWidth = false)

    if (showPreOp) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPreOp = false },
            properties = fullScreenDialogProps
        ) {
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showIntraOp = false },
            properties = fullScreenDialogProps
        ) {
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPostOp = false },
            properties = fullScreenDialogProps
        ) {
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showWardRound = false },
            properties = fullScreenDialogProps
        ) {
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDischarge = false },
            properties = fullScreenDialogProps
        ) {
            DischargeForm(
                admission = admission,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NoteCard(
    imageUrls: List<String>? = null,
    videoUrls: List<String>? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
            if (!imageUrls.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = NorituraColors.Divider)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Attached Images (${imageUrls.size})",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                AuthenticatedUrlImageRow(urls = imageUrls)
            }
            if (!videoUrls.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = NorituraColors.Divider)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Attached Videos (${videoUrls.size})",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                MediaUrlChipGrid(urls = videoUrls)
            }
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
    var mediaItems by remember { mutableStateOf(listOf<UploadedMedia>()) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            templates = templates,
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                template?.let {
                    procedure = it.procedure
                    approach = it.approach ?: approach
                    anaesthesia = it.anaesthesia.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: anaesthesia
                    investigations = (investigations.split(",").map { it.trim() }.filter { it.isNotBlank() } +
                        it.investigations).distinct().joinToString(", ")
                    riskLevel = it.riskLevel ?: riskLevel
                    instructions = it.specialInstructions ?: instructions
                }
                showTemplatePicker = false
            },
            showCustomOption = false
        )
    }

    FormCard(
        title = "Pre-Op Note",
        onDismiss = onDismiss,
        onSave = {
            val (imageUrls, videoUrls) = mediaItems.partitionByMimeType()
            onSave(
                PreOpNoteCreateRequest(
                    procedure = procedure,
                    approach = approach.takeIf { it.isNotBlank() },
                    anaesthesia = anaesthesia.takeIf { it.isNotBlank() },
                    investigations = investigations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    riskLevel = riskLevel.takeIf { it.isNotBlank() },
                    specialInstructions = instructions.takeIf { it.isNotBlank() },
                    imageUrls = imageUrls,
                    videoUrls = videoUrls
                )
            )
        },
        saveEnabled = procedure.isNotBlank()
    ) {
        OutlinedButton(
            onClick = { showTemplatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply from Template")
        }
        FormTextField(procedure, { procedure = it }, "Procedure *", autocomplete = true)
        FormTextField(approach, { approach = it }, "Approach", autocomplete = true)
        FormTextField(anaesthesia, { anaesthesia = it }, "Anaesthesia", autocomplete = true)
        FormTextField(investigations, { investigations = it }, "Investigations (comma separated)", autocomplete = true)
        FormTextField(riskLevel, { riskLevel = it }, "Risk Level")
        FormTextField(instructions, { instructions = it }, "Special Instructions", autocomplete = true)
        ImageAttachmentPicker(
            items = mediaItems,
            onItemsChange = { mediaItems = it },
            label = "Attach images / video",
            maxItems = 5,
            allowVideo = true
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
    var mediaItems by remember { mutableStateOf(listOf<UploadedMedia>()) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            templates = templates,
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                template?.let {
                    procedure = it.procedure
                    technique = it.technique ?: technique
                }
                showTemplatePicker = false
            },
            showCustomOption = false
        )
    }

    FormCard(
        title = "Intra-Op Note",
        onDismiss = onDismiss,
        onSave = {
            val (imageUrls, videoUrls) = mediaItems.partitionByMimeType()
            onSave(
                IntraOpNoteCreateRequest(
                    procedureDone = procedure,
                    findings = findings.takeIf { it.isNotBlank() },
                    technique = technique.takeIf { it.isNotBlank() },
                    complications = complications.takeIf { it.isNotBlank() },
                    bloodLoss = bloodLoss.takeIf { it.isNotBlank() },
                    otStart = otStart.takeIf { it.isNotBlank() },
                    otEnd = otEnd.takeIf { it.isNotBlank() },
                    imageUrls = imageUrls,
                    videoUrls = videoUrls
                )
            )
        },
        saveEnabled = procedure.isNotBlank()
    ) {
        OutlinedButton(
            onClick = { showTemplatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply from Template")
        }
        FormTextField(procedure, { procedure = it }, "Procedure Done *", autocomplete = true)
        FormTextField(findings, { findings = it }, "Findings", autocomplete = true)
        FormTextField(technique, { technique = it }, "Technique", autocomplete = true)
        FormTextField(complications, { complications = it }, "Complications", autocomplete = true)
        FormTextField(bloodLoss, { bloodLoss = it }, "Blood Loss")
        FormTextField(otStart, { otStart = it }, "OT Start (ISO)")
        FormTextField(otEnd, { otEnd = it }, "OT End (ISO)")
        ImageAttachmentPicker(
            items = mediaItems,
            onItemsChange = { mediaItems = it },
            label = "Attach images / video",
            maxItems = 5,
            allowVideo = true
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
    var mediaItems by remember { mutableStateOf(listOf<UploadedMedia>()) }

    FormCard(
        title = "Post-Op Note",
        onDismiss = onDismiss,
        onSave = {
            val (imageUrls, videoUrls) = mediaItems.partitionByMimeType()
            onSave(
                PostOpNoteCreateRequest(
                    dayNumber = day.toIntOrNull() ?: 1,
                    condition = condition,
                    vitalsJson = parseKeyValue(vitals),
                    woundStatus = wound.takeIf { it.isNotBlank() },
                    painScore = pain.toIntOrNull(),
                    diet = diet.takeIf { it.isNotBlank() },
                    imageUrls = imageUrls,
                    videoUrls = videoUrls
                )
            )
        },
        saveEnabled = condition.isNotBlank() && day.toIntOrNull() != null
    ) {
        FormTextField(day, { day = it }, "Day Number *")
        FormTextField(condition, { condition = it }, "Condition *", autocomplete = true)
        FormTextField(vitals, { vitals = it }, "Vitals (key=value, comma)")
        FormTextField(wound, { wound = it }, "Wound Status", autocomplete = true)
        FormTextField(pain, { pain = it }, "Pain Score (0-10)")
        FormTextField(diet, { diet = it }, "Diet", autocomplete = true)
        ImageAttachmentPicker(
            items = mediaItems,
            onItemsChange = { mediaItems = it },
            label = "Attach images / video",
            maxItems = 5,
            allowVideo = true
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
    var mediaItems by remember { mutableStateOf(listOf<UploadedMedia>()) }

    FormCard(
        title = "Ward Round Note",
        onDismiss = onDismiss,
        onSave = {
            val (imageUrls, videoUrls) = mediaItems.partitionByMimeType()
            onSave(
                WardRoundNoteCreateRequest(
                    subjective = subjective.takeIf { it.isNotBlank() },
                    objective = objective.takeIf { it.isNotBlank() },
                    assessment = assessment.takeIf { it.isNotBlank() },
                    plan = plan.takeIf { it.isNotBlank() },
                    readyForDischarge = ready,
                    imageUrls = imageUrls,
                    videoUrls = videoUrls
                )
            )
        },
        saveEnabled = true
    ) {
        FormTextField(subjective, { subjective = it }, "Subjective", autocomplete = true)
        FormTextField(objective, { objective = it }, "Objective", autocomplete = true)
        FormTextField(assessment, { assessment = it }, "Assessment", autocomplete = true)
        FormTextField(plan, { plan = it }, "Plan", autocomplete = true)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ready for discharge", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { ready = !ready }) {
                Text(if (ready) "Yes" else "No")
            }
        }
        ImageAttachmentPicker(
            items = mediaItems,
            onItemsChange = { mediaItems = it },
            label = "Attach images / video",
            maxItems = 5,
            allowVideo = true
        )
    }
}

@Composable
private fun DischargeForm(
    admission: AdmissionDto,
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
    var generated by remember { mutableStateOf(false) }
    var mediaItems by remember { mutableStateOf(listOf<UploadedMedia>()) }

    fun applyAutoGenerate() {
        val gen = autoGenerateDischargeSummary(admission)
        condition = gen.condition
        procedureSummary = gen.procedureSummary
        woundCare = gen.woundCare
        diet = gen.diet
        medications = gen.medications
        activity = gen.activity
        redFlags = gen.redFlags
        generated = true
    }

    FormCard(
        title = "Discharge Summary",
        onDismiss = onDismiss,
        onSave = {
            val (imageUrls, videoUrls) = mediaItems.partitionByMimeType()
            onSave(
                DischargeSummaryCreateRequest(
                    conditionAtDischarge = condition,
                    procedureSummary = procedureSummary,
                    dischargeMedicationsJson = parseKeyValue(medications),
                    woundCare = woundCare.takeIf { it.isNotBlank() },
                    activityRestrictions = activity.takeIf { it.isNotBlank() },
                    dietInstructions = diet.takeIf { it.isNotBlank() },
                    followUpDate = followUp.takeIf { it.isNotBlank() },
                    redFlags = redFlags.takeIf { it.isNotBlank() },
                    imageUrls = imageUrls,
                    videoUrls = videoUrls
                )
            )
        },
        saveEnabled = condition.isNotBlank() && procedureSummary.isNotBlank()
    ) {
        // Auto-generate banner
        Button(
            onClick = { applyAutoGenerate() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (generated) "Re-generate from notes" else "Generate from notes")
        }

        if (generated) {
            Text(
                text = "Fields pre-filled from admission notes — review and edit before saving.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FormTextField(condition, { condition = it }, "Condition at Discharge *", autocomplete = true)
        FormTextField(procedureSummary, { procedureSummary = it }, "Procedure Summary *", autocomplete = true)
        FormTextField(medications, { medications = it }, "Medications (key=value, comma)", autocomplete = true)
        FormTextField(woundCare, { woundCare = it }, "Wound Care", autocomplete = true)
        FormTextField(activity, { activity = it }, "Activity Restrictions", autocomplete = true)
        FormTextField(diet, { diet = it }, "Diet Instructions", autocomplete = true)
        FormTextField(followUp, { followUp = it }, "Follow-up Date (ISO)")
        FormTextField(redFlags, { redFlags = it }, "Red Flags", autocomplete = true)
        ImageAttachmentPicker(
            items = mediaItems,
            onItemsChange = { mediaItems = it },
            label = "Attach images / video",
            maxItems = 5,
            allowVideo = true
        )
    }
}

@Composable
private fun FormCard(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Scrollable fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                content()
            }

            // Pinned action buttons at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = onSave, enabled = saveEnabled) { Text("Save") }
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    autocomplete: Boolean = false,
    fieldType: String? = null
) {
    if (autocomplete) {
        MedicalAutoCompleteTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            fieldType = fieldType
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
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

private data class DischargeSummaryAutoFill(
    val condition: String,
    val procedureSummary: String,
    val woundCare: String,
    val diet: String,
    val medications: String,
    val activity: String,
    val redFlags: String
)

private fun List<UploadedMedia>.partitionByMimeType(): Pair<List<String>, List<String>> {
    val images = mutableListOf<String>()
    val videos = mutableListOf<String>()
    forEach { item ->
        if (item.mimeType.startsWith("video/")) {
            videos += item.url
        } else {
            images += item.url
        }
    }
    return images to videos
}

private fun autoGenerateDischargeSummary(admission: AdmissionDto): DischargeSummaryAutoFill {
    val latestIntraOp = admission.intraOpNotes?.lastOrNull()
    val latestPreOp   = admission.preOpNotes?.lastOrNull()
    val postOpNotes   = admission.postOpNotes ?: emptyList()
    val latestPostOp  = postOpNotes.maxByOrNull { it.dayNumber }
    val latestWard    = admission.wardRoundNotes?.lastOrNull()

    // Procedure summary — built from intra-op, falling back to pre-op
    val procedureSummary = buildString {
        val proc = latestIntraOp?.procedureDone ?: latestPreOp?.procedure
        proc?.let { append(it) }
        latestPreOp?.approach?.let { append(". Approach: $it") }
        latestIntraOp?.technique?.let { append(". Technique: $it") }
        latestIntraOp?.findings?.let { append(". Findings: $it") }
        latestIntraOp?.bloodLoss?.let { append(". EBL: $it") }
        val comp = latestIntraOp?.complications
        if (comp.isNullOrBlank()) append(". No intra-op complications.")
        else append(". Complications: $comp.")
    }.trim()

    // Condition at discharge — latest post-op condition + pain, supplemented by ward assessment
    val condition = buildString {
        latestPostOp?.condition?.let { append(it) }
        latestPostOp?.painScore?.let { score -> append(". Pain score $score/10.") }
        val assessment = latestWard?.assessment
        if (!assessment.isNullOrBlank() && isNotBlank()) append(" $assessment.")
        else if (!assessment.isNullOrBlank()) append(assessment)
    }.trim()

    // Wound care — latest non-null wound status across all post-op notes
    val woundCare = postOpNotes.mapNotNull { it.woundStatus }.lastOrNull() ?: ""

    // Diet — latest post-op diet instruction
    val diet = latestPostOp?.diet ?: ""

    // Medications — from latest post-op medications map
    val medications = (latestPostOp?.medicationsJson ?: emptyMap())
        .entries
        .filter { it.key.isNotBlank() }
        .joinToString(", ") { "${it.key}=${it.value ?: ""}" }

    // Activity — from pre-op special instructions or anaesthesia-based default
    val activity = latestPreOp?.specialInstructions
        ?: latestPreOp?.riskLevel?.let { "Restrict activity per risk level: $it" }
        ?: ""

    // Red flags — suggest based on procedure type and any complications noted
    val redFlags = buildString {
        val proc = (latestIntraOp?.procedureDone ?: latestPreOp?.procedure ?: "").lowercase()
        if ("appendic" in proc || "hernia" in proc || "laparoscop" in proc) {
            append("Fever >38°C, increasing abdominal pain, vomiting, wound discharge — return to A&E immediately.")
        } else if ("cardiac" in proc || "thorac" in proc) {
            append("Chest pain, breathlessness, palpitations — seek emergency care immediately.")
        } else if (latestIntraOp?.complications?.isNotBlank() == true) {
            append("Monitor for recurrence of intra-operative complications. Return if condition worsens.")
        } else {
            append("Fever, increasing pain, wound redness or discharge — return immediately.")
        }
    }

    return DischargeSummaryAutoFill(
        condition = condition,
        procedureSummary = procedureSummary,
        woundCare = woundCare,
        diet = diet,
        medications = medications,
        activity = activity,
        redFlags = redFlags
    )
}
