package com.scnu.schedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppThemeType { CLAUDE, OPENCODE, RETRO }

/** 课程块描边方式：ALL = 四边描边（border 修饰符）；TOP_BOTTOM_RIGHT = 只描上/下/右三边（左边留给竖条，TUI 用） */
enum class CourseBlockBorderEdges { ALL, TOP_BOTTOM_RIGHT }

/** 课程块左侧标记类型：NONE = 无标记（Claude 纯色块）；STRIP = 彩色竖条（TUI/复古）；DOT = 左上角圆点 */
enum class CourseBlockBarStyle { NONE, STRIP, DOT }

/**
 * 主题令牌：UI 组件只从这取色，不直接写 hex。
 *
 * 课程块样式 token（CourseBlock 消费）：
 *  - courseBlockRadius：块圆角
 *  - courseBlockBarWidth：左侧彩色竖条宽度
 *  - courseBlockBgAlpha / courseBlockSolidBg：背景 = 课程色半透明，或指定实色（复古米纸）
 *  - courseBlockBorderWidth / courseBlockBorderAlpha / courseBlockSolidBorder：描边 = 课程色半透明，或指定实色（复古墨黑）
 *  - courseBlockHardShadow / courseBlockHardShadowOffset：非 null 时画右下 offset 硬阴影（复古报刊感）
 */
data class AppPalette(
    val canvas: Color, val surfaceSoft: Color, val surfaceCard: Color,
    val primary: Color, val onPrimary: Color,
    val ink: Color, val muted: Color, val mutedSoft: Color, val hairline: Color,
    val todayLine: Color, val success: Color, val warning: Color, val error: Color,
    val courseMeta: Color,      // 课程块教室等次要文字色：两风格取不同值，保证鲜明对比
    val coursePalette: List<Color>,   // 课程块彩色左描边色序
    val isDark: Boolean,
    val courseBlockRadius: Dp = 8.dp,
    val courseBlockBarWidth: Dp = 0.dp,   // STRIP 竖条宽（Claude 4dp / TUI 2dp / 复古 6dp）
    val courseBlockBarStyle: CourseBlockBarStyle = CourseBlockBarStyle.STRIP,
    /** 竖条加深比例：>0 时竖条用课程色向黑混合该比例（奶油珊瑚深色竖条），0 = 原课程色 */
    val courseBlockBarDeepen: Float = 0f,
    /** 竖条不透明度：1 = 实色；<1 为半透明（奶油珊瑚 0.4 若隐若现） */
    val courseBlockBarAlpha: Float = 1f,
    val courseBlockBgAlpha: Float = 0.42f,
    val courseBlockSolidBg: Color? = null,
    val courseBlockBorderWidth: Dp = 1.dp,
    val courseBlockBorderAlpha: Float = 0.5f,
    val courseBlockSolidBorder: Color? = null,
    /** 描边加深比例：>0 时把课程色向黑混合该比例作为描边色（奶油珊瑚深色包边用），0 = 原色规则 */
    val courseBlockBorderDeepen: Float = 0f,
    val courseBlockBorderEdges: CourseBlockBorderEdges = CourseBlockBorderEdges.ALL,
    val courseBlockHardShadow: Color? = null,
    val courseBlockHardShadowOffset: Dp = 3.dp,
    /** 课程块内主文字色（课程名/单双周标签）；null = 主题 ink（深底白字主题可覆盖为白色） */
    val courseBlockFg: Color? = null,
    /** 课程块内次要文字色（教师/教室）；null = muted/courseMeta */
    val courseBlockFgMuted: Color? = null,
    /** 文字区右侧留白；null = 默认与左侧等距（barWidth + 1）。复古报刊设为更窄，让一行放下更多文字 */
    val courseBlockContentEndPadding: Dp? = null,
    val periodBadgeRadius: Dp = 8.dp,   // 左侧时间列节次气泡圆角：随主题风格（Claude 圆角 / TUI 微圆 / 复古直角）
    val periodBadgeBg: Color? = null,   // 气泡底色；null = 主题主色（午夜机房用灰色）
    val periodBadgeFg: Color? = null,   // 气泡文字色；null = 主题 onPrimary
)

/** Claude 奶油珊瑚（默认，design-library/claude DESIGN.md） */
/**
 * Claude 奶油珊瑚（默认，design-library/claude DESIGN.md）：
 * 课程块 = 浅色纯色实底 + 黑字 + 左侧深色圆角竖条（课程色向黑加深 40%，上下让出圆角）；
 * 无外框描边；色板用柔和浅色
 */
