package com.nonituracare.presentation.surgeon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.PatientCreateRequest
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.HospitalOption
import com.nonituracare.presentation.components.HospitalPickerField
import com.nonituracare.presentation.components.NorituraScaffold

@Composable
fun AddPatientScreen(
    viewModel: AddPatientViewModel = viewModel { AddPatientViewModel() },
    onBack: () -> Unit,
    onPatientAdded: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val hospitals by viewModel.hospitals.collectAsState()
    val hospitalOptions = hospitals.map { HospitalOption(it.hospitalId, it.hospitalName ?: "Hospital") }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var selectedHospitalId by remember { mutableStateOf<String?>(null) }
    var hospitalNameText by remember { mutableStateOf("") }
    var enteringNewHospital by remember { mutableStateOf(false) }

    LaunchedEffect(hospitals) {
        if (selectedHospitalId == null && hospitals.isNotEmpty()) {
            selectedHospitalId = hospitals.firstOrNull { it.isPrimary }?.hospitalId
                ?: hospitals.firstOrNull()?.hospitalId
        }
        if (hospitals.isEmpty()) {
            enteringNewHospital = true
        }
    }

    LaunchedEffect(uiState) {
        (uiState as? AddPatientViewModel.UiState.Success)?.let { success ->
            success.patient.id?.let { onPatientAdded(it) }
            viewModel.resetState()
        }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Add Patient",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Patient",
                style = MaterialTheme.typography.headlineMedium
            )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the patient and parent details.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Patient Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { age = it.filter { char -> char.isDigit() } },
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        GenderDropdown(
            selected = gender,
            onSelectedChange = { gender = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bloodGroup,
            onValueChange = { bloodGroup = it },
            label = { Text("Blood Group (optional)") },
            placeholder = { Text("B+") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = allergies,
            onValueChange = { allergies = it },
            label = { Text("Allergies (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.isSurgeon) {
            Spacer(modifier = Modifier.height(12.dp))

            if (enteringNewHospital) {
                OutlinedTextField(
                    value = hospitalNameText,
                    onValueChange = { hospitalNameText = it },
                    label = { Text("Hospital *") },
                    placeholder = { Text("Which hospital is this patient at?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hospitalOptions.isNotEmpty()) {
                    TextButton(onClick = {
                        enteringNewHospital = false
                        hospitalNameText = ""
                    }) {
                        Text("Choose from existing hospitals instead")
                    }
                }
            } else {
                HospitalPickerField(
                    hospitals = hospitalOptions,
                    selectedHospitalId = selectedHospitalId,
                    onSelectedChange = { selectedHospitalId = it.id },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    enteringNewHospital = true
                    selectedHospitalId = null
                }) {
                    Text("+ Add a different hospital")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = parentName,
            onValueChange = { parentName = it },
            label = { Text("Parent / Guardian Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = parentPhone,
            onValueChange = { parentPhone = it },
            label = { Text("Parent Phone") },
            placeholder = { Text("+919876543210") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val request = PatientCreateRequest(
                    name = name.trim(),
                    age = age.toIntOrNull() ?: 0,
                    gender = gender.trim().lowercase(),
                    bloodGroup = bloodGroup.trim().takeIf { it.isNotBlank() },
                    allergies = allergies.trim().takeIf { it.isNotBlank() },
                    parentName = parentName.trim(),
                    parentPhone = parentPhone.trim(),
                    hospitalId = if (enteringNewHospital) null else selectedHospitalId,
                    hospitalName = if (enteringNewHospital) hospitalNameText.trim().takeIf { it.isNotBlank() } else null
                )
                viewModel.createPatient(request)
            },
            enabled = uiState !is AddPatientViewModel.UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is AddPatientViewModel.UiState.Loading) {
                CircularProgressIndicator()
            } else {
                Text("Save Patient")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }

        if (uiState is AddPatientViewModel.UiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (uiState as AddPatientViewModel.UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    selected: String,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("male", "female", "other")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
