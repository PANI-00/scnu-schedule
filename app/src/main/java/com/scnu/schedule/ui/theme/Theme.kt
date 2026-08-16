package com.scnu.schedule.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalAppPalette = staticCompositionLocalOf<AppPalette> {
    error("No AppPalette provided")
}

@Composable
fun AppTheme(type: AppThemeType, content: @Composable () -> Unit) {
    val palette = when (type) {
        AppThemeType.CLAUDE -> ClaudePalette
        AppThemeType.OPENCODE -> OpenCodePalette
        AppThemeType.RETRO -> RetroPalette
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
    // 系统栏跟随主题底色：状态栏=画布色（与页面背景连续，不再是系统灰），
    // 导航栏=卡片色（贴合底部导航栏）；图标深浅随主题（浅色主题深图标/深色主题浅图标）。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.canvas.toArgb()
                window.navigationBarColor = palette.surfaceCard.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !palette.isDark
                    isAppearanceLightNavigationBars = !palette.isDark
                }
            }
        }
    }
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = when (type) {
                AppThemeType.CLAUDE -> ClaudeTypography
                AppThemeType.OPENCODE -> OpenCodeTypography
                AppThemeType.RETRO -> RetroTypography
            },
            shapes = shapes,
            content = content,
        )
    }
}
