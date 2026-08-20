package com.scnu.schedule.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 让 2×2 倒计时小组件按「整分钟」实时刷新。
 *
 * 系统级 updatePeriodMillis 最短只能到 30 分钟，无法满足倒计时的实时性，
 * 因此这里用 AlarmManager 精确闹钟在下一个整分钟触发一次刷新并自动续期。
 * 到达触发点后只刷新倒计时组件（比统一刷新全部组件更省电）。
 */
object CountdownRefreshScheduler {
    const val ACTION_TICK = "com.scnu.schedule.action.COUNTDOWN_TICK"
    private const val REQUEST_CODE = 4021
    private const val MINUTE_MS = 60_000L

    /** 排定下一整分钟的刷新闹钟（已有的会自动覆盖，不会重复叠加）。 */
    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = (System.currentTimeMillis() / MINUTE_MS + 1) * MINUTE_MS
        val pi = tickPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                // 未授予「闹钟和提醒」精确闹钟权限时退化为不精确刷新，避免抛 SecurityException
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            // 个别机型即使 canScheduleExactAlarms 也抛错，退化为不精确刷新
            try {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (_: Exception) {
                // 彻底失败则放弃，至少保留系统 30 分钟周期
            }
        }
    }

    /** 移除倒计时组件时取消闹钟，避免空转耗电。 */
    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = tickPendingIntent(context, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarm.cancel(pi)
    }

    /** 到达触发点：刷新所有倒计时组件实例，并预约下一整分钟。 */
    fun refreshAndReschedule(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(CountdownWidget::class.java).forEach {
                CountdownWidget().update(context, it)
            }
        }
        schedule(context)
    }

    /** App 启动/升级后调用：如果桌面上还有倒计时组件，就确保闹钟已排定。 */
    fun ensureScheduledIfWidgetExists(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            if (manager.getGlanceIds(CountdownWidget::class.java).isNotEmpty()) {
                schedule(context)
            }
        }
    }

    private fun tickPendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, CountdownTickReceiver::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
