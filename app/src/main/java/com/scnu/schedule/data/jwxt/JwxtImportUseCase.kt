package com.scnu.schedule.data.jwxt

import com.scnu.schedule.data.DefaultTimeTables
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 教务导入落库：
 * 1) 逐门 upsert 课程（id=0 走 Room 插入），colorIndex 循环分配课程色板下标；
 * 2) 激活默认作息表（isDefault 表优先）；
 * 3) 学期名 + 总周数写入 DataStore——总周数由导入课表推导（取课程最大周次），
 *    保留用户已设的开学日期。
 *
 * 已知取舍 [DECISION]：MVP 不做去重，重复导入会追加课程行；后续可加"先删当学期课程再导入"。
 */
class JwxtImportUseCase @Inject constructor(
    private val courseRepo: CourseRepository,
    private val timeTableRepo: TimeTableRepository,
    private val settingsRepo: SettingsRepository,
) {

    suspend fun import(courses: List<Course>, semesterName: String) {
        val timetableId = resolveTimetableId()
        courses.forEachIndexed { index, course ->
            courseRepo.upsert(course.copy(id = 0, colorIndex = index % COLOR_COUNT))
        }
        settingsRepo.setActiveTimeTable(timetableId)
        settingsRepo.setSemester(mergeSemesterName(semesterName, totalWeeksOf(courses)))
    }

    /** 学期总周数由导入课表推导：取所有课程周次的最大值（如课程到第 17 周 → 17 周）。 */
    private fun totalWeeksOf(courses: List<Course>): Int =
        courses.mapNotNull { it.weekPattern.weeks().maxOrNull() }.maxOrNull() ?: TOTAL_WEEKS

    private suspend fun mergeSemesterName(name: String, totalWeeks: Int): Semester {
        val existing = settingsRepo.semester.first()
        return existing.copy(name = name, totalWeeks = totalWeeks)
    }

    private suspend fun resolveTimetableId(): Long {
        val timetables = timeTableRepo.timetables.first()
        timetables.firstOrNull { it.isDefault }?.let { return it.id }
        timetables.firstOrNull()?.let { return it.id }
        // 防御：无作息表时种入首个内置作息（正常启动已种子化，理论不触发），upsert 直接返回新 id
        val (name, periods) = DefaultTimeTables.FIRST
        val tt = TimeTable(
            name = name,
            isDefault = true,
            periods = periods.map { (idx, start, end) ->
                Period(periodIndex = idx, startMinute = start, endMinute = end)
            },
        )
        return timeTableRepo.upsert(tt)
    }

    companion object {
        /** 课程色板 8 色（ClaudePalette.coursePalette 长度），按循环取模。 */
        const val COLOR_COUNT = 8

        /** 华师一学期总周数 [VERIFY]。 */
        const val TOTAL_WEEKS = 20
    }
}
