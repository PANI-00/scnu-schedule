package com.scnu.schedule.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val todayCourses: List<Course> = emptyList(),
    val timetable: TimeTable? = null,
    val currentWeek: Int = 1,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    courseRepo: CourseRepository,
    timeTableRepo: TimeTableRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        courseRepo.courses, timeTableRepo.timetables, settingsRepo.semester, settingsRepo.activeTimeTableId,
    ) { courses, tts, semester, activeId ->
        val active = tts.firstOrNull { it.id == activeId } ?: tts.firstOrNull()
        val today = LocalDate.now()
        val week = WeekCalculator.currentWeek(semester.startDate, today)
        val dayNum = today.dayOfWeek.value // 1=周一
        val todayCourses = WeekCalculator.coursesForWeek(courses.filter { it.dayOfWeek == dayNum }, week)
            .sortedBy { it.startPeriod }
        TodayUiState(today, todayCourses, active, week)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    /** 下一节：今天里 startPeriod 最小且当前时间在其开始前的课 */
    fun nextCourse(courses: List<Course>, periods: List<Period>): Course? {
        val now = LocalTime.now()
        return courses.firstOrNull { c ->
            val p = periods.firstOrNull { it.periodIndex == c.startPeriod } ?: return@firstOrNull false
            now.toSecondOfDay() / 60 < p.startMinute
        }
    }
}
