package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MayraSettings
import com.example.data.SettingsRepository
import com.example.service.AssistantMode
import com.example.service.MayraRuntimeState
import com.example.service.MayraVoiceService
import com.example.service.VoiceServiceBus
import com.example.service.VoiceServiceCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MayraUiState(
    val settings: MayraSettings = MayraSettings(),
    val runtime: MayraRuntimeState = MayraRuntimeState(),
    val isBatteryOptimizationIgnored: Boolean = false
)

class MayraViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val uiState: StateFlow<MayraUiState> = combine(
        settingsRepository.settingsFlow,
        VoiceServiceBus.state
    ) { settings, runtime ->
        MayraUiState(
            settings = settings,
            runtime = runtime,
            isBatteryOptimizationIgnored = checkBatteryOptimizationIgnored()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MayraUiState(
            isBatteryOptimizationIgnored = checkBatteryOptimizationIgnored()
        )
    )

    fun ensureBackgroundServiceRunning(hasMicPermission: Boolean) {
        if (!hasMicPermission) return
        val current = uiState.value
        if (current.settings.assistantEnabled && !current.runtime.isServiceRunning) {
            MayraVoiceService.startStandby(getApplication())
        }
    }

    fun toggleAssistantEnabled(enabled: Boolean, hasMicPermission: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAssistantEnabled(enabled)
            if (enabled && hasMicPermission) {
                MayraVoiceService.startStandby(getApplication())
            } else if (!enabled) {
                MayraVoiceService.stopService(getApplication())
            }
        }
    }

    fun toggleWakeWord(enabled: Boolean, hasMicPermission: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWakeWordEnabled(enabled)
            if (enabled && uiState.value.settings.assistantEnabled && hasMicPermission) {
                MayraVoiceService.startStandby(getApplication())
            }
        }
    }

    fun onOrbPrimaryAction(hasMicPermission: Boolean) {
        if (!hasMicPermission) return
        val mode = uiState.value.runtime.mode
        viewModelScope.launch {
            if (!uiState.value.settings.assistantEnabled) {
                settingsRepository.setAssistantEnabled(true)
            }
            when (mode) {
                AssistantMode.OFF,
                AssistantMode.STANDBY_WAKE_WORD,
                AssistantMode.ERROR -> {
                    MayraVoiceService.wakeLiveNow(getApplication())
                }
                AssistantMode.CONNECTING_LIVE,
                AssistantMode.LISTENING,
                AssistantMode.EXECUTING_TOOL -> {
                    MayraVoiceService.stopLiveSession(getApplication())
                }
                AssistantMode.SPEAKING -> {
                    VoiceServiceBus.sendCommand(VoiceServiceCommand.InterruptPlayback)
                }
            }
        }
    }

    fun stopLiveSession() {
        MayraVoiceService.stopLiveSession(getApplication())
    }

    fun interruptPlayback() {
        VoiceServiceBus.sendCommand(VoiceServiceCommand.InterruptPlayback)
    }

    fun setVoiceName(voiceName: String) {
        viewModelScope.launch { settingsRepository.setVoiceName(voiceName) }
    }

    fun setLanguageStyle(languageStyle: String) {
        viewModelScope.launch { settingsRepository.setLanguageStyle(languageStyle) }
    }

    fun setModelName(modelName: String) {
        viewModelScope.launch { settingsRepository.setModelName(modelName) }
    }

    fun setMaxOutputTokens(tokens: Int) {
        viewModelScope.launch { settingsRepository.setMaxOutputTokens(tokens) }
    }

    fun setSpeechRateFloat(rate: Float) {
        viewModelScope.launch { settingsRepository.setSpeechRateFloat(rate) }
    }

    fun setSpeechSpeedPreset(preset: String) {
        val rate = when (preset) {
            "Slow" -> 0.85f
            "Fast" -> 1.25f
            else -> 1.0f
        }
        setSpeechRateFloat(rate)
    }

    fun setThemeVariant(theme: String) {
        viewModelScope.launch { settingsRepository.setThemeVariant(theme) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setInstantModeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setInstantModeEnabled(enabled) }
    }

    fun setBargeInEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBargeInEnabled(enabled) }
    }

    fun setEchoCancellationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEchoCancellationEnabled(enabled) }
    }

    fun setWakeSensitivity(sensitivity: Float) {
        viewModelScope.launch { settingsRepository.setWakeSensitivity(sensitivity) }
    }

    fun resetSettingsToDefaults() {
        viewModelScope.launch { settingsRepository.resetToDefaults() }
    }

    fun openBatteryOptimizationSettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetails)
        }
    }

    fun openAppSettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shareMayraApp() {
        val context = getApplication<Application>()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out Mayra — my hands-free Gemini Live voice assistant!"
            )
        }
        val chooser = Intent.createChooser(shareIntent, "Share Mayra").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun checkBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val context = getApplication<Application>()
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
}
