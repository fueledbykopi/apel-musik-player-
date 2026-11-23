package com.pulseplayer.music.data.repository

import android.net.Uri
import com.pulseplayer.music.data.model.Song

/**
 * Royalty-free SoundHelix streaming demo tracks so developers & users can immediately
 * test Media3 ExoPlayer playback even on an emulator with zero local MP3 files.
 */
object DemoTracksProvider {

    fun getDemoSongs(): List<Song> = listOf(
        Song(
            id = -1001L,
            title = "Neon Horizon (Demo)",
            artist = "SoundHelix Collective",
            album = "Synthwave Sessions",
            albumId = -2001L,
            durationMs = 372_000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            albumArtUri = null,
            trackNumber = 1,
            dateAddedSec = 1_700_000_001L,
            isRemoteDemo = true
        ),
        Song(
            id = -1002L,
            title = "Midnight Pulse (Demo)",
            artist = "SoundHelix Collective",
            album = "Synthwave Sessions",
            albumId = -2001L,
            durationMs = 425_000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            albumArtUri = null,
            trackNumber = 2,
            dateAddedSec = 1_700_000_002L,
            isRemoteDemo = true
        ),
        Song(
            id = -1003L,
            title = "Electric Drift (Demo)",
            artist = "Nova Acoustic",
            album = "Ambient Escapes",
            albumId = -2002L,
            durationMs = 344_000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            albumArtUri = null,
            trackNumber = 1,
            dateAddedSec = 1_700_000_003L,
            isRemoteDemo = true
        ),
        Song(
            id = -1004L,
            title = "Golden Echoes (Demo)",
            artist = "Nova Acoustic",
            album = "Ambient Escapes",
            albumId = -2002L,
            durationMs = 302_000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
            albumArtUri = null,
            trackNumber = 2,
            dateAddedSec = 1_700_000_004L,
            isRemoteDemo = true
        )
    )
}
