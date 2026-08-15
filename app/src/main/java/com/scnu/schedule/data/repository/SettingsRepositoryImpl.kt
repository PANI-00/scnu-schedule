package com.scnu.schedule.data.repository

import android.content.Context
import com.scnu.schedule.data.prefs.SettingsDataStore
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.ui.theme.AppThemeType
import com.scnu.schedule.ui.widget.WidgetUpdateNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsDataStore,
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val theme: Flow<AppThemeType> get() = store.theme
    override val semester: Flow<Semester> get() = store.semester
    override val activeTimeTableId: Flow<Long?> get() = store.activeTimeTableId
    override suspend fun setTheme(type: AppThemeType) {
        store.setTheme(type)
        WidgetUpdateNotifier.notifyDataChanged(context)
    }
    override suspend fun setSemester(semester: Semester) {
        store.setSemester(semester)
        WidgetUpdateNotifier.notifyDataChanged(context)
    }
    override suspend fun setActiveTimeTable(id: Long?) {
        store.setActiveTimeTable(id)
        WidgetUpdateNotifier.notifyDataChanged(context)
    }
}
