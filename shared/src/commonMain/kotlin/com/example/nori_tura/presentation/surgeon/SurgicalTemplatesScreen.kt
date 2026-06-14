package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.SurgicalTemplateCreateRequest
import com.example.nori_tura.data.dto.SurgicalTemplateDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgicalTemplatesScreen(
    viewModel: SurgicalTemplatesViewModel = viewModel { SurgicalTemplatesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
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
            FloatingActionButton(onClick = { showCreateDialog = true }) {
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
                                onDelete = { viewModel.deleteTemplate(template.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            CreateTemplateDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { request ->
                    viewModel.createTemplate(request)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: SurgicalTemplateDto,
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
        }
    }
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onCreate: (SurgicalTemplateCreateRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var procedure by remember { mutableStateOf("") }
    var approach by remember { mutableStateOf("") }
    var anaesthesia by remember { mutableStateOf("") }
    var investigations by remember { mutableStateOf("") }
    var riskLevel by remember { mutableStateOf("") }
    var technique by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "New Surgical Template",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth()
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
                label = { Text("Approach") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = anaesthesia,
                onValueChange = { anaesthesia = it },
                label = { Text("Anaesthesia (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = investigations,
                onValueChange = { investigations = it },
                label = { Text("Investigations (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = riskLevel,
                onValueChange = { riskLevel = it },
                label = { Text("Risk Level") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = technique,
                onValueChange = { technique = it },
                label = { Text("Technique") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Special Instructions") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onCreate(
                            SurgicalTemplateCreateRequest(
                                name = name,
                                procedure = procedure,
                                approach = approach.takeIf { it.isNotBlank() },
                                anaesthesia = anaesthesia.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                investigations = investigations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                riskLevel = riskLevel.takeIf { it.isNotBlank() },
                                technique = technique.takeIf { it.isNotBlank() },
                                specialInstructions = instructions.takeIf { it.isNotBlank() }
                            )
                        )
                    },
                    enabled = name.isNotBlank() && procedure.isNotBlank()
                ) {
                    Text("Create")
                }
            }
        }
    }
}
