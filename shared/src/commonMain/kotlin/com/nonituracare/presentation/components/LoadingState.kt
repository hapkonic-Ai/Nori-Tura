package com.nonituracare.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.loading_pulse
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = NorituraColors.PrimaryBlue,
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = message,
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = NorituraColors.PrimaryBlue,
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
    }
}

/**
 * The one dedicated loading illustration in the app — a gently pulsing
 * heartbeat line, reserved for the longest/first loads (app launch, a
 * generated PDF) where a few seconds of wait is expected and a small
 * reassuring visual helps. Every other spinner in the app stays a plain
 * [LoadingState]/[FullScreenLoading] — using this everywhere would work
 * against the feeling that most loads are instant.
 */
@Composable
fun PulseLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    val transition = rememberInfiniteTransition(label = "pulseLoading")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.loading_pulse),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(48.dp)
                    .alpha(pulseAlpha)
            )
            Text(
                text = message,
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
