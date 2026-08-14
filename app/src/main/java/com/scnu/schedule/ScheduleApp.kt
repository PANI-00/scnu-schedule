package com.scnu.schedule

import android.app.Application
import com.scnu.schedule.data.repository.TimeTableRepositoryImpl
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
        scope.launch { timeTableRepository.ensureDefaultSeeded() }
    }
}
