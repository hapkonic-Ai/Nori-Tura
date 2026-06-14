package com.example.nori_tura.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun NorituraScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
        ) {
            topBar?.invoke()
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
        }
        floatingActionButton?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = if (bottomBar != null) 104.dp else 24.dp, end = 24.dp)
            ) {
                it()
            }
        }
        bottomBar?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                it()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NorituraTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    avatar: @Composable (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    notificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 4.dp else 20.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
            onBack?.let { back ->
                IconButton(
                    onClick = back,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NorituraColors.TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                avatar?.invoke()
                title()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(44.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(
                                containerColor = NorituraColors.Error,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = notificationCount.coerceAtMost(99).toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = NorituraColors.TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BrandTopBar(
    initials: String = "DR",
    title: String = "SurgiCare",
    onBack: (() -> Unit)? = null,
    notificationCount: Int = 3,
    onNotificationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    NorituraTopBar(
        title = {
            Text(
                text = title,
                color = NorituraColors.PrimaryBlue,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        onBack = onBack,
        avatar = {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = NorituraColors.PrimaryBlueLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = NorituraColors.PrimaryBlue,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        notificationCount = notificationCount,
        onNotificationClick = onNotificationClick,
        actions = actions
    )
}

@Composable
fun NorituraSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            onClick = onClick
        ) {
            content()
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SectionSpacer(height: Int = 24) {
    Spacer(modifier = Modifier.height(height.dp))
}
