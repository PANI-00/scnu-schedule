package com.scnu.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scnu.schedule.ui.theme.LocalAppPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekJumpSheet(
    totalWeeks: Int,
    currentWeek: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val p = LocalAppPalette.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = p.surfaceCard) {
        Text("跳转周次", fontSize = 15.sp, color = p.ink,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = (totalWeeks + 3) / 4
            (0 until rows).forEach { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { c ->
                        val week = r * 4 + c
                        if (week <= totalWeeks) {
                            val selected = week == currentWeek
                            Box(
                                modifier = Modifier.weight(1f).height(40.dp)
                                    .background(if (selected) p.primary else p.surfaceSoft, RoundedCornerShape(8.dp))
                                    .border(1.dp, p.hairline, RoundedCornerShape(8.dp))
                                    .clickable { onJump(week) },
                                contentAlignment = Alignment.Center,
                            ) { Text("W$week", fontSize = 12.sp, color = if (selected) p.onPrimary else p.muted) }
                        } else Box(Modifier.weight(1f))
                    }
                }
            }
        }
        Text("共 $totalWeeks 周", fontSize = 10.sp, color = p.mutedSoft,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
    }
}
