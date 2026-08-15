package com.scnu.schedule.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scnu.schedule.ui.theme.AppPalette
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.ui.theme.ClaudePalette
import com.scnu.schedule.ui.theme.OpenCodePalette
import java.time.LocalDate

/** 周课表 4×3：迷你 7 列周网格，仅渲染当前周有课（含单双周过滤）。 */
class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataReader.load(context)
        val palette = if (data?.theme == AppThemeType.CLAUDE) ClaudePalette else OpenCodePalette
        val model = data?.let { d ->
            WidgetFormatter.weekGrid(d.courses, d.semester, LocalDate.now())
        } ?: WeekGridModel(emptyList())
        provideContent { WeekContent(model, palette) }
    }
}

@Composable
fun WeekContent(model: WeekGridModel, palette: AppPalette) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(palette.canvas))
            .padding(6.dp),
    ) {
        if (model.days.all { it.courses.isEmpty() }) {
            Text(
                "本周暂无课程",
                style = TextStyle(color = ColorProvider(palette.muted), fontSize = 13.sp),
            )
        } else {
            // 表头：一~日
            Row(GlanceModifier.fillMaxWidth()) {
                model.days.forEach { d ->
                    Text(
                        d.label,
                        style = TextStyle(color = ColorProvider(palette.muted), fontSize = 9.sp, textAlign = TextAlign.Center),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
            // 3 行课程格子
            repeat(3) { r ->
                Row(GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                    model.days.forEach { day ->
                        val cell = day.courses.getOrNull(r)
                        Box(
                            GlanceModifier.defaultWeight().height(26.dp)
                                .then(
                                    if (cell != null) {
                                        GlanceModifier.background(ColorProvider(palette.coursePalette[cell.colorIndex % palette.coursePalette.size]))
                                    } else {
                                        GlanceModifier.background(ColorProvider(palette.surfaceSoft))
                                    }
                                ),
                        ) {
                            if (cell != null) {
                                Text(
                                    cell.name,
                                    maxLines = 1,
                                    style = TextStyle(color = ColorProvider(palette.ink), fontSize = 8.sp, textAlign = TextAlign.Center),
                                    modifier = GlanceModifier.padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
