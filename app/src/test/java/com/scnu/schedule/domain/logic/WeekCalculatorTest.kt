package com.scnu.schedule.domain.logic

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekCalculatorTest {
    // 2026-08-31 是周一
    private val semesterStart = LocalDate.of(2026, 8, 31)

    @Test fun `current week is 1 on start date`() {
        assertEquals(1, WeekCalculator.currentWeek(semesterStart, LocalDate.of(2026, 8, 31)))
    }

    @Test fun `current week advances every 7 days`() {
        assertEquals(2, WeekCalculator.currentWeek(semesterStart, LocalDate.of(2026, 9, 7)))
        assertEquals(3, WeekCalculator.currentWeek(semesterStart, LocalDate.of(2026, 9, 14)))
    }

    @Test fun `before start clamps to week 1`() {
        assertEquals(1, WeekCalculator.currentWeek(semesterStart, LocalDate.of(2026, 8, 1)))
    }

    @Test fun `date of weekday within a week`() {
        assertEquals(LocalDate.of(2026, 8, 31), WeekCalculator.dateOfWeekday(semesterStart, 0, 1))
        assertEquals(LocalDate.of(2026, 9, 2), WeekCalculator.dateOfWeekday(semesterStart, 0, 3))
        assertEquals(LocalDate.of(2026, 9, 11), WeekCalculator.dateOfWeekday(semesterStart, 1, 5))
    }

    @Test fun `week dates returns 7 days`() {
        val dates = WeekCalculator.weekDates(semesterStart, 0)
        assertEquals(7, dates.size)
        assertEquals(LocalDate.of(2026, 9, 6), dates.last())
    }

    @Test fun `courses filtered by current week`() {
        val odd = Course(name = "离散", dayOfWeek = 1, startPeriod = 1, endPeriod = 2,
            weekPattern = WeekPattern(WeekKind.ODD, 1, 16))
        val every = Course(name = "高数", dayOfWeek = 1, startPeriod = 1, endPeriod = 2,
            weekPattern = WeekPattern(WeekKind.ALL, 1, 16))
        val even = Course(name = "大物", dayOfWeek = 1, startPeriod = 1, endPeriod = 2,
            weekPattern = WeekPattern(WeekKind.EVEN, 1, 16))
        val week3 = WeekCalculator.coursesForWeek(listOf(odd, every, even), 3)
        assertTrue(week3.any { it.name == "离散" })
        assertTrue(week3.any { it.name == "高数" })
        assertFalse(week3.any { it.name == "大物" })
    }
}
