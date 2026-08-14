package com.scnu.schedule.data.repository

import com.scnu.schedule.data.prefs.SettingsDataStore
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.ui.theme.AppThemeType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val store: SettingsDataStore) : SettingsRepository {
    override val theme: Flow<AppThemeType> get() = store.theme
    override val semester: Flow<Semester> get() = store.semester
    override val activeTimeTableId: Flow<Long?> get() = store.activeTimeTableId
    override suspend fun setTheme(type: AppThemeType): Unit = store.setTheme(type)
    override suspend fun setSemester(semester: Semester): Unit = store.setSemester(semester)
    override suspend fun setActiveTimeTable(id: Long?): Unit = store.setActiveTimeTable(id)
}
