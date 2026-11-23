package com.pulseplayer.music.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAtMs: Long = System.currentTimeMillis()
)
