package com.example.nori_tura.presentation.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.DoctorDto
import com.example.nori_tura.data.dto.PatientDto
import com.example.nori_tura.presentation.components.Avatar
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.openUrl

@Composable
fun ParentProfileScreen(
    viewModel: ParentProfileViewModel = viewModel { ParentProfileViewModel() },
    onBack: () -> Unit,
    onNavigateToConsentView: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "PT",
                title = "Profile",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) { paddingValues ->
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
                ProfileContent(
                    profile = state.profile,
                    onNavigateToConsentView = onNavigateToConsentView,
                    onLogout = onLogout,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: ParentProfileViewModel.Profile,
    onNavigateToConsentView: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val child = profile.child
    val doctor = profile.doctor

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        if (child != null) {
            item {
                ChildCard(child = child)
            }

            item {
                SurgeonCard(doctor = doctor, onCall = { phone ->
                    openUrl("tel:$phone")
                })
            }
        } else {
            item {
                EmptyState(
                    title = "No child linked",
                    subtitle = "Contact your surgeon to link your phone number to a patient record."
                )
            }
        }

        item {
            Text(
                text = "Consent Form History",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (profile.consentForms.isEmpty()) {
            item {
                InlineProfileMessage("No consent forms yet.")
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profile.consentForms.forEach { consent ->
                        ConsentHistoryCard(
                            consent = consent,
                            onClick = {
                                if (consent.status == "signed") {
                                    openUrl(consent.signedPdfUrl ?: consent.pdfUrl ?: return@ConsentHistoryCard)
                                } else {
                                    consent.id?.let(onNavigateToConsentView)
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.Error),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ChildCard(child: PatientDto) {
    val hasAllergies = !child.allergies.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name = child.name ?: "?", size = 56.dp)
                Spacer(modifier = Modifier.height(12.dp).padding(start = 12.dp))
                Column {
                    Text(
                        text = child.name ?: "Unknown",
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${child.age ?: "-"} yrs • ${child.gender ?: "-"}${child.bloodGroup?.let { " • Blood: $it" } ?: ""}",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = NorituraColors.Divider)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Allergies",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasAllergies) NorituraColors.Error.copy(alpha = 0.12f)
                    else NorituraColors.AccentGreen.copy(alpha = 0.12f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = if (hasAllergies) child.allergies!! else "No Known Allergies",
                    color = if (hasAllergies) NorituraColors.Error else NorituraColors.AccentGreen,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SurgeonCard(
    doctor: DoctorDto?,
    onCall: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Treating Surgeon",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = doctor?.name ?: "-",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (!doctor?.hospitalName.isNullOrBlank()) {
                    Text(
                        text = doctor?.hospitalName ?: "",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            val surgeonPhone = doctor?.phone
            IconButton(
                onClick = { surgeonPhone?.let { onCall(it) } },
                enabled = !surgeonPhone.isNullOrBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call surgeon",
                    tint = if (surgeonPhone.isNullOrBlank()) NorituraColors.TextTertiary else NorituraColors.PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun ConsentHistoryCard(
    consent: ConsentFormDto,
    onClick: () -> Unit
) {
    val isSigned = consent.status == "signed"
    val statusColor = if (isSigned) NorituraColors.PostOp else NorituraColors.Warning
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consent.formType ?: "Consent Form",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = consent.status?.replaceFirstChar { it.uppercase() } ?: "Pending",
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generated: ${consent.generatedAt?.take(10) ?: "-"}",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InlineProfileMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
