package com.pulseplayer.music.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val trackNumber: Int = 0,
    val dateAddedSec: Long = 0L,
    val sizeBytes: Long = 0L,
    val isRemoteDemo: Boolean = false
)
