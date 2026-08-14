package com.scnu.schedule.domain.model

data class Course(
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int,          // 1..7 周一..周日
    val startPeriod: Int,        // 起始节次（对应作息表 periodIndex）
    val endPeriod: Int,          // 结束节次（含）
    val weekPattern: WeekPattern,
    val colorIndex: Int = 0,
)
