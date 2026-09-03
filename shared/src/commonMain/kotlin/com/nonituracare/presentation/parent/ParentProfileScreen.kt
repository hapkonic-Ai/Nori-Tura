package com.nonituracare.presentation.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.ui.theme.NorituraColors

@Composable
fun ParentProfileScreen(
    viewModel: ParentProfileViewModel = viewModel { ParentProfileViewModel() },
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "PT",
                title = "Profile",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        when (val state = uiState) {
            is ParentProfileViewModel.UiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }

            is ParentProfileViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadProfile() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is ParentProfileViewModel.UiState.Success -> {
                ProfileContent(profile = state.profile, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: ParentProfileViewModel.Profile,
    onLogout: () -> Unit
) {
    val name = profile.parentName ?: "Parent"
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
                        text = initials.ifBlank { "PT" },
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
                    text = profile.parentPhone ?: "-",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${profile.childrenCount} ${if (profile.childrenCount == 1) "child" else "children"} registered",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.Error),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
