package com.pulseplayer.music.data.model

import androidx.compose.runtime.Immutable

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

@Immutable
data class PlaybackState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentSong: Song? = null,
    val currentIndex: Int = -1,
    val queue: List<Song> = emptyList(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
