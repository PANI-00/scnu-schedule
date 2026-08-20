package com.scnu.schedule.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box

import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scnu.schedule.MainActivity
import com.scnu.schedule.ui.theme.AppPalette
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.ui.theme.paletteFor
import java.time.LocalDateTime

/** 下一节课 2×1：课程名 + 地点 + 固定开始时间；空态显示「暂无课程」。 */

class NextClassWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataReader.load(context)
        val palette = paletteFor(data?.theme ?: AppThemeType.CLAUDE)
        val model = data?.let { d ->
            WidgetFormatter.nextClass(
                d.courses,
                d.activeTimetable?.periods ?: emptyList(),
                d.semester,
                LocalDateTime.now(),
            )
        }
        provideContent { NextClassContent(model, palette) }
    }
}

@Composable
fun NextClassContent(model: NextClassModel?, palette: AppPalette) {
    Box(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(palette.canvas))
            .padding(9.dp)
            .cornerRadius(if (palette.isDark) 0.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (model == null) {
            Text(
                "暂无课程",
                style = TextStyle(color = ColorProvider(palette.muted), fontSize = 13.sp),
            )
        } else {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    model.name,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(palette.ink), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    model.location.ifBlank { "未填写地点" },
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 10.sp),
                    modifier = GlanceModifier.padding(top = 4.dp),
                )
                Text(
                    "${WidgetFormatter.hhmm(model.startMinute)} 开始上课",
                    style = TextStyle(color = ColorProvider(palette.primary), fontSize = 13.sp),
                    modifier = GlanceModifier.padding(top = 4.dp),
                )
            }
        }
    }
}
