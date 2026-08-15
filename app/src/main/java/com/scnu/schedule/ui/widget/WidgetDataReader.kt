package com.scnu.schedule.ui.widget

import android.content.Context
import com.scnu.schedule.data.db.CourseEntity
import com.scnu.schedule.data.db.DbProvider
import com.scnu.schedule.data.db.TimeTableWithPeriods
import com.scnu.schedule.data.prefs.SettingsDataStore
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.ui.theme.AppThemeType
import kotlinx.coroutines.flow.first

/** 小组件一次渲染所需的最小数据快照 */
data class WidgetData(
    val theme: AppThemeType,
    val semester: Semester,
    val courses: List<Course>,
    val activeTimetable: TimeTable?,
)

object WidgetDataReader {
    /**
     * 直读主 Room 库 + DataStore。
     * 任一步失败返回 null（组件侧显示占位，不崩溃）。
     */
    suspend fun load(context: Context): WidgetData? = runCatching {
        val db = DbProvider.schedule(context)
        val settings = SettingsDataStore(context)
        val theme = settings.theme.first()
        val semester = settings.semester.first()
        val courses = db.courseDao().observeAll().first().map { it.toDomain() }
        val tts = db.timeTableDao().observeAll().first()
        val activeId = settings.activeTimeTableId.first()
        val activeTimetable = tts.firstOrNull { it.timetable.id == activeId }?.toDomain()
            ?: tts.firstOrNull()?.toDomain()
        WidgetData(theme, semester, courses, activeTimetable)
    }.getOrNull()

    private fun CourseEntity.toDomain() =
        Course(id, name, teacher, location, dayOfWeek, startPeriod, endPeriod, weekPattern, colorIndex)

    private fun TimeTableWithPeriods.toDomain() =
        TimeTable(
            timetable.id, timetable.name, timetable.isDefault,
            periods.sortedBy { it.periodIndex }
                .map { Period(timetable.id, it.periodIndex, it.startMinute, it.endMinute) },
        )
}
