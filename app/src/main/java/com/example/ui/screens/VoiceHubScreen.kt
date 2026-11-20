package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.AssistantMode
import com.example.ui.components.CosmicGlassCard
import com.example.ui.components.MayraVoiceOrb
import com.example.viewmodel.MayraUiState

@Composable
fun VoiceHubScreen(
    uiState: MayraUiState,
    hasMicPermission: Boolean,
    hasAllDevicePermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onToggleAssistant: (Boolean) -> Unit,
    onOrbPrimaryAction: () -> Unit,
    onStopLiveSession: () -> Unit,
    onInterruptPlayback: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val runtime = uiState.runtime
    val isLive = runtime.mode == AssistantMode.CONNECTING_LIVE ||
        runtime.mode == AssistantMode.LISTENING ||
        runtime.mode == AssistantMode.SPEAKING ||
        runtime.mode == AssistantMode.EXECUTING_TOOL

    AnimatedContent(
        targetState = isLive,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "voiceModeTransition",
        modifier = modifier.fillMaxSize()
    ) { liveActive ->
        if (liveActive) {
            ActiveLiveVoiceView(
                uiState = uiState,
                onInterruptPlayback = onInterruptPlayback,
                onStopLiveSession = onStopLiveSession
            )
        } else {
            StandbyVoiceHomeView(
                uiState = uiState,
                hasMicPermission = hasMicPermission,
                hasAllDevicePermissions = hasAllDevicePermissions,
                onRequestPermissions = onRequestPermissions,
                onToggleAssistant = onToggleAssistant,
                onStartLive = onOrbPrimaryAction,
                onOpenSettings = onOpenSettings,
                onOpenAbout = onOpenAbout
            )
        }
    }
}

@Composable
private fun StandbyVoiceHomeView(
    uiState: MayraUiState,
    hasMicPermission: Boolean,
    hasAllDevicePermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onToggleAssistant: (Boolean) -> Unit,
    onStartLive: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val runtime = uiState.runtime
    val settings = uiState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar matching uploaded screenshot: Menu | MAYRA AI / YOUR AI ASSISTANT | Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    IconButton(
                        onClick = onOpenAbout,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_menu_about_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(id = R.string.nav_about),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                                append("MAYRA ")
                            }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("AI")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "YOUR AI VOICE ASSISTANT",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(id = R.string.nav_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Studio Portrait Stage + Overlapping Glowing Mic Orb + Waveform
            MayraVoiceOrb(
                mode = runtime.mode,
                inputAmplitude = runtime.inputAmplitude,
                outputAmplitude = runtime.outputAmplitude,
                showStudioPortrait = true,
                onClick = {
                    if (!hasMicPermission) onRequestPermissions() else onStartLive()
                },
                modifier = Modifier.testTag("mic_primary_button")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (!hasMicPermission) "Tap orb to enable mic" else "Tap to Speak",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (settings.wakeWordEnabled) {
                    "or say \"Hey Mayra\""
                } else {
                    "Wake-word paused in Settings"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Compact Assistant ON/OFF switch pill
            CosmicGlassCard(
                highlighted = settings.assistantEnabled,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (settings.assistantEnabled && hasMicPermission) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    },
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (settings.assistantEnabled) {
                                "Assistant is ON"
                            } else {
                                "Assistant is OFF"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = settings.assistantEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !hasMicPermission) {
                                onRequestPermissions()
                            } else {
                                onToggleAssistant(checked)
                            }
                        },
                        modifier = Modifier.testTag("assistant_master_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(visible = runtime.lastToolEvent != null) {
                runtime.lastToolEvent?.let { event ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (event.success) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                            }
                        ),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (event.success) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.ErrorOutline
                                },
                                contentDescription = null,
                                tint = if (event.success) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = event.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = runtime.errorMessage != null) {
                runtime.errorMessage?.let { errorText ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (!hasMicPermission || !hasAllDevicePermissions) {
                CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (!hasMicPermission) {
                                "Enable Mic for \"Hey Mayra\" & Live Voice"
                            } else {
                                "Grant Contacts & Phone for calls/WhatsApp"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onRequestPermissions,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("grant_permissions_button")
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveLiveVoiceView(
    uiState: MayraUiState,
    onInterruptPlayback: () -> Unit,
    onStopLiveSession: () -> Unit
) {
    val runtime = uiState.runtime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onStopLiveSession,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("live_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back_button)
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.img_mayra_portrait_1795120963893),
                        contentDescription = stringResource(id = R.string.cd_mayra_avatar),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Mayra",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Online",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onInterruptPlayback,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("live_interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Interrupt Mayra",
                        tint = if (runtime.mode == AssistantMode.SPEAKING) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                MayraVoiceOrb(
                    mode = runtime.mode,
                    inputAmplitude = runtime.inputAmplitude,
                    outputAmplitude = runtime.outputAmplitude,
                    showStudioPortrait = true,
                    onClick = onInterruptPlayback
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = runtime.statusHeadline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = runtime.statusSubtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                runtime.lastToolEvent?.let { event ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = event.summary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    border = BorderStroke(3.dp, Color.White.copy(alpha = 0.25f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(onClick = onStopLiveSession)
                        .testTag("stop_live_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Tap to Stop",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to Stop",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
