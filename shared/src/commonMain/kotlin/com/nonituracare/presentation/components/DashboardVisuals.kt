package com.nonituracare.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A thin ring around a stat number — flat/near-empty at zero, fills in as the
 * count grows relative to [target]. Purely decorative today (there's no
 * historical trend data yet) but signals "this number belongs to a live
 * metric," not a static label.
 */
@Composable
fun RadialProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = color.copy(alpha = 0.15f),
    content: @Composable () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "radialProgress"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.toPx() - stroke.width, size.toPx() - stroke.width),
                topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.toPx() - stroke.width, size.toPx() - stroke.width),
                    topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2)
                )
            }
        }
        content()
    }
}

/** A thin horizontal bar split into colored segments — a quiet sub-breakdown
 * under a headline number (e.g. total patients by ward/status). Segments with
 * zero share simply don't render, so it degrades gracefully to an empty track. */
@Composable
fun MiniSegmentBar(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0x14000000),
    height: Dp = 6.dp
) {
    val total = segments.sumOf { it.first.toDouble() }.toFloat()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (total > 0f) {
            for ((value, color) in segments) {
                if (value <= 0f) continue
                val animatedWeight by animateFloatAsState(
                    targetValue = value / total,
                    animationSpec = tween(600),
                    label = "segmentWeight"
                )
                Box(
                    modifier = Modifier
                        .weight(animatedWeight.coerceAtLeast(0.001f))
                        .height(height)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
    }
}

/** Animates a headline number counting up from 0 whenever [value] changes,
 * instead of just snapping the text in — a small but persistent bit of life
 * on an otherwise static dashboard. */
@Composable
fun AnimatedCountText(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(600),
        label = "countUp"
    )
    Text(
        text = animated.toInt().toString(),
        color = color,
        style = style,
        modifier = modifier
    )
}

/** A calm, muted icon + caption for a metric that's genuinely at zero right
 * now — reserving the big bold number treatment for when there's real data,
 * so an empty dashboard reads as "nothing scheduled" rather than "broken." */
@Composable
fun EmptyStateGlyph(
    caption: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.EventBusy,
    tint: Color = Color(0xFFB7BBCB)
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = caption,
            color = tint,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Horizontal patient-flow pipeline: Pre-op -> In-Operation -> Post-op
 * Recovery, each stage a colored pill with its count, connected by a thin
 * line. This is the mental model a surgeon actually has (a pipeline), not
 * three unrelated numbers.
 */
@Composable
fun PatientFlowStepper(
    preOp: Int,
    inOt: Int,
    postOp: Int,
    preOpColor: Color,
    inOtColor: Color,
    postOpColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowStage(label = "Pre-op", count = preOp, color = preOpColor, modifier = Modifier.weight(1f))
        FlowConnector(color = preOpColor)
        FlowStage(label = "In-Operation", count = inOt, color = inOtColor, modifier = Modifier.weight(1f))
        FlowConnector(color = inOtColor)
        FlowStage(label = "Recovery", count = postOp, color = postOpColor, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FlowConnector(color: Color) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .height(2.dp)
            .background(color.copy(alpha = 0.35f))
    )
}

@Composable
private fun FlowStage(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedCountText(
                value = count,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}
