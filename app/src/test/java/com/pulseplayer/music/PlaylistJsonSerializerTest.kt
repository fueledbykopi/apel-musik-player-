package com.pulseplayer.music

import com.pulseplayer.music.data.model.Playlist
import com.pulseplayer.music.data.util.PlaylistJsonSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistJsonSerializerTest {

    @Test
    fun roundTripSerializeAndDeserialize_preservesPlaylists() {
        val original = listOf(
            Playlist(
                id = "pl-1",
                name = "Fokus Coding",
                songIds = listOf(101L, 202L, 303L),
                createdAtMs = 1_700_000_000_000L
            ),
            Playlist(
                id = "pl-2",
                name = "Santai Sore",
                songIds = emptyList(),
                createdAtMs = 1_700_000_123_456L
            )
        )

        val json = PlaylistJsonSerializer.serialize(original)
        val restored = PlaylistJsonSerializer.deserialize(json)

        assertEquals(original, restored)
    }

    @Test
    fun deserialize_handlesNullBlankAndMalformedJsonSafely() {
        assertTrue(PlaylistJsonSerializer.deserialize(null).isEmpty())
        assertTrue(PlaylistJsonSerializer.deserialize("   ").isEmpty())
        assertTrue(PlaylistJsonSerializer.deserialize("{not-a-valid-array").isEmpty())
    }
}
