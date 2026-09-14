package com.scnu.schedule.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 到达课程开始时刻：刷新「下一节」2×2 组件。
 *
 * 刷新时组件会重新取数并自行排定**下一个**切换点，因此不需要在这里重复排闹钟。
 * 只刷新倒计时组件本身，不惊动其它组件。
 */
class NextClassTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NextClassRefreshScheduler.ACTION_NEXT_CLASS_TICK) return

        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val manager = GlanceAppWidgetManager(appContext)
            manager.getGlanceIds(CountdownWidget::class.java).forEach { id ->
                CountdownWidget().update(appContext, id)
            }
        }
    }
}
