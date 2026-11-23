package com.pulseplayer.music.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulseplayer.music.data.model.PlaybackState
import com.pulseplayer.music.data.model.RepeatMode
import com.pulseplayer.music.data.model.Song
import com.pulseplayer.music.data.util.TimeFormatter
import com.pulseplayer.music.ui.components.ArtworkImage
import com.pulseplayer.music.ui.theme.AppleMusicRed
import com.pulseplayer.music.ui.util.ArtworkPaletteExtractor
import com.pulseplayer.music.ui.util.DynamicBackdropColors
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    isEqualizerEnabled: Boolean,
    sleepTimerRemainingMs: Long?,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val song = playbackState.currentSong

    // Dynamic Palette API extraction from Album Art
    var extractedColors by remember { mutableStateOf(DynamicBackdropColors()) }
    LaunchedEffect(song?.albumArtUri) {
        extractedColors = ArtworkPaletteExtractor.extract(context, song?.albumArtUri)
    }

    val animatedPrimary by animateColorAsState(
        targetValue = extractedColors.primary,
        animationSpec = tween(durationMillis = 700),
        label = "bg_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = extractedColors.secondary,
        animationSpec = tween(durationMillis = 700),
        label = "bg_secondary"
    )
    val animatedDeep by animateColorAsState(
        targetValue = extractedColors.deep,
        animationSpec = tween(durationMillis = 700),
        label = "bg_deep"
    )

    // Interactive seekbar state
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val displayedPositionMs = if (isDraggingSlider) {
        dragPositionMs.roundToLong()
    } else {
        playbackState.positionMs.coerceIn(0L, durationMs)
    }
    val remainingMs = (playbackState.durationMs - displayedPositionMs).coerceAtLeast(0L)

    // System hardware media volume state
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volumeLevel by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
        )
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                volumeLevel = (current.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // Apple Music iOS-style album art scale & shadow animation
    val artworkScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1.0f else 0.84f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ios_artwork_scale"
    )
    val artworkShadow by animateDpAsState(
        targetValue = if (playbackState.isPlaying) 28.dp else 8.dp,
        animationSpec = tween(350),
        label = "ios_artwork_shadow"
    )

    var verticalSwipeDelta by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic Blurred Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(animatedPrimary, animatedSecondary, animatedDeep)
                    )
                )
        )

        // Luminous Apple Music style ambient color orbs with blur on API 31+
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(85.dp)
                    } else {
                        Modifier
                    }
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = 0.72f),
                            Color.Transparent
                        ),
                        center = Offset(220f, 260f),
                        radius = 780f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedSecondary.copy(alpha = 0.65f),
                            Color.Transparent
                        ),
                        center = Offset(860f, 1280f),
                        radius = 840f
                    )
                )
        )

        // Subtle dark scrim to guarantee crisp white typography contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS Drag Handle (swipe down or tap to dismiss)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { verticalSwipeDelta = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                verticalSwipeDelta += dragAmount
                                if (verticalSwipeDelta > 90f) {
                                    verticalSwipeDelta = 0f
                                    onCollapse()
                                }
                            },
                            onDragEnd = { verticalSwipeDelta = 0f }
                        )
                    }
                    .clickable(onClick = onCollapse)
                    .padding(top = 10.dp, bottom = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.42f))
                )
            }

            Spacer(modifier = Modifier.weight(0.55f))

            // Large Centered Album Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f)
                    .scale(artworkScale)
                    .shadow(
                        elevation = artworkShadow,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
            ) {
                ArtworkImage(
                    artworkUri = song?.albumArtUri,
                    contentDescription = song?.album,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(18.dp),
                    iconSize = 86.dp
                )
            }

            Spacer(modifier = Modifier.weight(0.65f))

            // Song Metadata + Favorite & Playlist Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "Belum Ada Lagu",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song?.artist ?: "Pilih lagu dari perpustakaan",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (song != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        IconButton(
                            onClick = { onToggleFavorite(song.id) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "Hapus dari favorit" else "Favoritkan",
                                tint = if (isFavorite) AppleMusicRed else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        IconButton(
                            onClick = { onAddToPlaylist(song) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = "Tambah ke playlist",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Progress Slider
            Slider(
                value = displayedPositionMs.toFloat(),
                onValueChange = { newPos ->
                    isDraggingSlider = true
                    dragPositionMs = newPos
                },
                onValueChangeFinished = {
                    onSeekTo(dragPositionMs.roundToLong())
                    isDraggingSlider = false
                },
                valueRange = 0f..durationMs.toFloat(),
                enabled = song != null,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.92f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TimeFormatter.formatDuration(displayedPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "-${TimeFormatter.formatDuration(remainingMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Playback Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Mode acak",
                        tint = if (playbackState.shuffleEnabled) {
                            AppleMusicRed
                        } else {
                            Color.White.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Lagu sebelumnya",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp)
                ) {
                    if (playbackState.isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (playbackState.isPlaying) {
                                Icons.Rounded.Pause
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                            contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Lagu berikutnya",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = onCycleRepeat) {
                    val repeatIcon = when (playbackState.repeatMode) {
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val repeatTint = if (playbackState.repeatMode == RepeatMode.OFF) {
                        Color.White.copy(alpha = 0.6f)
                    } else {
                        AppleMusicRed
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Mode ulang",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apple Music Signature Hardware Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = "Volume turun",
                    tint = Color.White.copy(alpha = 0.68f),
                    modifier = Modifier.size(18.dp)
                )

                Slider(
                    value = volumeLevel,
                    onValueChange = { fraction ->
                        volumeLevel = fraction
                        val targetStep = (fraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.9f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Volume naik",
                    tint = Color.White.copy(alpha = 0.68f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Utility Actions (Equalizer, Speed, Sleep Timer, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NowPlayingBottomAction(
                    icon = Icons.Rounded.GraphicEq,
                    label = "Equalizer",
                    active = isEqualizerEnabled,
                    onClick = onOpenEqualizer
                )
                NowPlayingBottomAction(
                    icon = Icons.Rounded.Speed,
                    label = String.format(Locale.US, "%.2gx", playbackState.playbackSpeed),
                    active = playbackState.playbackSpeed != 1.0f,
                    onClick = onOpenSpeed
                )
                NowPlayingBottomAction(
                    icon = Icons.Rounded.Timer,
                    label = if (sleepTimerRemainingMs != null) {
                        TimeFormatter.formatDuration(sleepTimerRemainingMs)
                    } else {
                        "Timer"
                    },
                    active = sleepTimerRemainingMs != null,
                    onClick = onOpenSleepTimer
                )
                NowPlayingBottomAction(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "Queue",
                    active = false,
                    onClick = onOpenQueue
                )
            }
        }
    }
}

@Composable
private fun NowPlayingBottomAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) Color.White else Color.White.copy(alpha = 0.72f)
    val bg = if (active) AppleMusicRed.copy(alpha = 0.85f) else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
    }
}
