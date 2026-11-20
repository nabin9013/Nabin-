package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.JanviWakeWordDetector
import com.example.data.MayraSettings
import com.example.data.SettingsRepository
import com.example.live.GeminiLiveEngine
import com.example.live.LivePhase
import com.example.tools.DeviceToolExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MayraVoiceService : Service() {

    companion object {
        private const val TAG = "MayraVoiceService"
        private const val CHANNEL_ID = "mayra_voice_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_STANDBY = "com.example.action.START_STANDBY"
        const val ACTION_WAKE_NOW = "com.example.action.WAKE_NOW"
        const val ACTION_STOP_LIVE = "com.example.action.STOP_LIVE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        fun startStandby(context: Context) {
            val intent = Intent(context, MayraVoiceService::class.java).apply {
                action = ACTION_START_STANDBY
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun wakeLiveNow(context: Context) {
            val intent = Intent(context, MayraVoiceService::class.java).apply {
                action = ACTION_WAKE_NOW
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopLiveSession(context: Context) {
            VoiceServiceBus.sendCommand(VoiceServiceCommand.StopLiveToStandby)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MayraVoiceService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var wakeDetector: JanviWakeWordDetector
    private lateinit var liveEngine: GeminiLiveEngine
    private var currentSettings = MayraSettings()
    private var orchestratorJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        wakeDetector = JanviWakeWordDetector(applicationContext)
        val toolExecutor = DeviceToolExecutor(applicationContext)
        liveEngine = GeminiLiveEngine(applicationContext, toolExecutor)

        createNotificationChannel()
        acquireWakeLock()

        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { updated ->
                val previous = currentSettings
                currentSettings = updated
                if (!updated.assistantEnabled) {
                    stopAssistantCompletely()
                } else if (previous.wakeWordEnabled != updated.wakeWordEnabled ||
                    previous.voiceName != updated.voiceName ||
                    previous.modelName != updated.modelName
                ) {
                    if (orchestratorJob == null || orchestratorJob?.isActive != true) {
                        restartOrchestrator(startLiveImmediately = false)
                    }
                }
            }
        }

        serviceScope.launch {
            VoiceServiceBus.commands.collect { command ->
                when (command) {
                    VoiceServiceCommand.StartLiveNow -> {
                        restartOrchestrator(startLiveImmediately = true)
                    }
                    VoiceServiceCommand.StopLiveToStandby -> {
                        if (currentSettings.wakeWordEnabled && currentSettings.assistantEnabled) {
                            restartOrchestrator(startLiveImmediately = false)
                        } else {
                            orchestratorJob?.cancel()
                            VoiceServiceBus.update {
                                it.copy(
                                    isServiceRunning = true,
                                    mode = AssistantMode.STANDBY_WAKE_WORD,
                                    statusHeadline = "Ready when you are",
                                    statusSubtext = "Tap the orb to talk to Mayra",
                                    inputAmplitude = 0f,
                                    outputAmplitude = 0f,
                                    errorMessage = null
                                )
                            }
                            updateNotification(isLive = false)
                        }
                    }
                    VoiceServiceCommand.InterruptPlayback -> {
                        liveEngine.interruptPlayback()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopAssistantCompletely()
            return START_NOT_STICKY
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Cannot start microphone ForegroundService without RECORD_AUDIO permission.")
            VoiceServiceBus.update {
                it.copy(
                    isServiceRunning = false,
                    mode = AssistantMode.ERROR,
                    statusHeadline = "Microphone access needed",
                    statusSubtext = "Grant microphone permission to activate Mayra",
                    errorMessage = "RECORD_AUDIO permission is required."
                )
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val startLiveImmediately = intent?.action == ACTION_WAKE_NOW
        promoteToForeground(isLive = startLiveImmediately)

        if (intent?.action == ACTION_STOP_LIVE) {
            VoiceServiceBus.sendCommand(VoiceServiceCommand.StopLiveToStandby)
            return START_STICKY
        }

        if (orchestratorJob?.isActive != true || startLiveImmediately) {
            restartOrchestrator(startLiveImmediately = startLiveImmediately)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        orchestratorJob?.cancel()
        releaseWakeLock()
        VoiceServiceBus.update {
            MayraRuntimeState(
                isServiceRunning = false,
                mode = AssistantMode.OFF,
                statusHeadline = "Assistant is paused",
                statusSubtext = "Flip the switch on to wake Mayra"
            )
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopAssistantCompletely() {
        orchestratorJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun restartOrchestrator(startLiveImmediately: Boolean) {
        orchestratorJob?.cancel()
        orchestratorJob = serviceScope.launch {
            var wakeNow = startLiveImmediately
            var triggeredByWakeWord = false

            while (currentSettings.assistantEnabled) {
                try {
                    if (!wakeNow && currentSettings.wakeWordEnabled) {
                        VoiceServiceBus.update {
                            it.copy(
                                isServiceRunning = true,
                                mode = AssistantMode.STANDBY_WAKE_WORD,
                                statusHeadline = "Listening for \"Hey Mayra\"",
                                statusSubtext = "Background wake-word engine active",
                                outputAmplitude = 0f,
                                errorMessage = null
                            )
                        }
                        updateNotification(isLive = false)

                        var wakeTriggered = false
                        wakeDetector.listenForWakeWord(
                            sensitivityProvider = { currentSettings.wakeSensitivity },
                            onAmplitude = { amp ->
                                VoiceServiceBus.update { it.copy(inputAmplitude = amp) }
                            },
                            onWakeDetected = {
                                wakeTriggered = true
                            }
                        )

                        if (wakeTriggered) {
                            triggeredByWakeWord = true
                            if (currentSettings.notificationsEnabled) {
                                wakeDetector.playWakeChime()
                            }
                            wakeNow = true
                        } else {
                            break
                        }
                    } else if (!wakeNow) {
                        VoiceServiceBus.update {
                            it.copy(
                                isServiceRunning = true,
                                mode = AssistantMode.STANDBY_WAKE_WORD,
                                statusHeadline = "Ready when you are",
                                statusSubtext = "Tap the orb to start a live session",
                                inputAmplitude = 0f,
                                outputAmplitude = 0f,
                                errorMessage = null
                            )
                        }
                        updateNotification(isLive = false)
                        break
                    }

                    if (wakeNow) {
                        wakeNow = false
                        updateNotification(isLive = true)
                        liveEngine.runSession(
                            settings = currentSettings,
                            triggeredByWakeWord = triggeredByWakeWord,
                            onPhaseChanged = { phase ->
                                val (mode, headline, subtext) = when (phase) {
                                    LivePhase.CONNECTING -> Triple(
                                        AssistantMode.CONNECTING_LIVE,
                                        "Waking Mayra...",
                                        "Connecting Gemini Live audio stream"
                                    )
                                    LivePhase.LISTENING -> Triple(
                                        AssistantMode.LISTENING,
                                        "I'm listening, go ahead",
                                        "Speak anytime — interrupt freely"
                                    )
                                    LivePhase.SPEAKING -> Triple(
                                        AssistantMode.SPEAKING,
                                        "Mayra is speaking",
                                        "Just talk to interrupt me anytime"
                                    )
                                    LivePhase.EXECUTING_TOOL -> Triple(
                                        AssistantMode.EXECUTING_TOOL,
                                        "On it...",
                                        "Running Android device action"
                                    )
                                }
                                VoiceServiceBus.update {
                                    it.copy(
                                        isServiceRunning = true,
                                        mode = mode,
                                        statusHeadline = headline,
                                        statusSubtext = subtext,
                                        errorMessage = null
                                    )
                                }
                            },
                            onInputAmplitude = { amp ->
                                VoiceServiceBus.update { it.copy(inputAmplitude = amp) }
                            },
                            onOutputAmplitude = { amp ->
                                VoiceServiceBus.update { it.copy(outputAmplitude = amp) }
                            },
                            onToolEvent = { event ->
                                VoiceServiceBus.update { it.copy(lastToolEvent = event) }
                            }
                        )
                        triggeredByWakeWord = false
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Voice orchestrator error", e)
                    triggeredByWakeWord = false
                    wakeNow = false
                    VoiceServiceBus.update {
                        it.copy(
                            isServiceRunning = true,
                            mode = AssistantMode.ERROR,
                            statusHeadline = "Connection hiccup",
                            statusSubtext = e.localizedMessage ?: "Live session interrupted",
                            inputAmplitude = 0f,
                            outputAmplitude = 0f,
                            errorMessage = e.localizedMessage ?: "Failed to connect Gemini Live."
                        )
                    }
                    delay(5000L)
                    if (!currentSettings.wakeWordEnabled) {
                        break
                    }
                }
            }
        }
    }

    private fun promoteToForeground(isLive: Boolean) {
        val notification = buildNotification(isLive)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(isLive: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(isLive))
    }

    private fun buildNotification(isLive: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeIntent = Intent(this, MayraVoiceService::class.java).apply {
            action = if (isLive) ACTION_STOP_LIVE else ACTION_WAKE_NOW
        }
        val wakePendingIntent = PendingIntent.getService(
            this,
            1,
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MayraVoiceService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = getString(
            if (isLive) R.string.notification_title_live else R.string.notification_title_standby
        )
        val text = getString(
            if (isLive) R.string.notification_text_live else R.string.notification_text_standby
        )
        val actionLabel = if (isLive) "Standby" else getString(R.string.notification_action_wake)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, actionLabel, wakePendingIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mayra::VoiceServiceWakeLock")?.apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L) // 1 hour renewable safety cap
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
