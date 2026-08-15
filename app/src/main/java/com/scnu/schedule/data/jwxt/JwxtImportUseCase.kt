package com.scnu.schedule.data.jwxt

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
 * 2) 激活「石牌校区」作息表（默认表优先）；
 * 3) 学期名/总周数写入 DataStore（保留用户已设的开学日期）。
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
        settingsRepo.setSemester(mergeSemesterName(semesterName))
    }

    private suspend fun mergeSemesterName(name: String): Semester {
        val existing = settingsRepo.semester.first()
        return existing.copy(name = name, totalWeeks = TOTAL_WEEKS)
    }

    private suspend fun resolveTimetableId(): Long {
        val timetables = timeTableRepo.timetables.first()
        timetables.firstOrNull { it.isDefault }?.let { return it.id }
        timetables.firstOrNull()?.let { return it.id }
        // 防御：无作息表时种入石牌默认（正常启动已种子化，理论不触发），upsert 直接返回新 id
        val tt = TimeTable(
            name = "石牌校区",
            isDefault = true,
            periods = SHIPAI_PERIODS.map { (idx, start, end) ->
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

        /** 石牌校区默认 10 节（与 Plan 1 Task 7 种子一致）。 */
        val SHIPAI_PERIODS: List<Triple<Int, Int, Int>> = listOf(
            Triple(1, 510, 550), Triple(2, 560, 600), Triple(3, 620, 660), Triple(4, 670, 710),
            Triple(5, 870, 910), Triple(6, 920, 960), Triple(7, 970, 1010), Triple(8, 1020, 1060),
            Triple(9, 1140, 1180), Triple(10, 1190, 1230),
        )
    }
}
