package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.PatientDto
import com.example.nori_tura.presentation.components.Avatar
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.SearchField
import com.example.nori_tura.presentation.components.StatusChip
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun PatientListScreen(
    modifier: Modifier = Modifier,
    viewModel: PatientListViewModel = viewModel { PatientListViewModel() },
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit,
    onAddPatient: (() -> Unit)? = null,
    fabBottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val diagnosisQuery by viewModel.diagnosisQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    val statusFilters = listOf(
        null to "All",
        "outpatient" to "Outpatient",
        "pre-op" to "Pre-op",
        "in-surgery" to "In OT",
        "recovery" to "Recovery",
        "discharged" to "Discharged"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Patients",
                onBack = onBack,
                notificationCount = 0
            )
        },
        floatingActionButton = {
            onAddPatient?.let {
                Box(
                    modifier = Modifier.padding(bottom = fabBottomPadding)
                ) {
                    FloatingActionButton(
                        onClick = it,
                        containerColor = NorituraColors.PrimaryBlue,
                        contentColor = NorituraColors.Surface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Patient",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = "Search by name or parent phone"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SearchField(
                value = diagnosisQuery,
                onValueChange = { viewModel.onDiagnosisQueryChange(it) },
                placeholder = "Search by diagnosis"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusFilters.forEach { (status, label) ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { viewModel.onStatusFilterSelected(status) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState) {
                is PatientListViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is PatientListViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.fetchPatients() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is PatientListViewModel.UiState.Success -> {
                    if (state.patients.isEmpty()) {
                        EmptyState(
                            title = "No patients found",
                            subtitle = if (searchQuery.isBlank()) {
                                "You haven't added any patients yet."
                            } else {
                                "Try a different search term."
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.patients, key = { it.id ?: it.hashCode() }) { patient ->
                                PatientListRow(
                                    patient = patient,
                                    onClick = {
                                        patient.id?.let(onPatientClick)
                                    }
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
private fun PatientListRow(
    patient: PatientDto,
    onClick: () -> Unit
) {
    val status = when (patient.ipdAdmissions?.firstOrNull()?.status?.lowercase()) {
        "pre-op" -> "Pre-op" to NorituraColors.PreOp
        "in-surgery" -> "In OT" to NorituraColors.InOt
        "recovery" -> "Recovery" to NorituraColors.PostOp
        else -> "Outpatient" to NorituraColors.TextTertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(name = patient.name ?: "?", size = 48.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = patient.name ?: "Unknown",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "ID: ${patient.id ?: "-"} • ${patient.age ?: "-"} yrs • ${patient.gender ?: "-"}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            StatusChip(
                label = status.first,
                color = status.second,
                showDot = true
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open",
                tint = NorituraColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
