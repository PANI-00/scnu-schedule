package com.scnu.schedule

import android.app.Application
import com.scnu.schedule.data.repository.TimeTableRepositoryImpl
import com.scnu.schedule.ui.widget.LegacyCountdownAlarmCleanup
import com.scnu.schedule.ui.widget.WidgetUpdateNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ScheduleApp : Application() {
    @Inject lateinit var timeTableRepository: TimeTableRepositoryImpl
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 取消旧版本遗留的「每分钟刷新」精确闹钟：该功能已取消，闹钟会是纯空转
        LegacyCountdownAlarmCleanup.cancelIfScheduled(applicationContext)
        scope.launch {
            timeTableRepository.ensureDefaultSeeded()
            WidgetUpdateNotifier.notifyDataChanged(applicationContext)
        }
    }
}
