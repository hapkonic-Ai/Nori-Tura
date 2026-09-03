package com.nonituracare.presentation.admin

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
import androidx.compose.material3.Switch
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
import com.nonituracare.data.dto.ContentTemplateCreateRequest
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.ContentTemplateUpdateRequest
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentTemplatesScreen(
    viewModel: AdminContentTemplatesViewModel = viewModel { AdminContentTemplatesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ContentTemplateDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AdminContentTemplatesViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AdminContentTemplatesViewModel.UiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content Templates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add content template")
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
                is AdminContentTemplatesViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AdminContentTemplatesViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load content templates")
                        Button(onClick = { viewModel.load() }) { Text("Retry") }
                    }
                }

                is AdminContentTemplatesViewModel.UiState.Success -> {
                    if (state.templates.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No content templates yet",
                                color = NorituraColors.TextSecondary
                            )
                            Text(
                                text = "Tap + to add one common to every doctor.",
                                color = NorituraColors.TextTertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.templates, key = { it.id }) { template ->
                                ContentTemplateCard(
                                    template = template,
                                    onEdit = { editingTemplate = template },
                                    onDelete = { viewModel.deleteTemplate(template.id) },
                                    onToggleActive = { viewModel.toggleActive(template) }
                                )
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
            ContentTemplateFormDialog(
                title = "New Content Template",
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
            ContentTemplateFormDialog(
                title = "Edit Content Template",
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
private fun ContentTemplateCard(
    template: ContentTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = template.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = template.procedure,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(
                    label = if (template.isActive) "Active" else "Inactive",
                    color = if (template.isActive) NorituraColors.AccentGreen else NorituraColors.TextTertiary
                )
                Switch(checked = template.isActive, onCheckedChange = { onToggleActive() })
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            template.approach?.let {
                Text(text = "Approach: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (template.investigations.isNotEmpty()) {
                Text(text = "Investigations: ${template.investigations.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            template.statutoryReference?.let {
                Text(text = "Statutory reference: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun ContentTemplateCreateRequest.toUpdateRequest(): ContentTemplateUpdateRequest =
    ContentTemplateUpdateRequest(
        name = name,
        procedure = procedure,
        approach = approach,
        technique = technique,
        riskLevel = riskLevel,
        specialInstructions = specialInstructions,
        investigations = investigations,
        procedureDescription = procedureDescription,
        anesthesia = anesthesia,
        risks = risks,
        benefits = benefits,
        alternatives = alternatives,
        possibleComplications = possibleComplications,
        materialRisks = materialRisks,
        postOpCare = postOpCare,
        expectedRecovery = expectedRecovery,
        statutoryReference = statutoryReference,
        isActive = isActive
    )

@Composable
private fun ContentTemplateFormCard(
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
private fun ContentTemplateFormDialog(
    title: String,
    initial: ContentTemplateDto?,
    onDismiss: () -> Unit,
    onSave: (ContentTemplateCreateRequest) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var procedure by remember { mutableStateOf(initial?.procedure ?: "") }
    var approach by remember { mutableStateOf(initial?.approach ?: "") }
    var technique by remember { mutableStateOf(initial?.technique ?: "") }
    var riskLevel by remember { mutableStateOf(initial?.riskLevel ?: "") }
    var specialInstructions by remember { mutableStateOf(initial?.specialInstructions ?: "") }
    var investigations by remember { mutableStateOf(initial?.investigations?.joinToString(", ") ?: "") }
    var procedureDescription by remember { mutableStateOf(initial?.procedureDescription ?: "") }
    var anesthesia by remember { mutableStateOf(initial?.anesthesia?.joinToString(", ") ?: "") }
    var risks by remember { mutableStateOf(initial?.risks?.joinToString(", ") ?: "") }
    var benefits by remember { mutableStateOf(initial?.benefits?.joinToString(", ") ?: "") }
    var alternatives by remember { mutableStateOf(initial?.alternatives?.joinToString(", ") ?: "") }
    var possibleComplications by remember { mutableStateOf(initial?.possibleComplications?.joinToString(", ") ?: "") }
    var materialRisks by remember { mutableStateOf(initial?.materialRisks ?: "") }
    var postOpCare by remember { mutableStateOf(initial?.postOpCare ?: "") }
    var expectedRecovery by remember { mutableStateOf(initial?.expectedRecovery ?: "") }
    var statutoryReference by remember { mutableStateOf(initial?.statutoryReference ?: "") }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }

    ContentTemplateFormCard(
        title = title,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                ContentTemplateCreateRequest(
                    name = name,
                    procedure = procedure,
                    approach = approach.takeIf { it.isNotBlank() },
                    technique = technique.takeIf { it.isNotBlank() },
                    riskLevel = riskLevel.takeIf { it.isNotBlank() },
                    specialInstructions = specialInstructions.takeIf { it.isNotBlank() },
                    investigations = investigations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    procedureDescription = procedureDescription.takeIf { it.isNotBlank() },
                    anesthesia = anesthesia.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    risks = risks.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    benefits = benefits.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    alternatives = alternatives.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    possibleComplications = possibleComplications.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    materialRisks = materialRisks.takeIf { it.isNotBlank() },
                    postOpCare = postOpCare.takeIf { it.isNotBlank() },
                    expectedRecovery = expectedRecovery.takeIf { it.isNotBlank() },
                    statutoryReference = statutoryReference.takeIf { it.isNotBlank() },
                    isActive = isActive
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
        OutlinedTextField(
            value = procedure,
            onValueChange = { procedure = it },
            label = { Text("Procedure *") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = approach,
            onValueChange = { approach = it },
            label = { Text("Approach (e.g. Laparoscopic, Open)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = technique,
            onValueChange = { technique = it },
            label = { Text("Technique") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = riskLevel,
            onValueChange = { riskLevel = it },
            label = { Text("Risk Level") },
            placeholder = { Text("e.g. Low / Moderate / High") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = investigations,
            onValueChange = { investigations = it },
            label = { Text("Pre-op Investigations (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = anesthesia,
            onValueChange = { anesthesia = it },
            label = { Text("Anesthesia (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = procedureDescription,
            onValueChange = { procedureDescription = it },
            label = { Text("Procedure Description") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = risks,
            onValueChange = { risks = it },
            label = { Text("Risks (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = benefits,
            onValueChange = { benefits = it },
            label = { Text("Benefits (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = alternatives,
            onValueChange = { alternatives = it },
            label = { Text("Alternatives (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = possibleComplications,
            onValueChange = { possibleComplications = it },
            label = { Text("Possible Complications (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = materialRisks,
            onValueChange = { materialRisks = it },
            label = { Text("Material Risks") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = postOpCare,
            onValueChange = { postOpCare = it },
            label = { Text("Post-operative Care") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = expectedRecovery,
            onValueChange = { expectedRecovery = it },
            label = { Text("Expected Recovery") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = statutoryReference,
            onValueChange = { statutoryReference = it },
            label = { Text("Statutory Reference") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = specialInstructions,
            onValueChange = { specialInstructions = it },
            label = { Text("Special Instructions / Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Active (visible to doctors)")
            Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
    }
}
