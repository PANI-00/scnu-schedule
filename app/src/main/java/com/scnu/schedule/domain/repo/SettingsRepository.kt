package com.scnu.schedule.domain.repo

import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.ui.theme.AppThemeType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val theme: Flow<AppThemeType>
    val semester: Flow<Semester>
    val activeTimeTableId: Flow<Long?>
    suspend fun setTheme(type: AppThemeType)
    suspend fun setSemester(semester: Semester)
    suspend fun setActiveTimeTable(id: Long?)
}
