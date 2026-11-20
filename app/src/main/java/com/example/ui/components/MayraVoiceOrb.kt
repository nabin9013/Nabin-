package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.service.AssistantMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Editorial studio portrait stage of Mayra feathered seamlessly with alpha masking
 * into the cosmic canvas, paired with an overlapping glowing 3D neon microphone orb.
 */
@Composable
fun MayraVoiceOrb(
    mode: AssistantMode,
    inputAmplitude: Float,
    outputAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStudioPortrait: Boolean = true
) {
    val activeEnergy = when (mode) {
        AssistantMode.SPEAKING -> maxOf(outputAmplitude, 0.28f)
        AssistantMode.LISTENING -> maxOf(inputAmplitude, 0.18f)
        AssistantMode.CONNECTING_LIVE,
        AssistantMode.EXECUTING_TOOL -> 0.38f
        AssistantMode.STANDBY_WAKE_WORD -> maxOf(inputAmplitude * 0.65f, 0.14f)
        AssistantMode.OFF, AssistantMode.ERROR -> 0.03f
    }

    val smoothEnergy by animateFloatAsState(
        targetValue = activeEnergy,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "orbEnergy"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mayraStudioMotion")

    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    val counterRotationDeg by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterHaloRotation"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val floatCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatCycle"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    val ringColors = when (mode) {
        AssistantMode.SPEAKING -> listOf(primary, Color(0xFFFF4D94), secondary, primary)
        AssistantMode.LISTENING -> listOf(secondary, tertiary, primary, secondary)
        AssistantMode.STANDBY_WAKE_WORD -> listOf(primary, Color(0xFFB388FF), secondary, primary)
        AssistantMode.CONNECTING_LIVE,
        AssistantMode.EXECUTING_TOOL -> listOf(secondary, primary, secondary)
        AssistantMode.ERROR -> listOf(errorColor, primary, errorColor)
        AssistantMode.OFF -> listOf(Color.Gray.copy(alpha = 0.35f), Color.DarkGray.copy(alpha = 0.35f))
    }

    val isActive = mode != AssistantMode.OFF
    val portraitLift = if (isActive) sin(floatCycle) * 3.5f else 0f
    val orbScale = 1f + (smoothEnergy * 0.08f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showStudioPortrait) 340.dp else 210.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (showStudioPortrait) {
                // Full-stage dark-studio portrait feathered to transparent edges via DstIn
                Box(
                    modifier = Modifier
                        .size(width = 256.dp, height = 316.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            translationY = portraitLift.dp.toPx()
                        }
                        .drawWithContent {
                            drawContent()
                            // Soft top & bottom transparency fade
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.08f to Color.Black,
                                    0.64f to Color.Black,
                                    0.98f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                            // Soft left & right transparency feathering
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0.0f to Color.Transparent,
                                    0.14f to Color.Black,
                                    0.86f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mayra_portrait_1795120963893),
                        contentDescription = stringResource(id = R.string.cd_mayra_avatar),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Overlapping Neon Microphone / Voice Orb
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension * 0.34f
                    val pulseR = baseR + (smoothEnergy * size.minDimension * 0.09f)

                    // Glowing aura behind the orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.42f + smoothEnergy * 0.25f),
                                secondary.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.50f
                        ),
                        center = center,
                        radius = size.minDimension * 0.50f
                    )

                    // Outer rotating neon ring
                    rotate(rotationDeg, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(ringColors, center = center),
                            radius = pulseR,
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Counter-rotating inner orbital arc
                    rotate(counterRotationDeg, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(secondary, Color.Transparent, primary, Color.Transparent)
                            ),
                            startAngle = 0f,
                            sweepAngle = 200f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - (pulseR + 6.dp.toPx()),
                                center.y - (pulseR + 6.dp.toPx())
                            ),
                            size = Size((pulseR + 6.dp.toPx()) * 2f, (pulseR + 6.dp.toPx()) * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                val interactionSource = remember { MutableInteractionSource() }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .size(102.dp)
                        .graphicsLayer {
                            scaleX = orbScale
                            scaleY = orbScale
                        }
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(ringColors),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = primary),
                            role = Role.Button,
                            onClick = onClick
                        )
                        .testTag("voice_orb_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (mode == AssistantMode.SPEAKING) {
                                Icons.Rounded.GraphicEq
                            } else {
                                Icons.Rounded.Mic
                            },
                            contentDescription = stringResource(id = R.string.cd_mic_toggle),
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        AudioWaveformStrip(
            energy = smoothEnergy,
            phase = wavePhase,
            active = isActive,
            primaryColor = primary,
            secondaryColor = secondary,
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(40.dp)
        )
    }
}

@Composable
private fun AudioWaveformStrip(
    energy: Float,
    phase: Float,
    active: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bars = 29
        val gap = size.width / (bars * 1.9f)
        val barWidth = (size.width - gap * (bars - 1)) / bars
        val centerY = size.height / 2f
        val maxHalfHeight = size.height * 0.45f

        for (i in 0 until bars) {
            val distanceFromMid = abs(i - (bars - 1) / 2f) / ((bars - 1) / 2f)
            val envelope = 1f - (distanceFromMid * 0.65f)
            val osc = if (active) {
                0.35f + 0.65f * abs(sin(phase * 1.4f + i * 0.5f))
            } else {
                0.15f
            }
            val baseHeight = if (active) (3.dp.toPx() + 4.dp.toPx() * envelope * osc) else 2.5.dp.toPx()
            val halfH = (baseHeight + maxHalfHeight * envelope * energy * osc)
                .coerceAtMost(maxHalfHeight)
            val x = i * (barWidth + gap)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, primaryColor, secondaryColor)
                ),
                topLeft = Offset(x, centerY - halfH),
                size = Size(barWidth, halfH * 2f),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                alpha = if (active) 0.94f else 0.35f
            )
        }
    }
}
