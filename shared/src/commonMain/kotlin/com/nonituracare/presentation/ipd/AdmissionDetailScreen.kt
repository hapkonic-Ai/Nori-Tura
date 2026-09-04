package com.nonituracare.presentation.ipd

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import com.nonituracare.data.UploadedMedia
import com.nonituracare.data.dto.AdmissionDto
import com.nonituracare.data.dto.DischargeSummaryCreateRequest
import com.nonituracare.data.dto.IntraOpNoteCreateRequest
import com.nonituracare.data.dto.OtNoteCreateRequest
import com.nonituracare.data.dto.OtNoteDto
import com.nonituracare.data.dto.OtNoteTemplateDto
import com.nonituracare.data.dto.PostOpNoteCreateRequest
import com.nonituracare.data.dto.PreOpNoteCreateRequest
import com.nonituracare.data.dto.SurgicalTemplateDto
import com.nonituracare.data.dto.TeamMemberDto
import com.nonituracare.data.dto.WardRoundNoteCreateRequest
import com.nonituracare.presentation.components.AuthenticatedUrlImageRow
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.ImageAttachmentPicker
import com.nonituracare.presentation.components.MediaUrlChipGrid
import com.nonituracare.presentation.components.MedicalAutoCompleteTextField
import com.nonituracare.presentation.components.TemplatePickerDialog
import com.nonituracare.presentation.components.TemplatePickerResult
import com.nonituracare.ui.theme.NorituraColors
import com.nonituracare.util.openUrl
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.empty_ot_notes
import noritura.shared.generated.resources.success_discharge
import noritura.shared.generated.resources.empty_consents

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
    val otNoteTemplates by viewModel.otNoteTemplates.collectAsState()
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
                        otNoteTemplates = otNoteTemplates,
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
    otNoteTemplates: List<OtNoteTemplateDto>,
    viewModel: AdmissionDetailViewModel,
    onNavigateToConsentForm: () -> Unit,
    onNavigateToConsentView: (String) -> Unit
) {
    var showOtNote by remember { mutableStateOf(false) }
    var showWardRound by remember { mutableStateOf(false) }
    var showDischarge by remember { mutableStateOf(false) }
    var previewOtNote by remember { mutableStateOf<OtNoteDto?>(null) }
    var previewConsent by remember { mutableStateOf<com.nonituracare.data.dto.ConsentFormDto?>(null) }

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

        // Historical admissions only — new admissions use the unified OT Notes
        // section below. These render read-only; there's no "Add" button for
        // any of the three since OT Notes replaces them going forward.
        if (!admission.preOpNotes.isNullOrEmpty()) {
            SectionTitle("Pre-Op Notes (legacy)")
            for (note in admission.preOpNotes) {
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
        }

        if (!admission.intraOpNotes.isNullOrEmpty()) {
            SectionTitle("Intra-Op Notes (legacy)")
            for (note in admission.intraOpNotes) {
                NoteCard(imageUrls = note.imageUrls, videoUrls = note.videoUrls) {
                    Text("Procedure: ${note.procedureDone}", fontWeight = FontWeight.SemiBold)
                    note.findings?.let { Text("Findings: $it") }
                    note.technique?.let { Text("Technique: $it") }
                    note.complications?.let { Text("Complications: $it") }
                    note.bloodLoss?.let { Text("Blood Loss: $it") }
                }
            }
        }

        if (!admission.postOpNotes.isNullOrEmpty()) {
            SectionTitle("Post-Op Notes (legacy)")
            for (note in admission.postOpNotes) {
                NoteCard(imageUrls = note.imageUrls, videoUrls = note.videoUrls) {
                    Text("Day ${note.dayNumber}: ${note.condition}", fontWeight = FontWeight.SemiBold)
                    Text("Vitals: ${note.vitalsJson.entries.joinToString { "${it.key}=${it.value}" }}")
                    note.woundStatus?.let { Text("Wound: $it") }
                    note.painScore?.let { Text("Pain: $it/10") }
                    note.diet?.let { Text("Diet: $it") }
                }
            }
        }

        SectionTitle("OT Notes")
        if (admission.otNotes.isNullOrEmpty()) {
            com.nonituracare.presentation.components.EmptyState(
                title = "No OT notes yet",
                subtitle = "Add an operative note once surgery is underway.",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                illustration = Res.drawable.empty_ot_notes
            )
        } else {
            for (note in admission.otNotes) {
                OtNoteCard(
                    note = note,
                    onClick = { previewOtNote = note },
                    onLongClick = { previewOtNote = note }
                )
            }
        }
        OutlinedButton(onClick = { showOtNote = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add OT Note")
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
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(
                    Res.drawable.success_discharge
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.45f).height(90.dp)
            )
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
        if (admission.consentForms.isNullOrEmpty()) {
            com.nonituracare.presentation.components.EmptyState(
                title = "No consent forms yet",
                subtitle = "Generate one before proceeding with surgery.",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                illustration = Res.drawable.empty_consents
            )
        } else {
            for (consent in admission.consentForms) {
                ConsentListCard(
                    consent = consent,
                    onClick = { consent.id.let(onNavigateToConsentView) },
                    onLongClick = { previewConsent = consent }
                )
            }
        }
        OutlinedButton(onClick = onNavigateToConsentForm, modifier = Modifier.fillMaxWidth()) {
            Text("Add Consent Form")
        }
    }

    val fullScreenDialogProps = DialogProperties(usePlatformDefaultWidth = false)

    if (showOtNote) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showOtNote = false },
            properties = fullScreenDialogProps
        ) {
            OtNoteForm(
                templates = otNoteTemplates,
                onDismiss = { showOtNote = false },
                onSave = { request ->
                    viewModel.createOtNote(request)
                    showOtNote = false
                }
            )
        }
    }

    previewOtNote?.let { note ->
        OtNotePreviewDialog(note = note, onDismiss = { previewOtNote = null })
    }

    previewConsent?.let { consent ->
        ConsentPreviewDialog(
            consent = consent,
            onDismiss = { previewConsent = null },
            onViewFull = {
                previewConsent = null
                consent.id.let(onNavigateToConsentView)
            }
        )
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

/** Card for a unified OT note. Both tap and long-press open the same preview
 * dialog (there's no separate full-screen view yet) — long-press is the
 * discoverable "peek" gesture users expect on record cards generally. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OtNoteCard(
    note: OtNoteDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit = onClick
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(note.procedure, fontWeight = FontWeight.SemiBold)
                Text(
                    text = (note.status ?: "draft").replaceFirstChar { it.uppercase() },
                    color = if (note.status == "submitted") MaterialTheme.colorScheme.primary else NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            note.approach?.let { Text("Approach: $it", style = MaterialTheme.typography.bodySmall) }
            Text(
                "${note.procedureSteps.size} step(s) · ${note.imageUrls.size + note.videoUrls.size} attachment(s)",
                style = MaterialTheme.typography.bodySmall,
                color = NorituraColors.TextTertiary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConsentListCard(
    consent: com.nonituracare.data.dto.ConsentFormDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    // "signed" only appears on historical rows from before in-app e-signing was
    // removed; new consents are done once generated (no further signing step).
    val isDone = consent.status == "signed" || consent.status == "generated" || consent.pdfUrl != null
    val statusColor = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
private fun OtNoteForm(
    templates: List<OtNoteTemplateDto>,
    onDismiss: () -> Unit,
    onSave: (OtNoteCreateRequest) -> Unit
) {
    var procedure by remember { mutableStateOf("") }
    var approach by remember { mutableStateOf("") }
    var anaesthesia by remember { mutableStateOf("") }
    var preopDiagnosis by remember { mutableStateOf("") }
    var postopDiagnosis by remember { mutableStateOf("") }
    var operationPerformed by remember { mutableStateOf("") }
    var positionPreparation by remember { mutableStateOf("") }
    var incisionApproach by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(listOf("")) }
    var closure by remember { mutableStateOf("") }
    var specimen by remember { mutableStateOf("") }
    var implants by remember { mutableStateOf("") }
    var drains by remember { mutableStateOf("") }
    var estimatedBloodLoss by remember { mutableStateOf("") }
    var counts by remember { mutableStateOf("") }
    var complications by remember { mutableStateOf("") }
    var postopPlan by remember { mutableStateOf("") }
    var teamRows by remember { mutableStateOf(listOf("" to "")) }
    var mediaItems by remember { mutableStateOf(listOf<UploadedMedia>()) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var appliedTemplateId by remember { mutableStateOf<String?>(null) }

    if (showTemplatePicker) {
        OtNoteTemplatePickerDialog(
            templates = templates,
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                procedure = template.procedure
                approach = template.approach ?: approach
                anaesthesia = template.anaesthesia ?: anaesthesia
                preopDiagnosis = template.preopDiagnosis ?: preopDiagnosis
                postopDiagnosis = template.postopDiagnosis ?: postopDiagnosis
                operationPerformed = template.operationPerformed ?: operationPerformed
                positionPreparation = template.positionPreparation ?: positionPreparation
                incisionApproach = template.incisionApproach ?: incisionApproach
                if (template.procedureSteps.isNotEmpty()) steps = template.procedureSteps
                closure = template.closure ?: closure
                specimen = template.specimen ?: specimen
                implants = template.implants ?: implants
                drains = template.drains ?: drains
                estimatedBloodLoss = template.estimatedBloodLoss ?: estimatedBloodLoss
                counts = template.counts ?: counts
                complications = template.standardComplications ?: complications
                postopPlan = template.postopPlan ?: postopPlan
                appliedTemplateId = template.id
                showTemplatePicker = false
            }
        )
    }

    FormCard(
        title = "OT Note",
        onDismiss = onDismiss,
        onSave = {
            val (imageUrls, videoUrls) = mediaItems.partitionByMimeType()
            onSave(
                OtNoteCreateRequest(
                    templateId = appliedTemplateId,
                    procedure = procedure,
                    approach = approach.takeIf { it.isNotBlank() },
                    anaesthesia = anaesthesia.takeIf { it.isNotBlank() },
                    preopDiagnosis = preopDiagnosis.takeIf { it.isNotBlank() },
                    postopDiagnosis = postopDiagnosis.takeIf { it.isNotBlank() },
                    operationPerformed = operationPerformed.takeIf { it.isNotBlank() },
                    positionPreparation = positionPreparation.takeIf { it.isNotBlank() },
                    incisionApproach = incisionApproach.takeIf { it.isNotBlank() },
                    findings = findings.takeIf { it.isNotBlank() },
                    procedureSteps = steps.map { it.trim() }.filter { it.isNotBlank() },
                    closure = closure.takeIf { it.isNotBlank() },
                    specimen = specimen.takeIf { it.isNotBlank() },
                    implants = implants.takeIf { it.isNotBlank() },
                    drains = drains.takeIf { it.isNotBlank() },
                    estimatedBloodLoss = estimatedBloodLoss.takeIf { it.isNotBlank() },
                    counts = counts.takeIf { it.isNotBlank() },
                    complications = complications.takeIf { it.isNotBlank() },
                    postopPlan = postopPlan.takeIf { it.isNotBlank() },
                    teamMembers = teamRows.filter { it.first.isNotBlank() && it.second.isNotBlank() }
                        .map { TeamMemberDto(role = it.first, name = it.second) },
                    imageUrls = imageUrls,
                    videoUrls = videoUrls,
                    status = "submitted"
                )
            )
        },
        saveEnabled = procedure.isNotBlank()
    ) {
        OutlinedButton(onClick = { showTemplatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Apply from Template")
        }
        FormTextField(procedure, { procedure = it }, "Procedure *", autocomplete = true)
        FormTextField(approach, { approach = it }, "Approach")
        FormTextField(anaesthesia, { anaesthesia = it }, "Anaesthesia", autocomplete = true)
        FormTextField(preopDiagnosis, { preopDiagnosis = it }, "Pre-operative Diagnosis", autocomplete = true)
        FormTextField(postopDiagnosis, { postopDiagnosis = it }, "Post-operative Diagnosis", autocomplete = true)
        FormTextField(operationPerformed, { operationPerformed = it }, "Operation Performed", autocomplete = true)
        FormTextField(positionPreparation, { positionPreparation = it }, "Position & Preparation")
        FormTextField(incisionApproach, { incisionApproach = it }, "Incision / Approach")
        FormTextField(findings, { findings = it }, "Findings", autocomplete = true)

        Text("Procedure Steps", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${index + 1}.", modifier = Modifier.padding(top = 14.dp))
                OutlinedTextField(
                    value = step,
                    onValueChange = { updated -> steps = steps.toMutableList().also { it[index] = updated } },
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 4
                )
                IconButton(onClick = { steps = steps.toMutableList().also { it.removeAt(index) }.ifEmpty { listOf("") } }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove step")
                }
            }
        }
        TextButton(onClick = { steps = steps + "" }) {
            Text("+ Add Step")
        }

        FormTextField(closure, { closure = it }, "Closure", autocomplete = true)
        FormTextField(specimen, { specimen = it }, "Specimen")
        FormTextField(implants, { implants = it }, "Implants")
        FormTextField(drains, { drains = it }, "Drains")
        FormTextField(estimatedBloodLoss, { estimatedBloodLoss = it }, "Estimated Blood Loss")
        FormTextField(counts, { counts = it }, "Counts (swab / instrument / needle)")
        FormTextField(complications, { complications = it }, "Complications", autocomplete = true)
        FormTextField(postopPlan, { postopPlan = it }, "Post-operative Plan", autocomplete = true)

        Text("Team", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        teamRows.forEachIndexed { index, (role, name) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = role,
                    onValueChange = { updated -> teamRows = teamRows.toMutableList().also { it[index] = updated to name } },
                    label = { Text("Role") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { updated -> teamRows = teamRows.toMutableList().also { it[index] = role to updated } },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { teamRows = teamRows.toMutableList().also { it.removeAt(index) }.ifEmpty { listOf("" to "") } }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove team member")
                }
            }
        }
        TextButton(onClick = { teamRows = teamRows + ("" to "") }) {
            Text("+ Add Team Member")
        }

        ImageAttachmentPicker(
            items = mediaItems,
            onItemsChange = { mediaItems = it },
            label = "Attach images / video",
            maxItems = 10,
            allowVideo = true
        )
    }
}

@Composable
private fun OtNoteTemplatePickerDialog(
    templates: List<OtNoteTemplateDto>,
    onDismiss: () -> Unit,
    onSelect: (OtNoteTemplateDto) -> Unit
) {
    val (globalTemplates, myTemplates) = templates.partition { it.isGlobal }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose an OT Note Template", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (myTemplates.isNotEmpty()) {
                        Text("My Templates", style = MaterialTheme.typography.labelLarge, color = NorituraColors.TextTertiary)
                        for (t in myTemplates) {
                            TemplatePickerRow(t, onSelect)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (globalTemplates.isNotEmpty()) {
                        Text("Global Library", style = MaterialTheme.typography.labelLarge, color = NorituraColors.TextTertiary)
                        for (t in globalTemplates) {
                            TemplatePickerRow(t, onSelect)
                        }
                    }
                    if (templates.isEmpty()) {
                        Text("No templates available yet.", color = NorituraColors.TextTertiary)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun TemplatePickerRow(template: OtNoteTemplateDto, onSelect: (OtNoteTemplateDto) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect(template) },
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(template.name, fontWeight = FontWeight.SemiBold)
            Text(
                template.procedure,
                style = MaterialTheme.typography.bodySmall,
                color = NorituraColors.TextTertiary
            )
        }
    }
}

@Composable
private fun OtNotePreviewDialog(note: OtNoteDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.procedure) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                note.approach?.let { PreviewRow("Approach", it) }
                note.anaesthesia?.let { PreviewRow("Anaesthesia", it) }
                note.preopDiagnosis?.let { PreviewRow("Pre-op Diagnosis", it) }
                note.findings?.let { PreviewRow("Findings", it) }
                if (note.procedureSteps.isNotEmpty()) {
                    Text("Steps", fontWeight = FontWeight.SemiBold)
                    note.procedureSteps.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodySmall) }
                }
                note.complications?.let { PreviewRow("Complications", it) }
                note.postopPlan?.let { PreviewRow("Post-op Plan", it) }
                if (note.imageUrls.isNotEmpty()) {
                    Text("Images (${note.imageUrls.size})", fontWeight = FontWeight.SemiBold)
                    AuthenticatedUrlImageRow(urls = note.imageUrls)
                }
                if (note.videoUrls.isNotEmpty()) {
                    Text("Videos (${note.videoUrls.size})", fontWeight = FontWeight.SemiBold)
                    MediaUrlChipGrid(urls = note.videoUrls)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ConsentPreviewDialog(
    consent: com.nonituracare.data.dto.ConsentFormDto,
    onDismiss: () -> Unit,
    onViewFull: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(consent.formType) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewRow("Status", consent.status?.replaceFirstChar { it.uppercase() } ?: "Pending")
                PreviewRow("Language", consent.language ?: "English")
                PreviewRow("Generated", consent.generatedAt?.take(10) ?: "-")
            }
        },
        confirmButton = {
            TextButton(onClick = onViewFull) { Text("View Full") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NorituraColors.TextTertiary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
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
