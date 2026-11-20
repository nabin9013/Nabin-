package com.example.service

import com.example.tools.ToolExecutionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AssistantMode {
    OFF,
    STANDBY_WAKE_WORD,
    CONNECTING_LIVE,
    LISTENING,
    SPEAKING,
    EXECUTING_TOOL,
    ERROR
}

data class MayraRuntimeState(
    val isServiceRunning: Boolean = false,
    val mode: AssistantMode = AssistantMode.OFF,
    val statusHeadline: String = "Assistant is paused",
    val statusSubtext: String = "Flip the switch on to start listening",
    val inputAmplitude: Float = 0f,
    val outputAmplitude: Float = 0f,
    val lastToolEvent: ToolExecutionEvent? = null,
    val errorMessage: String? = null
)

sealed interface VoiceServiceCommand {
    data object StartLiveNow : VoiceServiceCommand
    data object StopLiveToStandby : VoiceServiceCommand
    data object InterruptPlayback : VoiceServiceCommand
}

/**
 * Process-local reactive bus bridging MayraVoiceService and Jetpack Compose UI
 * without holding Context/Activity references.
 */
object VoiceServiceBus {
    private val _state = MutableStateFlow(MayraRuntimeState())
    val state: StateFlow<MayraRuntimeState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<VoiceServiceCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<VoiceServiceCommand> = _commands.asSharedFlow()

    fun update(transform: (MayraRuntimeState) -> MayraRuntimeState) {
        _state.update(transform)
    }

    fun sendCommand(command: VoiceServiceCommand) {
        _commands.tryEmit(command)
    }
}
