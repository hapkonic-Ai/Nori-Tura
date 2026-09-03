package com.nonituracare.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.AdminSurgicalTemplateDto
import com.nonituracare.ui.theme.NorituraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSurgicalTemplatesScreen(
    viewModel: AdminSurgicalTemplatesViewModel = viewModel { AdminSurgicalTemplatesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctors' Surgical Templates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AdminSurgicalTemplatesViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AdminSurgicalTemplatesViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load surgical templates")
                        Button(onClick = { viewModel.load() }) { Text("Retry") }
                    }
                }

                is AdminSurgicalTemplatesViewModel.UiState.Success -> {
                    if (state.templates.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No doctor-created templates yet",
                                color = NorituraColors.TextSecondary
                            )
                            Text(
                                text = "Templates doctors build for themselves will show up here.",
                                color = NorituraColors.TextTertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.templates, key = { it.id }) { template ->
                                AdminSurgicalTemplateCard(template = template)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSurgicalTemplateCard(template: AdminSurgicalTemplateDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = template.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = template.procedure,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Dr. ${template.doctor?.name ?: "Unknown"}" +
                    (template.doctor?.specialty?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = NorituraColors.TextSecondary
            )
            template.doctor?.hospitalName?.let {
                Text(text = "Hospital: $it", style = MaterialTheme.typography.bodySmall)
            }
            template.approach?.let {
                Text(text = "Approach: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (template.investigations.isNotEmpty()) {
                Text(
                    text = "Investigations: ${template.investigations.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.procedureDescription?.let {
                Text(text = "Procedure description: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (template.complications.isNotEmpty()) {
                Text(
                    text = "Complications: ${template.complications.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            template.materialRisks?.let {
                Text(text = "Material risks: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
