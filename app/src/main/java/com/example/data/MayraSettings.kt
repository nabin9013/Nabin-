package com.example.data

data class VoicePersonaOption(
    val id: String,
    val label: String,
    val vibe: String
)

data class MayraSettings(
    val assistantEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val voiceName: String = DEFAULT_VOICE_NAME,
    val languageStyle: String = DEFAULT_LANGUAGE_STYLE,
    val modelName: String = DEFAULT_LIVE_MODEL,
    val maxOutputTokens: Int = DEFAULT_MAX_TOKENS,
    val speechRateFloat: Float = DEFAULT_SPEECH_RATE,
    val themeVariant: String = DEFAULT_THEME,
    val notificationsEnabled: Boolean = true,
    val instantModeEnabled: Boolean = true,
    val bargeInEnabled: Boolean = true,
    val echoCancellationEnabled: Boolean = true,
    val wakeSensitivity: Float = DEFAULT_WAKE_SENSITIVITY
) {
    val speechSpeedLabel: String
        get() = when {
            speechRateFloat < 0.9f -> "Slow"
            speechRateFloat > 1.15f -> "Fast"
            else -> "Normal"
        }

    companion object {
        const val DEFAULT_VOICE_NAME = "Aoede"
        const val DEFAULT_LANGUAGE_STYLE = "Hinglish & English"
        const val DEFAULT_LIVE_MODEL = "gemini-3.1-flash-live-preview"
        const val FALLBACK_LIVE_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val DEFAULT_MAX_TOKENS = 1024
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val DEFAULT_THEME = "Purple"
        const val DEFAULT_WAKE_SENSITIVITY = 0.65f

        val AVAILABLE_VOICES = listOf(
            VoicePersonaOption("Aoede", "Aoede (Breezy)", "Playful, confident & witty"),
            VoicePersonaOption("Zephyr", "Zephyr (Bright)", "Upbeat, sparkling & crisp"),
            VoicePersonaOption("Leda", "Leda (Youthful)", "Sweet, teasing & expressive"),
            VoicePersonaOption("Pulcherrima", "Pulcherrima (Bold)", "Forward, sassy & energetic"),
            VoicePersonaOption("Kore", "Kore (Firm)", "Sharp, smart & assured"),
            VoicePersonaOption("Sulafat", "Sulafat (Warm)", "Smooth, charming & relaxed")
        )

        val AVAILABLE_MODELS = listOf(
            DEFAULT_LIVE_MODEL,
            FALLBACK_LIVE_MODEL
        )

        val AVAILABLE_MAX_TOKENS = listOf(512, 1024, 2048, 4096)

        val AVAILABLE_LANGUAGES = listOf(
            "Hinglish & English",
            "Hindi",
            "English"
        )

        val AVAILABLE_THEMES = listOf(
            "Purple",
            "Neon Orchid",
            "Midnight Cyan"
        )
    }
}
