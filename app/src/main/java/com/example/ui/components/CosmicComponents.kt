package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.service.AssistantMode

@Composable
fun CosmicBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(surface, bg, bg)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.16f),
                        radius = size.maxDimension * 0.48f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.72f),
                        radius = size.maxDimension * 0.42f
                    )
                )
            },
        content = content
    )
}

@Composable
fun CosmicGlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp,
        content = content
    )
}

@Composable
fun AssistantStatusPill(
    mode: AssistantMode,
    modifier: Modifier = Modifier
) {
    val dotTargetColor = when (mode) {
        AssistantMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        AssistantMode.STANDBY_WAKE_WORD -> MaterialTheme.colorScheme.tertiary
        AssistantMode.CONNECTING_LIVE -> MaterialTheme.colorScheme.secondary
        AssistantMode.LISTENING -> MaterialTheme.colorScheme.secondary
        AssistantMode.SPEAKING -> MaterialTheme.colorScheme.primary
        AssistantMode.EXECUTING_TOOL -> MaterialTheme.colorScheme.tertiary
        AssistantMode.ERROR -> MaterialTheme.colorScheme.error
    }
    val dotColor by animateColorAsState(dotTargetColor, label = "statusDotColor")

    val labelText = when (mode) {
        AssistantMode.OFF -> "OFFLINE"
        AssistantMode.STANDBY_WAKE_WORD -> "WAKE READY • \"MAYRA\""
        AssistantMode.CONNECTING_LIVE -> "CONNECTING LIVE"
        AssistantMode.LISTENING -> "LIVE • LISTENING"
        AssistantMode.SPEAKING -> "MAYRA SPEAKING"
        AssistantMode.EXECUTING_TOOL -> "RUNNING ACTION"
        AssistantMode.ERROR -> "ATTENTION NEEDED"
    }

    val pulseTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (mode == AssistantMode.OFF) dotColor
                        else dotColor.copy(alpha = pulseAlpha)
                    )
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
