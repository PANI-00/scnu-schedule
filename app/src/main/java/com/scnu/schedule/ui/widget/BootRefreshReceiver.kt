package com.scnu.schedule.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机后刷新全部小组件。
 *
 * AlarmManager 的闹钟在重启后会丢失，若不处理，「下一节」组件要等到系统 30 分钟周期刷新
 * 才会重新排定切换闹钟。这里在开机时主动刷一次，让切换点立刻恢复精确。
 */
class BootRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        WidgetUpdateNotifier.updateAllNow(context.applicationContext)
    }
}
