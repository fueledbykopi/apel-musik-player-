package com.pulseplayer.music.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulseplayer.music.data.model.Album
import com.pulseplayer.music.data.model.Artist
import com.pulseplayer.music.data.model.Playlist
import com.pulseplayer.music.data.model.Song
import com.pulseplayer.music.ui.components.AddToPlaylistBottomSheet
import com.pulseplayer.music.ui.components.CreatePlaylistDialog
import com.pulseplayer.music.ui.components.EqualizerBottomSheet
import com.pulseplayer.music.ui.components.MiniPlayer
import com.pulseplayer.music.ui.components.PlaybackSpeedBottomSheet
import com.pulseplayer.music.ui.components.QueueBottomSheet
import com.pulseplayer.music.ui.components.SleepTimerBottomSheet
import com.pulseplayer.music.ui.screens.CollectionDetailScreen
import com.pulseplayer.music.ui.screens.LibraryScreen
import com.pulseplayer.music.ui.screens.NowPlayingScreen
import com.pulseplayer.music.ui.screens.PermissionScreen
import com.pulseplayer.music.ui.screens.PlaylistsScreen
import com.pulseplayer.music.ui.screens.SearchScreen
import com.pulseplayer.music.ui.viewmodel.MusicViewModel

enum class BottomNavTab(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay),
    SEARCH("Search", Icons.Rounded.Search)
}

private sealed interface ActiveCollection {
    data class AlbumCollection(val album: Album) : ActiveCollection
    data class ArtistCollection(val artist: Artist) : ActiveCollection
    data object FavoritesCollection : ActiveCollection
    data class PlaylistCollection(val playlistId: String) : ActiveCollection
}

