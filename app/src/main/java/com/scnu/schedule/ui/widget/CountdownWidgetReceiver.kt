package com.scnu.schedule.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 下一节 2×2 组件。
 *
 * 组件只显示「下一节」的固定开始时间，不做秒/分级倒计时；
 * 切换时机由 [NextClassRefreshScheduler] 在每节课开始的那一刻精确触发。
 */
class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个组件被移除后取消闹钟，避免空转
        NextClassRefreshScheduler.cancel(context)
    }
}
