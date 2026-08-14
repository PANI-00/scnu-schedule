package com.scnu.schedule.domain.model

import java.time.LocalDate

data class Semester(
    val id: Long = 0,
    val name: String = "2026-2027 第一学期",
    val startDate: LocalDate,   // 开学日（应为周一）
    val totalWeeks: Int = 20,
)
