package com.scnu.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.ui.theme.CourseBlockBarStyle
import com.scnu.schedule.ui.theme.CourseBlockBorderEdges
import com.scnu.schedule.ui.theme.LocalAppPalette

/**
 * 课表课程块：卡片 + 可选左色条。行数受格子高度约束，不撑出正常尺寸：
 *  名称（多节最多 2 行、单节限 1 行）
 *  + 元数据行（教师为主，单/双周标签置于最右侧）
 *  + 教室行（放大 + 两主题不同的 courseMeta 高对比色）
 * 文字全部左对齐；文字区与课程框左右边界等距（左色条 + 右侧对称留白）。
 * 单/双周标签：无气泡，纯文字「单周」/「双周」，颜色用主题 ink，置于教师行最右侧。
 * 卡片外观完全由 AppPalette 的 courseBlock* token 决定（背景/描边/圆角/竖条/硬阴影）：
 *  - 左色条：drawBehind 直接绘制（fillMaxHeight/matchParentSize 在无界高度下都会失效，
 *    只有绘制不依赖测量）；宽度为 0 的主题（Claude/TUI）不画竖条，仅复古报刊有
 *  - 硬阴影：drawBehind 越界绘制（右下 offset），课程块整体在网格顶层渲染，
 *    因此阴影在空格格背景之上、米纸实底之下，不会被遮挡
 */
@Composable
fun CourseBlock(course: Course, color: Color, rowHeight: Int, onClick: () -> Unit) {
    val p = LocalAppPalette.current
    val span = course.endPeriod - course.startPeriod + 1
    val radius = p.courseBlockRadius
    val cardBg = p.courseBlockSolidBg ?: color.copy(alpha = p.courseBlockBgAlpha)
    val borderColor = p.courseBlockSolidBorder ?: run {
        val base = color.copy(alpha = p.courseBlockBorderAlpha)
        // 奶油珊瑚：描边用课程色向黑加深（深色包边）
        if (p.courseBlockBorderDeepen > 0f) lerp(base, Color.Black, p.courseBlockBorderDeepen) else base
    }
    val nameColor = p.courseBlockFg ?: p.ink
    val metaColor = p.courseBlockFgMuted ?: p.muted
    val nameSize = when {
        course.name.length > 12 -> 9.sp
        course.name.length > 9 -> 10.sp
        else -> 11.sp
    }
    // 左侧标记：STRIP = 彩色竖条（画在 Box 层全高）；NONE/DOT = 不画竖条
    val isDot = p.courseBlockBarStyle == CourseBlockBarStyle.DOT
    val stripWidth = when (p.courseBlockBarStyle) {
        CourseBlockBarStyle.STRIP -> p.courseBlockBarWidth
        else -> 0.dp
    }
    val dotSize = 8.dp
    val dotGap = 5.dp
    // 第二/三行相对第一行（课程名）的左缩进：DOT 时 = 圆点 + 间距，保证三行文字左对齐
    val textIndent = if (isDot) dotSize + dotGap else 0.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp)
            .height((rowHeight * span).dp)
            .drawBehind {
                // 硬阴影：整体向右下偏移，露出 L 形边（size 取全尺寸，向右下越界绘制；
                // 若减去偏移则会被背景完全盖住）
                val shadow = p.courseBlockHardShadow
                if (shadow != null) {
                    val off = p.courseBlockHardShadowOffset.toPx()
                    drawRoundRect(
                        color = shadow,
                        topLeft = Offset(off, off),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
                    )
                }
            }
            .background(cardBg, RoundedCornerShape(radius))
            .drawBehind {
                // STRIP 竖条：圆角绘制——上下各让出圆角半径，端部圆角与块一致，
                // 不破坏课程框圆角观感（奶油珊瑚 8dp 大圆角尤其明显）；颜色可按 token 加深
                val barWidth = stripWidth.toPx()
                if (barWidth > 0f) {
                    val barColor = if (p.courseBlockBarDeepen > 0f) lerp(color, Color.Black, p.courseBlockBarDeepen) else color
                    val r = radius.toPx()
                    drawRoundRect(
                        color = barColor.copy(alpha = p.courseBlockBarAlpha),
                        topLeft = Offset(0f, r),
                        size = Size(barWidth, size.height - 2f * r),
                        cornerRadius = CornerRadius(r, r),
                    )
                }
            }
            .then(
                if (p.courseBlockBorderEdges == CourseBlockBorderEdges.ALL) {
                    // 四边描边（Claude / 复古）
                    Modifier.border(p.courseBlockBorderWidth, borderColor, RoundedCornerShape(radius))
                } else {
                    // 只描上/下/右三边（TUI：左边留给彩色竖条，不描边）
                    Modifier.drawBehind {
                        val bw = p.courseBlockBorderWidth.toPx()
                        drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), bw)                    // 上
                        drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), bw)  // 下
                        drawLine(borderColor, Offset(size.width, 0f), Offset(size.width, size.height), bw)   // 右
                    }
                }
            )
            .clickable(onClick = onClick)
            .padding(start = 1.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = stripWidth + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f).padding(
                    // 右侧留白：主题可指定更窄的值（复古报刊为了文字行更宽），否则默认与左侧等距
                    end = p.courseBlockContentEndPadding ?: (p.courseBlockBarWidth + 1.dp),
                ),
            ) {
                // 名称行：DOT 时左侧为课程色圆点（课程框左上角），STRIP 时无额外元素
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDot) {
                        Box(Modifier.size(dotSize).background(color, CircleShape))
                        Spacer(Modifier.width(dotGap))
                    }
                    Text(
                        course.name,
                        fontSize = nameSize,
                        fontWeight = FontWeight.Bold,
                        color = nameColor,
                        maxLines = if (span >= 2) 2 else 1,
                        modifier = Modifier.weight(1f),
                    )
                }
                // 元数据行：教师为主（无教师则教室顶上），单/双周标签置于最右侧；与名称行左对齐
                Row(
                    Modifier.fillMaxWidth().padding(start = textIndent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        course.teacher.ifBlank { course.location },
                        fontSize = 9.sp,
                        color = metaColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    weekBadge(course)?.let {
                        Text(
                            it,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = nameColor,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
                // 教室行：字号放大 + 高对比（两主题取不同色 courseMeta）；仅当与元数据行不同时显示，避免重复
                if (course.teacher.isNotBlank() && course.location.isNotBlank()) {
                    Text(
                        course.location,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.courseBlockFgMuted ?: p.courseMeta,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(start = textIndent),
                    )
                }
            }
        }
    }
}

/** 单/双周标签文字（无气泡纯文字）；全周次课程不显示。 */
private fun weekBadge(course: Course): String? = when (course.weekPattern.kind) {
    WeekKind.ODD -> "单周"
    WeekKind.EVEN -> "双周"
    else -> null
}
