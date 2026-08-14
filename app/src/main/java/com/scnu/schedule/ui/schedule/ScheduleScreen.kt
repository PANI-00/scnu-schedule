package com.scnu.schedule.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
private val weekdays = listOf("一", "二", "三", "四", "五")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val p = LocalAppPalette.current
    val pagerState = rememberPagerState(initialPage = 0) { 99 }
    LaunchedEffect(state.initialWeek) {
        pagerState.scrollToPage(state.initialWeek - 1)
    }
    val scope = rememberCoroutineScope()
    var showJump by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // 顶栏：周标题 + 左右箭头
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("课表", fontSize = 16.sp, color = p.ink, modifier = Modifier.weight(1f))
            Text("◂", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            })
            Text("第 ${pagerState.currentPage + 1} 周 ⌄", color = p.ink, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp).clickable { showJump = true })
            Text("▸", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            })
        }
        Text("◂ 左右滑动切换周次 ▸", fontSize = 9.sp, color = p.mutedSoft,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp))

        HorizontalPager(state = pagerState) { page ->
            // page index p → week = p + 1
            WeeklyGrid(state = state, week = page + 1)
        }

        if (showJump) {
            WeekJumpSheet(
                totalWeeks = state.semester?.totalWeeks ?: 20,
                currentWeek = pagerState.currentPage + 1,
                onDismiss = { showJump = false },
                onJump = { week -> scope.launch { pagerState.scrollToPage(week - 1) }; showJump = false },
            )
        }
    }
}

@Composable
private fun WeeklyGrid(state: ScheduleUiState, week: Int) {
    val p = LocalAppPalette.current
    val rowHeight = 48
    // 本周日期 = weekDates(startDate, week - 1) —— 绝对周号，不是相对偏移
    val weekDates = state.semester?.let { WeekCalculator.weekDates(it.startDate, week - 1) }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).verticalScroll(rememberScrollState())) {
        // 日期条（周几 + 月日）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekdays.forEachIndexed { index, label ->
                val date = weekDates?.get(index)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 12.sp, color = p.muted)
                    Text(date?.format(dateFormatter) ?: "", fontSize = 9.sp, color = p.mutedSoft)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Column(Modifier.width(30.dp)) {
                state.timetable?.periods?.forEach { period ->
                    Text(period.startLabel(), fontSize = 8.sp, color = p.mutedSoft,
                        modifier = Modifier.height(rowHeight.dp))
                }
            }
            weekdays.forEachIndexed { index, _ ->
                val day = index + 1
                Column(Modifier.weight(1f)) {
                    val dayCourses = WeekCalculator.coursesForWeek(state.courses.filter { it.dayOfWeek == day }, week)
                    Box {
                        Column {
                            state.timetable?.periods?.forEach { _ ->
                                Box(Modifier.height(rowHeight.dp).fillMaxWidth().padding(vertical = 1.dp)
                                    .background(p.surfaceSoft, RoundedCornerShape(if (p.isDark) 2.dp else 8.dp)))
                            }
                        }
                        dayCourses.forEach { course ->
                            val color = p.coursePalette[course.colorIndex % p.coursePalette.size]
                            Box(Modifier.offset(y = ((course.startPeriod - 1) * rowHeight).dp).fillMaxWidth()) {
                                CourseBlock(course, color, rowHeight) { /* Task 13 编辑 */ }
                            }
                        }
                    }
                }
            }
        }
    }
}
