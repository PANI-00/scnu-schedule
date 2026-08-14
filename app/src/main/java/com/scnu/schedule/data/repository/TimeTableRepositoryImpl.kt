package com.scnu.schedule.data.repository

import com.scnu.schedule.data.db.PeriodEntity
import com.scnu.schedule.data.db.TimeTableDao
import com.scnu.schedule.data.db.TimeTableEntity
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.TimeTableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeTableRepositoryImpl @Inject constructor(private val dao: TimeTableDao) : TimeTableRepository {
    override val timetables: Flow<List<TimeTable>> = dao.observeAll().map { rows ->
        rows.map { row ->
            TimeTable(row.timetable.id, row.timetable.name, row.timetable.isDefault,
                row.periods.sortedBy { p -> p.periodIndex }.map { Period(row.timetable.id, it.periodIndex, it.startMinute, it.endMinute) })
        }
    }

    override suspend fun upsert(timetable: TimeTable) {
        val ttId = dao.upsertTimetable(TimeTableEntity(timetable.id, timetable.name, timetable.isDefault))
        dao.replacePeriods(ttId, timetable.periods.map { PeriodEntity(0, ttId, it.periodIndex, it.startMinute, it.endMinute) })
    }

    override suspend fun delete(id: Long) { dao.deletePeriods(id); dao.deleteTimetable(id) }

    /** 首次启动无作息时，种入石牌校区默认 10 节 */
    suspend fun ensureDefaultSeeded() {
        if (dao.count() == 0) {
            val ttId = dao.upsertTimetable(TimeTableEntity(name = "石牌校区", isDefault = true))
            val mins = listOf(
                1 to (510 to 550), 2 to (560 to 600), 3 to (620 to 660), 4 to (670 to 710),
                5 to (870 to 910), 6 to (920 to 960), 7 to (970 to 1010), 8 to (1020 to 1060),
                9 to (1140 to 1180), 10 to (1190 to 1230),
            )
            dao.upsertPeriods(mins.map { (idx, t) -> PeriodEntity(0, ttId, idx, t.first, t.second) })
        }
    }
}
