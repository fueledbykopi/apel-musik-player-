package com.pulseplayer.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleMusicRed,
    onPrimary = Color.White,
    primaryContainer = IosDarkPrimaryContainer,
    onPrimaryContainer = IosDarkOnPrimaryContainer,
    secondary = AppleMusicPinkDark,
    onSecondary = Color.White,
    secondaryContainer = IosDarkSurfaceVariant,
    onSecondaryContainer = IosDarkOnSurface,
    tertiary = AppleMusicRed,
    onTertiary = Color.White,
    tertiaryContainer = IosDarkPrimaryContainer,
    onTertiaryContainer = IosDarkOnPrimaryContainer,
    background = IosDarkBackground,
    onBackground = IosDarkOnSurface,
    surface = IosDarkSurface,
    onSurface = IosDarkOnSurface,
    surfaceVariant = IosDarkSurfaceElevated,
    onSurfaceVariant = IosDarkOnSurfaceVariant,
    surfaceContainerHigh = IosDarkSurfaceElevated,
    surfaceContainerHighest = IosDarkSurfaceVariant,
    outline = IosDarkOutline,
    outlineVariant = IosDarkOutline.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = AppleMusicRed,
    onPrimary = Color.White,
    primaryContainer = IosLightPrimaryContainer,
    onPrimaryContainer = IosLightOnPrimaryContainer,
    secondary = AppleMusicRed,
    onSecondary = Color.White,
    secondaryContainer = IosLightSurfaceElevated,
    onSecondaryContainer = IosLightOnSurface,
    tertiary = AppleMusicRed,
    onTertiary = Color.White,
    tertiaryContainer = IosLightPrimaryContainer,
    onTertiaryContainer = IosLightOnPrimaryContainer,
    background = IosLightBackground,
    onBackground = IosLightOnSurface,
    surface = IosLightSurface,
    onSurface = IosLightOnSurface,
    surfaceVariant = IosLightSurfaceElevated,
    onSurfaceVariant = IosLightOnSurfaceVariant,
    surfaceContainerHigh = IosLightSurfaceElevated,
    surfaceContainerHighest = IosLightSurfaceVariant,
    outline = IosLightOutline,
    outlineVariant = IosLightOutline.copy(alpha = 0.6f)
)

@Composable
fun PulsePlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        content = content
    )
}
