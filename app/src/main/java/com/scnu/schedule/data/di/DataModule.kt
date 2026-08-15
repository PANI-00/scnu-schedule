package com.scnu.schedule.data.di

import android.content.Context
import com.scnu.schedule.data.db.CourseDao
import com.scnu.schedule.data.db.DbProvider
import com.scnu.schedule.data.db.ScheduleDatabase
import com.scnu.schedule.data.db.SemesterDao
import com.scnu.schedule.data.db.TimeTableDao
import com.scnu.schedule.data.prefs.SettingsDataStore
import com.scnu.schedule.data.repository.CourseRepositoryImpl
import com.scnu.schedule.data.repository.SettingsRepositoryImpl
import com.scnu.schedule.data.repository.TimeTableRepositoryImpl
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton fun provideDb(@ApplicationContext ctx: Context): ScheduleDatabase =
        DbProvider.schedule(ctx)

    @Provides fun provideCourseDao(db: ScheduleDatabase): CourseDao = db.courseDao()
    @Provides fun provideTimeTableDao(db: ScheduleDatabase): TimeTableDao = db.timeTableDao()
    @Provides fun provideSemesterDao(db: ScheduleDatabase): SemesterDao = db.semesterDao()
    @Provides @Singleton fun provideSettingsDataStore(@ApplicationContext ctx: Context): SettingsDataStore =
        SettingsDataStore(ctx)
}

/** Repository 接口绑定：ViewModel 注入接口，这里把实现绑上去 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository
    @Binds abstract fun bindTimeTableRepository(impl: TimeTableRepositoryImpl): TimeTableRepository
    @Binds abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
