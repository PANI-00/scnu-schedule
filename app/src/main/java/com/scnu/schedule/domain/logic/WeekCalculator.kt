package com.scnu.schedule.domain.logic

import com.scnu.schedule.domain.model.Course
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekCalculator {
    /** 当前是第几周；startDate 约定为第 1 周周一 */
    fun currentWeek(startDate: LocalDate, today: LocalDate = LocalDate.now()): Int {
        val days = ChronoUnit.DAYS.between(startDate, today)
        return if (days < 0) 1 else (days / 7 + 1).toInt()
    }

    /** weekOffset=0 表示开学那一周；weekday 1=周一..7=周日 */
    fun dateOfWeekday(startDate: LocalDate, weekOffset: Int, weekday: Int): LocalDate =
        startDate.plusWeeks(weekOffset.toLong()).plusDays((weekday - 1).toLong())

    fun weekDates(startDate: LocalDate, weekOffset: Int): List<LocalDate> =
        (1..7).map { dateOfWeekday(startDate, weekOffset, it) }

    fun coursesForWeek(courses: List<Course>, currentWeek: Int): List<Course> =
        courses.filter { it.weekPattern.contains(currentWeek) }
}
