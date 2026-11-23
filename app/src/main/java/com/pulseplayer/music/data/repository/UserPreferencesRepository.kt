package com.pulseplayer.music.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulseplayer.music.data.model.Playlist
import com.pulseplayer.music.data.model.SortOrder
import com.pulseplayer.music.data.util.PlaylistJsonSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_player_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_song_ids")
        val PLAYLISTS_JSON = stringPreferencesKey("user_playlists_json")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val USE_DEMO_TRACKS = booleanPreferencesKey("use_demo_tracks")
    }

    val favoriteSongIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_IDS]
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    val playlists: Flow<List<Playlist>> = context.dataStore.data.map { prefs ->
        PlaylistJsonSerializer.deserialize(prefs[Keys.PLAYLISTS_JSON])
    }

    val sortOrder: Flow<SortOrder> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SORT_ORDER]
        SortOrder.entries.firstOrNull { it.name == raw } ?: SortOrder.TITLE_ASC
    }

    val useDemoTracks: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.USE_DEMO_TRACKS] ?: false
    }

    suspend fun toggleFavorite(songId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_IDS]?.toMutableSet() ?: mutableSetOf()
            val key = songId.toString()
            if (!current.add(key)) {
                current.remove(key)
            }
            prefs[Keys.FAVORITE_IDS] = current
        }
    }

    suspend fun createPlaylist(name: String): Playlist {
        val trimmed = name.trim().ifEmpty { "Playlist Baru" }
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            songIds = emptyList(),
            createdAtMs = System.currentTimeMillis()
        )
        context.dataStore.edit { prefs ->
            val current = PlaylistJsonSerializer.deserialize(prefs[Keys.PLAYLISTS_JSON])
            prefs[Keys.PLAYLISTS_JSON] = PlaylistJsonSerializer.serialize(current + newPlaylist)
        }
        return newPlaylist
    }

    suspend fun deletePlaylist(playlistId: String) {
        context.dataStore.edit { prefs ->
            val current = PlaylistJsonSerializer.deserialize(prefs[Keys.PLAYLISTS_JSON])
            val updated = current.filterNot { it.id == playlistId }
            prefs[Keys.PLAYLISTS_JSON] = PlaylistJsonSerializer.serialize(updated)
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = PlaylistJsonSerializer.deserialize(prefs[Keys.PLAYLISTS_JSON])
            val updated = current.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(name = trimmed) else playlist
            }
            prefs[Keys.PLAYLISTS_JSON] = PlaylistJsonSerializer.serialize(updated)
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: Long) {
        context.dataStore.edit { prefs ->
            val current = PlaylistJsonSerializer.deserialize(prefs[Keys.PLAYLISTS_JSON])
            val updated = current.map { playlist ->
                if (playlist.id == playlistId && songId !in playlist.songIds) {
                    playlist.copy(songIds = playlist.songIds + songId)
                } else {
                    playlist
                }
            }
            prefs[Keys.PLAYLISTS_JSON] = PlaylistJsonSerializer.serialize(updated)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        context.dataStore.edit { prefs ->
            val current = PlaylistJsonSerializer.deserialize(prefs[Keys.PLAYLISTS_JSON])
            val updated = current.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(songIds = playlist.songIds.filterNot { it == songId })
                } else {
                    playlist
                }
            }
            prefs[Keys.PLAYLISTS_JSON] = PlaylistJsonSerializer.serialize(updated)
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_ORDER] = order.name
        }
    }

    suspend fun setUseDemoTracks(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_DEMO_TRACKS] = enabled
        }
    }
}
