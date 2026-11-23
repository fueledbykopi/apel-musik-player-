package com.pulseplayer.music.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class DynamicBackdropColors(
    val primary: Color = Color(0xFF7B1B30),
    val secondary: Color = Color(0xFF3E182B),
    val deep: Color = Color(0xFF140B12)
)

object ArtworkPaletteExtractor {

    private val defaultColors = DynamicBackdropColors()

    suspend fun extract(context: Context, artworkUri: Uri?): DynamicBackdropColors {
        if (artworkUri == null) return defaultColors

        return withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .allowHardware(false)
                    .size(256)
                    .build()

                val result = loader.execute(request) as? SuccessResult ?: return@runCatching defaultColors
                val bitmap: Bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching defaultColors

                val palette = Palette.from(bitmap)
                    .maximumColorCount(16)
                    .generate()

                val vibrant = palette.vibrantSwatch?.rgb
                val darkVibrant = palette.darkVibrantSwatch?.rgb
                val dominant = palette.dominantSwatch?.rgb
                val muted = palette.mutedSwatch?.rgb
                val darkMuted = palette.darkMutedSwatch?.rgb

                val primaryInt = vibrant ?: dominant ?: darkVibrant ?: defaultColors.primary.toArgb()
                val secondaryInt = darkVibrant ?: muted ?: dominant ?: defaultColors.secondary.toArgb()
                val deepInt = darkMuted ?: darkVibrant ?: defaultColors.deep.toArgb()

                DynamicBackdropColors(
                    primary = darkenForReadability(Color(primaryInt), maxLuminance = 0.38f),
                    secondary = darkenForReadability(Color(secondaryInt), maxLuminance = 0.24f),
                    deep = darkenForReadability(Color(deepInt), maxLuminance = 0.10f)
                )
            }.getOrDefault(defaultColors)
        }
    }

    private fun darkenForReadability(color: Color, maxLuminance: Float): Color {
        val currentLum = color.luminance()
        if (currentLum <= maxLuminance) return color
        val blendRatio = ((currentLum - maxLuminance) / currentLum.coerceAtLeast(0.01f))
            .coerceIn(0.25f, 0.78f)
        val blended = ColorUtils.blendARGB(color.toArgb(), android.graphics.Color.BLACK, blendRatio)
        return Color(blended)
    }
}
