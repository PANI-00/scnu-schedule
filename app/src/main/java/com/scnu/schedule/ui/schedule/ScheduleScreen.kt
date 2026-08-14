package com.scnu.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
private val weekdays = listOf("一", "二", "三", "四", "五")

@Composable
fun ScheduleScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val p = LocalAppPalette.current
    val timetable = state.timetable
    val rowHeight = 48

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Text("第 ${state.currentWeek} 周 · ${state.semester?.name ?: ""}",
            fontSize = 14.sp, color = p.ink, modifier = Modifier.padding(vertical = 10.dp))

        // 日期条：周一~周五（周几 + 月日）
        val weekDates = state.semester?.let { WeekCalculator.weekDates(it.startDate, 0) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekdays.forEachIndexed { day, label ->
                val date = weekDates?.get(day)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 12.sp, color = p.muted)
                    Text(date?.format(dateFormatter) ?: "", fontSize = 9.sp, color = p.mutedSoft)
                }
            }
        }

        // 周课表网格：时间槽 + 5 列
        Row(Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp).verticalScroll(rememberScrollState())) {
            Column(Modifier.width(30.dp)) {
                timetable?.periods?.forEach { period ->
                    Text(period.startLabel(), fontSize = 8.sp, color = p.mutedSoft,
                        modifier = Modifier.height(rowHeight.dp))
                }
            }
            weekdays.forEachIndexed { index, _ ->
                val day = index + 1
                Column(Modifier.weight(1f)) {
                    val dayCourses = WeekCalculator.coursesForWeek(state.courses.filter { it.dayOfWeek == day }, state.currentWeek)
                    Box {
                        Column {
                            timetable?.periods?.forEach { _ ->
                                Box(Modifier.height(rowHeight.dp).fillMaxWidth().padding(vertical = 1.dp)
                                    .background(p.surfaceSoft, RoundedCornerShape(if (p.isDark) 2.dp else 8.dp)))
                            }
                        }
                        dayCourses.forEach { course ->
                            val color = p.coursePalette[course.colorIndex % p.coursePalette.size]
                            Box(Modifier.offset(y = ((course.startPeriod - 1) * rowHeight).dp).fillMaxWidth()) {
                                CourseBlock(course, color, rowHeight) { /* Task 13 编辑 */ }
                            }
                        }
                    }
                }
            }
        }
    }
}
