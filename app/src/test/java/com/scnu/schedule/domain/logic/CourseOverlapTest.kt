package com.scnu.schedule.domain.logic

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseOverlapTest {
    private fun course(name: String, day: Int, start: Int, end: Int, kind: WeekKind = WeekKind.ALL) =
        Course(name = name, dayOfWeek = day, startPeriod = start, endPeriod = end,
            weekPattern = WeekPattern(kind, 1, 16))

    @Test fun `no overlap when different day`() {
        val a = course("A", 1, 1, 2); val b = course("B", 2, 1, 2)
        assertEquals(0, CourseOverlap.findOverlaps(listOf(a, b)).size)
    }

    @Test fun `overlap when same day and period ranges intersect`() {
        val a = course("A", 1, 1, 2); val b = course("B", 1, 2, 3)
        assertEquals(1, CourseOverlap.findOverlaps(listOf(a, b)).size)
    }

    @Test fun `no overlap when adjacent periods`() {
        val a = course("A", 1, 1, 1); val b = course("B", 1, 2, 2)
        assertEquals(0, CourseOverlap.findOverlaps(listOf(a, b)).size)
    }

    @Test fun `no overlap when odd and even week`() {
        val a = course("A", 1, 1, 2, WeekKind.ODD)
        val b = course("B", 1, 1, 2, WeekKind.EVEN)
        assertEquals(0, CourseOverlap.findOverlaps(listOf(a, b)).size)
    }

    @Test fun `multiple overlaps reported pairwise`() {
        val a = course("A", 1, 1, 2); val b = course("B", 1, 1, 1); val c = course("C", 1, 2, 3)
        assertEquals(2, CourseOverlap.findOverlaps(listOf(a, b, c)).size)
    }
}
