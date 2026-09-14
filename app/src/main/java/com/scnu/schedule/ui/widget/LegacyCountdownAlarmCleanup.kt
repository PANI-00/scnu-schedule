package com.scnu.schedule.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * 清理历史版本遗留的「每分钟刷新倒计时小组件」精确闹钟。
 *
 * 旧版本用 AlarmManager 每分钟唤醒一次来刷新 2×2 组件；该组件现已改为静态展示
 * （只显示下一节固定开始时间，不再做倒计时），每分钟唤醒变成纯空转：
 * 一天 1440 次、一周约一万次唤醒，会造成持续耗电、发热，并触发系统热节流/电池优化，
 * 表现为用几天后整体掉帧。
 *
 * 闹钟在应用升级后仍会保留，因此每次启动时按旧版完全相同的
 * component + action + requestCode 找回该 PendingIntent 并取消。
 */
object LegacyCountdownAlarmCleanup {
    private const val LEGACY_RECEIVER = "com.scnu.schedule.ui.widget.CountdownTickReceiver"
    private const val LEGACY_ACTION = "com.scnu.schedule.action.COUNTDOWN_TICK"
    private const val LEGACY_REQUEST_CODE = 4021

    fun cancelIfScheduled(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent()
            .setClassName(context, LEGACY_RECEIVER)
            .setAction(LEGACY_ACTION)
        val pendingIntent = try {
            PendingIntent.getBroadcast(
                context,
                LEGACY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        } catch (_: Exception) {
            null
        } ?: return

        try {
            alarm.cancel(pendingIntent)
        } catch (_: Exception) {
            // 取消失败时不影响启动流程
        } finally {
            pendingIntent.cancel()
        }
    }
}
