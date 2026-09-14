package com.scnu.schedule.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.scnu.schedule.domain.logic.WeekCalculator
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 让「下一节」2×2 组件在**课程开始的那一刻**立即切换，不留滞后。
 *
 * 组件内容只在「某节课开始」这一瞬间才会变（那一刻起该课不再是"下一节"），
 * 所以这里不做每分钟唤醒——旧实现一天唤醒 1440 次，耗电且被系统节流；
 * 现在每个切换点只排一次闹钟，一天通常 4~10 次。
 *
 * 今天课上完后排到次日 0 点，让组件切到新的一天。
 */
object NextClassRefreshScheduler {
    const val ACTION_NEXT_CLASS_TICK = "com.scnu.schedule.action.NEXT_CLASS_TICK"

    private const val REQUEST_CODE = 7311

    /** 边界后多等 2 秒，避开整分钟边界的时间抖动与调度竞态。 */
    private const val BOUNDARY_GRACE_SECONDS = 2L

    /** 按当前数据算出下一次切换时刻并排定闹钟。 */
    fun schedule(context: Context, data: WidgetData?, now: LocalDateTime = LocalDateTime.now()) {
        if (data == null) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = nextSwitchEpochMillis(data, now)
        val pendingIntent = pendingIntent(
            context,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                // 没有精确闹钟权限时退化：不早于该时刻触发，但可能被系统推迟
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (_: Exception) {
                // 彻底失败则退回系统 30 分钟周期刷新
            }
        }
    }

    /** 移除组件后取消闹钟，避免空转。 */
    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = pendingIntent(
            context,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarm.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * 下一次需要刷新组件的时刻：
     *  - 今天还有未开始的课 → 其中**最早的开始时间**（那一刻它变成已开始，组件切到再下一节）
     *  - 今天已无课 → 次日 0 点（组件切到新的一天）
     */
    fun nextSwitchEpochMillis(data: WidgetData, now: LocalDateTime): Long {
        val today = now.toLocalDate()
        val week = WeekCalculator.currentWeek(data.semester.startDate, today)
        val minuteNow = now.hour * 60 + now.minute
        val periodByIndex = (data.activeTimetable?.periods ?: emptyList())
            .associateBy { it.periodIndex }

        val nextStartMinute = data.courses
            .filter { it.dayOfWeek == today.dayOfWeek.value && it.weekPattern.contains(week) }
            .mapNotNull { course -> periodByIndex[course.startPeriod]?.startMinute }
            .filter { it > minuteNow }
            .minOrNull()

        val target = if (nextStartMinute != null) {
            today.atStartOfDay()
                .plusMinutes(nextStartMinute.toLong())
                .plusSeconds(BOUNDARY_GRACE_SECONDS)
        } else {
            today.plusDays(1).atStartOfDay().plusSeconds(BOUNDARY_GRACE_SECONDS)
        }
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, NextClassTickReceiver::class.java)
            .setAction(ACTION_NEXT_CLASS_TICK)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
