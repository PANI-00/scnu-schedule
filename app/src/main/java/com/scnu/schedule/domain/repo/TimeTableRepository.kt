package com.scnu.schedule.domain.repo

import com.scnu.schedule.domain.model.TimeTable
import kotlinx.coroutines.flow.Flow

interface TimeTableRepository {
    val timetables: Flow<List<TimeTable>>
    suspend fun upsert(timetable: TimeTable)
    suspend fun delete(id: Long)
}
