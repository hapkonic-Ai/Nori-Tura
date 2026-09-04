package com.nonituracare.presentation.surgeon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors

@Composable
fun DoctorProfileTab(
    modifier: Modifier = Modifier,
    viewModel: DoctorProfileViewModel = viewModel { DoctorProfileViewModel() },
    onLogout: () -> Unit,
    onOpenAlerts: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Profile",
                onNotificationClick = onOpenAlerts
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = uiState) {
                is DoctorProfileViewModel.UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DoctorProfileViewModel.UiState.Error -> {
                    Text(
                        text = "Could not load profile: ${state.message}",
                        color = NorituraColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                is DoctorProfileViewModel.UiState.Success -> {
                    val profile = state.me.profile ?: state.me.doctor
                    val name = profile?.name ?: "Dr. Unknown"
                    val phone = profile?.phone ?: state.me.phone ?: "—"
                    val specialty = profile?.specialty ?: "Surgeon"
                    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

                    val hospitalNames = state.me.hospitals.orEmpty().mapNotNull { it.hospitalName }

                    // Avatar + name card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(NorituraColors.PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials.ifBlank { "DR" },
                                    color = NorituraColors.PrimaryBlue,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = NorituraColors.TextPrimary,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = specialty,
                                color = NorituraColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            // Freelancing badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NorituraColors.AccentGreenLight)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Freelancing Surgeon",
                                    color = NorituraColors.AccentGreen,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    // Details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ProfileRow(
                                icon = Icons.Default.Phone,
                                label = "Phone",
                                value = phone
                            )
                            HorizontalDivider(color = NorituraColors.Divider)
                            ProfileRow(
                                icon = Icons.Default.MedicalServices,
                                label = "Specialty",
                                value = specialty
                            )
                            HorizontalDivider(color = NorituraColors.Divider)
                            if (hospitalNames.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = NorituraColors.PrimaryBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Works At",
                                            color = NorituraColors.TextTertiary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRowCompat(hospitalNames)
                                    }
                                }
                            } else {
                                ProfileRow(
                                    icon = Icons.Default.Badge,
                                    label = "Practice Type",
                                    value = "Multi-hospital · Freelancing"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.Error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(hospitalNames: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        hospitalNames.forEach { name ->
            StatusChip(label = name, color = NorituraColors.PrimaryBlue, showDot = false)
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NorituraColors.PrimaryBlue,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = label,
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
