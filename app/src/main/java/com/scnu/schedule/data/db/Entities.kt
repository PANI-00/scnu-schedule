package com.scnu.schedule.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.scnu.schedule.domain.model.WeekPattern
import java.time.LocalDate

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val teacher: String, val location: String,
    val dayOfWeek: Int, val startPeriod: Int, val endPeriod: Int,
    val weekPattern: WeekPattern, val colorIndex: Int,
)

@Entity(tableName = "timetables")
data class TimeTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val isDefault: Boolean = false,
)

@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableId: Long, val periodIndex: Int,
    val startMinute: Int, val endMinute: Int,
)

@Entity(tableName = "semester")
data class SemesterEntity(
    @PrimaryKey val id: Long = 0,
    val name: String, val startDate: LocalDate, val totalWeeks: Int,
)
