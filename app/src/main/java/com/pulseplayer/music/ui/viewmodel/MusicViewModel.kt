package com.pulseplayer.music.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulseplayer.music.PulsePlayerApplication
import com.pulseplayer.music.data.model.Album
import com.pulseplayer.music.data.model.Artist
import com.pulseplayer.music.data.model.PlaybackState
import com.pulseplayer.music.data.model.Playlist
import com.pulseplayer.music.data.model.Song
import com.pulseplayer.music.data.model.SortOrder
import com.pulseplayer.music.data.repository.DemoTracksProvider
import com.pulseplayer.music.data.repository.MediaStoreRepository
import com.pulseplayer.music.data.repository.UserPreferencesRepository
import com.pulseplayer.music.player.AudioEffectsManager
import com.pulseplayer.music.player.EqualizerPreset
import com.pulseplayer.music.player.EqualizerState
import com.pulseplayer.music.player.MusicControllerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class LibraryUiState(
    val isLoading: Boolean = false,
    val allSongs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val favoriteSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val isUsingDemoTracks: Boolean = false
)

class MusicViewModel(
    private val mediaStoreRepository: MediaStoreRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val controllerManager: MusicControllerManager,
    private val audioEffectsManager: AudioEffectsManager
) : ViewModel() {

    private val localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val isLoadingLibrary = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = controllerManager.playbackState
    val equalizerState: StateFlow<EqualizerState> = audioEffectsManager.state

    private val combinedSongsFlow = combine(
        localSongs,
        userPreferencesRepository.useDemoTracks
    ) { local, useDemo ->
        if (useDemo) {
            local + DemoTracksProvider.getDemoSongs()
        } else {
            local
        }
    }

    private val libraryFiltersFlow = combine(
        isLoadingLibrary,
        combinedSongsFlow,
        searchQuery,
        userPreferencesRepository.sortOrder
    ) { loading, songs, query, sort ->
        LibraryFilters(loading, songs, query, sort)
    }

    private val userCollectionsFlow = combine(
        userPreferencesRepository.favoriteSongIds,
        userPreferencesRepository.playlists,
        userPreferencesRepository.useDemoTracks
    ) { favorites, playlists, demoEnabled ->
        UserCollections(favorites, playlists, demoEnabled)
    }

    val libraryState: StateFlow<LibraryUiState> = combine(
        libraryFiltersFlow,
        userCollectionsFlow
    ) { filters, collections ->
        val sortedSongs = sortSongs(filters.songs, filters.sort)
        val filtered = filterSongs(sortedSongs, filters.query)
        val albums = mediaStoreRepository.extractAlbums(filtered)
        val artists = mediaStoreRepository.extractArtists(filtered)
        val favSongs = sortedSongs.filter { it.id in collections.favorites }

        LibraryUiState(
            isLoading = filters.loading,
            allSongs = sortedSongs,
            filteredSongs = filtered,
            albums = albums,
            artists = artists,
            favoriteIds = collections.favorites,
            favoriteSongs = favSongs,
            playlists = collections.playlists,
            searchQuery = filters.query,
            sortOrder = filters.sort,
            isUsingDemoTracks = collections.demoEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LibraryUiState(isLoading = true)
    )

    private data class LibraryFilters(
        val loading: Boolean,
        val songs: List<Song>,
        val query: String,
        val sort: SortOrder
    )

    private data class UserCollections(
        val favorites: Set<Long>,
        val playlists: List<Playlist>,
        val demoEnabled: Boolean
    )

    init {
        controllerManager.connect()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            isLoadingLibrary.value = true
            localSongs.value = mediaStoreRepository.loadLocalSongs()
            isLoadingLibrary.value = false
        }
    }

    fun setDemoTracksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseDemoTracks(enabled)
            showMessage(
                if (enabled) "Lagu demo online ditambahkan ke perpustakaan"
                else "Lagu demo online dinonaktifkan"
            )
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        viewModelScope.launch {
            userPreferencesRepository.setSortOrder(order)
        }
    }

    // Playback Controls
    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        controllerManager.playQueue(songs, startIndex, shuffle)
    }

    fun playAllShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        val randomIndex = songs.indices.random()
        controllerManager.playQueue(songs, startIndex = randomIndex, shuffle = true)
    }

    fun togglePlayPause() = controllerManager.togglePlayPause()
    fun seekTo(positionMs: Long) = controllerManager.seekTo(positionMs)
    fun skipNext() = controllerManager.skipNext()
    fun skipPrevious() = controllerManager.skipPrevious()
    fun skipToQueueIndex(index: Int) = controllerManager.skipToQueueIndex(index)
    fun removeFromQueue(index: Int) = controllerManager.removeFromQueue(index)
    fun toggleShuffle() = controllerManager.toggleShuffle()
    fun cycleRepeatMode() = controllerManager.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = controllerManager.setPlaybackSpeed(speed)

    fun addToQueue(song: Song) {
        controllerManager.addToQueue(song)
        showMessage("\"${song.title}\" ditambahkan ke antrean")
    }

    fun clearPlaybackError() = controllerManager.clearError()

    // Favorites & Playlists
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            userPreferencesRepository.toggleFavorite(songId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val created = userPreferencesRepository.createPlaylist(name)
            showMessage("Playlist \"${created.name}\" dibuat")
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            userPreferencesRepository.deletePlaylist(playlistId)
            showMessage("Playlist dihapus")
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            userPreferencesRepository.renamePlaylist(playlistId, newName)
            showMessage("Nama playlist diperbarui")
        }
    }

    fun addSongToPlaylist(playlistId: String, playlistName: String, song: Song) {
        viewModelScope.launch {
            userPreferencesRepository.addSongToPlaylist(playlistId, song.id)
            showMessage("\"${song.title}\" ditambahkan ke $playlistName")
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            userPreferencesRepository.removeSongFromPlaylist(playlistId, songId)
            showMessage("Lagu dihapus dari playlist")
        }
    }

    fun songsForAlbum(albumId: Long): List<Song> {
        return libraryState.value.allSongs
            .filter { it.albumId == albumId }
            .sortedWith(compareBy<Song> { it.trackNumber }.thenBy { it.title.lowercase() })
    }

    fun songsForArtist(artistName: String): List<Song> {
        return libraryState.value.allSongs
            .filter { it.artist == artistName }
            .sortedBy { it.title.lowercase() }
    }

    fun songsForPlaylist(playlist: Playlist): List<Song> {
        val songMap = libraryState.value.allSongs.associateBy { it.id }
        return playlist.songIds.mapNotNull { songMap[it] }
    }

    // Equalizer
    fun setEqualizerEnabled(enabled: Boolean) = audioEffectsManager.setEnabled(enabled)
    fun setEqualizerBandLevel(bandIndex: Short, levelMb: Short) =
        audioEffectsManager.setBandLevel(bandIndex, levelMb)
    fun selectEqualizerPreset(preset: EqualizerPreset) = audioEffectsManager.selectPreset(preset)
    fun setBassBoostStrength(strength: Int) = audioEffectsManager.setBassBoostStrength(strength)
    fun resetEqualizer() = audioEffectsManager.resetBands()

    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingMs.value = null
            showMessage("Sleep timer dimatikan")
            return
        }

        val totalMs = minutes * 60_000L
        _sleepTimerRemainingMs.value = totalMs
        showMessage("Sleep timer diatur ke $minutes menit")

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1_000L)
                remaining = (remaining - 1_000L).coerceAtLeast(0L)
                _sleepTimerRemainingMs.value = remaining
            }
            if (isActive && remaining == 0L) {
                controllerManager.pause()
                _sleepTimerRemainingMs.value = null
                showMessage("Sleep timer selesai — pemutaran dijeda")
            }
        }
    }

    fun showMessage(message: String) {
        _uiMessage.value = message
    }

    fun consumeMessage() {
        _uiMessage.value = null
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        super.onCleared()
    }

    private fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
        SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
        SortOrder.ARTIST_ASC -> songs.sortedWith(
            compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
        )
        SortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAddedSec }
        SortOrder.DURATION_DESC -> songs.sortedByDescending { it.durationMs }
    }

    private fun filterSongs(songs: List<Song>, query: String): List<Song> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return songs
        return songs.filter { song ->
            song.title.contains(trimmed, ignoreCase = true) ||
                song.artist.contains(trimmed, ignoreCase = true) ||
                song.album.contains(trimmed, ignoreCase = true)
        }
    }

    companion object {
        fun provideFactory(application: PulsePlayerApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MusicViewModel(
                        mediaStoreRepository = application.mediaStoreRepository,
                        userPreferencesRepository = application.userPreferencesRepository,
                        controllerManager = application.musicControllerManager,
                        audioEffectsManager = application.audioEffectsManager
                    ) as T
                }
            }
        }
    }
}
