package com.scnu.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.ui.theme.AppThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val courseRepo: CourseRepository,
) : ViewModel() {
    val theme: Flow<AppThemeType> = repo.theme
    val semester: Flow<Semester> = repo.semester

    fun setTheme(t: AppThemeType) = viewModelScope.launch { repo.setTheme(t) }
    fun setSemester(s: Semester) = viewModelScope.launch { repo.setSemester(s) }

    /** 清空当前课表（删除所有课程）。 */
    fun clearAllCourses() = viewModelScope.launch { courseRepo.clearAll() }
}
