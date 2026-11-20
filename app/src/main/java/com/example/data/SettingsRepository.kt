package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.mayraDataStore: DataStore<Preferences> by preferencesDataStore(name = "mayra_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val LANGUAGE_STYLE = stringPreferencesKey("language_style")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val MAX_OUTPUT_TOKENS = intPreferencesKey("max_output_tokens")
        val SPEECH_RATE_FLOAT = floatPreferencesKey("speech_rate_float")
        val THEME_VARIANT = stringPreferencesKey("theme_variant")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val INSTANT_MODE_ENABLED = booleanPreferencesKey("instant_mode_enabled")
        val BARGE_IN_ENABLED = booleanPreferencesKey("barge_in_enabled")
        val ECHO_CANCELLATION_ENABLED = booleanPreferencesKey("echo_cancellation_enabled")
        val WAKE_SENSITIVITY = floatPreferencesKey("wake_sensitivity")
    }

    val settingsFlow: Flow<MayraSettings> = context.mayraDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            MayraSettings(
                assistantEnabled = prefs[Keys.ASSISTANT_ENABLED] ?: true,
                wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
                voiceName = prefs[Keys.VOICE_NAME] ?: MayraSettings.DEFAULT_VOICE_NAME,
                languageStyle = prefs[Keys.LANGUAGE_STYLE] ?: MayraSettings.DEFAULT_LANGUAGE_STYLE,
                modelName = prefs[Keys.MODEL_NAME] ?: MayraSettings.DEFAULT_LIVE_MODEL,
                maxOutputTokens = prefs[Keys.MAX_OUTPUT_TOKENS] ?: MayraSettings.DEFAULT_MAX_TOKENS,
                speechRateFloat = prefs[Keys.SPEECH_RATE_FLOAT] ?: MayraSettings.DEFAULT_SPEECH_RATE,
                themeVariant = prefs[Keys.THEME_VARIANT] ?: MayraSettings.DEFAULT_THEME,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                instantModeEnabled = prefs[Keys.INSTANT_MODE_ENABLED] ?: true,
                bargeInEnabled = prefs[Keys.BARGE_IN_ENABLED] ?: true,
                echoCancellationEnabled = prefs[Keys.ECHO_CANCELLATION_ENABLED] ?: true,
                wakeSensitivity = prefs[Keys.WAKE_SENSITIVITY] ?: MayraSettings.DEFAULT_WAKE_SENSITIVITY
            )
        }

    suspend fun setAssistantEnabled(enabled: Boolean) {
        context.mayraDataStore.edit { it[Keys.ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.mayraDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setVoiceName(voiceName: String) {
        context.mayraDataStore.edit { it[Keys.VOICE_NAME] = voiceName }
    }

    suspend fun setLanguageStyle(languageStyle: String) {
        context.mayraDataStore.edit { it[Keys.LANGUAGE_STYLE] = languageStyle }
    }

    suspend fun setModelName(modelName: String) {
        context.mayraDataStore.edit { it[Keys.MODEL_NAME] = modelName }
    }

    suspend fun setMaxOutputTokens(tokens: Int) {
        context.mayraDataStore.edit { it[Keys.MAX_OUTPUT_TOKENS] = tokens }
    }

    suspend fun setSpeechRateFloat(rate: Float) {
        context.mayraDataStore.edit { it[Keys.SPEECH_RATE_FLOAT] = rate.coerceIn(0.75f, 1.5f) }
    }

    suspend fun setThemeVariant(theme: String) {
        context.mayraDataStore.edit { it[Keys.THEME_VARIANT] = theme }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.mayraDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setInstantModeEnabled(enabled: Boolean) {
        context.mayraDataStore.edit { it[Keys.INSTANT_MODE_ENABLED] = enabled }
        context.mayraDataStore.edit { it[Keys.BARGE_IN_ENABLED] = enabled }
    }

    suspend fun setBargeInEnabled(enabled: Boolean) {
        context.mayraDataStore.edit { it[Keys.BARGE_IN_ENABLED] = enabled }
    }

    suspend fun setEchoCancellationEnabled(enabled: Boolean) {
        context.mayraDataStore.edit { it[Keys.ECHO_CANCELLATION_ENABLED] = enabled }
    }

    suspend fun setWakeSensitivity(sensitivity: Float) {
        context.mayraDataStore.edit { it[Keys.WAKE_SENSITIVITY] = sensitivity.coerceIn(0.2f, 1.0f) }
    }

    suspend fun resetToDefaults() {
        context.mayraDataStore.edit { it.clear() }
    }
}
