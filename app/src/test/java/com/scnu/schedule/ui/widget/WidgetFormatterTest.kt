package com.scnu.schedule.ui.widget

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WidgetFormatterTest {
    // 2026-08-31 是周一（与 core WeekCalculatorTest 一致）
    private val semester = Semester(startDate = LocalDate.of(2026, 8, 31), totalWeeks = 20)

    private fun course(name: String, day: Int, start: Int, end: Int, kind: WeekKind = WeekKind.ALL) =
        Course(name = name, dayOfWeek = day, startPeriod = start, endPeriod = end,
            weekPattern = WeekPattern(kind, 1, 16))

    private val periods = listOf(
        Period(periodIndex = 1, startMinute = 510, endMinute = 550), // 08:30–09:10
        Period(periodIndex = 2, startMinute = 560, endMinute = 600), // 09:20–10:00
        Period(periodIndex = 5, startMinute = 870, endMinute = 910), // 14:30–15:10
    )

    // ---- nextClass ----

    @Test fun `next class picks nearest upcoming`() {
        val now = LocalDateTime.of(2026, 9, 1, 8, 0) // 周二 第1周
        val next = WidgetFormatter.nextClass(
            listOf(course("高数", 2, 1, 2), course("体育", 2, 5, 5)), periods, semester, now)
        assertEquals("高数", next!!.name)
        assertEquals(30, next.minutesUntil) // 8:00 -> 8:30
    }

    @Test fun `next class skips already started`() {
        val now = LocalDateTime.of(2026, 9, 1, 9, 0) // 高数 8:30 已开始
        val next = WidgetFormatter.nextClass(
            listOf(course("高数", 2, 1, 2), course("体育", 2, 5, 5)), periods, semester, now)
        assertEquals("体育", next!!.name)
        assertEquals(330, next.minutesUntil) // 9:00 -> 14:30
    }

    @Test fun `next class null when all passed`() {
        val now = LocalDateTime.of(2026, 9, 1, 15, 0)
        assertNull(WidgetFormatter.nextClass(
            listOf(course("高数", 2, 1, 2), course("体育", 2, 5, 5)), periods, semester, now))
    }

    @Test fun `next class respects single double week`() {
        val now = LocalDateTime.of(2026, 9, 2, 8, 0) // 周三 第1周(单周)
        val odd = course("离散", 3, 1, 1, WeekKind.ODD)
        val even = course("大物", 3, 1, 1, WeekKind.EVEN)
        val next = WidgetFormatter.nextClass(listOf(odd, even), periods, semester, now)
        assertEquals("离散", next!!.name)
    }

    // ---- todayEntries ----

    @Test fun `today entries filtered sorted with time range`() {
        val today = LocalDate.of(2026, 9, 1) // 周二
        val entries = WidgetFormatter.todayEntries(
            listOf(course("体育", 2, 5, 5), course("高数", 2, 1, 2), course("英语", 3, 1, 2)),
            periods, semester, today)
        assertEquals(2, entries.size)
        assertEquals("高数", entries[0].name)
        assertEquals(510, entries[0].startMinute)
        assertEquals(600, entries[0].endMinute) // 第1-2节 -> 08:30–10:00
        assertEquals("体育", entries[1].name)
        assertEquals(870, entries[1].startMinute)
    }

    // ---- weekGrid ----

    @Test fun `week grid filters odd even weeks`() {
        val today = LocalDate.of(2026, 8, 31) // 周一 第1周(单周)
        val odd = course("离散", 1, 1, 2, WeekKind.ODD)
        val even = course("大物", 1, 1, 2, WeekKind.EVEN)
        val every = course("体育", 2, 5, 5)
        val grid = WidgetFormatter.weekGrid(listOf(odd, even, every), semester, today)
        assertEquals(7, grid.days.size)
        assertEquals(listOf("一", "二", "三", "四", "五", "六", "日"), grid.days.map { it.label })
        assertEquals(listOf("离散"), grid.days[0].courses.map { it.name })
        assertEquals(listOf("体育"), grid.days[1].courses.map { it.name })
        assertNull(grid.days[2].courses.firstOrNull()) // 周三无课
    }

    @Test fun `week grid caps rows`() {
        val today = LocalDate.of(2026, 9, 1) // 周二
        val courses = (1..5).map { course("课$it", 2, it, it) }
        val grid = WidgetFormatter.weekGrid(courses, semester, today)
        assertEquals(3, grid.days[1].courses.size) // maxRows 默认 3
    }

    // ---- helpers ----

    @Test fun `countdown text`() {
        assertEquals("正在上课", WidgetFormatter.countdownText(0))
        assertEquals("正在上课", WidgetFormatter.countdownText(-5))
        assertEquals("5 分钟后", WidgetFormatter.countdownText(5))
        assertEquals("1 小时 15 分后", WidgetFormatter.countdownText(75))
    }

    @Test fun `hhmm formatting`() {
        assertEquals("08:30", WidgetFormatter.hhmm(510))
        assertEquals("14:30", WidgetFormatter.hhmm(870))
    }

    @Test fun `date label`() {
        assertEquals("9月1日", WidgetFormatter.dateLabel(LocalDate.of(2026, 9, 1)))
    }
}
