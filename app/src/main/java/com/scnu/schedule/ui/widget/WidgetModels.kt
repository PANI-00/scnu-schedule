package com.scnu.schedule.ui.widget

/** 下一节课 2×1 渲染模型 */
data class NextClassModel(
    val name: String,
    val location: String,
    val startMinute: Int,
    val minutesUntil: Int,
)

/** 今日一条课程 */
data class TodayEntry(
    val startMinute: Int,
    val endMinute: Int,
    val name: String,
    val location: String,
)

/** 今日课程 4×2 渲染模型 */
data class TodayWidgetModel(
    val dateLabel: String,
    val entries: List<TodayEntry>,
)

/** 周课表一个格子 */
data class CourseCell(val name: String, val colorIndex: Int)

/** 周课表一列 = 一天 */
data class DayColumn(val label: String, val courses: List<CourseCell>)

/** 周课表 4×3 渲染模型 */
data class WeekGridModel(val days: List<DayColumn>)
