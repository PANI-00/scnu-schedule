package com.scnu.schedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ScheduleUiState(
    val courses: List<Course> = emptyList(),
    val timetable: TimeTable? = null,
    val semester: Semester? = null,
    val currentWeek: Int = 1,
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val courseRepo: CourseRepository,
    timeTableRepo: TimeTableRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        courseRepo.courses,
        timeTableRepo.timetables,
        settingsRepo.semester,
        settingsRepo.activeTimeTableId,
    ) { courses, tts, semester, activeId ->
        val active = tts.firstOrNull { it.id == activeId } ?: tts.firstOrNull()
        val week = if (semester != null) WeekCalculator.currentWeek(semester.startDate) else 1
        ScheduleUiState(courses, active, semester, week)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())
}
