package com.scnu.schedule.ui.widget

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import com.scnu.schedule.ui.theme.AppThemeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 锁定「下一节」组件的切换时刻计算：切换必须正好发生在课程开始的那一刻，不留滞后。
 */
class NextClassRefreshSchedulerTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val semester = Semester(name = "测试学期", startDate = today, totalWeeks = 20)

    private fun periods(): List<Period> = listOf(
        Period(periodIndex = 1, startMinute = 8 * 60, endMinute = 8 * 60 + 45),
        Period(periodIndex = 5, startMinute = 14 * 60, endMinute = 14 * 60 + 45),
    )

    private fun course(startPeriod: Int, kind: WeekKind = WeekKind.ALL): Course = Course(
        id = startPeriod.toLong(),
        name = "课$startPeriod",
        teacher = "",
        location = "",
        dayOfWeek = today.dayOfWeek.value,
        startPeriod = startPeriod,
        endPeriod = startPeriod,
        weekPattern = WeekPattern(kind, 1, 20),
        colorIndex = 0,
    )

    private fun data(courses: List<Course>): WidgetData = WidgetData(
        theme = AppThemeType.CLAUDE,
        semester = semester,
        courses = courses,
        activeTimetable = TimeTable(id = 1, name = "默认作息", isDefault = true, periods = periods()),
    )

    private fun epoch(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `下一节课的开始时刻就是切换时刻`() {
        val data = data(listOf(course(1), course(5)))
        val now = LocalDateTime.of(today, LocalTime.of(13, 59))

        assertEquals(
            epoch(LocalDateTime.of(today, LocalTime.of(14, 0, 2))),
            NextClassRefreshScheduler.nextSwitchEpochMillis(data, now),
        )
    }

    @Test
    fun `课上完后不再有切换点_排到次日零点`() {
        val data = data(listOf(course(1), course(5)))
        val now = LocalDateTime.of(today, LocalTime.of(14, 30))

        assertEquals(
            epoch(LocalDateTime.of(today.plusDays(1), LocalTime.of(0, 0, 2))),
            NextClassRefreshScheduler.nextSwitchEpochMillis(data, now),
        )
    }

    @Test
    fun `不符合周次规则的课程不参与切换点计算`() {
        // 今天是第 1 周：双周课不生效，上午那节已开始 → 今天没有切换点
        val data = data(listOf(course(1), course(5, WeekKind.EVEN)))
        val now = LocalDateTime.of(today, LocalTime.of(10, 0))

        assertEquals(
            epoch(LocalDateTime.of(today.plusDays(1), LocalTime.of(0, 0, 2))),
            NextClassRefreshScheduler.nextSwitchEpochMillis(data, now),
        )
    }

    @Test
    fun `只取当天最早的未开始课程作为切换点`() {
        val data = data(listOf(course(1), course(5)))
        val now = LocalDateTime.of(today, LocalTime.of(7, 30))

        assertEquals(
            epoch(LocalDateTime.of(today, LocalTime.of(8, 0, 2))),
            NextClassRefreshScheduler.nextSwitchEpochMillis(data, now),
        )
    }
}