val ClaudePalette = AppPalette(
    canvas = Color(0xFFFAF9F5), surfaceSoft = Color(0xFFF5F0E8), surfaceCard = Color(0xFFEFE9DE),
    primary = Color(0xFFCC785C), onPrimary = Color(0xFFFFFFFF),
    ink = Color(0xFF141413), muted = Color(0xFF6C6A64), mutedSoft = Color(0xFF8E8B82), hairline = Color(0xFFE6DFD8),
    todayLine = Color(0xFFCC785C), success = Color(0xFF5DB872), warning = Color(0xFFE8A55A), error = Color(0xFFC64545),
    courseMeta = Color(0xFF5C5348),
    coursePalette = listOf(
        Color(0xFFE8B09A), Color(0xFFA9D8CF), Color(0xFFF0C78F), Color(0xFFC9C7C2),
        Color(0xFFCBADD0), Color(0xFFAFC9DD), Color(0xFFD8B59E), Color(0xFFBFD0A8),
    ),
    isDark = false,
    courseBlockBarStyle = CourseBlockBarStyle.STRIP,   // 竖条占位（保持文字缩进）
    courseBlockBarWidth = 4.dp,
    courseBlockBarAlpha = 0f,                          // 竖条完全透明（仅占位，不显示颜色）
    courseBlockBgAlpha = 1f,                           // 浅色纯色实底（不透明）
    courseBlockBorderWidth = 0.dp,                     // 无外框（去掉深色包边）
    // courseBlockFg / FgMuted 保持 null → 黑字（ink / muted）
)

/**
 * OpenCode TUI（design-library/opencode.ai DESIGN.md）：
 * 课程框 = 灰黑实底 + 浅灰上/下/右三边描边 + 3dp 彩色左竖条（左边留给竖条，不描边）
 */
val OpenCodePalette = AppPalette(
    canvas = Color(0xFF201D1D), surfaceSoft = Color(0xFF1A1A19), surfaceCard = Color(0xFF302C2C),
    primary = Color(0xFF30D158), onPrimary = Color(0xFF201D1D),
    ink = Color(0xFFFDFCFC), muted = Color(0xFF9A9898), mutedSoft = Color(0xFF646262), hairline = Color(0xFF3A3A38),
    todayLine = Color(0xFF30D158), success = Color(0xFF30D158), warning = Color(0xFFFF9F0A), error = Color(0xFFFF3B30),
    courseMeta = Color(0xFFB9B7B2),
    coursePalette = listOf(
        Color(0xFF30D158), Color(0xFF007AFF), Color(0xFFFF9F0A), Color(0xFFFF3B30),
        Color(0xFFAF52DE), Color(0xFF5AC8FA), Color(0xFFFF6B5B), Color(0xFF8E8E93),
    ),
    isDark = true,
    courseBlockRadius = 2.dp,
    courseBlockBarWidth = 2.dp,               // TUI 细竖条
    courseBlockBgAlpha = 1f,
    courseBlockSolidBg = Color(0xFF2A2826),           // 灰黑实底
    courseBlockBorderWidth = 1.dp,
    courseBlockBorderAlpha = 1f,
    courseBlockSolidBorder = Color(0xFF8E8C88),       // 浅灰三边
    courseBlockBorderEdges = CourseBlockBorderEdges.TOP_BOTTOM_RIGHT,
    periodBadgeRadius = 2.dp,                 // TUI 微圆角，与课程框一致
    periodBadgeBg = Color(0xFF4E4C49),        // 气泡深灰底
    periodBadgeFg = Color.White,              // 气泡白字
)

/**
 * 复古报刊：米黄旧报纸底 · 粗墨黑描边 + 硬阴影 · 6dp 彩色竖条 · 全衬线（风格库 04）。
 * 与 Claude（奶油白/珊瑚）刻意拉开色系：米黄纸面 + 深锈红 + 墨黑描边，一眼可辨。
 */
val RetroPalette = AppPalette(
    canvas = Color(0xFFF0E8D6), surfaceSoft = Color(0xFFE8DEC6), surfaceCard = Color(0xFFFBF6EA),
    primary = Color(0xFFA8432F), onPrimary = Color(0xFFFFFFFF),
    ink = Color(0xFF262019), muted = Color(0xFF83755C), mutedSoft = Color(0xFFB0A488), hairline = Color(0xFFD4C7A8),
    todayLine = Color(0xFFA8432F), success = Color(0xFF4E6B33), warning = Color(0xFFC89A2C), error = Color(0xFFA63A2A),
    courseMeta = Color(0xFF5C513E),
    coursePalette = listOf(
        Color(0xFFB8432F), Color(0xFF2C6459), Color(0xFFC8942B), Color(0xFF34517E),
        Color(0xFF7D4462), Color(0xFF4F6E33), Color(0xFF9C5529), Color(0xFF51457A),
    ),
    isDark = false,
    courseBlockRadius = 2.dp,
    courseBlockBarWidth = 6.dp,
    courseBlockBgAlpha = 1f,
    courseBlockSolidBg = Color(0xFFFBF5E8),
    courseBlockBorderWidth = 2.dp,
    courseBlockBorderAlpha = 1f,
    courseBlockSolidBorder = Color(0xFF262019),
    courseBlockHardShadow = Color(0x99262019),   // 半透明墨黑（60%），复古报刊硬阴影
    courseBlockHardShadowOffset = 3.dp,
    courseBlockContentEndPadding = 2.dp,          // 文字区右侧留白收窄，一行能放下更多课程名
    periodBadgeRadius = 0.dp,                 // 复古报刊：直角气泡（报刊贴纸方角感）
)

/** 按主题取色板（App 与小组件共用） */
fun paletteFor(type: AppThemeType): AppPalette = when (type) {
    AppThemeType.CLAUDE -> ClaudePalette
    AppThemeType.OPENCODE -> OpenCodePalette
    AppThemeType.RETRO -> RetroPalette
}
