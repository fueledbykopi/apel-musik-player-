package com.pulseplayer.music.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: Uri?,
    val songCount: Int,
    val totalDurationMs: Long
)
