package com.scnu.schedule.ui.anim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scnu.schedule.ui.theme.LocalAppPalette
import kotlin.math.floor
import kotlinx.coroutines.delay

/** 相位推进间隔：像素动画不需要满帧重绘，10fps 已足够顺滑，重绘量降到约 1/6。 */
private const val FRAME_INTERVAL_MS = 100L

/** 一圈时长：蒸汽走 6 格，约 1 秒跳动一格。 */
private const val CYCLE_MS = 6_000f

/**
 * 没课像素动画：冒烟咖啡杯（完全套用 word-formatter-ui PixelCup 的动画算法）。
 *
 * 15x14 像素网格，蒸汽 = steam(p, 4, 10, 5, 4)：一束四像素斜排（x=4/6/8/10），
 * 每像素相位错开 i/4，y 从 5 起上移 6 步，透明度 (1-ph)*0.6 渐隐，ph<0.7 时补 (x+1,y-1) 30% 尾迹。
 * 节奏与参考一致：6 秒一圈。
 *
 * 性能：用 delay 驱动相位而不是 rememberInfiniteTransition——后者会让 Canvas 在整个页面
 * 存活期间每帧重绘（60/90/120fps 常亮），是没课时今日页持续掉帧与耗电的来源。
 * 配色取主题 token：杯身 surfaceCard / 把手 primary / 描边 ink / 液面 courseMeta / 托盘 hairline / 蒸汽 mutedSoft。
 */
@Composable
fun SteamingCoffee(
    modifier: Modifier = Modifier,
    canvasSize: Dp = 120.dp,
) {
    val p = LocalAppPalette.current
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val step = FRAME_INTERVAL_MS / CYCLE_MS
        while (true) {
            delay(FRAME_INTERVAL_MS)
            phase = (phase + step) % 1f
        }
    }
    Canvas(modifier.size(canvasSize)) {
        val cell = size.minDimension / 15f
        val steam = p.mutedSoft

        // ---- 蒸汽（参考算法原样） ----
        val x1 = 4f
        val x2 = 10f
        val y0 = 5f
        val w = 4
        for (i in 0 until w) {
            val ph = (phase + i / w.toFloat()) % 1f
            val y = y0 - floor(ph * 6f)
            val op = ((1f - ph) * 0.6f).coerceAtLeast(0f)
            val x = x1 + i * ((x2 - x1) / (w - 1).toFloat())
            drawRect(
                color = steam.copy(alpha = op),
                topLeft = Offset(x * cell, y * cell),
                size = Size(cell, cell),
            )
            if (ph < 0.7f) {
                drawRect(
                    color = steam.copy(alpha = op * 0.3f),
                    topLeft = Offset((x + 1f) * cell, (y - 1f) * cell),
                    size = Size(cell, cell),
                )
            }
        }

        // ---- 杯子（结构同参考 SVG） ----
        // 托盘
        drawRoundRect(
            color = p.hairline,
            topLeft = Offset(2f * cell, 13f * cell),
            size = Size(10f * cell, 1f * cell),
            cornerRadius = CornerRadius(0.5f * cell, 0.5f * cell),
        )
        drawRoundRect(
            color = p.ink,
            topLeft = Offset(2f * cell, 13f * cell),
            size = Size(10f * cell, 1f * cell),
            cornerRadius = CornerRadius(0.5f * cell, 0.5f * cell),
            style = Stroke(width = 0.3f * cell),
        )
        // 杯把
        val handlePath = Path().apply {
            moveTo(10.5f * cell, 10f * cell)
            lineTo(10.5f * cell, 9f * cell)
            lineTo(11.5f * cell, 9f * cell)
            quadraticTo(12.5f * cell, 9f * cell, 12.5f * cell, 10f * cell)
            lineTo(12.5f * cell, 11f * cell)
            quadraticTo(12.5f * cell, 12f * cell, 11.5f * cell, 12f * cell)
            lineTo(10.5f * cell, 12f * cell)
            lineTo(10.5f * cell, 11f * cell)
        }
        drawPath(
            path = handlePath,
            color = p.primary,
            style = Stroke(width = 1f * cell, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // 杯身
        drawRoundRect(
            color = p.surfaceCard,
            topLeft = Offset(3f * cell, 7f * cell),
            size = Size(8f * cell, 6f * cell),
            cornerRadius = CornerRadius(cell, cell),
        )
        drawRoundRect(
            color = p.ink,
            topLeft = Offset(3f * cell, 7f * cell),
            size = Size(8f * cell, 6f * cell),
            cornerRadius = CornerRadius(cell, cell),
            style = Stroke(width = 0.3f * cell),
        )
        // 咖啡液面
        drawRoundRect(
            color = p.courseMeta,
            topLeft = Offset(4f * cell, 8f * cell),
            size = Size(6f * cell, 1f * cell),
            cornerRadius = CornerRadius(0.5f * cell, 0.5f * cell),
        )
    }
}
