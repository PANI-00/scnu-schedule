package com.scnu.schedule.domain.model

data class TimeTable(
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val periods: List<Period> = emptyList(),   // 按 periodIndex 升序
) {
    val periodCount: Int get() = periods.size
}
