package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.SurgeonRepository
import com.example.nori_tura.data.dto.PatientDto
import com.example.nori_tura.data.dto.ScheduleSlotDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.StatusChip
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.formatDisplayDate
import com.example.nori_tura.util.isoDateDayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = viewModel { ScheduleViewModel() },
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val patientRepository = remember { SurgeonRepository() }

    LaunchedEffect(Unit) {
        val result = patientRepository.getPatients("")
        result.onSuccess { viewModel.setPatients(it) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Schedule",
                onBack = onBack,
                notificationCount = 0
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showBookingDialog,
                containerColor = NorituraColors.PrimaryBlue,
                contentColor = NorituraColors.Surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Booking")
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

            when (val state = uiState) {
                is ScheduleViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is ScheduleViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadSlots() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ScheduleViewModel.UiState.Success -> {
                    ScheduleContent(
                        state = state,
                        onSelectTab = viewModel::selectTab,
                        onSelectDate = viewModel::selectDate,
                        onPreviousWeek = viewModel::previousWeek,
                        onNextWeek = viewModel::nextWeek,
                        onSlotClick = { slot ->
                            slot.patient?.id?.let(onPatientClick)
                        },
                        onBookEmptySlot = { time ->
                            viewModel.updateBookingForm(
                                state.bookingForm.copy(time = time)
                            )
                            viewModel.showBookingDialog()
                        }
                    )

                    if (state.showBookingDialog) {
                        BookingDialog(
                            state = state,
                            onDismiss = viewModel::dismissBookingDialog,
                            onFormChange = viewModel::updateBookingForm,
                            onBook = { viewModel.bookSlot() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    state: ScheduleViewModel.UiState.Success,
    onSelectTab: (ScheduleViewModel.Tab) -> Unit,
    onSelectDate: (String) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSlotClick: (ScheduleSlotDto) -> Unit,
    onBookEmptySlot: (String) -> Unit
) {
    val tabTitles = listOf("OT Schedule", "OPD Schedule")
    val selectedIndex = if (state.selectedTab == ScheduleViewModel.Tab.OT) 0 else 1

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = NorituraColors.Background,
        contentColor = NorituraColors.PrimaryBlue
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedIndex == index,
                onClick = {
                    onSelectTab(
                        if (index == 0) ScheduleViewModel.Tab.OT else ScheduleViewModel.Tab.OPD
                    )
                },
                text = { Text(title, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    WeekStrip(
        weekDays = state.weekDays,
        selectedDate = state.selectedDate,
        onSelectDate = onSelectDate,
        onPreviousWeek = onPreviousWeek,
        onNextWeek = onNextWeek
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "${formatDisplayDate(state.selectedDate)} • ${state.slots.size} slots",
        color = NorituraColors.TextSecondary,
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(12.dp))

    SlotList(
        slots = state.slots,
        onSlotClick = onSlotClick,
        onBookEmptySlot = onBookEmptySlot
    )
}

@Composable
private fun WeekStrip(
    weekDays: List<String>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Previous week",
                tint = NorituraColors.PrimaryBlue
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { day ->
                val selected = day == selectedDate
                val dayNumber = day.takeLast(2)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) NorituraColors.PrimaryBlue else Color.Transparent)
                        .clickable { onSelectDate(day) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = isoDateDayName(day),
                        color = if (selected) Color.White else NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = dayNumber,
                        color = if (selected) Color.White else NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        IconButton(onClick = onNextWeek) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Next week",
                tint = NorituraColors.PrimaryBlue
            )
        }
    }
}

@Composable
private fun SlotList(
    slots: List<ScheduleSlotDto>,
    onSlotClick: (ScheduleSlotDto) -> Unit,
    onBookEmptySlot: (String) -> Unit
) {
    val times = remember { generateTimeSlots() }
    val bookedTimes = slots.mapNotNull { it.slotDatetime?.timePart() }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (slots.isEmpty()) {
            item {
                EmptyState(
                    title = "No bookings",
                    subtitle = "Tap an empty slot or the + button to create one.",
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        }

        items(times) { time ->
            val slot = slots.find { it.slotDatetime?.timePart() == time }
            if (slot != null) {
                SlotCard(slot = slot, onClick = { onSlotClick(slot) })
            } else {
                EmptySlotRow(time = time, onClick = { onBookEmptySlot(time) })
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: ScheduleSlotDto,
    onClick: () -> Unit
) {
    val isOt = slot.visitType?.lowercase()?.contains("surgery") == true ||
            slot.visitType?.lowercase()?.contains("ot") == true
    val statusColor = when (slot.status?.lowercase()) {
        "scheduled" -> NorituraColors.PrimaryBlue
        "in-progress", "in_surgery" -> NorituraColors.InOt
        "completed" -> NorituraColors.PostOp
        "cancelled" -> NorituraColors.Error
        else -> NorituraColors.TextTertiary
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NorituraColors.PrimaryBlueLight)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = slot.slotDatetime?.timePart() ?: "--:--",
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.patient?.name ?: "Unknown",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = if (isOt) {
                        "${slot.procedure ?: "Surgery"} • ${slot.urgency ?: "routine"}"
                    } else {
                        slot.visitType?.replaceFirstChar { it.uppercase() } ?: "Consult"
                    },
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            StatusChip(
                label = slot.status?.replaceFirstChar { it.uppercase() } ?: "Booked",
                color = statusColor,
                showDot = true
            )
        }
    }
}

@Composable
private fun EmptySlotRow(
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = NorituraColors.Divider,
                shape = RoundedCornerShape(12.dp)
            )
            .background(NorituraColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = time,
            color = NorituraColors.TextTertiary,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Book slot",
            tint = NorituraColors.PrimaryBlue
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingDialog(
    state: ScheduleViewModel.UiState.Success,
    onDismiss: () -> Unit,
    onFormChange: (ScheduleViewModel.BookingForm) -> Unit,
    onBook: () -> Unit
) {
    val form = state.bookingForm
    val isOt = state.selectedTab == ScheduleViewModel.Tab.OT

    var patientMenuExpanded by remember { mutableStateOf(false) }
    var timeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isOt) "New OT Booking" else "New OPD Booking",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NorituraColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Patient dropdown
            Box {
                OutlinedTextField(
                    value = state.patients.find { it.id == form.patientId }?.name ?: "Select patient",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Patient") },
                    trailingIcon = {
                        TextButton(onClick = { patientMenuExpanded = true }) {
                            Text("Choose")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = patientMenuExpanded,
                    onDismissRequest = { patientMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    state.patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.name ?: "Unknown") },
                            onClick = {
                                patient.id?.let {
                                    onFormChange(form.copy(patientId = it))
                                }
                                patientMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time dropdown
            Box {
                OutlinedTextField(
                    value = form.time,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Time") },
                    trailingIcon = {
                        TextButton(onClick = { timeMenuExpanded = true }) {
                            Text("Choose")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = timeMenuExpanded,
                    onDismissRequest = { timeMenuExpanded = false }
                ) {
                    generateTimeSlots().forEach { time ->
                        DropdownMenuItem(
                            text = { Text(time) },
                            onClick = {
                                onFormChange(form.copy(time = time))
                                timeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isOt) {
                OutlinedTextField(
                    value = form.procedure,
                    onValueChange = { onFormChange(form.copy(procedure = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Procedure") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                SimpleDropdown(
                    label = "Urgency",
                    value = form.urgency,
                    options = listOf("routine", "urgent", "emergency"),
                    onValueChange = { onFormChange(form.copy(urgency = it)) }
                )
            } else {
                SimpleDropdown(
                    label = "Visit type",
                    value = form.visitType,
                    options = listOf("opd", "consult", "follow-up"),
                    onValueChange = { onFormChange(form.copy(visitType = it)) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onBook,
                    modifier = Modifier.weight(1f),
                    enabled = form.patientId.isNotBlank() && form.time.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Book")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                TextButton(onClick = { expanded = true }) {
                    Text("Choose")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun generateTimeSlots(): List<String> {
    val slots = mutableListOf<String>()
    for (hour in 8..16) {
        val h = hour.toString().padStart(2, '0')
        slots.add("$h:00")
        slots.add("$h:30")
    }
    return slots
}

private fun String.timePart(): String? {
    val index = indexOf('T')
    if (index == -1) return null
    return substring(index + 1).take(5)
}
