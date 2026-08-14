package com.scnu.schedule.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CourseEntity::class, TimeTableEntity::class, PeriodEntity::class, SemesterEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun timeTableDao(): TimeTableDao
    abstract fun semesterDao(): SemesterDao
}
