package com.scnu.schedule.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scnu.schedule.ui.jwxt.JwxtLoginScreen
import com.scnu.schedule.ui.schedule.ScheduleScreen
import com.scnu.schedule.ui.settings.SettingsScreen
import com.scnu.schedule.ui.settings.TimeTableScreen
import com.scnu.schedule.ui.theme.LocalAppPalette
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
            // 自定义紧凑底栏：66dp，选中胶囊仿 Material3（主题色浅底、贴合图标+文字），
            // 胶囊与栏上/下缘各留 ~10dp，不会被裁剪。
            val p = LocalAppPalette.current
            Box(Modifier.fillMaxWidth().background(p.surfaceCard)) {
                Row(Modifier.fillMaxWidth().height(66.dp)) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        // 选中胶囊与图标/文字颜色丝滑过渡
                        val capsuleColor by animateColorAsState(
                            targetValue = if (selected) p.primary.copy(alpha = 0.20f) else Color.Transparent,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "tabCapsule",
                        )
                        val tintColor by animateColorAsState(
                            targetValue = if (selected) p.primary else p.muted,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "tabTint",
                        )
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(capsuleColor, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 18.dp, vertical = 5.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        tint = tintColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        tab.label,
                                        fontSize = 10.sp,
                                        color = tintColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = "schedule",
            modifier = Modifier.padding(padding),
            // 丝滑页面过渡：淡入淡出 + 轻微横向推移（tab 与子页面共用）
            enterTransition = {
                fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 16 }
            },
            exitTransition = {
                fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 16 }
            },
            popEnterTransition = {
                fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 16 }
            },
            popExitTransition = {
                fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 16 }
            },
        ) {
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
