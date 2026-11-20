package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MayraSettings
import com.example.ui.components.CosmicGlassCard
import com.example.viewmodel.MayraUiState

private enum class ActiveSettingsDialog {
    NONE, VOICE, LANGUAGE, SPEED, THEME
}

@Composable
fun SettingsScreen(
    uiState: MayraUiState,
    hasMicPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onNavigateBack: () -> Unit,
    onToggleAssistant: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectVoice: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSelectSpeedPreset: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onOpenAdvancedSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onRateApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    var activeDialog by remember { mutableStateOf(ActiveSettingsDialog.NONE) }

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
            // Header matching Photo Screen 3 (Back arrow + Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back_button)
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Mayra v1.0.0 Profile Card matching Photo Screen 3
            CosmicGlassCard(
                highlighted = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAbout)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mayra_portrait_1795120963893),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mayra",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Settings List matching every item in Photo Screen 3
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Mic,
                        title = "Assistant On/Off",
                        subtitle = "Wake-word \"Hey Mayra\" & live voice service",
                        checked = settings.assistantEnabled,
                        testTag = "settings_assistant_switch",
                        onCheckedChange = { checked ->
                            if (checked && !hasMicPermission) {
                                onRequestPermissions()
                            } else {
                                onToggleAssistant(checked)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.RecordVoiceOver,
                        title = "Voice Settings",
                        valueText = settings.voiceName,
                        testTag = "settings_voice_row",
                        onClick = { activeDialog = ActiveSettingsDialog.VOICE }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.Language,
                        title = "Language",
                        valueText = settings.languageStyle,
                        testTag = "settings_language_row",
                        onClick = { activeDialog = ActiveSettingsDialog.LANGUAGE }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.Speed,
                        title = "Speech Rate",
                        valueText = settings.speechSpeedLabel,
                        testTag = "settings_speed_row",
                        onClick = { activeDialog = ActiveSettingsDialog.SPEED }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.Palette,
                        title = "Theme",
                        valueText = settings.themeVariant,
                        testTag = "settings_theme_row",
                        onClick = { activeDialog = ActiveSettingsDialog.THEME }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = "Wake chime & status alerts",
                        checked = settings.notificationsEnabled,
                        testTag = "settings_notifications_switch",
                        onCheckedChange = onToggleNotifications
                    )
                }
            }

            // Bottom Navigation Rows matching Photo Screen 3
            CosmicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsNavRow(
                        icon = Icons.Rounded.Tune,
                        title = "Advanced Settings",
                        valueText = "",
                        testTag = "advanced_settings_row",
                        onClick = onOpenAdvancedSettings
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.Info,
                        title = "About App",
                        valueText = "",
                        testTag = "about_row",
                        onClick = onOpenAbout
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingsNavRow(
                        icon = Icons.Rounded.Star,
                        title = "Rate App",
                        valueText = "",
                        testTag = "rate_app_row",
                        onClick = onRateApp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    when (activeDialog) {
        ActiveSettingsDialog.VOICE -> {
            AlertDialog(
                onDismissRequest = { activeDialog = ActiveSettingsDialog.NONE },
                title = { Text("Voice Settings") },
                text = {
                    Column {
                        MayraSettings.AVAILABLE_VOICES.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        onSelectVoice(option.id)
                                        activeDialog = ActiveSettingsDialog.NONE
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.voiceName == option.id,
                                    onClick = {
                                        onSelectVoice(option.id)
                                        activeDialog = ActiveSettingsDialog.NONE
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
                    TextButton(onClick = { activeDialog = ActiveSettingsDialog.NONE }) {
                        Text("Close")
                    }
                }
            )
        }

        ActiveSettingsDialog.LANGUAGE -> {
            SimpleOptionDialog(
                title = "Language",
                options = MayraSettings.AVAILABLE_LANGUAGES,
                selected = settings.languageStyle,
                onSelect = {
                    onSelectLanguage(it)
                    activeDialog = ActiveSettingsDialog.NONE
                },
                onDismiss = { activeDialog = ActiveSettingsDialog.NONE }
            )
        }

        ActiveSettingsDialog.SPEED -> {
            SimpleOptionDialog(
                title = "Speech Rate",
                options = listOf("Slow", "Normal", "Fast"),
                selected = settings.speechSpeedLabel,
                onSelect = {
                    onSelectSpeedPreset(it)
                    activeDialog = ActiveSettingsDialog.NONE
                },
                onDismiss = { activeDialog = ActiveSettingsDialog.NONE }
            )
        }

        ActiveSettingsDialog.THEME -> {
            SimpleOptionDialog(
                title = "Theme",
                options = MayraSettings.AVAILABLE_THEMES,
                selected = settings.themeVariant,
                onSelect = {
                    onSelectTheme(it)
                    activeDialog = ActiveSettingsDialog.NONE
                },
                onDismiss = { activeDialog = ActiveSettingsDialog.NONE }
            )
        }

        ActiveSettingsDialog.NONE -> Unit
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (valueText.isNotBlank()) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SimpleOptionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelect(option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = { onSelect(option) }
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
