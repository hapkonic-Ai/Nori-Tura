package com.example.nori_tura.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
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
import com.example.nori_tura.data.dto.DoctorDto
import com.example.nori_tura.presentation.components.Avatar
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.KpiTile
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.NorituraSurfaceCard
import com.example.nori_tura.presentation.components.SectionTitle
import com.example.nori_tura.presentation.components.StatusChip
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun AdminHomeScreen(
    isSuperAdmin: Boolean = false,
    viewModel: AdminViewModel = viewModel { AdminViewModel() },
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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

            when (val state = uiState) {
                is AdminViewModel.UiState.Loading -> {
                    LoadingState(
                        message = "Loading approvals...",
                        modifier = Modifier.weight(1f)
                    )
                }

                is AdminViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadDashboard() },
                        modifier = Modifier.weight(1f)
                    )
                }

                is AdminViewModel.UiState.Success -> {
                    val dashboard = state.dashboard

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

                    SectionTitle(
                        title = "Pending Registrations",
                        actionLabel = "Refresh",
                        onAction = { viewModel.loadDashboard() }
                    )

                    if (dashboard.pendingDoctors.isEmpty()) {
                        EmptyState(
                            title = "No pending registrations",
                            subtitle = "All doctor registrations have been reviewed.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                        ) {
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
}

@Composable
private fun DoctorApprovalCard(
    doctor: DoctorDto,
    onApprove: () -> Unit
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
            InfoRow(label = "Hospital", value = doctor.hospital ?: "-")

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
