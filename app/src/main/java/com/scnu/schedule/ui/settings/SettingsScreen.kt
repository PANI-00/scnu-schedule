package com.scnu.schedule.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun SettingsScreen(
    onOpenTimeTable: () -> Unit = {},
    onJwxtImport: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val theme by vm.theme.collectAsState(initial = AppThemeType.CLAUDE)
    val semester by vm.semester.collectAsState(initial = Semester(startDate = LocalDate.now().with(DayOfWeek.MONDAY)))
    val p = LocalAppPalette.current
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("我的", fontSize = 18.sp, color = p.ink)

        Text("主题", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        AppThemeType.entries.forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { vm.setTheme(t) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (t) {
                        AppThemeType.CLAUDE -> "奶油珊瑚"
                        AppThemeType.OPENCODE -> "午夜机房"
                        AppThemeType.RETRO -> "老报刊亭"
                    },
                    fontSize = 15.sp, color = p.ink, modifier = Modifier.weight(1f),
                )
                if (theme == t) Text("●", color = p.primary)
            }
        }

        Text("学期", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text(semester.name, fontSize = 15.sp, color = p.ink)
        Text("开学 ${semester.startDate} · 共 ${semester.totalWeeks} 周", fontSize = 12.sp, color = p.muted)

        Text("作息时间", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text("管理节次时段 →", fontSize = 15.sp, color = p.primary, modifier = Modifier.clickable { onOpenTimeTable() })

        Text("教务导入", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text(
            "从华师教务系统导入本学期课表 →",
            fontSize = 15.sp,
            color = p.primary,
            modifier = Modifier.clickable(onClick = onJwxtImport),
        )

        Text("数据", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text(
            "清空当前课表",
            fontSize = 15.sp,
            color = p.error,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showClearDialog = true }
                .padding(vertical = 10.dp),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空当前课表？") },
            text = { Text("将删除所有已导入/手动添加的课程，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllCourses()
                    showClearDialog = false
                    Toast.makeText(context, "已清空课表", Toast.LENGTH_SHORT).show()
                }) { Text("清空", color = p.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}
