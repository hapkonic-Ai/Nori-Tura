package com.example.nori_tura.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null
)

@Composable
fun NorituraBottomNav(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = NorituraColors.PrimaryBlue.copy(alpha = 0.08f),
                spotColor = NorituraColors.PrimaryBlue.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(NorituraColors.Surface)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                NavItem(
                    item = item,
                    selected = selected,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 110.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) NorituraColors.PrimaryBlue else NorituraColors.TextTertiary,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .width(pillWidth)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) NorituraColors.PrimaryBlueLight else NorituraColors.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (selected && item.selectedIcon != null) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
            ) {
                Text(
                    text = item.label,
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }
        }
    }
}
