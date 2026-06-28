package com.example.nori_tura.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable

@Composable
fun ParentBottomNav(
    selectedRoute: String = "home",
    onHome: () -> Unit,
    onRecords: () -> Unit,
    onProfile: () -> Unit
) {
    val items = listOf(
        BottomNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem("Records", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
        BottomNavItem("Profile", Icons.Outlined.Person, Icons.Filled.Person)
    )

    val selectedIndex = when (selectedRoute) {
        "records" -> 1
        "profile" -> 2
        else -> 0
    }

    NorituraBottomNav(
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = { index ->
            when (index) {
                0 -> onHome()
                1 -> onRecords()
                2 -> onProfile()
            }
        }
    )
}

@Composable
fun NurseBottomNav(
    selectedRoute: String = "home",
    onHome: () -> Unit,
    onPatients: () -> Unit,
    onAppointments: () -> Unit
) {
    val items = listOf(
        BottomNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem("Patients", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
        BottomNavItem("Appointments", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    )

    val selectedIndex = when (selectedRoute) {
        "patients" -> 1
        "appointments" -> 2
        else -> 0
    }

    NorituraBottomNav(
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = { index ->
            when (index) {
                0 -> onHome()
                1 -> onPatients()
                2 -> onAppointments()
            }
        }
    )
}
