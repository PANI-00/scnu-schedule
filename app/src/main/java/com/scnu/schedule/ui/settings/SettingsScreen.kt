package com.scnu.schedule.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    vm: SettingsViewModel = hiltViewModel(),
) {
    val theme by vm.theme.collectAsState(initial = AppThemeType.CLAUDE)
    val semester by vm.semester.collectAsState(initial = Semester(startDate = LocalDate.now().with(DayOfWeek.MONDAY)))
    val p = LocalAppPalette.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("我的", fontSize = 18.sp, color = p.ink)

        Text("主题", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        AppThemeType.entries.forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { vm.setTheme(t) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (t == AppThemeType.CLAUDE) "Claude 奶油珊瑚" else "OpenCode TUI",
                    fontSize = 15.sp, color = p.ink, modifier = Modifier.weight(1f))
                if (theme == t) Text("●", color = p.primary)
            }
        }

        Text("学期", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text(semester.name, fontSize = 15.sp, color = p.ink)
        Text("开学 ${semester.startDate} · 共 ${semester.totalWeeks} 周", fontSize = 12.sp, color = p.muted)

        Text("作息时间", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text("管理节次时段 →", fontSize = 15.sp, color = p.primary, modifier = Modifier.clickable { onOpenTimeTable() })
    }
}
