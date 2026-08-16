package com.scnu.schedule

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
}
