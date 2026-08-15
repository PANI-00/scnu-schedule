package com.scnu.schedule.data.di

import com.scnu.schedule.data.jwxt.WebViewCookieJar
import com.scnu.schedule.data.jwxt.ZhengFangClient
import com.scnu.schedule.data.jwxt.ZhengFangParser
import com.scnu.schedule.domain.importing.CourseParser
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.CookieJar

@Module
@InstallIn(SingletonComponent::class)
abstract class JwxtModule {

    @Binds
    abstract fun bindCourseParser(impl: ZhengFangParser): CourseParser

    companion object {
        @Provides
        @Singleton
        fun provideCookieJar(): CookieJar = WebViewCookieJar()

        @Provides
        @Singleton
        fun provideZhengFangClient(parser: CourseParser, cookieJar: CookieJar): ZhengFangClient =
            ZhengFangClient(parser, cookieJar)
    }
}
