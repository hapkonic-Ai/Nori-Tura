package com.nonituracare.presentation.surgeon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nonituracare.presentation.components.BottomNavItem
import com.nonituracare.presentation.components.NorituraBottomNav
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.ipd.AdmissionsListScreen

@Composable
fun SurgeonMainScreen(
    onNavigateToPatientProfile: (String) -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToOtNoteTemplates: () -> Unit = {},
    onNavigateToFollowUpPreview: (String) -> Unit,
    onNavigateToConsentView: (String) -> Unit = {},
    onNavigateToAdmissionDetail: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAlerts by rememberSaveable { mutableStateOf(false) }

    val items = listOf(
        BottomNavItem("Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
        BottomNavItem("Patients", Icons.Outlined.People, Icons.Filled.People),
        BottomNavItem("Admissions", Icons.Outlined.LocalHospital, Icons.Filled.LocalHospital),
        BottomNavItem("Follow-ups", Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Filled.Assignment),
        BottomNavItem("Profile", Icons.Outlined.Person, Icons.Filled.Person)
    )

    NorituraScaffold(
        bottomBar = {
            NorituraBottomNav(
                items = items,
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        }
    ) {
        if (showAlerts) {
            SurgeonAlertsTab(
                modifier = Modifier.fillMaxSize(),
                onBack = { showAlerts = false },
                onNavigateToConsent = onNavigateToConsentView,
                onNavigateToAppointment = { onNavigateToAppointments() },
                onNavigateToReview = { patientId -> onNavigateToPatientProfile(patientId) },
                onNavigateToAdmission = onNavigateToAdmissionDetail
            )
            return@NorituraScaffold
        }
        val openAlerts = { showAlerts = true }
        when (selectedTab.coerceIn(0, items.lastIndex)) {
            0 -> SurgeonDashboardTab(
                modifier = Modifier.fillMaxSize(),
                onNavigateToAddPatient = onNavigateToAddPatient,
                onNavigateToSurgicalTemplates = onNavigateToSurgicalTemplates,
                onNavigateToOtNoteTemplates = onNavigateToOtNoteTemplates,
                onNavigateToAdmissions = { selectedTab = 2 },
                onOpenAlerts = openAlerts
            )
            1 -> SurgeonPatientsTab(
                modifier = Modifier.fillMaxSize(),
                onPatientClick = onNavigateToPatientProfile,
                onAddPatient = onNavigateToAddPatient,
                onOpenAlerts = openAlerts
            )
            2 -> AdmissionsListScreen(
                onAdmissionClick = onNavigateToAdmissionDetail,
                onOpenAlerts = openAlerts
            )
            3 -> SurgeonFollowUpsTab(
                modifier = Modifier.fillMaxSize(),
                onNavigateToPreview = onNavigateToFollowUpPreview,
                onOpenAlerts = openAlerts
            )
            4 -> DoctorProfileTab(
                modifier = Modifier.fillMaxSize(),
                onLogout = onLogout,
                onOpenAlerts = openAlerts
            )
        }
    }
}
