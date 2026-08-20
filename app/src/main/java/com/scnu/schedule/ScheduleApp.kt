package com.scnu.schedule

import android.app.Application
import com.scnu.schedule.data.repository.TimeTableRepositoryImpl
import com.scnu.schedule.ui.widget.CountdownRefreshScheduler
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
        scope.launch {
            timeTableRepository.ensureDefaultSeeded()
            // 已有桌面小组件（升级/重启后）也确保倒计时每分钟刷新闹钟已排定
            CountdownRefreshScheduler.ensureScheduledIfWidgetExists(applicationContext)
            WidgetUpdateNotifier.notifyDataChanged(applicationContext)
        }
    }
}
