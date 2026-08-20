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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column


import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize

import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height


import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scnu.schedule.MainActivity
import com.scnu.schedule.ui.theme.AppPalette
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.ui.theme.paletteFor
import java.time.LocalDate

/** 今日课程 4×2：节次时间段 + 名称 + 地点；空态显示「今天没有课」。 */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataReader.load(context)
        val palette = paletteFor(data?.theme ?: AppThemeType.CLAUDE)
        val today = LocalDate.now()
        val model = data?.let { d ->
            TodayWidgetModel(
                dateLabel = WidgetFormatter.dateLabel(today),
                entries = WidgetFormatter.todayEntries(
                    d.courses,
                    d.activeTimetable?.periods ?: emptyList(),
                    d.semester,
                    today,
                ),
            )
        } ?: TodayWidgetModel("", emptyList())
        provideContent { TodayContentB(model, palette) }
    }
}

@Composable
fun TodayContent(model: TodayWidgetModel, palette: AppPalette) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(palette.canvas))
            .padding(6.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            "今日 · ${model.dateLabel}",
            style = TextStyle(color = ColorProvider(palette.muted), fontSize = 10.sp),
        )
        if (model.entries.isEmpty()) {
            Text(
                "今天没有课",
                style = TextStyle(color = ColorProvider(palette.muted), fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 8.dp),
            )
        } else {
            model.entries.take(4).forEach { e ->
                Row(GlanceModifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        WidgetFormatter.hhmm(e.startMinute),
                        style = TextStyle(color = ColorProvider(palette.primary), fontSize = 10.sp),
                        modifier = GlanceModifier.width(36.dp),
                    )
                    Text(
                        "${e.name} · ${e.location}",
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(palette.ink), fontSize = 11.sp),
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayContentB(model: TodayWidgetModel, palette: AppPalette) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(palette.canvas))
            .padding(6.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxSize()) {
            Column(
                GlanceModifier.width(54.dp).fillMaxHeight().padding(end = 8.dp),
            ) {
                Text(
                    "今日",
                    style = TextStyle(color = ColorProvider(palette.ink), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    model.dateLabel,
                      style = TextStyle(color = ColorProvider(palette.muted), fontSize = 10.sp),
                )
                Text(
                    "${model.entries.size} 节课程",
                    style = TextStyle(color = ColorProvider(palette.primary), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(top = 4.dp),
                )
            }
            Column(GlanceModifier.fillMaxWidth().fillMaxHeight().padding(start = 4.dp)) {
                Text(
                    "课程安排",
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(bottom = 3.dp),
                )
                if (model.entries.isEmpty()) {
                    Text(
                        "今天没有课",
                        style = TextStyle(color = ColorProvider(palette.muted), fontSize = 12.sp),
                    )
                } else {
                    model.entries.take(4).forEach { entry ->
                        Row(
                            GlanceModifier.fillMaxWidth()
                                .height(26.dp)
                                .background(ColorProvider(palette.surfaceSoft))
                                .padding(horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                WidgetFormatter.hhmm(entry.startMinute),
                                style = TextStyle(color = ColorProvider(palette.primary), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier.width(40.dp),
                            )
                            Text(
                                "${entry.name} · ${entry.location}",
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(palette.ink), fontSize = 12.sp),
                                modifier = GlanceModifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

