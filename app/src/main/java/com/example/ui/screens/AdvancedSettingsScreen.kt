package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MayraSettings
import com.example.ui.components.CosmicGlassCard
import com.example.viewmodel.MayraUiState
import java.util.Locale
import kotlin.math.roundToInt

private enum class AdvancedPicker {
    NONE, MODEL, MAX_TOKENS, VOICE
}

@Composable
fun AdvancedSettingsScreen(
    uiState: MayraUiState,
    hasMicPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectMaxTokens: (Int) -> Unit,
    onSelectVoice: (String) -> Unit,
    onChangeSpeechRate: (Float) -> Unit,
    onToggleInstantMode: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleEchoCancellation: (Boolean) -> Unit,
    onChangeSensitivity: (Float) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onResetDefaults: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    var activePicker by remember { mutableStateOf(AdvancedPicker.NONE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("advanced_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back_button)
                    )
                }
                Text(
                    text = "Advanced Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Section 1: AI Settings (Photo Screen 4)
            Text(
                text = "AI Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsNavRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Model",
                        valueText = settings.modelName,
                        testTag = "advanced_model_row",
                        onClick = { activePicker = AdvancedPicker.MODEL }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.DataUsage,
                        title = "Max Tokens",
                        valueText = settings.maxOutputTokens.toString(),
                        testTag = "advanced_max_tokens_row",
                        onClick = { activePicker = AdvancedPicker.MAX_TOKENS }
                    )
                }
            }

            // Section 2: Voice Settings (Photo Screen 4)
            Text(
                text = "Voice Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsNavRow(
                        icon = Icons.Rounded.RecordVoiceOver,
                        title = "TTS Voice",
                        valueText = settings.voiceName,
                        testTag = "advanced_voice_row",
                        onClick = { activePicker = AdvancedPicker.VOICE }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Speech Speed",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.1fx", settings.speechRateFloat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = settings.speechRateFloat,
                            onValueChange = onChangeSpeechRate,
                            valueRange = 0.75f..1.5f,
                            modifier = Modifier.testTag("speech_speed_slider")
                        )
                    }
                }
            }

            // Section 3: Performance (Photo Screen 4)
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Bolt,
                        title = "Instant Mode",
                        subtitle = "Ultra-fast response & smooth interruption",
                        checked = settings.instantModeEnabled,
                        testTag = "barge_in_switch",
                        onCheckedChange = onToggleInstantMode
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Hearing,
                        title = "Offline Wake-Word (\"Hey Mayra\")",
                        subtitle = "Local AudioRecord standby detection",
                        checked = settings.wakeWordEnabled,
                        testTag = "advanced_wakeword_switch",
                        onCheckedChange = { checked ->
                            if (checked && !hasMicPermission) {
                                onRequestPermissions()
                            } else {
                                onToggleWakeWord(checked)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsSwitchRow(
                        icon = Icons.Rounded.NoiseControlOff,
                        title = "Echo Cancellation",
                        subtitle = "AcousticEchoCanceler & noise suppression",
                        checked = settings.echoCancellationEnabled,
                        testTag = "aec_switch",
                        onCheckedChange = onToggleEchoCancellation
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Battery Reliability",
                        valueText = if (uiState.isBatteryOptimizationIgnored) "Unrestricted" else "Optimize",
                        testTag = "advanced_battery_row",
                        onClick = onOpenBatterySettings
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "\"Mayra\" Wake Sensitivity",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${(settings.wakeSensitivity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = settings.wakeSensitivity,
                            onValueChange = onChangeSensitivity,
                            valueRange = 0.2f..1.0f,
                            modifier = Modifier.testTag("sensitivity_slider")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Reset to Default Button matching Photo Screen 4
            Button(
                onClick = onResetDefaults,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("reset_defaults_button")
            ) {
                Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Reset to Default", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    when (activePicker) {
        AdvancedPicker.MODEL -> {
            AlertDialog(
                onDismissRequest = { activePicker = AdvancedPicker.NONE },
                title = { Text("Select Model") },
                text = {
                    Column {
                        MayraSettings.AVAILABLE_MODELS.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        onSelectModel(model)
                                        activePicker = AdvancedPicker.NONE
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.modelName == model,
                                    onClick = {
                                        onSelectModel(model)
                                        activePicker = AdvancedPicker.NONE
                                    }
                                )
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePicker = AdvancedPicker.NONE }) { Text("Close") }
                }
            )
        }

        AdvancedPicker.MAX_TOKENS -> {
            AlertDialog(
                onDismissRequest = { activePicker = AdvancedPicker.NONE },
                title = { Text("Max Tokens") },
                text = {
                    Column {
                        MayraSettings.AVAILABLE_MAX_TOKENS.forEach { tokens ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        onSelectMaxTokens(tokens)
                                        activePicker = AdvancedPicker.NONE
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.maxOutputTokens == tokens,
                                    onClick = {
                                        onSelectMaxTokens(tokens)
                                        activePicker = AdvancedPicker.NONE
                                    }
                                )
                                Text(
                                    text = "$tokens tokens",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePicker = AdvancedPicker.NONE }) { Text("Close") }
                }
            )
        }

        AdvancedPicker.VOICE -> {
            AlertDialog(
                onDismissRequest = { activePicker = AdvancedPicker.NONE },
                title = { Text("TTS Voice") },
                text = {
                    Column {
                        MayraSettings.AVAILABLE_VOICES.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        onSelectVoice(option.id)
                                        activePicker = AdvancedPicker.NONE
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.voiceName == option.id,
                                    onClick = {
                                        onSelectVoice(option.id)
                                        activePicker = AdvancedPicker.NONE
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(option.label, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        option.vibe,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activePicker = AdvancedPicker.NONE }) { Text("Close") }
                }
            )
        }

        AdvancedPicker.NONE -> Unit
    }
}
