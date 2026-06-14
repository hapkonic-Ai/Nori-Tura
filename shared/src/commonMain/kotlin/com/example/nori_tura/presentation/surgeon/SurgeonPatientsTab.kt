package com.example.nori_tura.presentation.surgeon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SurgeonPatientsTab(
    modifier: Modifier = Modifier,
    onPatientClick: (String) -> Unit
) {
    PatientListScreen(
        modifier = modifier,
        onBack = {},
        onPatientClick = onPatientClick,
        onAddPatient = {}
    )
}
