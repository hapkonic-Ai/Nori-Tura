package com.example.nori_tura.presentation.opd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.AiDiagnosisResponse
import com.example.nori_tura.data.dto.AiSuggestions
import com.example.nori_tura.data.dto.DifferentialDiagnosis
import com.example.nori_tura.data.dto.InvestigationCreateDto
import com.example.nori_tura.data.dto.MedicationCreateDto
import com.example.nori_tura.data.dto.OpdRecordCreateRequest

private data class MedicationFormData(
    val name: String = "",
    val dose: String = "",
    val frequency: String = "",
    val duration: String = ""
)

private data class InvestigationFormData(
    val type: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdConsultScreen(
    patientId: String,
    isNurse: Boolean = false,
    viewModel: OpdConsultViewModel = viewModel(key = patientId) { OpdConsultViewModel(patientId) },
    onBack: () -> Unit,
    onRecordSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val patientName by viewModel.patientName.collectAsState()

    var visitType by remember { mutableStateOf("new") }
    var complaint by remember { mutableStateOf("") }
    var examination by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var surgicalDecision by remember { mutableStateOf("") }
    var plannedProcedure by remember { mutableStateOf("") }
    var advice by remember { mutableStateOf("") }
    var followUpDate by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf(listOf<MedicationFormData>()) }
    var investigations by remember { mutableStateOf(listOf<InvestigationFormData>()) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is OpdConsultViewModel.UiState.Success -> {
                onRecordSaved()
            }

            is OpdConsultViewModel.UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetError()
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OPD Consult - ${patientName ?: "Patient"}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (isNurse) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = "For Surgeon Review",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item {
                    VisitTypeDropdown(
                        selected = visitType,
                        onSelectedChange = { visitType = it }
                    )
                }

                item {
                    OutlinedTextField(
                        value = complaint,
                        onValueChange = { complaint = it },
                        label = { Text("Chief Complaint") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item {
                    OutlinedTextField(
                        value = examination,
                        onValueChange = { examination = it },
                        label = { Text("Examination Findings") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }

                item {
                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        label = { Text(if (isNurse) "Provisional Findings" else "Diagnosis") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item {
                    OutlinedTextField(
                        value = plannedProcedure,
                        onValueChange = { plannedProcedure = it },
                        label = { Text("Planned Procedure") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    if (isNurse) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Nurses cannot enter surgical decisions. Leave this field empty or ask the surgeon.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = surgicalDecision,
                            onValueChange = { surgicalDecision = it },
                            label = { Text("Surgical Decision") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = advice,
                        onValueChange = { advice = it },
                        label = { Text("Advice") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item {
                    OutlinedTextField(
                        value = followUpDate,
                        onValueChange = { followUpDate = it },
                        label = { Text("Follow-up Date (ISO 8601)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    MedicationsSection(
                        medications = medications,
                        onMedicationsChange = { medications = it }
                    )
                }

                item {
                    InvestigationsSection(
                        investigations = investigations,
                        onInvestigationsChange = { investigations = it }
                    )
                }

                if (!isNurse) {
                    item {
                        AiPanel(
                            uiState = uiState,
                            onGetSuggestions = {
                                viewModel.requestAiSuggestions(complaint, examination)
                            },
                            onUseInvestigations = { suggestedTypes ->
                                val existing = investigations.map { it.type }.toSet()
                                val new = suggestedTypes
                                    .filter { it.isNotBlank() && it !in existing }
                                    .map { InvestigationFormData(it) }
                                investigations = investigations + new
                                viewModel.clearAiSuggestions()
                            }
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            val request = OpdRecordCreateRequest(
                                visitType = visitType,
                                complaint = complaint,
                                examination = examination,
                                diagnosis = diagnosis.takeIf { it.isNotBlank() },
                                surgicalDecision = if (isNurse) null else surgicalDecision.takeIf { it.isNotBlank() },
                                plannedProcedure = plannedProcedure.takeIf { it.isNotBlank() },
                                advice = advice.takeIf { it.isNotBlank() },
                                followUpDate = followUpDate.takeIf { it.isNotBlank() },
                                medications = medications.map {
                                    MedicationCreateDto(
                                        name = it.name,
                                        dose = it.dose,
                                        frequency = it.frequency,
                                        duration = it.duration
                                    )
                                },
                                investigations = investigations.map {
                                    InvestigationCreateDto(type = it.type)
                                }
                            )
                            viewModel.saveOpdRecord(request)
                        },
                        enabled = uiState !is OpdConsultViewModel.UiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isNurse) "Submit for Surgeon Review" else "Save OPD Record")
                    }
                }
            }

            if (uiState is OpdConsultViewModel.UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitTypeDropdown(
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("new" to "New", "follow_up" to "Follow Up")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Visit Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelectedChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MedicationsSection(
    medications: List<MedicationFormData>,
    onMedicationsChange: (List<MedicationFormData>) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Medications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            medications.forEachIndexed { index, medication ->
                MedicationRow(
                    medication = medication,
                    onChange = { updated ->
                        onMedicationsChange(medications.toMutableList().apply { set(index, updated) })
                    },
                    onRemove = {
                        onMedicationsChange(medications.toMutableList().apply { removeAt(index) })
                    }
                )
                if (index < medications.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onMedicationsChange(medications + MedicationFormData()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Medication")
            }
        }
    }
}

@Composable
private fun MedicationRow(
    medication: MedicationFormData,
    onChange: (MedicationFormData) -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = medication.name,
            onValueChange = { onChange(medication.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = medication.dose,
                onValueChange = { onChange(medication.copy(dose = it)) },
                label = { Text("Dose") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = medication.frequency,
                onValueChange = { onChange(medication.copy(frequency = it)) },
                label = { Text("Frequency") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = medication.duration,
                onValueChange = { onChange(medication.copy(duration = it)) },
                label = { Text("Duration") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRemove) {
                Text("Remove")
            }
        }
    }
}

@Composable
private fun InvestigationsSection(
    investigations: List<InvestigationFormData>,
    onInvestigationsChange: (List<InvestigationFormData>) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Investigations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            investigations.forEachIndexed { index, investigation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = investigation.type,
                        onValueChange = { updated ->
                            onInvestigationsChange(
                                investigations.toMutableList().apply { set(index, InvestigationFormData(updated)) }
                            )
                        },
                        label = { Text("Investigation Type") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onInvestigationsChange(investigations.toMutableList().apply { removeAt(index) })
                    }) {
                        Text("Remove")
                    }
                }
                if (index < investigations.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onInvestigationsChange(investigations + InvestigationFormData()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Investigation")
            }
        }
    }
}

@Composable
private fun AiPanel(
    uiState: OpdConsultViewModel.UiState,
    onGetSuggestions: () -> Unit,
    onUseInvestigations: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Suggestive Diagnosis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is OpdConsultViewModel.UiState.AiSuggestions -> {
                    AiSuggestionsContent(
                        suggestions = state.response.suggestions,
                        disclaimer = state.response.disclaimer,
                        onUseInvestigations = onUseInvestigations
                    )
                }

                is OpdConsultViewModel.UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing complaint and examination...")
                    }
                }

                is OpdConsultViewModel.UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onGetSuggestions) {
                        Text("Retry")
                    }
                }

                else -> {
                    Button(onClick = onGetSuggestions) {
                        Text("Get AI Suggestions")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSuggestionsContent(
    suggestions: AiSuggestions,
    disclaimer: String?,
    onUseInvestigations: (List<String>) -> Unit
) {
    suggestions.differentialDiagnosis.forEachIndexed { index, diagnosis ->
        SuggestionCard(index = index, diagnosis = diagnosis)
        Spacer(modifier = Modifier.height(8.dp))
    }

    val effectiveDisclaimer = disclaimer ?: suggestions.disclaimer
    if (!effectiveDisclaimer.isNullOrBlank()) {
        Text(
            text = "Disclaimer: $effectiveDisclaimer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    val allInvestigations = suggestions.recommendedInvestigations
    if (allInvestigations.isNotEmpty()) {
        OutlinedButton(
            onClick = { onUseInvestigations(allInvestigations) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use Suggested Investigations")
        }
    }
}

@Composable
private fun SuggestionCard(index: Int, diagnosis: DifferentialDiagnosis) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${index + 1}. ${diagnosis.name ?: "Unnamed diagnosis"}",
                fontWeight = FontWeight.SemiBold
            )
            if (!diagnosis.reasoning.isNullOrBlank()) {
                Text(
                    text = diagnosis.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
