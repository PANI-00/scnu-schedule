package com.scnu.schedule.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 下一节 2×2 组件。
 *
 * 该组件只显示「下一节」的固定开始时间，不做秒/分级倒计时，
 * 因此刷新完全交给系统的 updatePeriodMillis（30 分钟）与数据变更时的统一刷新，
 * 不再使用每分钟精确闹钟。
 */
class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}
