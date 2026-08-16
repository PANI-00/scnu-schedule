package com.scnu.schedule.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
    var deleteTarget by remember { mutableStateOf<TimeTable?>(null) }
    val semester by vm.semesterFlow.collectAsState(initial = Semester(startDate = LocalDate.now().with(DayOfWeek.MONDAY)))
    var showSemesterEdit by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("作息时间", fontSize = 18.sp, color = p.ink)

        // 兜底：DataStore 未设置激活作息时，默认第一张为使用中（双保险，避免状态丢失）
        val effectiveActiveId = activeId ?: timetables.firstOrNull()?.id
        timetables.forEach { tt ->
            val selected = tt.id == effectiveActiveId
            // 框选效果：每行一个圆角框，启用中的作息用主题主色描边 + 浅色底，
            // 圆角随主题（Claude 12dp 奶油圆角 / TUI 2dp 终端方角）；背景/描边颜色丝滑过渡
            val boxRadius = if (p.isDark) 2.dp else 12.dp
            val bgColor by animateColorAsState(
                targetValue = if (selected) p.primary.copy(alpha = if (p.isDark) 0.24f else 0.10f)
                else p.surfaceSoft,
                animationSpec = tween(220),
                label = "ttRowBg",
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) p.primary else p.hairline,
                animationSpec = tween(220),
                label = "ttRowBorder",
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(bgColor, RoundedCornerShape(boxRadius))
                    .border(
                        if (selected) 1.5.dp else 1.dp,
                        borderColor,
                        RoundedCornerShape(boxRadius),
                    )
                    .clickable { vm.activate(tt.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tt.name, fontSize = 15.sp, color = p.ink, modifier = Modifier.weight(1f))
                // 启用中的作息：对勾胶囊缩放淡入；未启用显示「点击启用」
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(180)),
                    exit = fadeOut(tween(120)),
                ) {
                    Row(
                        Modifier
                            .background(p.primary, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = p.onPrimary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text("使用中", fontSize = 11.sp, color = p.onPrimary, modifier = Modifier.padding(start = 3.dp))
                    }
                }
                AnimatedVisibility(
                    visible = !selected,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(120)),
                ) {
                    Text("点击启用", fontSize = 11.sp, color = p.muted)
                }
                Icon(Icons.Outlined.Edit, contentDescription = "编辑", tint = p.primary,
                    modifier = Modifier.padding(start = 12.dp).size(18.dp).clickable { editingTt = tt })
                if (!selected) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = p.error,
                        modifier = Modifier.padding(start = 12.dp).size(18.dp).clickable { deleteTarget = tt })
                }
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
            var err by remember { mutableStateOf("") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("学期名称") }, singleLine = true)
                OutlinedTextField(start, { start = it }, label = { Text("开学日期 YYYY-MM-DD") }, singleLine = true)
                Text("总周数 ${semester.totalWeeks} 周（由导入课表自动计算）", fontSize = 11.sp, color = p.mutedSoft)
                if (err.isNotEmpty()) Text(err, fontSize = 11.sp, color = p.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({
                        val startDate = runCatching { LocalDate.parse(start.trim()) }.getOrNull()
                        if (startDate == null || startDate.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                            err = "开学日期需 YYYY-MM-DD 且为周一"
                        } else {
                            vm.updateSemester(Semester(id = 0, name = name.ifBlank { "学期" }, startDate = startDate, totalWeeks = semester.totalWeeks))
                            showSemesterEdit = false
                        }
                    }) { Text("保存") }
                    TextButton({ showSemesterEdit = false }) { Text("取消") }
                }
            }
        }

        // ---- 作息编辑面板（名称 + 每节起止时间可改） ----
        editingTt?.let { tt ->
            var name by remember(tt.id) { mutableStateOf(tt.name) }
            var periods by remember(tt.id) { mutableStateOf(tt.periods.sortedBy { it.periodIndex }) }
            val startInputs = remember(tt.id) {
                mutableStateMapOf<Int, String>().apply { periods.forEach { put(it.periodIndex, hhmm(it.startMinute)) } }
            }
            val endInputs = remember(tt.id) {
                mutableStateMapOf<Int, String>().apply { periods.forEach { put(it.periodIndex, hhmm(it.endMinute)) } }
            }
            var err by remember(tt.id) { mutableStateOf("") }
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                periods.sortedBy { it.periodIndex }.forEach { per ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("第${per.periodIndex}节", fontSize = 13.sp, color = p.ink, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            startInputs[per.periodIndex] ?: "",
                            { startInputs[per.periodIndex] = it },
                            modifier = Modifier.width(72.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true,
                        )
                        Text("–", color = p.muted, modifier = Modifier.padding(horizontal = 4.dp))
                        OutlinedTextField(
                            endInputs[per.periodIndex] ?: "",
                            { endInputs[per.periodIndex] = it },
                            modifier = Modifier.width(72.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true,
                        )
                        Text("删", color = p.error, modifier = Modifier.padding(start = 10.dp).clickable {
                            periods = periods.filter { it.periodIndex != per.periodIndex }
                            startInputs.remove(per.periodIndex); endInputs.remove(per.periodIndex)
                        })
                    }
                }
                Text("时间格式 HH:MM，如 08:00（有的校区第一节 8 点上课）", fontSize = 10.sp, color = p.mutedSoft)
                if (err.isNotEmpty()) Text(err, fontSize = 11.sp, color = p.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({
                        val next = (periods.maxOfOrNull { it.periodIndex } ?: 0) + 1
                        periods = periods + Period(periodIndex = next, startMinute = 510, endMinute = 550)
                        startInputs[next] = "08:30"; endInputs[next] = "09:10"
                    }) { Text("+ 加一节") }
                    TextButton({ editingTt = null }) { Text("取消") }
                    TextButton({
                        val parsed = periods.mapNotNull { per ->
                            val s = parseHhmm(startInputs[per.periodIndex])
                            val e = parseHhmm(endInputs[per.periodIndex])
                            if (s == null || e == null || s >= e) null else per.copy(startMinute = s, endMinute = e)
                        }
                        if (parsed.size != periods.size) {
                            err = "时间格式需 HH:MM，且开始须早于结束"
                        } else {
                            val safeName = name.ifBlank { "自定义" }
                            val safePeriods = if (parsed.isEmpty()) listOf(Period(periodIndex = 1, startMinute = 480, endMinute = 520)) else parsed
                            if (tt.id == 0L) vm.addTimeTable(safeName, safePeriods) else vm.savePeriods(tt.copy(name = safeName), safePeriods.map { it.copy(id = 0) })
                            editingTt = null
                        }
                    }) { Text("保存") }
                }
            }
        }
    }

    deleteTarget?.let { tt ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除作息「${tt.name}」？") },
            text = { Text("删除后该作息下的节次时段一并移除，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTimetable(tt.id)
                    deleteTarget = null
                }) { Text("删除", color = p.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

private fun hhmm(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** 解析 "HH:MM" 为距 0 点分钟数；格式非法或开始不早于结束时返回 null。 */
private fun parseHhmm(s: String?): Int? {
    if (s == null) return null
    val parts = s.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    val minutes = h * 60 + m
    return if (minutes in 0..1439) minutes else null
}
