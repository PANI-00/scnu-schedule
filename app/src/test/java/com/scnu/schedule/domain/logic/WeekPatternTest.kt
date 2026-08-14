package com.scnu.schedule.domain.logic

import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekPatternTest {
    private val all = WeekPattern(WeekKind.ALL, rangeStart = 1, rangeEnd = 20)
    private val odd = WeekPattern(WeekKind.ODD, rangeStart = 1, rangeEnd = 16)
    private val even = WeekPattern(WeekKind.EVEN, rangeStart = 1, rangeEnd = 16)
    private val custom = WeekPattern(WeekKind.CUSTOM, customWeeks = setOf(1, 3, 9))

    @Test fun `every week course shows in range`() {
        assertTrue(all.contains(1)); assertTrue(all.contains(20)); assertFalse(all.contains(0)); assertFalse(all.contains(21))
    }

    @Test fun `odd week course only odd weeks`() {
        assertTrue(odd.contains(1)); assertTrue(odd.contains(15)); assertFalse(odd.contains(2)); assertFalse(odd.contains(16))
    }

    @Test fun `even week course only even weeks`() {
        assertFalse(even.contains(1)); assertTrue(even.contains(2)); assertTrue(even.contains(16))
    }

    @Test fun `custom week set`() {
        assertTrue(custom.contains(9)); assertFalse(custom.contains(4))
    }

    @Test fun `weeks list materializes`() {
        assertTrue(WeekPattern(WeekKind.ODD, 1, 5).weeks() == listOf(1, 3, 5))
    }

    @Test fun `overlap detection between patterns`() {
        val odd16 = WeekPattern(WeekKind.ODD, 1, 16)
        val all20 = WeekPattern(WeekKind.ALL, 1, 20)
        assertTrue(odd16.overlaps(all20))
        assertFalse(odd16.overlaps(WeekPattern(WeekKind.EVEN, 1, 16)))
    }
}
