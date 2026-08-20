package com.scnu.schedule.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 每分钟触发：刷新 2×2 倒计时小组件并续期下一分钟。 */
class CountdownTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CountdownRefreshScheduler.ACTION_TICK) {
            CountdownRefreshScheduler.refreshAndReschedule(context)
        }
    }
}
