package com.scnu.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.ui.theme.LocalAppPalette

@Composable
fun CourseBlock(course: Course, color: Color, rowHeight: Int, onClick: () -> Unit) {
    val p = LocalAppPalette.current
    val span = course.endPeriod - course.startPeriod + 1
    val radius = if (p.isDark) 2.dp else 8.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp)
            .heightIn(min = (rowHeight * span).dp)
            .background(color = if (p.isDark) p.surfaceCard else Color.Transparent, shape = RoundedCornerShape(radius))
            .then(
                if (p.isDark) Modifier.border(1.dp, p.hairline, RoundedCornerShape(radius))
                else Modifier.border(1.dp, p.hairline, RoundedCornerShape(radius))
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxHeight().width(3.dp).background(color))
        }
        Column {
            Text(course.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.ink, maxLines = 1)
            Text(course.location, fontSize = 9.sp, color = p.muted, maxLines = 1)
            if (course.weekPattern.kind == WeekKind.ODD || course.weekPattern.kind == WeekKind.EVEN) {
                Text(if (course.weekPattern.kind == WeekKind.ODD) "单" else "双", fontSize = 8.sp, color = p.warning)
            }
        }
    }
}
