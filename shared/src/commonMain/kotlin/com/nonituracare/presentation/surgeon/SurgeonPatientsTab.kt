package com.nonituracare.presentation.surgeon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SurgeonPatientsTab(
    modifier: Modifier = Modifier,
    onPatientClick: (String) -> Unit,
    onAddPatient: () -> Unit,
    onOpenAlerts: (() -> Unit)? = null
) {
    PatientListScreen(
        modifier = modifier,
        onBack = {},
        onPatientClick = onPatientClick,
        onAddPatient = onAddPatient,
        fabBottomPadding = 120.dp,
        onOpenAlerts = onOpenAlerts
    )
}
