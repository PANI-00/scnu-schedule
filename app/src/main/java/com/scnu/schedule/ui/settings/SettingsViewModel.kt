package com.scnu.schedule.ui.settings

import androidx.lifecycle.ViewModel
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.ui.theme.AppThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {
    val theme: Flow<AppThemeType> = repo.theme
}
