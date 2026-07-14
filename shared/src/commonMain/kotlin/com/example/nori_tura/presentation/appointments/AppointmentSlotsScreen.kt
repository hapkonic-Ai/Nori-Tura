package com.example.nori_tura.presentation.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nori_tura.data.dto.AvailableSlotDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.SectionTitle
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun AppointmentSlotsScreen(
    appointmentId: String,
    slots: List<AvailableSlotDto>,
    viewModel: AppointmentViewModel,
    onBack: () -> Unit,
    onSlotSelected: (appointmentId: String) -> Unit
) {
    val selectedSlot by viewModel.selectedSlot.collectAsState()

    // Group slots by date (first 10 chars of slot_datetime: "YYYY-MM-DD")
    val slotsByDate = slots
        .filter { it.is_available }
        .groupBy { it.slot_datetime.take(10) }
        .toSortedMap()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                title = "Choose a Slot",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (slots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No available slots at this time.",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(NorituraColors.Background)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    slotsByDate.forEach { (date, dateSlots) ->
                        item {
                            SectionTitle(title = date)
                        }
                        items(dateSlots) { slot ->
                            val isSelected = selectedSlot?.slot_id == slot.slot_id
                            SlotCard(
                                slot = slot,
                                isSelected = isSelected,
                                onSelect = { viewModel.selectSlot(slot) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // Bottom confirm button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NorituraColors.Surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        selectedSlot?.let { viewModel.selectSlot(it) }
                        onSlotSelected(appointmentId)
                    },
                    enabled = selectedSlot != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NorituraColors.PrimaryBlue,
                        disabledContainerColor = NorituraColors.PrimaryBlue.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "Confirm Slot",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: AvailableSlotDto,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    // Extract time portion: chars 11-16 (HH:mm)
    val timeDisplay = if (slot.slot_datetime.length >= 16) slot.slot_datetime.substring(11, 16) else slot.slot_datetime

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (isSelected) Modifier.border(
                width = 2.dp,
                color = NorituraColors.PrimaryBlue,
                shape = RoundedCornerShape(16.dp)
            ) else Modifier
        )

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NorituraColors.PrimaryBlueLight else NorituraColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = timeDisplay,
                    color = if (isSelected) NorituraColors.PrimaryBlue else NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = slot.surgeon_name,
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(
                onClick = onSelect,
                shape = RoundedCornerShape(8.dp),
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = NorituraColors.PrimaryBlue
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = NorituraColors.TextSecondary
                    )
                }
            ) {
                Text(
                    text = if (isSelected) "Selected" else "Select",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
