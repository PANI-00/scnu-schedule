package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import com.scnu.schedule.ui.theme.AppThemeType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class JwxtImportUseCaseTest {

    private class FakeCourseRepo : CourseRepository {
        val saved = mutableListOf<Course>()
        private val state = MutableStateFlow<List<Course>>(emptyList())
        override val courses: Flow<List<Course>> = state
        override suspend fun upsert(course: Course) {
            saved += course
            state.value = saved.toList()
        }

        override suspend fun delete(id: Long) {
            saved.removeAll { it.id == id }
            state.value = saved.toList()
        }
    }

    private class FakeTtRepo(initial: List<TimeTable> = listOf(TimeTable(id = 1, name = "石牌校区", isDefault = true))) : TimeTableRepository {
        val state = MutableStateFlow<List<TimeTable>>(initial)
        override val timetables: Flow<List<TimeTable>> = state
        override suspend fun upsert(timetable: TimeTable): Long {
            val id = if (timetable.id != 0L) timetable.id
            else (state.value.maxOfOrNull { it.id } ?: 0L) + 1
            state.value = state.value.filter { it.id != id } + timetable.copy(id = id)
            return id
        }

        override suspend fun delete(id: Long) {
            state.value = state.value.filter { it.id != id }
        }
    }

    private class FakeSettingsRepo : SettingsRepository {
        val state = MutableStateFlow(Semester(startDate = LocalDate.of(2026, 8, 31)))
        val activeId = MutableStateFlow<Long?>(null)
        override val theme: Flow<AppThemeType> = MutableStateFlow(AppThemeType.CLAUDE)
        override val semester: Flow<Semester> = state
        override val activeTimeTableId: Flow<Long?> = activeId
        override suspend fun setTheme(type: AppThemeType) {}
        override suspend fun setSemester(semester: Semester) { state.value = semester }
        override suspend fun setActiveTimeTable(id: Long?) { activeId.value = id }
    }

    @Test
    fun `导入课程并激活默认作息表与学期名`() = runBlocking {
        val courseRepo = FakeCourseRepo()
        val ttRepo = FakeTtRepo()
        val settingsRepo = FakeSettingsRepo()
        val useCase = JwxtImportUseCase(courseRepo, ttRepo, settingsRepo)

        useCase.import(
            courses = listOf(
                Course(name = "高数", dayOfWeek = 1, startPeriod = 1, endPeriod = 2, weekPattern = WeekPattern(WeekKind.ALL, 1, 16)),
                Course(name = "英语", dayOfWeek = 2, startPeriod = 3, endPeriod = 4, weekPattern = WeekPattern(WeekKind.ODD, 1, 16)),
            ),
            semesterName = "2026-2027 第一学期（秋）",
        )

        assertEquals(2, courseRepo.saved.size)
        assertEquals(0L, courseRepo.saved[0].id)          // id=0 走插入
        assertEquals(0, courseRepo.saved[0].colorIndex)   // 循环取模
        assertEquals(1, courseRepo.saved[1].colorIndex)
        assertEquals(1L, settingsRepo.activeId.value)      // 石牌默认作息
        assertEquals("2026-2027 第一学期（秋）", settingsRepo.state.value.name)
        assertEquals(20, settingsRepo.state.value.totalWeeks)
    }

    @Test
    fun `保留用户开学日期只改学期名`() = runBlocking {
        val settingsRepo = FakeSettingsRepo()
        val useCase = JwxtImportUseCase(FakeCourseRepo(), FakeTtRepo(), settingsRepo)

        useCase.import(emptyList(), "2026-2027 第一学期（秋）")

        assertEquals(LocalDate.of(2026, 8, 31), settingsRepo.state.value.startDate)
        assertEquals("2026-2027 第一学期（秋）", settingsRepo.state.value.name)
    }

    @Test
    fun `无作息表时种入石牌默认并激活`() = runBlocking {
        val courseRepo = FakeCourseRepo()
        val ttRepo = FakeTtRepo(emptyList())
        val settingsRepo = FakeSettingsRepo()
        val useCase = JwxtImportUseCase(courseRepo, ttRepo, settingsRepo)

        useCase.import(
            courses = listOf(
                Course(name = "高数", dayOfWeek = 1, startPeriod = 1, endPeriod = 2, weekPattern = WeekPattern(WeekKind.ALL, 1, 16)),
            ),
            semesterName = "2026-2027 第一学期（秋）",
        )

        assertEquals(1, ttRepo.state.value.size)
        assertEquals("石牌校区", ttRepo.state.value[0].name)
        assertEquals(10, ttRepo.state.value[0].periods.size)
        assertEquals(ttRepo.state.value[0].id, settingsRepo.activeId.value)
    }
}
