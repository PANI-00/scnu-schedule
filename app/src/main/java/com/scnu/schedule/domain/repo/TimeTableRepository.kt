package com.scnu.schedule.domain.repo

import com.scnu.schedule.domain.model.TimeTable
import kotlinx.coroutines.flow.Flow

interface TimeTableRepository {
    val timetables: Flow<List<TimeTable>>
    /** 返回 upsert 后的作息表 id（新增时由实现生成）。 */
    suspend fun upsert(timetable: TimeTable): Long
    suspend fun delete(id: Long)
}
