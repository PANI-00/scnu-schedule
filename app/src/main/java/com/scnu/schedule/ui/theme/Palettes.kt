package com.scnu.schedule.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemeType { CLAUDE, OPENCODE }

/** 主题令牌：UI 组件只从这取色，不直接写 hex */
data class AppPalette(
    val canvas: Color, val surfaceSoft: Color, val surfaceCard: Color,
    val primary: Color, val onPrimary: Color,
    val ink: Color, val muted: Color, val mutedSoft: Color, val hairline: Color,
    val todayLine: Color, val success: Color, val warning: Color, val error: Color,
    val coursePalette: List<Color>,   // 课程块彩色左描边色序
    val isDark: Boolean,
)

/** Claude 奶油珊瑚（默认，design-library/claude DESIGN.md） */
val ClaudePalette = AppPalette(
    canvas = Color(0xFFFAF9F5), surfaceSoft = Color(0xFFF5F0E8), surfaceCard = Color(0xFFEFE9DE),
    primary = Color(0xFFCC785C), onPrimary = Color(0xFFFFFFFF),
    ink = Color(0xFF141413), muted = Color(0xFF6C6A64), mutedSoft = Color(0xFF8E8B82), hairline = Color(0xFFE6DFD8),
    todayLine = Color(0xFFCC785C), success = Color(0xFF5DB872), warning = Color(0xFFE8A55A), error = Color(0xFFC64545),
    coursePalette = listOf(
        Color(0xFFCC785C), Color(0xFF5DB8A6), Color(0xFFE8A55A), Color(0xFF141413),
        Color(0xFF9A6FA0), Color(0xFF6B8FAF), Color(0xFFB07A5A), Color(0xFF7F9E5B),
    ),
    isDark = false,
)

/** OpenCode TUI（design-library/opencode.ai DESIGN.md） */
val OpenCodePalette = AppPalette(
    canvas = Color(0xFF201D1D), surfaceSoft = Color(0xFF1A1A19), surfaceCard = Color(0xFF302C2C),
    primary = Color(0xFF30D158), onPrimary = Color(0xFF201D1D),
    ink = Color(0xFFFDFCFC), muted = Color(0xFF9A9898), mutedSoft = Color(0xFF646262), hairline = Color(0xFF3A3A38),
    todayLine = Color(0xFF30D158), success = Color(0xFF30D158), warning = Color(0xFFFF9F0A), error = Color(0xFFFF3B30),
    coursePalette = listOf(
        Color(0xFF30D158), Color(0xFF007AFF), Color(0xFFFF9F0A), Color(0xFFFF3B30),
        Color(0xFFAF52DE), Color(0xFF5AC8FA), Color(0xFFFF6B5B), Color(0xFF8E8E93),
    ),
    isDark = true,
)
