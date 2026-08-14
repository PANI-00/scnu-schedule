package com.scnu.schedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalAppPalette = staticCompositionLocalOf<AppPalette> {
    error("No AppPalette provided")
}

@Composable
fun AppTheme(type: AppThemeType, content: @Composable () -> Unit) {
    val palette = when (type) {
        AppThemeType.CLAUDE -> ClaudePalette
        AppThemeType.OPENCODE -> OpenCodePalette
    }
    val scheme = if (palette.isDark) darkColorScheme(
        primary = palette.primary, onPrimary = palette.onPrimary,
        background = palette.canvas, surface = palette.canvas,
        onBackground = palette.ink, onSurface = palette.ink,
        surfaceVariant = palette.surfaceCard,
        surfaceContainer = palette.surfaceCard,
        surfaceContainerHigh = palette.surfaceCard,
        surfaceContainerHighest = palette.surfaceCard,
        secondaryContainer = palette.primary,
        onSecondaryContainer = palette.onPrimary,
        onSurfaceVariant = palette.muted,
        outline = palette.hairline,
        error = palette.error, onError = Color.White,
    ) else lightColorScheme(
        primary = palette.primary, onPrimary = palette.onPrimary,
        background = palette.canvas, surface = palette.canvas,
        onBackground = palette.ink, onSurface = palette.ink,
        surfaceVariant = palette.surfaceCard,
        surfaceContainer = palette.surfaceCard,
        surfaceContainerHigh = palette.surfaceCard,
        surfaceContainerHighest = palette.surfaceCard,
        secondaryContainer = palette.primary,
        onSecondaryContainer = palette.onPrimary,
        onSurfaceVariant = palette.muted,
        outline = palette.hairline,
        error = palette.error, onError = Color.White,
    )
    val shapes = Shapes(
        small = RoundedCornerShape(if (palette.isDark) 2.dp else 8.dp),
        medium = RoundedCornerShape(if (palette.isDark) 4.dp else 12.dp),
        large = RoundedCornerShape(if (palette.isDark) 4.dp else 16.dp),
    )
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = if (type == AppThemeType.CLAUDE) ClaudeTypography else OpenCodeTypography,
            shapes = shapes,
            content = content,
        )
    }
}
