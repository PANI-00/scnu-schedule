package com.scnu.schedule.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun TimeTableScreen(vm: TimeTableViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsState()
    val timetables = uiState.first
    val activeId = uiState.second
    val p = LocalAppPalette.current
    var editingTt by remember { mutableStateOf<TimeTable?>(null) }
    val semester by vm.semesterFlow.collectAsState(initial = Semester(startDate = LocalDate.now().with(DayOfWeek.MONDAY)))
    var showSemesterEdit by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("作息时间", fontSize = 18.sp, color = p.ink)

        timetables.forEach { tt ->
            Row(Modifier.fillMaxWidth().clickable { vm.activate(tt.id) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(tt.name, fontSize = 15.sp, color = p.ink, modifier = Modifier.weight(1f))
                Text(if (tt.id == activeId) "● 使用中" else "点击启用", fontSize = 11.sp, color = if (tt.id == activeId) p.primary else p.muted)
                Text("编辑", fontSize = 12.sp, color = p.primary, modifier = Modifier.padding(start = 12.dp).clickable { editingTt = tt })
            }
        }
        Text("+ 新建作息", fontSize = 14.sp, color = p.primary, modifier = Modifier.padding(top = 8.dp).clickable { editingTt = TimeTable(name = "自定义") })

        // ---- 学期设置 ----
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("学期设置", fontSize = 14.sp, color = p.muted, modifier = Modifier.weight(1f))
            Text(if (showSemesterEdit) "收起" else "编辑", fontSize = 12.sp, color = p.primary, modifier = Modifier.clickable { showSemesterEdit = !showSemesterEdit })
        }
        if (!showSemesterEdit) {
            Text(semester.name, fontSize = 15.sp, color = p.ink)
            Text("开学 ${semester.startDate} · 共 ${semester.totalWeeks} 周", fontSize = 12.sp, color = p.muted)
        } else {
            var name by remember { mutableStateOf(semester.name) }
            var start by remember { mutableStateOf(semester.startDate.toString()) }
            var weeks by remember { mutableStateOf(semester.totalWeeks.toString()) }
            var err by remember { mutableStateOf("") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("学期名称") }, singleLine = true)
                OutlinedTextField(start, { start = it }, label = { Text("开学日期 YYYY-MM-DD") }, singleLine = true)
                OutlinedTextField(weeks, { weeks = it }, label = { Text("总周数") }, singleLine = true)
                if (err.isNotEmpty()) Text(err, fontSize = 11.sp, color = p.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({
                        val startDate = runCatching { LocalDate.parse(start.trim()) }.getOrNull()
                        val weeksInt = weeks.trim().toIntOrNull()
                        if (startDate == null || weeksInt == null || weeksInt <= 0) {
                            err = "开学日期需 YYYY-MM-DD，周数为正整数"
                        } else {
                            vm.updateSemester(Semester(id = 0, name = name.ifBlank { "学期" }, startDate = startDate, totalWeeks = weeksInt))
                            showSemesterEdit = false
                        }
                    }) { Text("保存") }
                    TextButton({ showSemesterEdit = false }) { Text("取消") }
                }
            }
        }

        // ---- 作息编辑面板 ----
        editingTt?.let { tt ->
            var name by remember(tt.id) { mutableStateOf(tt.name) }
            var periods by remember(tt.id) { mutableStateOf(tt.periods) }
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                periods.sortedBy { it.periodIndex }.forEach { per ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("第${per.periodIndex}节", fontSize = 13.sp, color = p.ink, modifier = Modifier.weight(1f))
                        Text("${hhmm(per.startMinute)}–${hhmm(per.endMinute)}", fontSize = 13.sp, color = p.muted)
                        Text("删", color = p.error, modifier = Modifier.padding(start = 10.dp).clickable { periods = periods.filter { it.periodIndex != per.periodIndex } })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({
                        val next = (periods.maxOfOrNull { it.periodIndex } ?: 0) + 1
                        periods = periods + Period(periodIndex = next, startMinute = 510, endMinute = 550)
                    }) { Text("+ 加一节") }
                    TextButton({ editingTt = null }) { Text("取消") }
                    TextButton({
                        if (tt.id == 0L) vm.addTimeTable(name, periods) else vm.savePeriods(tt, periods.map { it.copy(id = 0) })
                        editingTt = null
                    }) { Text("保存") }
                }
            }
        }
    }
}

private fun hhmm(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
