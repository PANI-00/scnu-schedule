package com.scnu.schedule.ui.today

import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("M月d日 EEEE")

@Composable
fun TodayScreen(vm: TodayViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val p = LocalAppPalette.current
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(state.today.format(dateFmt), fontSize = 18.sp, color = p.ink, fontWeight = FontWeight.Bold)
        Text("第 ${state.currentWeek} 周", fontSize = 12.sp, color = p.muted)

        // 下一节课卡片
        val next = vm.nextCourse(state.todayCourses, state.timetable?.periods ?: emptyList())
        if (next != null) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)
                .background(p.surfaceCard, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text("● NEXT", fontSize = 11.sp, color = p.primary)
                Text(next.name, fontSize = 20.sp, color = p.ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                val start = state.timetable?.periods?.firstOrNull { it.periodIndex == next.startPeriod }
                Text("${next.location} · 第 ${next.startPeriod}-${next.endPeriod} 节 ${start?.let { time(it.startMinute) } ?: ""}",
                    fontSize = 12.sp, color = p.muted, modifier = Modifier.padding(top = 2.dp))
            }
        }

        // 今日时间线
        state.todayCourses.forEach { course ->
            val periods = state.timetable?.periods?.filter { it.periodIndex in course.startPeriod..course.endPeriod } ?: emptyList()
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                .background(p.surfaceSoft, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Text(timeRange(periods), fontSize = 11.sp, color = p.mutedSoft)
                Row(Modifier.padding(top = 2.dp)) {
                    Text(course.name, fontSize = 15.sp, color = p.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(course.location, fontSize = 12.sp, color = p.muted)
                }
            }
        }
        if (state.todayCourses.isEmpty()) {
            Text("今天没有课", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

private fun time(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
private fun timeRange(periods: List<Period>): String =
    if (periods.isEmpty()) "" else "${time(periods.first().startMinute)}–${time(periods.last().endMinute)}"
