package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight, local AudioRecord-based wake-word engine tuned for "Janvi".
 *
 * Uses a zero-dependency PCM DSP pipeline (adaptive noise floor, RMS SNR gating,
 * vocal zero-crossing-rate bounds, and two-syllable speech cadence detection)
 * so background listening stays low-power and releases the microphone cleanly
 * before Gemini Live opens its bidirectional audio stream.
 */
class JanviWakeWordDetector(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "JanviWakeDetector"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SAMPLES = 320 // 20 ms at 16 kHz
        private const val MIN_SPEECH_MS = 220L
        private const val MAX_SPEECH_MS = 1450L
        private const val END_PAUSE_MS = 180L
        private const val COOLDOWN_MS = 1800L
    }

    suspend fun listenForWakeWord(
        sensitivityProvider: () -> Float,
        onAmplitude: (Float) -> Unit,
        onWakeDetected: () -> Unit
    ) = withContext(ioDispatcher) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "RECORD_AUDIO permission missing; wake-word listener paused.")
            return@withContext
        }

        val minBufBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(FRAME_SAMPLES * 4)

        val audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
        val recorder = AudioRecord(
            audioSource,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufBytes * 2
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord failed to initialize for wake-word detection.")
        }

        val frameBuffer = ShortArray(FRAME_SAMPLES)
        var noiseFloor = 220.0
        var inUtterance = false
        var utteranceStartMs = 0L
        var lastVoicedMs = 0L
        var voicedFrameCount = 0
        var syllablePeaks = 0
        var prevFrameHigh = false
        var lastTriggerMs = 0L

        try {
            recorder.startRecording()
            Log.d(TAG, "Local 'Janvi' AudioRecord wake-word listener started.")

            while (currentCoroutineContext().isActive) {
                val read = recorder.read(frameBuffer, 0, FRAME_SAMPLES)
                if (read <= 0) continue

                val now = System.currentTimeMillis()
                val metrics = analyzeFrame(frameBuffer, read)
                val normalizedAmp = (metrics.rms / 6500.0).toFloat().coerceIn(0f, 1f)
                onAmplitude(normalizedAmp)

                val sensitivity = sensitivityProvider().coerceIn(0.2f, 1.0f)
                // Higher sensitivity lowers the required SNR multiplier above adaptive floor.
                val snrMultiplier = 4.6 - (sensitivity * 2.4) // range ~2.2x .. 4.1x
                val minAbsoluteRms = 750.0 - (sensitivity * 420.0) // range ~330 .. 666
                val speechThreshold = maxOf(noiseFloor * snrMultiplier, minAbsoluteRms)

                // Vocal formants typically exhibit moderate zero-crossing rate (0.03 .. 0.34).
                val isVocalSpectrum = metrics.zcr in 0.025..0.38
                val isSpeechFrame = metrics.rms > speechThreshold && isVocalSpectrum

                if (!inUtterance) {
                    // Slowly adapt noise floor when idle.
                    if (metrics.rms < speechThreshold * 0.85) {
                        noiseFloor = noiseFloor * 0.96 + metrics.rms * 0.04
                        noiseFloor = noiseFloor.coerceIn(110.0, 1400.0)
                    }

                    if (isSpeechFrame && (now - lastTriggerMs) > COOLDOWN_MS) {
                        inUtterance = true
                        utteranceStartMs = now
                        lastVoicedMs = now
                        voicedFrameCount = 1
                        syllablePeaks = 1
                        prevFrameHigh = true
                    }
                } else {
                    if (isSpeechFrame) {
                        lastVoicedMs = now
                        voicedFrameCount++
                        val isPeak = metrics.rms > speechThreshold * 1.15
                        if (isPeak && !prevFrameHigh) {
                            syllablePeaks++
                        }
                        prevFrameHigh = isPeak
                    } else {
                        prevFrameHigh = false
                    }

                    val silenceDuration = now - lastVoicedMs
                    val totalDuration = lastVoicedMs - utteranceStartMs

                    if (silenceDuration >= END_PAUSE_MS) {
                        val validCadence =
                            totalDuration in MIN_SPEECH_MS..MAX_SPEECH_MS &&
                                voicedFrameCount >= 7 &&
                                syllablePeaks >= 1

                        inUtterance = false
                        voicedFrameCount = 0
                        syllablePeaks = 0

                        if (validCadence) {
                            lastTriggerMs = now
                            Log.i(
                                TAG,
                                "Wake-word candidate 'Janvi' triggered (dur=${totalDuration}ms, sensitivity=$sensitivity)"
                            )
                            onWakeDetected()
                            break
                        }
                    } else if ((now - utteranceStartMs) > MAX_SPEECH_MS + 400L) {
                        // Continuous background speech/media: reset to avoid false triggers.
                        inUtterance = false
                        voicedFrameCount = 0
                        syllablePeaks = 0
                    }
                }
            }
        } finally {
            try {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Error stopping wake AudioRecord", e)
            } finally {
                recorder.release()
                onAmplitude(0f)
                Log.d(TAG, "Local 'Janvi' AudioRecord released cleanly.")
            }
        }
    }

    private data class FrameMetrics(
        val rms: Double,
        val zcr: Double
    )

    private fun analyzeFrame(samples: ShortArray, length: Int): FrameMetrics {
        var sumSq = 0.0
        var zeroCrossings = 0
        var prev = samples[0].toInt()
        for (i in 0 until length) {
            val s = samples[i].toInt()
            sumSq += s.toDouble() * s.toDouble()
            if (i > 0 && ((prev >= 0 && s < 0) || (prev < 0 && s >= 0))) {
                zeroCrossings++
            }
            prev = s
        }
        val rms = sqrt(sumSq / length.coerceAtLeast(1))
        val zcr = zeroCrossings.toDouble() / length.coerceAtLeast(1)
        return FrameMetrics(rms = rms, zcr = zcr)
    }

    /**
     * Synthesizes a brief dual-tone cosmic chime on AudioTrack when Mayra awakens.
     */
    suspend fun playWakeChime() = withContext(ioDispatcher) {
        val sampleRate = 24000
        val durationMs = 210
        val totalSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i < totalSamples / 2) 587.33 else 880.0 // D5 -> A5
            val localT = if (i < totalSamples / 2) t else t - (durationMs / 2000.0)
            val env = sin(min(1.0, localT / 0.015) * (PI / 2.0)) *
                kotlin.math.exp(-localT * 14.0)
            val sample = (sin(2.0 * PI * freq * t) * env * 9000.0).toInt()
            pcm[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val byteBuffer = ByteArray(pcm.size * 2)
        for (i in pcm.indices) {
            val v = pcm[i].toInt()
            byteBuffer[i * 2] = (v and 0xFF).toByte()
            byteBuffer[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(byteBuffer.size)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
            .build()

        try {
            track.write(byteBuffer, 0, byteBuffer.size)
            track.play()
            kotlinx.coroutines.delay(durationMs.toLong() + 40L)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to play wake chime", e)
        } finally {
            try {
                track.stop()
            } catch (_: IllegalStateException) {
            }
            track.release()
        }
    }
}
