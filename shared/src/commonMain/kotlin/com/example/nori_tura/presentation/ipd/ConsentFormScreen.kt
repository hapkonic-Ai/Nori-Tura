package com.example.nori_tura.presentation.ipd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.nori_tura.data.ConsentFormResponse
import com.example.nori_tura.data.dto.ConsentFormCreateRequest
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ConsentFormScreen(
    admissionId: String,
    viewModel: ConsentFormViewModel = viewModel { ConsentFormViewModel() },
    onBack: () -> Unit,
    onConsentCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var formType by remember { mutableStateOf("Surgical Consent") }
    var procedure by remember { mutableStateOf("") }
    var anesthesia by remember { mutableStateOf("") }
    var risks by remember { mutableStateOf("") }
    var benefits by remember { mutableStateOf("") }
    var alternatives by remember { mutableStateOf("") }
    var postOpCare by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is ConsentFormViewModel.UiState.Success) {
            val consentId = (uiState as ConsentFormViewModel.UiState.Success).response.consentForm.id
            onConsentCreated(consentId)
            viewModel.resetState()
        }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "New Consent Form",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Admission ID: $admissionId",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = formType,
                onValueChange = { formType = it },
                label = { Text("Form Type *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = procedure,
                onValueChange = { procedure = it },
                label = { Text("Procedure *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = anesthesia,
                onValueChange = { anesthesia = it },
                label = { Text("Anesthesia *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = risks,
                onValueChange = { risks = it },
                label = { Text("Risks *") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = benefits,
                onValueChange = { benefits = it },
                label = { Text("Benefits *") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = alternatives,
                onValueChange = { alternatives = it },
                label = { Text("Alternatives *") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = postOpCare,
                onValueChange = { postOpCare = it },
                label = { Text("Post-operative Care *") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState is ConsentFormViewModel.UiState.Error) {
                Text(
                    text = (uiState as ConsentFormViewModel.UiState.Error).message,
                    color = NorituraColors.Error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    viewModel.createConsentForm(
                        ConsentFormCreateRequest(
                            admissionId = admissionId,
                            formType = formType,
                            procedure = procedure,
                            anesthesia = anesthesia,
                            risks = risks,
                            benefits = benefits,
                            alternatives = alternatives,
                            postOpCare = postOpCare
                        )
                    )
                },
                enabled = uiState !is ConsentFormViewModel.UiState.Loading &&
                    formType.isNotBlank() &&
                    procedure.isNotBlank() &&
                    anesthesia.isNotBlank() &&
                    risks.isNotBlank() &&
                    benefits.isNotBlank() &&
                    alternatives.isNotBlank() &&
                    postOpCare.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is ConsentFormViewModel.UiState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Text("Generate Consent Form")
                }
            }
        }
    }
}
