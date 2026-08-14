package com.scnu.schedule.domain.model

enum class WeekKind { ALL, ODD, EVEN, CUSTOM }

data class WeekPattern(
    val kind: WeekKind,
    val rangeStart: Int = 1,
    val rangeEnd: Int = 20,
    val customWeeks: Set<Int> = emptySet(),
) {
    fun contains(week: Int): Boolean = when (kind) {
        WeekKind.ALL -> week in rangeStart..rangeEnd
        WeekKind.ODD -> week in rangeStart..rangeEnd && week % 2 == 1
        WeekKind.EVEN -> week in rangeStart..rangeEnd && week % 2 == 0
        WeekKind.CUSTOM -> week in customWeeks
    }

    fun weeks(): List<Int> = when (kind) {
        WeekKind.ALL -> (rangeStart..rangeEnd).toList()
        WeekKind.ODD -> (rangeStart..rangeEnd).filter { it % 2 == 1 }
        WeekKind.EVEN -> (rangeStart..rangeEnd).filter { it % 2 == 0 }
        WeekKind.CUSTOM -> customWeeks.toList()
    }

    fun overlaps(other: WeekPattern): Boolean =
        weeks().intersect(other.weeks().toSet()).isNotEmpty()
}
