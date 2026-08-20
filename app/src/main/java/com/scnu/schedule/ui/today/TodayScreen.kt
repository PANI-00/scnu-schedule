package com.scnu.schedule.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.ui.anim.SteamingCoffee
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val dateFmt = DateTimeFormatter.ofPattern("M月d日 EEEE")

@Composable
fun TodayScreen(vm: TodayViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val p = LocalAppPalette.current
    var nowMinute by remember { mutableStateOf(LocalTime.now().toSecondOfDay() / 60) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            nowMinute = now.toSecondOfDay() / 60
            delay(((60 - now.second).coerceAtLeast(1)) * 1000L)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(state.today.format(dateFmt), fontSize = 18.sp, color = p.ink, fontWeight = FontWeight.Bold)
        Text("第 ${state.currentWeek} 周", fontSize = 12.sp, color = p.muted)

        // 今日时间线（在课程卡片内直接高亮「上课中 / 下一节」，不再有重复的独立 NEXT 卡片）
        val periods = state.timetable?.periods ?: emptyList()
        val periodByIndex = periods.associateBy { it.periodIndex }
        // 判断每门课的状态标签
        fun inProgress(course: Course): Boolean {
            val start = periodByIndex[course.startPeriod] ?: return false
            val end = periodByIndex[course.endPeriod] ?: start
            return nowMinute >= start.startMinute && nowMinute < end.endMinute
        }
        // 只在今天课程列表内选下一节，不会跨到明天
        val next = state.todayCourses
            .filter { course ->
                val start = periodByIndex[course.startPeriod]
                start != null && start.startMinute > nowMinute
            }
            .minByOrNull { periodByIndex[it.startPeriod]?.startMinute ?: Int.MAX_VALUE }

        state.todayCourses.forEach { course ->
            val coursePeriods = periods.filter { it.periodIndex in course.startPeriod..course.endPeriod }
            val isNext = next != null && course === next
            val isNow = inProgress(course)
            // 状态文字统一使用主题主色
            //  - 上课中 IN CLASS：primary（主题主色），视觉最突出
            //  - 下一节 NEXT：warning（各主题均与 primary 有明显区别）
            val statusColor = p.primary
            // NEXT 仅通过状态标签显示，不额外改变卡片颜色
            // 为了在 OpenCode（primary 与 success 同为绿色）等主题下仍能区分，NEXT 改用「描边 + 左竖条」样式，上课中用「实底」样式
            val bg = when {
                isNow -> p.primary.copy(alpha = if (p.isDark) 0.30f else 0.16f)
                isNext -> p.surfaceSoft
                else -> p.surfaceSoft
            }
            val borderColor = when {
                isNow -> p.primary
                isNext -> p.hairline
                else -> p.hairline
            }
            val borderWidth = when { isNow -> 2.dp; isNext -> 0.dp; else -> 0.dp }
            val leftBarColor = when {
                isNow -> p.primary
                isNext -> Color.Transparent
                else -> Color.Transparent
            }
            if (isNow || isNext) {
                // 高亮课程使用原 NEXT 卡片的大卡片布局；课程只在列表中出现一次
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        .background(p.surfaceCard, RoundedCornerShape(12.dp))
                        .then(Modifier.border(1.5.dp, statusColor, RoundedCornerShape(12.dp)))
                        .padding(14.dp),
                ) {
                    Text(
                        if (isNow) "● IN CLASS" else "● NEXT",
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        course.name,
                        fontSize = 20.sp,
                        color = p.ink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "${course.location.ifBlank { "未填写地点" }} · 第 ${course.startPeriod}-${course.endPeriod} 节",
                        fontSize = 12.sp,
                        color = p.muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "${time(coursePeriods.firstOrNull()?.startMinute ?: 0)} 开始上课",
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .background(p.surfaceSoft, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Text(timeRange(coursePeriods), fontSize = 11.sp, color = p.mutedSoft)
                    Row(Modifier.padding(top = 2.dp)) {
                        Text(course.name, fontSize = 15.sp, color = p.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(course.location, fontSize = 12.sp, color = p.muted)
                    }
                }
            }
        }
        if (state.todayCourses.isEmpty()) {
            // 没课空态：冒烟咖啡杯像素动画（连续相位，0.5x 速度约 22 秒一圈；有课时本分支不组合，不影响课程展示）
            Column(
                Modifier.fillMaxWidth().padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SteamingCoffee(canvasSize = 120.dp)
                Text("今天没课，喝杯咖啡吧", fontSize = 13.sp, color = p.muted, modifier = Modifier.padding(top = 14.dp))
            }
        }
    }
}

private fun time(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
private fun timeRange(periods: List<Period>): String =
    if (periods.isEmpty()) "" else "${time(periods.first().startMinute)}–${time(periods.last().endMinute)}"

/** 课程状态小圆标（如 IN CLASS / NEXT） */
@Composable
private fun StatusBadge(text: String, bg: Color, fg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}
