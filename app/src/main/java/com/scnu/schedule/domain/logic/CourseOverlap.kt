package com.scnu.schedule.domain.logic

import com.scnu.schedule.domain.model.Course

object CourseOverlap {
    fun findOverlaps(courses: List<Course>): List<Pair<Course, Course>> {
        val result = mutableListOf<Pair<Course, Course>>()
        for (i in courses.indices) {
            for (j in i + 1 until courses.size) {
                val a = courses[i]; val b = courses[j]
                if (a.dayOfWeek == b.dayOfWeek &&
                    a.startPeriod <= b.endPeriod && b.startPeriod <= a.endPeriod &&
                    a.weekPattern.overlaps(b.weekPattern)
                ) result.add(a to b)
            }
        }
        return result
    }
}
