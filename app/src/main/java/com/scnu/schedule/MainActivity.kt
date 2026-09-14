package com.scnu.schedule

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.ui.navigation.ScheduleNavHost
import com.scnu.schedule.ui.settings.SettingsViewModel
import com.scnu.schedule.ui.theme.AppTheme
import com.scnu.schedule.ui.theme.AppThemeType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockFrameRateAt60()
        setContent {
            val vm: SettingsViewModel = hiltViewModel()
            val theme by vm.theme.collectAsState(initial = AppThemeType.CLAUDE)
            // 主题切换：Crossfade 渐变过渡（丝滑换肤）
            Crossfade(targetState = theme, animationSpec = tween(320), label = "themeCrossfade") { t ->
                AppTheme(t) {
                    ScheduleNavHost()
                }
            }
        }
    }

    /**
     * 把窗口刷新率锁到 60Hz。
     *
     * 高刷屏（90/120Hz）下每帧预算只有 8~11ms，课表页这类组合较重的页面容易掉帧、
     * 帧间隔忽长忽短（看起来一顿一顿）。锁定 60fps 后每帧预算回到 16.7ms，节奏更稳定，
     * 同时也减少了每秒的组合与绘制次数。
     *
     * Android 14(API 34) 起对该视图请求固定帧率；更低版本退化为设置 preferredRefreshRate。
     */
    @Suppress("DEPRECATION")
    private fun lockFrameRateAt60() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                window.decorView.setRequestedFrameRate(FRAME_RATE)
            } else {
                window.attributes = window.attributes.apply { preferredRefreshRate = FRAME_RATE }
            }
        } catch (_: Exception) {
            // 个别机型/系统实现不支持该提示，忽略即可，不影响功能
        }
    }

    private companion object {
        const val FRAME_RATE = 60f
    }
}
