package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulseplayer.music.data.model.Album
import com.pulseplayer.music.data.model.Artist
import com.pulseplayer.music.data.model.PlaybackState
import com.pulseplayer.music.data.model.Song
import com.pulseplayer.music.ui.components.ArtworkImage
import com.pulseplayer.music.ui.components.SongListItem
import com.pulseplayer.music.ui.viewmodel.LibraryUiState

@Composable
fun SearchScreen(
    libraryState: LibraryUiState,
    playbackState: PlaybackState,
    onSearchQueryChange: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = libraryState.searchQuery

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
        )

        // iOS-style Filled Rounded Search Field
        TextField(
            value = query,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Artis, Lagu, dan Album",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus pencarian",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (query.isBlank()) {
            // Browse shortcuts when search query is empty
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 170.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Akses Cepat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrowseTile(
                            title = "Lagu Favorit",
                            subtitle = "${libraryState.favoriteSongs.size} lagu",
                            icon = Icons.Rounded.Favorite,
                            accentColor = Color(0xFFFA2D48),
                            onClick = onOpenFavorites,
                            modifier = Modifier.weight(1f)
                        )
                        BrowseTile(
                            title = "Acak Semua",
                            subtitle = "${libraryState.allSongs.size} lagu",
                            icon = Icons.Rounded.MusicNote,
                            accentColor = Color(0xFFAF52DE),
                            onClick = { onShuffleAll(libraryState.allSongs) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrowseTile(
                            title = "Equalizer 5-Band",
                            subtitle = "Atur suara & bass",
                            icon = Icons.Rounded.GraphicEq,
                            accentColor = Color(0xFFFF6B35),
                            onClick = onOpenEqualizer,
                            modifier = Modifier.weight(1f)
                        )
                        BrowseTile(
                            title = "Album Lokal",
                            subtitle = "${libraryState.albums.size} album",
                            icon = Icons.Rounded.Album,
                            accentColor = Color(0xFF0A84FF),
                            onClick = {
                                libraryState.albums.firstOrNull()?.let(onOpenAlbum)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (libraryState.albums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Album di Perangkat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(libraryState.albums.take(10), key = { it.id }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(126.dp)
                                        .clickable { onOpenAlbum(album) }
                                ) {
                                    ArtworkImage(
                                        artworkUri = album.albumArtUri,
                                        contentDescription = album.name,
                                        modifier = Modifier.size(126.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        iconSize = 36.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = album.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val hasResults = libraryState.filteredSongs.isNotEmpty() ||
                libraryState.albums.isNotEmpty() ||
                libraryState.artists.isNotEmpty()

            if (!hasResults) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tidak ada hasil untuk \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coba kata kunci judul lagu, nama artis, atau album lain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 170.dp)
                ) {
                    if (libraryState.artists.isNotEmpty()) {
                        item {
                            SectionHeader("Artis")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(libraryState.artists, key = { it.name }) { artist ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(84.dp)
                                            .clickable { onOpenArtist(artist) }
                                    ) {
                                        ArtworkImage(
                                            artworkUri = artist.artworkUri,
                                            contentDescription = artist.name,
                                            modifier = Modifier.size(68.dp),
                                            shape = CircleShape,
                                            iconSize = 26.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (libraryState.albums.isNotEmpty()) {
                        item {
                            SectionHeader("Album")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(libraryState.albums, key = { it.id }) { album ->
                                    Column(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable { onOpenAlbum(album) }
                                    ) {
                                        ArtworkImage(
                                            artworkUri = album.albumArtUri,
                                            contentDescription = album.name,
                                            modifier = Modifier.size(120.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            iconSize = 34.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (libraryState.filteredSongs.isNotEmpty()) {
                        item {
                            SectionHeader("Lagu")
                        }
                        itemsIndexed(
                            items = libraryState.filteredSongs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                isCurrentSong = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                isFavorite = song.id in libraryState.favoriteIds,
                                onClick = { onPlaySong(libraryState.filteredSongs, index) },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun BrowseTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.16f),
        modifier = modifier.height(92.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
