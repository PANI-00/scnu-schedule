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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scnu.schedule.MainActivity
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.ui.theme.AppPalette
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.ui.theme.paletteFor
import java.time.LocalDateTime

/**
 * 下一节课 2×2（精致卡片版）：卡片底 + 分层信息 + 主题配色。
 * 顶部：周几 · 第几周 + 「下一节」标签；中部：课程名 + 地点；
 * 底部：倒计时。三套主题（奶油珊瑚/午夜机房/老报刊亭）自动适配配色。
 */
class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataReader.load(context)
        val palette = paletteFor(data?.theme ?: AppThemeType.CLAUDE)
        val now = LocalDateTime.now()
        val model = data?.let { d ->
            WidgetFormatter.nextClass(
                d.courses,
                d.activeTimetable?.periods ?: emptyList(),
                d.semester,
                now,
            )
        }
        val week = data?.let { WeekCalculator.currentWeek(it.semester.startDate, now.toLocalDate()) } ?: 1
        val dayCn = DAY_CN[now.dayOfWeek.value - 1]
        val label = model?.let {
            if (it.minutesUntil <= 0) "正在上课" else "${WidgetFormatter.countdownText(it.minutesUntil)}开始"
        } ?: ""
        provideContent {
            CountdownContent(model, "周$dayCn · 第${week}周", label, palette)
        }
    }

    private companion object {
        val DAY_CN = listOf("一", "二", "三", "四", "五", "六", "日")
    }
}

@Composable
fun CountdownContent(model: NextClassModel?, subLabel: String, label: String, palette: AppPalette) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.surfaceCard))
            .cornerRadius(if (palette.isDark) 0.dp else 16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (model == null) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "暂无课程",
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 13.sp),
                )
            }
        } else {
            Row(GlanceModifier.fillMaxWidth()) {
                Text(
                    subLabel,
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 9.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    "下一节",
                    style = TextStyle(color = ColorProvider(palette.primary), fontSize = 9.sp, fontWeight = FontWeight.Bold),
                )
            }
            Box(GlanceModifier.fillMaxWidth().defaultWeight()) { }
            Text(
                model.name,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(palette.ink), fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                model.location.ifBlank { "未填写地点" },
                maxLines = 1,
                style = TextStyle(color = ColorProvider(palette.muted), fontSize = 10.sp),
                modifier = GlanceModifier.padding(top = 2.dp),
            )
            Box(GlanceModifier.fillMaxWidth().defaultWeight()) { }
            Row(GlanceModifier.fillMaxWidth()) {
                Text(
                    "▸",
                    style = TextStyle(color = ColorProvider(palette.primary), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(end = 5.dp),
                )
                Text(
                    label,
                    style = TextStyle(color = ColorProvider(palette.primary), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
