package com.scnu.schedule.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 首次放置即开始按整分钟实时刷新
        CountdownRefreshScheduler.schedule(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 每次更新/放置都确保刷新闹钟已排定（多实例共用同一闹钟，自动覆盖不叠加）
        CountdownRefreshScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个倒计时组件被移除后，停止每分钟空转，节省电量
        CountdownRefreshScheduler.cancel(context)
    }
}
