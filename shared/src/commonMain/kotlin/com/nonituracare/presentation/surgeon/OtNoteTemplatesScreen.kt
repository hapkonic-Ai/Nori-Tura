package com.nonituracare.presentation.surgeon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.OtNoteTemplateCreateRequest
import com.nonituracare.data.dto.OtNoteTemplateDto
import com.nonituracare.presentation.components.MedicalAutoCompleteTextField

/**
 * A doctor's own OT note templates. The global corpus-seeded library is
 * read-only here (browsed/applied from [com.nonituracare.presentation.ipd.AdmissionDetailScreen]'s
 * template picker) — this screen only manages the doctor's personal copies,
 * mirroring [SurgicalTemplatesScreen] exactly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtNoteTemplatesScreen(
    viewModel: OtNoteTemplatesViewModel = viewModel { OtNoteTemplatesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<OtNoteTemplateDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is OtNoteTemplatesViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as OtNoteTemplatesViewModel.UiState.Error).message)
            viewModel.resetError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OT Note Templates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add template")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is OtNoteTemplatesViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is OtNoteTemplatesViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load templates")
                        Button(onClick = { viewModel.loadTemplates() }) { Text("Retry") }
                    }
                }

                is OtNoteTemplatesViewModel.UiState.Success -> {
                    val (mine, global) = state.templates.partition { !it.isGlobal }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (mine.isNotEmpty()) {
                            item {
                                Text(
                                    "My Templates",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(mine) { template ->
                                OtNoteTemplateCard(
                                    template = template,
                                    onEdit = { editingTemplate = template },
                                    onDelete = { viewModel.deleteTemplate(template.id) }
                                )
                            }
                        }
                        if (global.isNotEmpty()) {
                            item {
                                Text(
                                    "Global Library",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(global) { template ->
                                OtNoteTemplateCard(template = template, onEdit = null, onDelete = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        Dialog(
            onDismissRequest = { showCreateDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OtNoteTemplateFormDialog(
                title = "New OT Note Template",
                initial = null,
                onDismiss = { showCreateDialog = false },
                onSave = { request ->
                    viewModel.createTemplate(request)
                    showCreateDialog = false
                }
            )
        }
    }

    editingTemplate?.let { template ->
        Dialog(
            onDismissRequest = { editingTemplate = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OtNoteTemplateFormDialog(
                title = "Edit OT Note Template",
                initial = template,
                onDismiss = { editingTemplate = null },
                onSave = { request ->
                    viewModel.updateTemplate(template.id, request)
                    editingTemplate = null
                }
            )
        }
    }
}

@Composable
private fun OtNoteTemplateCard(
    template: OtNoteTemplateDto,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(template.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(template.procedure, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            template.approach?.let { Text("Approach: $it", style = MaterialTheme.typography.bodySmall) }
            if (template.procedureSteps.isNotEmpty()) {
                Text("${template.procedureSteps.size} step(s)", style = MaterialTheme.typography.bodySmall)
            }
            template.sourceReference?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OtNoteTemplateFormCard(
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
            .padding(horizontal = 12.dp, vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = onSave, enabled = saveEnabled) { Text("Save") }
            }
        }
    }
}

@Composable
private fun OtNoteTemplateFormDialog(
    title: String,
    initial: OtNoteTemplateDto?,
    onDismiss: () -> Unit,
    onSave: (OtNoteTemplateCreateRequest) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var procedure by remember { mutableStateOf(initial?.procedure ?: "") }
    var approach by remember { mutableStateOf(initial?.approach ?: "") }
    var anaesthesia by remember { mutableStateOf(initial?.anaesthesia ?: "") }
    var preopDiagnosis by remember { mutableStateOf(initial?.preopDiagnosis ?: "") }
    var postopDiagnosis by remember { mutableStateOf(initial?.postopDiagnosis ?: "") }
    var operationPerformed by remember { mutableStateOf(initial?.operationPerformed ?: "") }
    var positionPreparation by remember { mutableStateOf(initial?.positionPreparation ?: "") }
    var incisionApproach by remember { mutableStateOf(initial?.incisionApproach ?: "") }
    var steps by remember { mutableStateOf(initial?.procedureSteps?.takeIf { it.isNotEmpty() } ?: listOf("")) }
    var closure by remember { mutableStateOf(initial?.closure ?: "") }
    var specimen by remember { mutableStateOf(initial?.specimen ?: "") }
    var implants by remember { mutableStateOf(initial?.implants ?: "") }
    var drains by remember { mutableStateOf(initial?.drains ?: "") }
    var estimatedBloodLoss by remember { mutableStateOf(initial?.estimatedBloodLoss ?: "") }
    var counts by remember { mutableStateOf(initial?.counts ?: "") }
    var standardComplications by remember { mutableStateOf(initial?.standardComplications ?: "") }
    var postopPlan by remember { mutableStateOf(initial?.postopPlan ?: "") }

    OtNoteTemplateFormCard(
        title = title,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                OtNoteTemplateCreateRequest(
                    name = name,
                    procedure = procedure,
                    approach = approach.takeIf { it.isNotBlank() },
                    anaesthesia = anaesthesia.takeIf { it.isNotBlank() },
                    preopDiagnosis = preopDiagnosis.takeIf { it.isNotBlank() },
                    postopDiagnosis = postopDiagnosis.takeIf { it.isNotBlank() },
                    operationPerformed = operationPerformed.takeIf { it.isNotBlank() },
                    positionPreparation = positionPreparation.takeIf { it.isNotBlank() },
                    incisionApproach = incisionApproach.takeIf { it.isNotBlank() },
                    procedureSteps = steps.map { it.trim() }.filter { it.isNotBlank() },
                    closure = closure.takeIf { it.isNotBlank() },
                    specimen = specimen.takeIf { it.isNotBlank() },
                    implants = implants.takeIf { it.isNotBlank() },
                    drains = drains.takeIf { it.isNotBlank() },
                    estimatedBloodLoss = estimatedBloodLoss.takeIf { it.isNotBlank() },
                    counts = counts.takeIf { it.isNotBlank() },
                    standardComplications = standardComplications.takeIf { it.isNotBlank() },
                    postopPlan = postopPlan.takeIf { it.isNotBlank() }
                )
            )
        },
        saveEnabled = name.isNotBlank() && procedure.isNotBlank()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Template Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        MedicalAutoCompleteTextField(
            value = procedure,
            onValueChange = { procedure = it },
            label = { Text("Procedure *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
        MedicalAutoCompleteTextField(
            value = approach,
            onValueChange = { approach = it },
            label = { Text("Approach (e.g. Laparoscopic, Open)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        MedicalAutoCompleteTextField(
            value = anaesthesia,
            onValueChange = { anaesthesia = it },
            label = { Text("Anaesthesia") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
        MedicalAutoCompleteTextField(
            value = preopDiagnosis,
            onValueChange = { preopDiagnosis = it },
            label = { Text("Pre-operative Diagnosis") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
        MedicalAutoCompleteTextField(
            value = postopDiagnosis,
            onValueChange = { postopDiagnosis = it },
            label = { Text("Post-operative Diagnosis") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
        MedicalAutoCompleteTextField(
            value = operationPerformed,
            onValueChange = { operationPerformed = it },
            label = { Text("Operation Performed") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
        MedicalAutoCompleteTextField(
            value = positionPreparation,
            onValueChange = { positionPreparation = it },
            label = { Text("Position & Preparation") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
        MedicalAutoCompleteTextField(
            value = incisionApproach,
            onValueChange = { incisionApproach = it },
            label = { Text("Incision / Approach") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )

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
                    Icon(Icons.Default.Close, contentDescription = "Remove step")
                }
            }
        }
        TextButton(onClick = { steps = steps + "" }) { Text("+ Add Step") }

        MedicalAutoCompleteTextField(
            value = closure,
            onValueChange = { closure = it },
            label = { Text("Closure") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 4
        )
        OutlinedTextField(
            value = specimen,
            onValueChange = { specimen = it },
            label = { Text("Specimen") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = implants,
            onValueChange = { implants = it },
            label = { Text("Implants") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = drains,
            onValueChange = { drains = it },
            label = { Text("Drains") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = estimatedBloodLoss,
            onValueChange = { estimatedBloodLoss = it },
            label = { Text("Estimated Blood Loss") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = counts,
            onValueChange = { counts = it },
            label = { Text("Counts (swab / instrument / needle)") },
            modifier = Modifier.fillMaxWidth()
        )
        MedicalAutoCompleteTextField(
            value = standardComplications,
            onValueChange = { standardComplications = it },
            label = { Text("Standard Complications") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = postopPlan,
            onValueChange = { postopPlan = it },
            label = { Text("Post-operative Plan") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 4
        )
    }
}
