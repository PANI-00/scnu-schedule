package com.scnu.schedule.data.db

import android.content.Context
import androidx.room.Room

/**
 * 主库单例访问器。App 与小组件同进程，共用这一个实例，
 * 避免「App 的 Hilt 实例」与「小组件自建实例」双实例缓存不一致。
 */
object DbProvider {
    @Volatile
    private var instance: ScheduleDatabase? = null

    fun schedule(context: Context): ScheduleDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            ScheduleDatabase::class.java,
            "schedule.db",
        ).build().also { instance = it }
    }
}
