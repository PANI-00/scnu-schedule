package com.scnu.schedule.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scnu.schedule.ui.jwxt.JwxtLoginScreen
import com.scnu.schedule.ui.schedule.ScheduleScreen
import com.scnu.schedule.ui.settings.SettingsScreen
import com.scnu.schedule.ui.settings.TimeTableScreen
import com.scnu.schedule.ui.today.TodayScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab("today", "今日", Icons.Filled.Home),
    Tab("schedule", "课表", Icons.Filled.CalendarMonth),
    Tab("settings", "我的", Icons.Filled.Settings),
)

@Composable
fun ScheduleNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "schedule", modifier = Modifier.padding(padding)) {
            composable("today") { TodayScreen() }
            composable("schedule") { ScheduleScreen() }
            composable("timetable") { TimeTableScreen() }
            composable("settings") {
                SettingsScreen(
                    onOpenTimeTable = { nav.navigate("timetable") },
                    onJwxtImport = { nav.navigate("jwxt_import") },
                )
            }
            composable("jwxt_import") {
                JwxtLoginScreen(
                    onBack = { nav.popBackStack() },
                    onImported = {
                        nav.navigate("schedule") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
