package com.nonituracare.presentation.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.dashboard_greeting_banner
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.DoctorDto
import com.nonituracare.presentation.components.Avatar
import com.nonituracare.presentation.components.BottomNavItem
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.KpiTile
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.LongPressCardPreview
import com.nonituracare.presentation.components.NorituraBottomNav
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.NorituraSurfaceCard
import com.nonituracare.presentation.components.SectionTitle
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors

@Composable
fun AdminHomeScreen(
    isSuperAdmin: Boolean = false,
    viewModel: AdminViewModel = viewModel { AdminViewModel() },
    onLogout: () -> Unit,
    onNavigateToNurses: () -> Unit = {},
    onNavigateToHospitals: () -> Unit = {},
    onNavigateToContentTemplates: () -> Unit = {},
    onNavigateToSurgicalTemplates: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
        BottomNavItem("Nurses", Icons.Outlined.PersonAdd, Icons.Filled.PersonAdd),
        BottomNavItem("Hospitals", Icons.Outlined.LocalHospital, Icons.Filled.LocalHospital),
        BottomNavItem("Content", Icons.Outlined.Description, Icons.Filled.Description),
        BottomNavItem("Templates", Icons.Outlined.MedicalServices, Icons.Filled.MedicalServices)
    )

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = if (isSuperAdmin) "SA" else "AD",
                title = if (isSuperAdmin) "Super Admin" else "Admin",
                notificationCount = 0,
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = NorituraColors.TextSecondary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NorituraBottomNav(
                items = bottomNavItems,
                selectedIndex = 0,
                onItemSelected = { index ->
                    when (index) {
                        1 -> onNavigateToNurses()
                        2 -> onNavigateToHospitals()
                        3 -> onNavigateToContentTemplates()
                        4 -> onNavigateToSurgicalTemplates()
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(Res.drawable.dashboard_greeting_banner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Doctor Approvals",
                            color = NorituraColors.TextPrimary,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Review and approve pending doctor registrations.",
                            color = NorituraColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            when (val state = uiState) {
                is AdminViewModel.UiState.Loading -> {
                    item {
                        LoadingState(
                            message = "Loading approvals...",
                            modifier = Modifier.fillParentMaxHeight(0.6f)
                        )
                    }
                }

                is AdminViewModel.UiState.Error -> {
                    item {
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadDashboard() },
                            modifier = Modifier.fillParentMaxHeight(0.6f)
                        )
                    }
                }

                is AdminViewModel.UiState.Success -> {
                    val dashboard = state.dashboard

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiTile(
                                label = "Pending",
                                value = dashboard.pendingCount.toString(),
                                icon = Icons.Default.Person,
                                iconTint = NorituraColors.Warning,
                                accentColor = NorituraColors.Warning,
                                modifier = Modifier.weight(1f)
                            )
                            KpiTile(
                                label = "Total Doctors",
                                value = dashboard.totalCount.toString(),
                                icon = Icons.Default.MedicalServices,
                                iconTint = NorituraColors.PrimaryBlue,
                                accentColor = NorituraColors.PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item { SectionTitle(title = "Platform") }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiTile(
                                label = "Hospitals",
                                value = dashboard.stats.hospitals.toString(),
                                icon = Icons.Default.LocalHospital,
                                iconTint = NorituraColors.AccentGreen,
                                accentColor = NorituraColors.AccentGreen,
                                modifier = Modifier.weight(1f)
                            )
                            KpiTile(
                                label = "Nurses",
                                value = "${dashboard.stats.nurses.active}/${dashboard.stats.nurses.total}",
                                icon = Icons.Default.Person,
                                iconTint = NorituraColors.PrimaryBlue,
                                accentColor = NorituraColors.PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiTile(
                                label = "Patients",
                                value = dashboard.stats.patients.toString(),
                                icon = Icons.Default.Person,
                                iconTint = NorituraColors.AccentGreen,
                                accentColor = NorituraColors.AccentGreen,
                                modifier = Modifier.weight(1f)
                            )
                            KpiTile(
                                label = "Active Admissions",
                                value = dashboard.stats.admissions.active.toString(),
                                icon = Icons.Default.MedicalServices,
                                iconTint = NorituraColors.Warning,
                                accentColor = NorituraColors.Warning,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SectionTitle(
                            title = "Pending Registrations",
                            actionLabel = "Refresh",
                            onAction = { viewModel.loadDashboard() }
                        )
                    }

                    if (dashboard.pendingDoctors.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No pending registrations",
                                subtitle = "All doctor registrations have been reviewed.",
                                modifier = Modifier.fillParentMaxHeight(0.4f)
                            )
                        }
                    } else {
                        items(dashboard.pendingDoctors, key = { it.id }) { doctor ->
                            DoctorApprovalCard(
                                doctor = doctor,
                                onApprove = { viewModel.approveDoctor(doctor.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorApprovalCard(
    doctor: DoctorDto,
    onApprove: () -> Unit
) {
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Doctor Preview"
    ) {
        NorituraSurfaceCard {
            Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(name = doctor.name, size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doctor.name,
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = doctor.specialty ?: "-",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusChip(
                    label = "Pending",
                    color = NorituraColors.Warning,
                    showDot = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = NorituraColors.Divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Phone", value = doctor.phone)

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.AccentGreen),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Approve Doctor")
            }
        }
    }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = NorituraColors.TextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}
