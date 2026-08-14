package com.scnu.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TimeTableViewModel @Inject constructor(
    private val ttRepo: TimeTableRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val semesterFlow = settingsRepo.semester

    val uiState = combine(ttRepo.timetables, settingsRepo.activeTimeTableId) { tts, activeId ->
        tts to (tts.firstOrNull { it.id == activeId } ?: tts.firstOrNull()?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<TimeTable>() to null)

    fun activate(id: Long) = viewModelScope.launch { settingsRepo.setActiveTimeTable(id) }
    fun savePeriods(tt: TimeTable, periods: List<Period>) =
        viewModelScope.launch { ttRepo.upsert(tt.copy(periods = periods)) }
    fun addTimeTable(name: String, periods: List<Period>) =
        viewModelScope.launch { ttRepo.upsert(TimeTable(name = name, periods = periods)) }
    fun updateSemester(s: Semester) = viewModelScope.launch { settingsRepo.setSemester(s) }
}
