package com.pulseplayer.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.pulseplayer.music.data.model.PlaybackState
import com.pulseplayer.music.data.model.RepeatMode
import com.pulseplayer.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicControllerManager(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionTickerJob: Job? = null

    private val songCache = mutableMapOf<String, Song>()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncStateFromController()
            updatePositionTicker(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncStateFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncStateFromController()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncStateFromController()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncStateFromController()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncStateFromController()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            syncStateFromController()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.localizedMessage ?: "Gagal memutar file audio"
                )
            }
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return

        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { mediaController ->
                        controller = mediaController
                        mediaController.addListener(playerListener)
                        syncStateFromController()
                        updatePositionTicker(mediaController.isPlaying)
                    }
                    .onFailure { throwable ->
                        _playbackState.update {
                            it.copy(
                                isConnected = false,
                                errorMessage = throwable.localizedMessage
                            )
                        }
                    }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        val mediaController = controller ?: return
        if (songs.isEmpty()) return

        songs.forEach { songCache[it.id.toString()] = it }
        val safeStartIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { it.toMediaItem() }

        mediaController.shuffleModeEnabled = shuffle
        mediaController.setMediaItems(mediaItems, safeStartIndex, 0L)
        mediaController.prepare()
        mediaController.play()
        clearError()
        syncStateFromController()
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.mediaItemCount == 0) return

        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            if (mediaController.playbackState == Player.STATE_ENDED) {
                mediaController.seekTo(0, 0L)
            }
            mediaController.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        val mediaController = controller ?: return
        val clamped = positionMs.coerceIn(0L, mediaController.duration.coerceAtLeast(0L))
        mediaController.seekTo(clamped)
        _playbackState.update { it.copy(positionMs = clamped) }
    }

    fun skipNext() {
        val mediaController = controller ?: return
        if (mediaController.hasNextMediaItem()) {
            mediaController.seekToNext()
        } else if (mediaController.mediaItemCount > 0) {
            mediaController.seekToDefaultPosition(0)
        }
        mediaController.play()
    }

    fun skipPrevious() {
        val mediaController = controller ?: return
        if (mediaController.currentPosition > 3_000L) {
            mediaController.seekTo(0L)
        } else if (mediaController.hasPreviousMediaItem()) {
            mediaController.seekToPrevious()
        } else {
            mediaController.seekTo(0L)
        }
    }

    fun skipToQueueIndex(index: Int) {
        val mediaController = controller ?: return
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.seekToDefaultPosition(index)
            mediaController.play()
        }
    }

    fun addToQueue(song: Song) {
        val mediaController = controller ?: return
        songCache[song.id.toString()] = song
        mediaController.addMediaItem(song.toMediaItem())
        if (mediaController.playbackState == Player.STATE_IDLE) {
            mediaController.prepare()
        }
        syncStateFromController()
    }

    fun removeFromQueue(index: Int) {
        val mediaController = controller ?: return
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.removeMediaItem(index)
            syncStateFromController()
        }
    }

    fun toggleShuffle() {
        val mediaController = controller ?: return
        mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val mediaController = controller ?: return
        mediaController.repeatMode = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val mediaController = controller ?: return
        val clamped = speed.coerceIn(0.5f, 2.0f)
        mediaController.setPlaybackSpeed(clamped)
        syncStateFromController()
    }

    fun clearError() {
        _playbackState.update { it.copy(errorMessage = null) }
    }

    fun release() {
        positionTickerJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun updatePositionTicker(isPlaying: Boolean) {
        positionTickerJob?.cancel()
        if (!isPlaying) return

        positionTickerJob = scope.launch {
            while (isActive) {
                val mediaController = controller ?: break
                val duration = mediaController.duration.coerceAtLeast(0L)
                val pos = mediaController.currentPosition.coerceAtLeast(0L)
                _playbackState.update { state ->
                    state.copy(
                        positionMs = pos,
                        durationMs = if (duration > 0L) duration else state.currentSong?.durationMs ?: 0L
                    )
                }
                delay(250L)
            }
        }
    }

    private fun syncStateFromController() {
        val mediaController = controller ?: return
        val count = mediaController.mediaItemCount
        val queue = buildList(count) {
            for (i in 0 until count) {
                add(mediaController.getMediaItemAt(i).toSong())
            }
        }
        val currentIndex = mediaController.currentMediaItemIndex
        val currentSong = mediaController.currentMediaItem?.toSong()
            ?: queue.getOrNull(currentIndex)

        val controllerDuration = mediaController.duration.coerceAtLeast(0L)
        val resolvedDuration = if (controllerDuration > 0L) {
            controllerDuration
        } else {
            currentSong?.durationMs ?: 0L
        }

        val repeat = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

        _playbackState.update { prev ->
            prev.copy(
                isConnected = true,
                isPlaying = mediaController.isPlaying,
                isBuffering = mediaController.playbackState == Player.STATE_BUFFERING,
                currentSong = currentSong,
                currentIndex = if (currentSong != null) currentIndex else -1,
                queue = queue,
                positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                durationMs = resolvedDuration,
                shuffleEnabled = mediaController.shuffleModeEnabled,
                repeatMode = repeat,
                playbackSpeed = mediaController.playbackParameters.speed
            )
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong(EXTRA_ALBUM_ID, albumId)
            putLong(EXTRA_DURATION_MS, durationMs)
            putBoolean(EXTRA_IS_REMOTE_DEMO, isRemoteDemo)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toSong(): Song {
        songCache[mediaId]?.let { return it }
        val meta = mediaMetadata
        val extras = meta.extras
        return Song(
            id = mediaId.toLongOrNull() ?: mediaId.hashCode().toLong(),
            title = meta.title?.toString().orEmpty().ifBlank { "Tanpa Judul" },
            artist = meta.artist?.toString().orEmpty().ifBlank { "Artis Tidak Diketahui" },
            album = meta.albumTitle?.toString().orEmpty().ifBlank { "Album Tidak Diketahui" },
            albumId = extras?.getLong(EXTRA_ALBUM_ID, 0L) ?: 0L,
            durationMs = extras?.getLong(EXTRA_DURATION_MS, 0L) ?: 0L,
            contentUri = localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = meta.artworkUri,
            isRemoteDemo = extras?.getBoolean(EXTRA_IS_REMOTE_DEMO, false) ?: false
        )
    }

    companion object {
        private const val EXTRA_ALBUM_ID = "extra_album_id"
        private const val EXTRA_DURATION_MS = "extra_duration_ms"
        private const val EXTRA_IS_REMOTE_DEMO = "extra_is_remote_demo"
    }
}
