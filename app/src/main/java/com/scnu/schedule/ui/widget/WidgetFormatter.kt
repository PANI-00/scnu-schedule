package com.scnu.schedule.ui.widget

import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 小组件纯逻辑：把 domain 数据格式化为渲染模型。
 * 只依赖 domain 模型 + java.time，无 Android 依赖，可 JVM 单测。
 */
object WidgetFormatter {
    private val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    /** 最近一节未开始的课（今天内）；没有返回 null */
    fun nextClass(
        courses: List<Course>,
        periods: List<Period>,
        semester: Semester,
        now: LocalDateTime,
    ): NextClassModel? {
        val today = now.toLocalDate()
        val week = WeekCalculator.currentWeek(semester.startDate, today)
        val minuteNow = now.hour * 60 + now.minute
        val periodByIndex = periods.associateBy { it.periodIndex }
        return courses
            .filter { it.dayOfWeek == today.dayOfWeek.value && it.weekPattern.contains(week) }
            .mapNotNull { c ->
                val p = periodByIndex[c.startPeriod] ?: return@mapNotNull null
                if (p.startMinute > minuteNow) {
                    NextClassModel(c.name, c.location, p.startMinute, p.startMinute - minuteNow)
                } else null
            }
            .minByOrNull { it.startMinute }
    }

    /** 今日全部课程，按开始时间升序；时间段取第一节起点到最后一节终点 */
    fun todayEntries(
        courses: List<Course>,
        periods: List<Period>,
        semester: Semester,
        today: LocalDate,
    ): List<TodayEntry> {
        val week = WeekCalculator.currentWeek(semester.startDate, today)
        val periodByIndex = periods.associateBy { it.periodIndex }
        return courses
            .filter { it.dayOfWeek == today.dayOfWeek.value && it.weekPattern.contains(week) }
            .mapNotNull { c ->
                val start = periodByIndex[c.startPeriod] ?: return@mapNotNull null
                val end = periodByIndex[c.endPeriod] ?: return@mapNotNull null
                TodayEntry(start.startMinute, end.endMinute, c.name, c.location)
            }
            .sortedBy { it.startMinute }
    }

    /** 迷你周网格：每列一天，按节次升序，最多 maxRows 门课（含单双周过滤） */
    fun weekGrid(
        courses: List<Course>,
        semester: Semester,
        today: LocalDate,
        maxRows: Int = 3,
    ): WeekGridModel {
        val week = WeekCalculator.currentWeek(semester.startDate, today)
        val days = (1..7).map { day ->
            DayColumn(
                label = dayLabels[day - 1],
                courses = courses
                    .filter { it.dayOfWeek == day && it.weekPattern.contains(week) }
                    .sortedBy { it.startPeriod }
                    .take(maxRows)
                    .map { CourseCell(it.name, it.colorIndex) },
            )
        }
        return WeekGridModel(days)
    }

    fun countdownText(minutes: Int): String = when {
        minutes <= 0 -> "正在上课"
        minutes < 60 -> "${minutes} 分钟后"
        else -> "${minutes / 60} 小时 ${minutes % 60} 分后"
    }

    fun hhmm(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

    fun dateLabel(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"
}
