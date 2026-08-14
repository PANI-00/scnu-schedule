package com.scnu.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            AppTheme(theme) {
                ScheduleNavHost()
            }
        }
    }
}
