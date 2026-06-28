package com.example.nori_tura.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ParentBottomNav(
    selectedRoute: String = "home",
    onHome: () -> Unit,
    onRecords: () -> Unit,
    onProfile: () -> Unit
) {
    NavigationBar(containerColor = NorituraColors.Surface) {
        val items = listOf(
            Triple("home", "Home", Icons.Filled.Home),
            Triple("records", "Records", Icons.AutoMirrored.Filled.List),
            Triple("profile", "Profile", Icons.Filled.Person)
        )

        items.forEach { (route, label, icon) ->
            val selected = selectedRoute == route
            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = selected,
                onClick = {
                    when (route) {
                        "home" -> onHome()
                        "records" -> onRecords()
                        "profile" -> onProfile()
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NorituraColors.PrimaryBlue,
                    selectedTextColor = NorituraColors.PrimaryBlue,
                    indicatorColor = NorituraColors.PrimaryBlueLight,
                    unselectedIconColor = NorituraColors.TextSecondary,
                    unselectedTextColor = NorituraColors.TextSecondary
                )
            )
        }
    }
}

@Composable
fun NurseBottomNav(
    selectedRoute: String = "home",
    onHome: () -> Unit,
    onPatients: () -> Unit,
    onAppointments: () -> Unit
) {
    NavigationBar(containerColor = NorituraColors.Surface) {
        val items = listOf(
            Triple("home", "Home", Icons.Filled.Home),
            Triple("patients", "Patients", Icons.AutoMirrored.Filled.List),
            Triple("appointments", "Appointments", Icons.Filled.CalendarMonth)
        )

        items.forEach { (route, label, icon) ->
            val selected = selectedRoute == route
            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = selected,
                onClick = {
                    when (route) {
                        "home" -> onHome()
                        "patients" -> onPatients()
                        "appointments" -> onAppointments()
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NorituraColors.PrimaryBlue,
                    selectedTextColor = NorituraColors.PrimaryBlue,
                    indicatorColor = NorituraColors.PrimaryBlueLight,
                    unselectedIconColor = NorituraColors.TextSecondary,
                    unselectedTextColor = NorituraColors.TextSecondary
                )
            )
        }
    }
}
