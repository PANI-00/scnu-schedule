package com.scnu.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import com.scnu.schedule.ui.theme.LocalAppPalette

@Composable
fun CourseEditDialog(
    initial: Course?,
    periodCount: Int,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: ((Long) -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var teacher by remember { mutableStateOf(initial?.teacher ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var day by remember { mutableIntStateOf(initial?.dayOfWeek ?: 1) }
    var start by remember { mutableIntStateOf(initial?.startPeriod ?: 1) }
    var end by remember { mutableIntStateOf(initial?.endPeriod ?: 2) }
    var kind by remember { mutableStateOf(initial?.weekPattern?.kind ?: WeekKind.ALL) }
    val p = LocalAppPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surfaceCard,
        title = { Text(if (initial == null || initial.id == 0L) "新建课程" else "编辑课程", color = p.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("课程名") }, singleLine = true)
                OutlinedTextField(teacher, { teacher = it }, label = { Text("教师") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("地点") }, singleLine = true)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier
                                .background(if (day == i + 1) p.primary else p.surfaceSoft, RoundedCornerShape(6.dp))
                                .clickable { day = i + 1 }
                                .padding(horizontal = 7.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, color = if (day == i + 1) p.onPrimary else p.muted) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("节次", color = p.muted, modifier = Modifier.weight(1f))
                    var showStart by remember { mutableStateOf(false) }
                    Box {
                        Text("第 $start 节", color = p.ink, modifier = Modifier.clickable { showStart = true }.padding(6.dp))
                        DropdownMenu(expanded = showStart, onDismissRequest = { showStart = false }) {
                            (1..periodCount).forEach { v ->
                                DropdownMenuItem(text = { Text("第 $v 节") }, onClick = { start = v; if (v > end) end = v; showStart = false })
                            }
                        }
                    }
                    Text(" 到 ", color = p.muted)
                    var showEnd by remember { mutableStateOf(false) }
                    Box {
                        Text("第 $end 节", color = p.ink, modifier = Modifier.clickable { showEnd = true }.padding(6.dp))
                        DropdownMenu(expanded = showEnd, onDismissRequest = { showEnd = false }) {
                            (start..periodCount).forEach { v ->
                                DropdownMenuItem(text = { Text("第 $v 节") }, onClick = { end = v; showEnd = false })
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("周次", color = p.muted, modifier = Modifier.weight(1f))
                    listOf(WeekKind.ALL, WeekKind.ODD, WeekKind.EVEN).forEach { k ->
                        val label = when (k) { WeekKind.ALL -> "每周"; WeekKind.ODD -> "单周"; WeekKind.EVEN -> "双周"; WeekKind.CUSTOM -> "自定义" }
                        Box(
                            modifier = Modifier.padding(horizontal = 3.dp)
                                .background(if (kind == k) p.primary else p.surfaceSoft, RoundedCornerShape(6.dp))
                                .clickable { kind = k }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) { Text(label, color = if (kind == k) p.onPrimary else p.muted, fontSize = 12.sp) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(Course(initial?.id ?: 0, name.ifBlank { "未命名" }, teacher, location, day, start, end,
                    WeekPattern(kind, 1, 16),
                    if (initial == null || initial.id == 0L) (day + start) % 8 else initial.colorIndex))
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            if (initial != null && initial.id != 0L && onDelete != null) {
                IconButton({ onDelete(initial.id); onDismiss() }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = p.error)
                }
            } else {
                IconButton(onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "取消", tint = p.muted)
                }
            }
        },
    )
}
