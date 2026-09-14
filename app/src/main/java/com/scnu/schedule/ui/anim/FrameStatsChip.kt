package com.scnu.schedule.ui.anim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 临时诊断用：显示实时帧率、卡顿帧数与最长帧耗时。
 *
 * 用途是定位课表页掉帧究竟是「静止也掉帧」（有持续重组/后台任务）
 * 还是「只有滑动/切周时掉帧」（组合与文本测量成本）。
 * 问题定位并修复后应删除本文件与调用处。
 */
@Composable
fun FrameStatsChip(modifier: Modifier = Modifier) {
    var label by remember { mutableStateOf("-- fps") }
    LaunchedEffect(Unit) {
        var frames = 0
        var jank = 0
        var worstMs = 0f
        var windowStartMs = 0L
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNs != 0L) {
                    val dtMs = (now - lastFrameNs) / 1_000_000f
                    if (dtMs > worstMs) worstMs = dtMs
                    // 超过 2 帧（约 33ms）算一次明显卡顿
                    if (dtMs > 33f) jank++
                }
                lastFrameNs = now
                frames++
                val nowMs = now / 1_000_000L
                if (windowStartMs == 0L) windowStartMs = nowMs
                val elapsed = nowMs - windowStartMs
                if (elapsed >= 1000L) {
                    val fps = frames * 1000f / elapsed
                    label = "%.0ffps 卡%d 峰%.0fms".format(fps, jank, worstMs)
                    frames = 0
                    jank = 0
                    worstMs = 0f
                    windowStartMs = nowMs
                }
            }
        }
    }
    Text(
        label,
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        color = Color(0xFF66FF99),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
    )
}
