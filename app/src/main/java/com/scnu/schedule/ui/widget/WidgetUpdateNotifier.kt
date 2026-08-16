package com.scnu.schedule.ui.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 数据变更统一刷新：App 侧调用 [notifyDataChanged]，广播触发 [WidgetRefreshReceiver]。 */
object WidgetUpdateNotifier {
    const val ACTION_REFRESH = "com.scnu.schedule.action.WIDGET_REFRESH"

    /** 课程/作息/学期/主题变更后调用；无组件实例时广播是空操作，安全。 */
    fun notifyDataChanged(context: Context) {
        context.sendBroadcast(Intent(ACTION_REFRESH).setPackage(context.packageName))
    }

    /** 刷新所有已放置的组件实例（在协程里逐个 update）。 */
    fun updateAllNow(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(NextClassWidget::class.java).forEach { NextClassWidget().update(context, it) }
            manager.getGlanceIds(TodayWidget::class.java).forEach { TodayWidget().update(context, it) }
            manager.getGlanceIds(WeekWidget::class.java).forEach { WeekWidget().update(context, it) }
            manager.getGlanceIds(CountdownWidget::class.java).forEach { CountdownWidget().update(context, it) }
        }
    }
}
