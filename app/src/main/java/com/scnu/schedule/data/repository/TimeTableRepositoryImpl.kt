package com.scnu.schedule.data.repository

import android.content.Context
import com.scnu.schedule.data.DefaultTimeTables
import com.scnu.schedule.data.db.PeriodEntity
import com.scnu.schedule.data.db.TimeTableDao
import com.scnu.schedule.data.db.TimeTableEntity
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.TimeTableRepository
import com.scnu.schedule.ui.widget.WidgetUpdateNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeTableRepositoryImpl @Inject constructor(
    private val dao: TimeTableDao,
    @ApplicationContext private val context: Context,
) : TimeTableRepository {
    override val timetables: Flow<List<TimeTable>> = dao.observeAll().map { rows ->
        rows.map { row ->
            TimeTable(row.timetable.id, row.timetable.name, row.timetable.isDefault,
                row.periods.sortedBy { p -> p.periodIndex }.map { Period(periodIndex = it.periodIndex, startMinute = it.startMinute, endMinute = it.endMinute) })
        }
    }

    override suspend fun upsert(timetable: TimeTable): Long {
        val ttId = dao.upsertTimetable(TimeTableEntity(timetable.id, timetable.name, timetable.isDefault))
        dao.replacePeriods(ttId, timetable.periods.map { PeriodEntity(0, ttId, it.periodIndex, it.startMinute, it.endMinute) })
        WidgetUpdateNotifier.notifyDataChanged(context)
        return ttId
    }

    override suspend fun delete(id: Long) {
        dao.deletePeriods(id)
        dao.deleteTimetable(id)
        WidgetUpdateNotifier.notifyDataChanged(context)
    }

    /** 首次启动无作息时，种入两套内置默认作息（石牌/滨海 + 大学城/南海），首个标记默认 */
    suspend fun ensureDefaultSeeded() {
        if (dao.count() == 0) {
            DefaultTimeTables.ALL.forEachIndexed { index, (name, periods) ->
                val ttId = dao.upsertTimetable(TimeTableEntity(name = name, isDefault = index == 0))
                dao.upsertPeriods(
                    periods.map { (idx, s, e) -> PeriodEntity(0, ttId, idx, s, e) },
                )
            }
            WidgetUpdateNotifier.notifyDataChanged(context)
        }
    }
}
