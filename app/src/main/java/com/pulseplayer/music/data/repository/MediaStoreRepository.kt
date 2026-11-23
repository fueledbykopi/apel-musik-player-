package com.pulseplayer.music.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pulseplayer.music.data.model.Album
import com.pulseplayer.music.data.model.Artist
import com.pulseplayer.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun loadLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "(" +
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
            "${MediaStore.Audio.Media.MIME_TYPE} IN (" +
            "'audio/mpeg', 'audio/flac', 'audio/x-flac', 'audio/mp4', 'audio/x-m4a', " +
            "'audio/wav', 'audio/x-wav', 'audio/ogg', 'application/ogg'" +
            ")) AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Tanpa Judul"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = sanitizeMetadata(rawArtist, "Artis Tidak Diketahui")
                    val rawAlbum = cursor.getString(albumCol)
                    val album = sanitizeMetadata(rawAlbum, "Album Tidak Diketahui")
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val trackNumber = cursor.getInt(trackCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val size = cursor.getLong(sizeCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArtUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId)
                    } else {
                        null
                    }

                    songs += Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        durationMs = duration,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        trackNumber = trackNumber,
                        dateAddedSec = dateAdded,
                        sizeBytes = size,
                        isRemoteDemo = false
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }

        songs
    }

    fun extractAlbums(songs: List<Song>): List<Album> {
        return songs
            .groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = albumId,
                    name = first.album,
                    artist = first.artist,
                    albumArtUri = first.albumArtUri,
                    songCount = albumSongs.size,
                    totalDurationMs = albumSongs.sumOf { it.durationMs }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun extractArtists(songs: List<Song>): List<Artist> {
        return songs
            .groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                Artist(
                    name = artistName,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.map { it.albumId }.distinct().size,
                    artworkUri = artistSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun sanitizeMetadata(raw: String?, fallback: String): String {
        val cleaned = raw?.trim().orEmpty()
        return if (cleaned.isEmpty() || cleaned.equals("<unknown>", ignoreCase = true)) {
            fallback
        } else {
            cleaned
        }
    }

    companion object {
        private const val MIN_DURATION_MS = 5_000L
    }
}
