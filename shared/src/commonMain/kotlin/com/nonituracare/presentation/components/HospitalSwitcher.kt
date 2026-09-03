package com.nonituracare.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nonituracare.ui.theme.NorituraColors

data class HospitalOption(
    val id: String,
    val name: String
)

/**
 * A compact "current location" style pill (hospital icon + name + chevron) that
 * opens a dropdown of hospitals on tap. Mirrors the Zomato-style location switcher:
 * one hospital is always the current context, tapping it lets you pick another.
 */
@Composable
fun HospitalSwitcher(
    hospitals: List<HospitalOption>,
    selectedHospitalId: String?,
    onSelect: (HospitalOption) -> Unit,
    modifier: Modifier = Modifier
) {
    if (hospitals.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val selected = hospitals.firstOrNull { it.id == selectedHospitalId } ?: hospitals.first()

    Row(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(NorituraColors.PrimaryBlueLight)
                .clickable(enabled = hospitals.size > 1) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .widthIn(max = 220.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                tint = NorituraColors.PrimaryBlue,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = selected.name,
                color = NorituraColors.PrimaryBlue,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (hospitals.size > 1) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Switch hospital",
                    tint = NorituraColors.PrimaryBlue,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            hospitals.forEach { hospital ->
                DropdownMenuItem(
                    text = { Text(hospital.name) },
                    onClick = {
                        expanded = false
                        onSelect(hospital)
                    }
                )
            }
        }
    }
}
