package com.scnu.schedule.data.db

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Robolectric 用 Application：跳过 ScheduleApp.onCreate 里 Dispatchers.IO 上的
 * ensureDefaultSeeded() 后台协程。该协程不受 runTest 控制，会跨测试存活，
 * 在下一个测试的 runTest 启动时因 SQLite 连接指针失效抛
 * IllegalStateException，被捕获为 UncaughtExceptionsBeforeTest 导致偶发失败。
 */
class NoSeedApp : Application()

@RunWith(RobolectricTestRunner::class)
@Config(application = NoSeedApp::class)
class DaoTest {
    private lateinit var db: ScheduleDatabase

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, ScheduleDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    @Test fun `courseDao roundtrip`() = runTest {
        val dao = db.courseDao()
        dao.upsert(CourseEntity(name = "高数", teacher = "王", location = "A204",
            dayOfWeek = 1, startPeriod = 1, endPeriod = 2,
            weekPattern = WeekPattern(WeekKind.ODD, 1, 16), colorIndex = 0))
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(WeekKind.ODD, all[0].weekPattern.kind)
    }

    @Test fun `timeTable with periods roundtrip`() = runTest {
        val dao = db.timeTableDao()
        val id = dao.upsertTimetable(TimeTableEntity(name = "默认", isDefault = true))
        dao.upsertPeriods(listOf(PeriodEntity(timetableId = id, periodIndex = 1, startMinute = 510, endMinute = 550)))
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(1, all[0].periods.size)
        assertEquals(510, all[0].periods[0].startMinute)
    }

    @Test fun `semester roundtrip`() = runTest {
        db.semesterDao().upsert(SemesterEntity(id = 0, name = "2026秋", startDate = LocalDate.of(2026, 8, 31), totalWeeks = 20))
        val s = db.semesterDao().observe().first()
        assertTrue(s != null && s.name == "2026秋")
    }
}
