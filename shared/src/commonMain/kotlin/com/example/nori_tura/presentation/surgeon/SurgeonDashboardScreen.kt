package com.example.nori_tura.presentation.surgeon

import androidx.compose.runtime.Composable

@Composable
fun SurgeonDashboardScreen(
    onNavigateToPatientList: () -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit,
    onNavigateToPatientProfile: (String) -> Unit
) {
    SurgeonDashboardTab(
        onNavigateToPatientList = onNavigateToPatientList,
        onNavigateToAddPatient = onNavigateToAddPatient,
        onNavigateToAppointments = onNavigateToAppointments,
        onNavigateToSurgicalTemplates = onNavigateToSurgicalTemplates,
        onNavigateToAdmissions = onNavigateToAdmissions,
        onNavigateToPatientProfile = onNavigateToPatientProfile
    )
}
