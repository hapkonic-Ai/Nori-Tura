package com.nonituracare.presentation.surgeon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateCreateRequest
import com.nonituracare.data.dto.SurgicalTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateUpdateRequest
import com.nonituracare.presentation.components.MedicalAutoCompleteTextField
import com.nonituracare.presentation.components.TemplatePickerDialog
import com.nonituracare.presentation.components.TemplatePickerResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgicalTemplatesScreen(
    viewModel: SurgicalTemplatesViewModel = viewModel { SurgicalTemplatesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentTemplates by viewModel.contentTemplates.collectAsState()
    var showSourcePicker by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createSeed by remember { mutableStateOf<SurgicalTemplateDto?>(null) }
    var editingTemplate by remember { mutableStateOf<SurgicalTemplateDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is SurgicalTemplatesViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as SurgicalTemplatesViewModel.UiState.Error).message)
            viewModel.resetError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Surgical Templates") },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showSourcePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add template")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SurgicalTemplatesViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is SurgicalTemplatesViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load templates")
                        Button(onClick = { viewModel.loadTemplates() }) {
                            Text("Retry")
                        }
                    }
                }

                is SurgicalTemplatesViewModel.UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.templates) { template ->
                            TemplateCard(
                                template = template,
                                onEdit = { editingTemplate = template },
                                onDelete = { viewModel.deleteTemplate(template.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSourcePicker) {
        TemplatePickerDialog(
            mine = null,
            global = contentTemplates,
            onDismiss = { showSourcePicker = false },
            onSelect = { result ->
                createSeed = when (result) {
                    is TemplatePickerResult.Blank -> null
                    is TemplatePickerResult.Content -> result.template.toSurgicalTemplateSeed()
                    is TemplatePickerResult.Surgical -> null // not offered here
                }
                showSourcePicker = false
                showCreateDialog = true
            }
        )
    }

    if (showCreateDialog) {
        Dialog(
            onDismissRequest = { showCreateDialog = false; createSeed = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TemplateFormDialog(
                title = "New Surgical Template",
                initial = createSeed,
                onDismiss = { showCreateDialog = false; createSeed = null },
                onSave = { request ->
                    viewModel.createTemplate(request)
                    showCreateDialog = false
                    createSeed = null
                }
            )
        }
    }

    editingTemplate?.let { template ->
        Dialog(
            onDismissRequest = { editingTemplate = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TemplateFormDialog(
                title = "Edit Surgical Template",
                initial = template,
                onDismiss = { editingTemplate = null },
                onSave = { request ->
                    viewModel.updateTemplate(template.id, request.toUpdateRequest())
                    editingTemplate = null
                }
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: SurgicalTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = template.procedure,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            template.approach?.let {
                Text(
                    text = "Approach: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.anaesthesia.isNotEmpty()) {
                Text(
                    text = "Anaesthesia: ${template.anaesthesia.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.investigations.isNotEmpty()) {
                Text(
                    text = "Investigations: ${template.investigations.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.riskLevel?.let {
                Text(
                    text = "Risk: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.technique?.let {
                Text(
                    text = "Technique: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.risks.isNotEmpty()) {
                Text(
                    text = "Risks: ${template.risks.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.benefits.isNotEmpty()) {
                Text(
                    text = "Benefits: ${template.benefits.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.alternatives.isNotEmpty()) {
                Text(
                    text = "Alternatives: ${template.alternatives.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.complications.isNotEmpty()) {
                Text(
                    text = "Complications: ${template.complications.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.materialRisks?.let {
                Text(
                    text = "Material risks: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.postOpCare?.let {
                Text(
                    text = "Post-op care: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.expectedRecovery?.let {
                Text(
                    text = "Expected recovery: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Seeds the create form from a global content template — the doctor edits and
 * saves it as their own new surgical template; nothing is written until Save. */
private fun ContentTemplateDto.toSurgicalTemplateSeed(): SurgicalTemplateDto =
    SurgicalTemplateDto(
        id = "",
        name = name,
        procedure = procedure,
        approach = approach,
        anaesthesia = anesthesia,
        investigations = investigations,
        riskLevel = riskLevel,
        technique = technique,
        specialInstructions = specialInstructions,
        procedureDescription = procedureDescription,
        risks = risks,
        benefits = benefits,
        alternatives = alternatives,
        complications = possibleComplications,
        materialRisks = materialRisks,
        postOpCare = postOpCare,
        expectedRecovery = expectedRecovery
    )

private fun SurgicalTemplateCreateRequest.toUpdateRequest(): SurgicalTemplateUpdateRequest =
    SurgicalTemplateUpdateRequest(
        name = name,
        procedure = procedure,
        approach = approach,
        anaesthesia = anaesthesia,
        investigations = investigations,
        riskLevel = riskLevel,
        technique = technique,
        specialInstructions = specialInstructions,
        procedureDescription = procedureDescription,
        risks = risks,
        benefits = benefits,
        alternatives = alternatives,
        complications = complications,
        materialRisks = materialRisks,
        postOpCare = postOpCare,
        expectedRecovery = expectedRecovery
    )

@Composable
private fun TemplateFormCard(
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                content()
            }
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
private fun TemplateFormDialog(
    title: String,
    initial: SurgicalTemplateDto?,
    onDismiss: () -> Unit,
    onSave: (SurgicalTemplateCreateRequest) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var procedure by remember { mutableStateOf(initial?.procedure ?: "") }
    var approach by remember { mutableStateOf(initial?.approach ?: "") }
    var anaesthesia by remember { mutableStateOf(initial?.anaesthesia?.joinToString(", ") ?: "") }
    var investigations by remember { mutableStateOf(initial?.investigations?.joinToString(", ") ?: "") }
    var riskLevel by remember { mutableStateOf(initial?.riskLevel ?: "") }
    var technique by remember { mutableStateOf(initial?.technique ?: "") }
    var instructions by remember { mutableStateOf(initial?.specialInstructions ?: "") }
    var procedureDescription by remember { mutableStateOf(initial?.procedureDescription ?: "") }
    var risks by remember { mutableStateOf(initial?.risks?.joinToString(", ") ?: "") }
    var benefits by remember { mutableStateOf(initial?.benefits?.joinToString(", ") ?: "") }
    var alternatives by remember { mutableStateOf(initial?.alternatives?.joinToString(", ") ?: "") }
    var complications by remember { mutableStateOf(initial?.complications?.joinToString(", ") ?: "") }
    var materialRisks by remember { mutableStateOf(initial?.materialRisks ?: "") }
    var postOpCare by remember { mutableStateOf(initial?.postOpCare ?: "") }
    var expectedRecovery by remember { mutableStateOf(initial?.expectedRecovery ?: "") }

    TemplateFormCard(
        title = title,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                SurgicalTemplateCreateRequest(
                    name = name,
                    procedure = procedure,
                    approach = approach.takeIf { it.isNotBlank() },
                    anaesthesia = anaesthesia.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    investigations = investigations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    riskLevel = riskLevel.takeIf { it.isNotBlank() },
                    technique = technique.takeIf { it.isNotBlank() },
                    specialInstructions = instructions.takeIf { it.isNotBlank() },
                    procedureDescription = procedureDescription.takeIf { it.isNotBlank() },
                    risks = risks.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    benefits = benefits.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    alternatives = alternatives.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    complications = complications.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    materialRisks = materialRisks.takeIf { it.isNotBlank() },
                    postOpCare = postOpCare.takeIf { it.isNotBlank() },
                    expectedRecovery = expectedRecovery.takeIf { it.isNotBlank() }
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
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = approach,
            onValueChange = { approach = it },
            label = { Text("Approach (e.g. Laparoscopic, Open)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        MedicalAutoCompleteTextField(
            value = technique,
            onValueChange = { technique = it },
            label = { Text("Technique") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = anaesthesia,
            onValueChange = { anaesthesia = it },
            label = { Text("Anaesthesia (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        MedicalAutoCompleteTextField(
            value = investigations,
            onValueChange = { investigations = it },
            label = { Text("Pre-op Investigations (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )
        OutlinedTextField(
            value = riskLevel,
            onValueChange = { riskLevel = it },
            label = { Text("Risk Level") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Low / Moderate / High") },
            singleLine = true
        )
        MedicalAutoCompleteTextField(
            value = procedureDescription,
            onValueChange = { procedureDescription = it },
            label = { Text("Procedure Description (optional — overrides the auto-composed one)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5
        )
        MedicalAutoCompleteTextField(
            value = risks,
            onValueChange = { risks = it },
            label = { Text("Risks (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = benefits,
            onValueChange = { benefits = it },
            label = { Text("Benefits (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = alternatives,
            onValueChange = { alternatives = it },
            label = { Text("Alternatives (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = complications,
            onValueChange = { complications = it },
            label = { Text("Complications (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = materialRisks,
            onValueChange = { materialRisks = it },
            label = { Text("Material Risks (optional — falls back to Complications if left blank)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = postOpCare,
            onValueChange = { postOpCare = it },
            label = { Text("Post-operative Care") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = expectedRecovery,
            onValueChange = { expectedRecovery = it },
            label = { Text("Expected Recovery") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        MedicalAutoCompleteTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("Special Instructions / Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )
    }
}
