package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.nori_tura.presentation.components.BottomNavItem
import com.example.nori_tura.presentation.components.NorituraBottomNav
import com.example.nori_tura.presentation.components.NorituraScaffold

@Composable
fun SurgeonMainScreen(
    onNavigateToPatientProfile: (String) -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToSurgicalTemplates: () -> Unit,
    onNavigateToAdmissions: () -> Unit,
    onNavigateToFollowUpPreview: (String) -> Unit,
    onNavigateToConsentView: (String) -> Unit = {},
    onNavigateToAdmissionDetail: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val items = listOf(
        BottomNavItem("Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
        BottomNavItem("Patients", Icons.Outlined.People, Icons.Filled.People),
        BottomNavItem("Follow-ups", Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Filled.Assignment),
        BottomNavItem("Alerts", Icons.Outlined.Notifications, Icons.Filled.Notifications),
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
        when (selectedTab) {
            0 -> SurgeonDashboardTab(
                modifier = Modifier.fillMaxSize(),
                onNavigateToPatientList = { selectedTab = 1 },
                onNavigateToAddPatient = onNavigateToAddPatient,
                onNavigateToAppointments = onNavigateToAppointments,
                onNavigateToSurgicalTemplates = onNavigateToSurgicalTemplates,
                onNavigateToAdmissions = onNavigateToAdmissions,
                onNavigateToPatientProfile = onNavigateToPatientProfile
            )
            1 -> SurgeonPatientsTab(
                modifier = Modifier.fillMaxSize(),
                onPatientClick = onNavigateToPatientProfile,
                onAddPatient = onNavigateToAddPatient
            )
            2 -> SurgeonFollowUpsTab(
                modifier = Modifier.fillMaxSize(),
                onNavigateToPreview = onNavigateToFollowUpPreview
            )
            3 -> SurgeonAlertsTab(
                modifier = Modifier.fillMaxSize(),
                onNavigateToConsent = onNavigateToConsentView,
                onNavigateToAppointment = { onNavigateToAppointments() },
                onNavigateToReview = { patientId -> onNavigateToPatientProfile(patientId) },
                onNavigateToAdmission = onNavigateToAdmissionDetail
            )
            4 -> DoctorProfileTab(
                modifier = Modifier.fillMaxSize(),
                onLogout = onLogout
            )
        }
    }
}
