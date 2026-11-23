package com.pulseplayer.music.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkUri: Uri?
)
