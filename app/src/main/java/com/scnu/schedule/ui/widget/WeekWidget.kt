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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scnu.schedule.MainActivity
import com.scnu.schedule.ui.theme.AppPalette
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.ui.theme.paletteFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 周课表 4×3：迷你 7 列周网格，仅渲染当前周有课（含单双周过滤）。 */
class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataReader.load(context)
        val palette = paletteFor(data?.theme ?: AppThemeType.CLAUDE)
        val model = data?.let { d ->
            buildWeekWidgetModel(d)
        } ?: emptyWeekWidgetModel()
        provideContent { WeekContentGrid(model, palette) }
    }
}

@Composable
fun WeekContent(model: WeekGridModel, palette: AppPalette) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(palette.canvas))
            .padding(6.dp)
            .clickable(actionStartActivity<MainActivity>()),
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
private fun buildWeekWidgetModel(data: WidgetData): WeekWidgetModel {
    val today = LocalDate.now()
    val week = WeekCalculator.currentWeek(data.semester.startDate, today)
    val periods = data.activeTimetable?.periods.orEmpty().take(5)
    val courses = data.courses.filter { it.weekPattern.contains(week) }
    val dates = WeekCalculator.weekDates(data.semester.startDate, week - 1)
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    return WeekWidgetModel(
        week = week,
        dayLabels = labels,
        dayDates = dates.map { it.format(DateTimeFormatter.ofPattern("MM/dd")) },
        periodLabels = periods.map { it.startLabel() },
        cells = periods.map { period ->
            labels.indices.map { index ->
                courses.firstOrNull { course ->
                    course.dayOfWeek == index + 1 &&
                        course.startPeriod <= period.periodIndex &&
                        course.endPeriod >= period.periodIndex
                }?.let { WeekSlot(it.name, it.colorIndex) }
            }
        },
    )
}

private fun emptyWeekWidgetModel() = WeekWidgetModel(
    week = 1,
    dayLabels = listOf("一", "二", "三", "四", "五", "六", "日"),
    dayDates = List(7) { "" },
    periodLabels = emptyList(),
    cells = emptyList(),
)

@Composable
private fun WeekContentGrid(model: WeekWidgetModel, palette: AppPalette) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(palette.canvas))
            .padding(6.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth().padding(bottom = 3.dp)) {
            Text(
                "第${model.week}周",
                style = TextStyle(color = ColorProvider(palette.primary), fontSize = 8.sp, fontWeight = androidx.glance.text.FontWeight.Bold),
                modifier = GlanceModifier.width(34.dp),
            )
            model.dayLabels.forEachIndexed { index, label ->
                Text(
                    "$label\n${model.dayDates.getOrNull(index).orEmpty()}",
                    style = TextStyle(color = ColorProvider(if (model.dayDates.getOrNull(index) == LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd"))) palette.primary else palette.muted), fontSize = 8.sp, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        model.cells.forEachIndexed { row, cells ->
            Row(GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                Text(
                    model.periodLabels.getOrNull(row).orEmpty(),
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 7.sp),
                    modifier = GlanceModifier.width(34.dp).padding(top = 4.dp),
                )
                cells.forEach { cell ->
                    Box(
                        GlanceModifier.defaultWeight().height(28.dp).background(
                            ColorProvider(if (cell == null) palette.surfaceSoft else palette.coursePalette[cell.colorIndex % palette.coursePalette.size]),
                        ),
                    ) {
                        if (cell != null) {
                            Text(
                                cell.name,
                                maxLines = 2,
                                style = TextStyle(color = ColorProvider(palette.ink), fontSize = 7.sp, textAlign = TextAlign.Center),
                                modifier = GlanceModifier.padding(2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private object WeekWidgetTail {
}
