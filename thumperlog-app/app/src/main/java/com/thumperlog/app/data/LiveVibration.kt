package com.thumperlog.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LiveState(
    val isRecording: Boolean = false,
    val currentMag: Float = 0f,
    val elapsedMs: Long = 0L,
    val peak: Float = 0f,
    val avg: Float = 0f,
    val recentBuffer: List<Sample> = emptyList()
)

/**
 * In-process singleton bridging the foreground Service (producer) and the
 * Compose UI (consumer). Both run in the same process, so a plain StateFlow
 * is enough -- no need for a bound service / Messenger.
 */
object LiveVibration {
    private val _state = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = _state

    fun update(newState: LiveState) {
        _state.value = newState
    }
}
