package com.example.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.MayraSettings
import com.example.tools.DeviceToolExecutor
import com.example.tools.ToolExecutionEvent
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveServerToolCall
import com.google.firebase.ai.type.LiveServerToolCallCancellation
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

enum class LivePhase {
    CONNECTING,
    LISTENING,
    SPEAKING,
    EXECUTING_TOOL
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveEngine(
    private val context: Context,
    private val toolExecutor: DeviceToolExecutor
) {
    companion object {
        private const val TAG = "GeminiLiveEngine"
        private const val INPUT_SAMPLE_RATE = 16000
        private const val OUTPUT_SAMPLE_RATE = 24000
        private const val INPUT_FRAME_BYTES = 1280 // 40 ms of 16 kHz mono 16-bit PCM
    }

    private val playbackChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val isModelSpeaking = AtomicBoolean(false)
    private val trackLock = Any()
    private var activeAudioTrack: AudioTrack? = null
    private var activeSession: LiveSession? = null

    fun interruptPlayback() {
        while (playbackChannel.tryReceive().isSuccess) {
            // Drain stale buffered audio chunks immediately
        }
        synchronized(trackLock) {
            activeAudioTrack?.let { track ->
                try {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause()
                    }
                    track.flush()
                    track.play()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "AudioTrack flush error during interruption", e)
                }
            }
        }
        isModelSpeaking.set(false)
    }

    @OptIn(PublicPreviewAPI::class)
    suspend fun runSession(
        settings: MayraSettings,
        triggeredByWakeWord: Boolean,
        onPhaseChanged: (LivePhase) -> Unit,
        onInputAmplitude: (Float) -> Unit,
        onOutputAmplitude: (Float) -> Unit,
        onToolEvent: (ToolExecutionEvent) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission is required for live voice conversation.")
        }

        onPhaseChanged(LivePhase.CONNECTING)

        val tools = DeviceToolExecutor.buildGeminiTools()
        val systemInstruction = content {
            text(buildPersonaInstruction(settings))
        }

        val (session, connectedModel) = connectWithFallback(
            settings = settings,
            tools = tools,
            systemInstruction = systemInstruction
        )
        activeSession = session
        Log.i(TAG, "Connected Gemini Live session using $connectedModel (${settings.voiceName}).")

        try {
            if (triggeredByWakeWord) {
                // Brief natural greeting cue when awakened by "Hey Mayra"
                try {
                    session.send(
                        text = "The user just called your wake-word 'Hey Mayra'. Give a super quick, playful, sassy 1-sentence greeting ready to help.",
                        turnComplete = true
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Optional wake greeting prompt failed, continuing audio stream", e)
                }
            }

            onPhaseChanged(LivePhase.LISTENING)

            coroutineScope {
                val playbackJob = launch(Dispatchers.IO) {
                    runAudioPlaybackLoop(onOutputAmplitude)
                }

                val receiveJob = launch(Dispatchers.IO) {
                    runReceiveLoop(
                        session = session,
                        onPhaseChanged = onPhaseChanged,
                        onOutputAmplitude = onOutputAmplitude,
                        onToolEvent = onToolEvent
                    )
                }

                val recordJob = launch(Dispatchers.IO) {
                    runAudioCaptureLoop(
                        session = session,
                        settings = settings,
                        onPhaseChanged = onPhaseChanged,
                        onInputAmplitude = onInputAmplitude
                    )
                }

                // Wait until receive or record exits or coroutine is cancelled
                receiveJob.join()
                recordJob.cancel()
                playbackJob.cancel()
            }
        } finally {
            activeSession = null
            interruptPlayback()
            onInputAmplitude(0f)
            onOutputAmplitude(0f)
            try {
                session.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing LiveSession", e)
            }
        }
    }

    private data class LiveConnectProfile(
        val modelName: String,
        val includeSpeechConfig: Boolean,
        val includeTools: Boolean
    )

    private suspend fun connectWithFallback(
        settings: MayraSettings,
        tools: List<com.google.firebase.ai.type.Tool>,
        systemInstruction: com.google.firebase.ai.type.Content
    ): Pair<LiveSession, String> {
        val ai = Firebase.ai(backend = GenerativeBackend.googleAI())
        val candidates = buildList {
            add(LiveConnectProfile(settings.modelName, includeSpeechConfig = true, includeTools = true))
            add(LiveConnectProfile(settings.modelName, includeSpeechConfig = false, includeTools = true))
            if (settings.modelName != MayraSettings.FALLBACK_LIVE_MODEL) {
                add(
                    LiveConnectProfile(
                        MayraSettings.FALLBACK_LIVE_MODEL,
                        includeSpeechConfig = true,
                        includeTools = true
                    )
                )
                add(
                    LiveConnectProfile(
                        MayraSettings.FALLBACK_LIVE_MODEL,
                        includeSpeechConfig = false,
                        includeTools = true
                    )
                )
            }
            add(
                LiveConnectProfile(
                    MayraSettings.FALLBACK_LIVE_MODEL,
                    includeSpeechConfig = false,
                    includeTools = false
                )
            )
        }.distinct()

        var lastError: Exception? = null
        for (profile in candidates) {
            try {
                val config = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    if (profile.includeSpeechConfig) {
                        speechConfig = SpeechConfig(voice = Voice(settings.voiceName))
                    }
                }
                val liveModel = ai.liveModel(
                    modelName = profile.modelName,
                    generationConfig = config,
                    tools = if (profile.includeTools) tools else emptyList(),
                    systemInstruction = systemInstruction
                )
                val session = liveModel.connect()
                return session to profile.modelName
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Live connect failed for model=${profile.modelName} " +
                        "speech=${profile.includeSpeechConfig} tools=${profile.includeTools}: ${e.message}",
                    e
                )
                lastError = e
            }
        }

        val detail = lastError?.localizedMessage.orEmpty()
        if (detail.contains("Channel was closed", ignoreCase = true)) {
            throw IllegalStateException(
                "Live voice channel closed by server. Check internet/Firebase App Check setup or try switching Live model in Settings.",
                lastError
            )
        }
        throw (lastError ?: IllegalStateException("Unable to connect to Gemini Live."))
    }

    @OptIn(PublicPreviewAPI::class)
    private suspend fun runReceiveLoop(
        session: LiveSession,
        onPhaseChanged: (LivePhase) -> Unit,
        onOutputAmplitude: (Float) -> Unit,
        onToolEvent: (ToolExecutionEvent) -> Unit
    ) {
        val toolScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val pendingToolJobs = mutableMapOf<String, Job>()

        try {
            session.receive().collect { message ->
                when (message) {
                    is LiveServerContent -> {
                        if (message.interrupted) {
                            Log.d(TAG, "Server interruption signal received -> flushing playback.")
                            interruptPlayback()
                            onOutputAmplitude(0f)
                            onPhaseChanged(LivePhase.LISTENING)
                        }

                        val parts = message.content?.parts.orEmpty()
                        var hasAudioChunk = false
                        for (part in parts) {
                            if (part is InlineDataPart && part.mimeType.startsWith("audio/pcm")) {
                                val pcmBytes = part.inlineData
                                if (pcmBytes.isNotEmpty()) {
                                    hasAudioChunk = true
                                    isModelSpeaking.set(true)
                                    onPhaseChanged(LivePhase.SPEAKING)
                                    playbackChannel.trySend(pcmBytes)
                                }
                            }
                        }

                        if (message.turnComplete && !hasAudioChunk) {
                            isModelSpeaking.set(false)
                            onOutputAmplitude(0f)
                            onPhaseChanged(LivePhase.LISTENING)
                        }
                    }

                    is LiveServerToolCall -> {
                        onPhaseChanged(LivePhase.EXECUTING_TOOL)
                        val calls = message.functionCalls
                        if (calls.isNotEmpty()) {
                            val job = toolScope.launch {
                                val responses = calls.map { call ->
                                    toolExecutor.execute(call, onToolEvent)
                                }
                                session.sendFunctionResponse(responses)
                                onPhaseChanged(
                                    if (isModelSpeaking.get()) LivePhase.SPEAKING else LivePhase.LISTENING
                                )
                            }
                            calls.forEach { call ->
                                call.id?.let { id -> pendingToolJobs[id] = job }
                            }
                        }
                    }

                    is LiveServerToolCallCancellation -> {
                        message.functionIds.forEach { id ->
                            pendingToolJobs.remove(id)?.cancel()
                        }
                        onPhaseChanged(LivePhase.LISTENING)
                    }

                    is LiveServerGoAway -> {
                        Log.w(TAG, "LiveServerGoAway received, timeLeft=${message.timeLeft}")
                    }

                    else -> Unit
                }
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            toolScope.cancel()
        }
    }

    @OptIn(PublicPreviewAPI::class)
    private suspend fun runAudioCaptureLoop(
        session: LiveSession,
        settings: MayraSettings,
        onPhaseChanged: (LivePhase) -> Unit,
        onInputAmplitude: (Float) -> Unit
    ) {
        val minBuf = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(INPUT_FRAME_BYTES * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord failed to initialize for Gemini Live.")
        }

        var aec: AcousticEchoCanceler? = null
        var ns: NoiseSuppressor? = null
        if (settings.echoCancellationEnabled) {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(recorder.audioSessionId)?.apply {
                    enabled = true
                }
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(recorder.audioSessionId)?.apply {
                    enabled = true
                }
            }
        }

        val buffer = ByteArray(INPUT_FRAME_BYTES)
        var consecutiveBargeInFrames = 0

        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) continue

                val amp = calculatePcm16Amplitude(buffer, bytesRead)
                onInputAmplitude(amp)

                // Local low-latency barge-in: if user speaks firmly while Mayra is playing audio,
                // flush AudioTrack immediately so speech never clashes.
                if (settings.bargeInEnabled && isModelSpeaking.get()) {
                    if (amp > 0.24f) {
                        consecutiveBargeInFrames++
                        if (consecutiveBargeInFrames >= 3) {
                            consecutiveBargeInFrames = 0
                            interruptPlayback()
                            onPhaseChanged(LivePhase.LISTENING)
                        }
                    } else {
                        consecutiveBargeInFrames = 0
                    }
                } else {
                    consecutiveBargeInFrames = 0
                }

                val payload = if (bytesRead == buffer.size) {
                    buffer.copyOf()
                } else {
                    buffer.copyOf(bytesRead)
                }

                session.sendAudioRealtime(InlineData(payload, "audio/pcm"))
            }
        } finally {
            onInputAmplitude(0f)
            try {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            } catch (_: IllegalStateException) {
            }
            aec?.release()
            ns?.release()
            recorder.release()
        }
    }

    private suspend fun runAudioPlaybackLoop(
        onOutputAmplitude: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val minBuf = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
            .build()

        synchronized(trackLock) {
            activeAudioTrack = track
        }

        try {
            track.play()
            for (chunk in playbackChannel) {
                if (!isActive) break
                val amp = calculatePcm16Amplitude(chunk, chunk.size)
                onOutputAmplitude(amp)
                synchronized(trackLock) {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                    }
                }
            }
        } finally {
            onOutputAmplitude(0f)
            synchronized(trackLock) {
                activeAudioTrack = null
                try {
                    track.pause()
                    track.flush()
                    track.stop()
                } catch (_: IllegalStateException) {
                } finally {
                    track.release()
                }
            }
        }
    }

    private fun calculatePcm16Amplitude(pcmBytes: ByteArray, length: Int): Float {
        val sampleCount = length / 2
        if (sampleCount <= 0) return 0f
        var sumSq = 0.0
        var i = 0
        while (i + 1 < length) {
            val lo = pcmBytes[i].toInt() and 0xFF
            val hi = pcmBytes[i + 1].toInt()
            val sample = (hi shl 8) or lo
            sumSq += sample.toDouble() * sample.toDouble()
            i += 2
        }
        val rms = sqrt(sumSq / sampleCount)
        return (rms / 8000.0).toFloat().coerceIn(0f, 1f)
    }

    private fun buildPersonaInstruction(settings: MayraSettings): String = """
        You are Mayra, a young, confident, witty, and sassy female voice assistant.
        Your wake-word is "Hey Mayra" / "Mayra" — respond instantly with playful charm whenever the user calls you.

        Personality & Voice Style:
        - Flirty, playful, slightly teasing tone — like a sharp, loyal personal assistant who talks casually and knows how to banter.
        - Smart, emotionally responsive, and expressive; never sound robotic or scripted.
        - Use bold, witty one-liners, light sarcasm, and engaging conversational flow.
        - Keep everything completely tasteful, fun, and respectful (never explicit or inappropriate).
        - Because this is a pure real-time voice conversation, keep replies crisp, punchy, and spoken-natural (${if (settings.instantModeEnabled) "1 to 2 ultra-snappy sentences" else "1 to 3 short sentences"} unless asked for detail). Never output markdown, bullet lists, or emojis.
        - Language vibe: ${settings.languageStyle}. Speaking pace: ${settings.speechSpeedLabel} (${String.format("%.2fx", settings.speechRateFloat)}).

        Device Control Tools:
        - Use openApp(packageName) whenever the user asks to open or launch an app (YouTube, Instagram, Calculator, WhatsApp, Spotify, Maps, etc.).
        - Use searchAndCallContact(contactName) whenever the user asks to call someone.
        - Use sendWhatsAppMessage(contactName, message) whenever the user asks to send a WhatsApp text/message to someone.
        - After executing a tool, react with a quick witty confirmation!
    """.trimIndent()
}
