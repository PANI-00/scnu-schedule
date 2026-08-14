package com.scnu.schedule.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.ui.theme.AppThemeType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val semesterNameKey = stringPreferencesKey("semester_name")
    private val semesterStartKey = longPreferencesKey("semester_start")
    private val semesterWeeksKey = intPreferencesKey("semester_weeks")
    private val activeTtKey = longPreferencesKey("active_timetable")

    val theme: Flow<AppThemeType> = context.dataStore.data.map {
        runCatching { AppThemeType.valueOf(it[themeKey] ?: "CLAUDE") }.getOrDefault(AppThemeType.CLAUDE)
    }

    val semester: Flow<Semester> = context.dataStore.data.map { prefs ->
        val startDay = prefs[semesterStartKey]
        if (startDay == null) Semester(startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY))
        else Semester(
            name = prefs[semesterNameKey] ?: "2026-2027 第一学期",
            startDate = LocalDate.ofEpochDay(startDay),
            totalWeeks = prefs[semesterWeeksKey] ?: 20,
        )
    }

    val activeTimeTableId: Flow<Long?> = context.dataStore.data.map { it[activeTtKey] }

    suspend fun setTheme(t: AppThemeType) {
        context.dataStore.edit { it[themeKey] = t.name }
    }
    suspend fun setSemester(s: Semester) {
        context.dataStore.edit {
            it[semesterNameKey] = s.name; it[semesterStartKey] = s.startDate.toEpochDay(); it[semesterWeeksKey] = s.totalWeeks
        }
    }
    suspend fun setActiveTimeTable(id: Long?) {
        context.dataStore.edit { if (id == null) it.remove(activeTtKey) else it[activeTtKey] = id }
    }
}