@Composable
fun PulsePlayerApp(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    val permissionsToRequest = remember {
        buildList {
            add(audioPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var bypassWithDemo by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionRequestedOnce = true
        val granted = result[audioPermission] == true ||
            ContextCompat.checkSelfPermission(context, audioPermission) ==
            PackageManager.PERMISSION_GRANTED
        hasAudioPermission = granted
        if (granted) {
            viewModel.refreshLibrary()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val currentlyGranted = ContextCompat.checkSelfPermission(context, audioPermission) ==
            PackageManager.PERMISSION_GRANTED
        if (currentlyGranted != hasAudioPermission) {
            hasAudioPermission = currentlyGranted
        }
        if (currentlyGranted) {
            viewModel.refreshLibrary()
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refreshLibrary()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    LaunchedEffect(playbackState.errorMessage) {
        val error = playbackState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearPlaybackError()
    }

    if (!hasAudioPermission && !bypassWithDemo && !libraryState.isUsingDemoTracks) {
        PermissionScreen(
            permanentlyDenied = permissionRequestedOnce && !hasAudioPermission,
            onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            },
            onContinueWithDemo = {
                bypassWithDemo = true
                viewModel.setDemoTracksEnabled(true)
            }
        )
        return
    }

    var selectedTab by remember { mutableStateOf(BottomNavTab.LIBRARY) }
    var activeCollection by remember { mutableStateOf<ActiveCollection?>(null) }
    var nowPlayingExpanded by remember { mutableStateOf(false) }

    // Bottom sheets & dialogs state
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    BackHandler(enabled = nowPlayingExpanded || activeCollection != null) {
        when {
            nowPlayingExpanded -> nowPlayingExpanded = false
            activeCollection != null -> activeCollection = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Content Layer (scrolls behind the floating mini player & translucent iOS bottom bar)
        val currentCollection = activeCollection
        if (currentCollection == null) {
            when (selectedTab) {
                BottomNavTab.LIBRARY -> LibraryScreen(
                    libraryState = libraryState,
                    playbackState = playbackState,
                    onSortOrderChange = viewModel::updateSortOrder,
                    onRefresh = viewModel::refreshLibrary,
                    onToggleDemoTracks = viewModel::setDemoTracksEnabled,
                    onOpenEqualizer = { showEqualizerSheet = true },
                    onPlaySong = { list, index -> viewModel.playSongs(list, index) },
                    onShuffleAll = viewModel::playAllShuffled,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToQueue = viewModel::addToQueue,
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onOpenAlbum = { activeCollection = ActiveCollection.AlbumCollection(it) },
                    onOpenArtist = { activeCollection = ActiveCollection.ArtistCollection(it) }
                )

                BottomNavTab.PLAYLISTS -> PlaylistsScreen(
                    favoriteCount = libraryState.favoriteSongs.size,
                    playlists = libraryState.playlists,
                    onOpenFavorites = { activeCollection = ActiveCollection.FavoritesCollection },
                    onOpenPlaylist = { activeCollection = ActiveCollection.PlaylistCollection(it.id) },
                    onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                    onRenamePlaylistClick = { playlistToRename = it },
                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) }
                )

                BottomNavTab.SEARCH -> SearchScreen(
                    libraryState = libraryState,
                    playbackState = playbackState,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onPlaySong = { list, index -> viewModel.playSongs(list, index) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToQueue = viewModel::addToQueue,
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onOpenAlbum = { activeCollection = ActiveCollection.AlbumCollection(it) },
                    onOpenArtist = { activeCollection = ActiveCollection.ArtistCollection(it) },
                    onOpenFavorites = { activeCollection = ActiveCollection.FavoritesCollection },
                    onOpenEqualizer = { showEqualizerSheet = true },
                    onShuffleAll = viewModel::playAllShuffled
                )
            }
        } else {
            when (currentCollection) {
                is ActiveCollection.AlbumCollection -> {
                    val songs = viewModel.songsForAlbum(currentCollection.album.id)
                    CollectionDetailScreen(
                        title = currentCollection.album.name,
                        subtitle = currentCollection.album.artist,
                        artworkUri = currentCollection.album.albumArtUri,
                        songs = songs,
                        playbackState = playbackState,
                        favoriteIds = libraryState.favoriteIds,
                        onBack = { activeCollection = null },
                        onPlaySong = { list, idx -> viewModel.playSongs(list, idx) },
                        onShuffleAll = viewModel::playAllShuffled,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = { songToAddToPlaylist = it }
                    )
                }

                is ActiveCollection.ArtistCollection -> {
                    val songs = viewModel.songsForArtist(currentCollection.artist.name)
                    CollectionDetailScreen(
                        title = currentCollection.artist.name,
                        subtitle = "${currentCollection.artist.albumCount} album",
                        artworkUri = currentCollection.artist.artworkUri,
                        songs = songs,
                        playbackState = playbackState,
                        favoriteIds = libraryState.favoriteIds,
                        onBack = { activeCollection = null },
                        onPlaySong = { list, idx -> viewModel.playSongs(list, idx) },
                        onShuffleAll = viewModel::playAllShuffled,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = { songToAddToPlaylist = it }
                    )
                }

                ActiveCollection.FavoritesCollection -> {
                    CollectionDetailScreen(
                        title = "Favorite Songs",
                        subtitle = "Koleksi disukai",
                        artworkUri = null,
                        songs = libraryState.favoriteSongs,
                        playbackState = playbackState,
                        favoriteIds = libraryState.favoriteIds,
                        onBack = { activeCollection = null },
                        onPlaySong = { list, idx -> viewModel.playSongs(list, idx) },
                        onShuffleAll = viewModel::playAllShuffled,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onAddToPlaylist = { songToAddToPlaylist = it }
                    )
                }

                is ActiveCollection.PlaylistCollection -> {
                    val playlist = libraryState.playlists.firstOrNull {
                        it.id == currentCollection.playlistId
                    }
                    if (playlist == null) {
                        activeCollection = null
                    } else {
                        val songs = viewModel.songsForPlaylist(playlist)
                        CollectionDetailScreen(
                            title = playlist.name,
                            subtitle = "Playlist",
                            artworkUri = null,
                            songs = songs,
                            playbackState = playbackState,
                            favoriteIds = libraryState.favoriteIds,
                            onBack = { activeCollection = null },
                            onPlaySong = { list, idx -> viewModel.playSongs(list, idx) },
                            onShuffleAll = viewModel::playAllShuffled,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onAddToQueue = viewModel::addToQueue,
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onRemoveSongFromCollection = { song ->
                                viewModel.removeSongFromPlaylist(playlist.id, song.id)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Overlay: Snackbar + Floating Mini Player + Semi-Transparent iOS Bottom Navigation Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            SnackbarHost(hostState = snackbarHostState)

            if (playbackState.currentSong != null) {
                MiniPlayer(
                    playbackState = playbackState,
                    onExpand = { nowPlayingExpanded = true },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    modifier = if (activeCollection != null) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    }
                )
            }

            if (activeCollection == null) {
                TranslucentIosBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        selectedTab = tab
                        if (tab != BottomNavTab.SEARCH && libraryState.searchQuery.isNotEmpty()) {
                            viewModel.updateSearchQuery("")
                        }
                    }
                )
            }
        }

        // Full-Screen Apple Music Style Now Playing Sheet
        AnimatedVisibility(
            visible = nowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val currentId = playbackState.currentSong?.id
            NowPlayingScreen(
                playbackState = playbackState,
                isFavorite = currentId != null && currentId in libraryState.favoriteIds,
                isEqualizerEnabled = equalizerState.isEnabled,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                onCollapse = { nowPlayingExpanded = false },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSkipNext = viewModel::skipNext,
                onSkipPrevious = viewModel::skipPrevious,
                onSeekTo = viewModel::seekTo,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { songToAddToPlaylist = it },
                onOpenQueue = { showQueueSheet = true },
                onOpenEqualizer = { showEqualizerSheet = true },
                onOpenSleepTimer = { showSleepTimerSheet = true },
                onOpenSpeed = { showSpeedSheet = true }
            )
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            playbackState = playbackState,
            onSelectIndex = viewModel::skipToQueueIndex,
            onRemoveIndex = viewModel::removeFromQueue,
            onDismiss = { showQueueSheet = false }
        )
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            state = equalizerState,
            onSetEnabled = viewModel::setEqualizerEnabled,
            onSelectPreset = viewModel::selectEqualizerPreset,
            onBandLevelChange = viewModel::setEqualizerBandLevel,
            onBassBoostChange = viewModel::setBassBoostStrength,
            onReset = viewModel::resetEqualizer,
            onDismiss = { showEqualizerSheet = false }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            remainingMs = sleepTimerRemainingMs,
            onSelectMinutes = viewModel::setSleepTimer,
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    if (showSpeedSheet) {
        PlaybackSpeedBottomSheet(
            currentSpeed = playbackState.playbackSpeed,
            onSelectSpeed = viewModel::setPlaybackSpeed,
            onDismiss = { showSpeedSheet = false }
        )
    }

    songToAddToPlaylist?.let { targetSong ->
        AddToPlaylistBottomSheet(
            song = targetSong,
            playlists = libraryState.playlists,
            onSelectPlaylist = { playlist ->
                viewModel.addSongToPlaylist(playlist.id, playlist.name, targetSong)
            },
            onCreateNewPlaylist = { showCreatePlaylistDialog = true },
            onDismiss = { songToAddToPlaylist = null }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onConfirm = viewModel::createPlaylist,
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }

    playlistToRename?.let { playlist ->
        CreatePlaylistDialog(
            initialName = playlist.name,
            title = "Ubah Nama Playlist",
            confirmLabel = "Simpan",
            onConfirm = { newName -> viewModel.renamePlaylist(playlist.id, newName) },
            onDismiss = { playlistToRename = null }
        )
    }
}

@Composable
private fun TranslucentIosBottomBar(
    selectedTab: BottomNavTab,
    onSelectTab: (BottomNavTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
