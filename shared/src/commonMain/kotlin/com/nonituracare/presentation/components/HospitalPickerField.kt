package com.nonituracare.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A hospital field for record-creation forms (new patient, OPD record, admission).
 * Only meaningful when the surgeon is affiliated with more than one hospital —
 * they work at all of them at once, so each record must say which one it's for.
 * Callers should skip rendering this entirely when `hospitals.size <= 1` and let
 * the backend default silently to the doctor's one affiliation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalPickerField(
    hospitals: List<HospitalOption>,
    selectedHospitalId: String?,
    onSelectedChange: (HospitalOption) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Hospital *"
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = hospitals.firstOrNull { it.id == selectedHospitalId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            hospitals.forEach { hospital ->
                DropdownMenuItem(
                    text = { Text(hospital.name) },
                    onClick = {
                        onSelectedChange(hospital)
                        expanded = false
                    }
                )
            }
        }
    }
}
