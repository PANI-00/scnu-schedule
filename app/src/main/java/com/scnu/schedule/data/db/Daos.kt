package com.scnu.schedule.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class TimeTableWithPeriods(
    @Embedded val timetable: TimeTableEntity,
    @Relation(parentColumn = "id", entityColumn = "timetableId")
    val periods: List<PeriodEntity>,
)

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startPeriod")
    fun observeAll(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity): Long

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM courses")
    suspend fun clearAll()
}

@Dao
interface TimeTableDao {
    @Transaction
    @Query("SELECT * FROM timetables ORDER BY id")
    fun observeAll(): Flow<List<TimeTableWithPeriods>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimetable(t: TimeTableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriods(periods: List<PeriodEntity>)

    @Query("DELETE FROM periods WHERE timetableId = :timetableId")
    suspend fun deletePeriods(timetableId: Long)

    @Query("DELETE FROM timetables WHERE id = :id")
    suspend fun deleteTimetable(id: Long)

    @Transaction
    suspend fun replacePeriods(timetableId: Long, periods: List<PeriodEntity>) {
        deletePeriods(timetableId)
        upsertPeriods(periods)
    }

    @Query("SELECT COUNT(*) FROM timetables")
    suspend fun count(): Int
}

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semester WHERE id = 0 LIMIT 1")
    fun observe(): Flow<SemesterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SemesterEntity)
}
