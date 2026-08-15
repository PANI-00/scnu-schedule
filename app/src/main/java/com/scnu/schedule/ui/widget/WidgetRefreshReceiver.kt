package com.scnu.schedule.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 收到数据变更广播后统一刷新所有已放置的组件实例。 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetUpdateNotifier.ACTION_REFRESH) {
            WidgetUpdateNotifier.updateAllNow(context)
        }
    }
}
