package com.example.nori_tura.presentation.surgeon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SurgeonPatientsTab(
    modifier: Modifier = Modifier,
    onPatientClick: (String) -> Unit,
    onAddPatient: () -> Unit
) {
    PatientListScreen(
        modifier = modifier,
        onBack = {},
        onPatientClick = onPatientClick,
        onAddPatient = onAddPatient,
        fabBottomPadding = 120.dp
    )
}
