package com.scnu.schedule.ui.schedule

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
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
    var editing by remember { mutableStateOf<Course?>(null) }
    var creatingDay by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        // 顶栏：周标题 + 左右箭头（紧凑高度）
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("课表", fontSize = 16.sp, color = p.ink, modifier = Modifier.weight(1f))
            Text("◂", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            })
            Text("第 ${pagerState.currentPage + 1} 周", color = p.ink, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp).clickable { showJump = true })
            Text("▸", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            })
        }

        HorizontalPager(state = pagerState) { page ->
            // page index p → week = p + 1
            WeeklyGrid(state = state, week = page + 1, onEmptyClick = { day -> creatingDay = day }, onCourseClick = { course -> editing = course })
        }

        if (showJump) {
            WeekJumpSheet(
                totalWeeks = state.semester?.totalWeeks ?: 20,
                currentWeek = pagerState.currentPage + 1,
                startDate = state.semester?.startDate,
                onDismiss = { showJump = false },
                onJump = { week -> scope.launch { pagerState.scrollToPage(week - 1) }; showJump = false },
            )
        }

        if (editing != null || creatingDay != null) {
            val init = editing ?: Course(name = "", dayOfWeek = creatingDay ?: 1, startPeriod = 1, endPeriod = 2, weekPattern = WeekPattern(WeekKind.ALL))
            CourseEditDialog(
                initial = init,
                periodCount = state.timetable?.periodCount ?: 10,
                onDismiss = { editing = null; creatingDay = null },
                onSave = { vm.saveCourse(it) },
                onDelete = editing?.let { e -> { vm.deleteCourse(e.id) } },
            )
        }
    }
}

@Composable
private fun WeeklyGrid(state: ScheduleUiState, week: Int, onEmptyClick: (Int) -> Unit, onCourseClick: (Course) -> Unit) {
    val p = LocalAppPalette.current
    val rowHeight = 56
    // 本周日期 = weekDates(startDate, week - 1) —— 绝对周号，不是相对偏移
    val weekDates = state.semester?.let { WeekCalculator.weekDates(it.startDate, week - 1) }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).verticalScroll(rememberScrollState())) {
        // 日期条（周几 + 月日）：左侧预留 40dp 时间列占位，与下方课程空格格逐列对齐；
        // 「今天」的周几 + 日期用主题主色加粗高亮
        val today = LocalDate.now()
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(40.dp))
            weekdays.forEachIndexed { index, label ->
                val date = weekDates?.get(index)
                val isToday = date != null && date == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (isToday) p.primary else p.muted,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        date?.format(dateFormatter) ?: "",
                        fontSize = 11.sp,
                        color = if (isToday) p.primary else p.mutedSoft,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            // 每列宽 = (总宽 - 时间列 40dp) / 5
            val colWidth = (maxWidth - 40.dp) * 0.2f
            // 空格格（可点新建课程）
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.width(40.dp)) {
                    state.timetable?.periods?.forEach { period ->
                        // 每格三行：节次气泡（紧凑，仅比数字略大）+ 开始时间 + 结束时间
                        Column(
                            Modifier.height(rowHeight.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "${period.periodIndex}",
                                fontSize = 8.sp,
                                lineHeight = 8.sp,   // 行高压到与字号一致：气泡只包住数字本身，上下不再虚高
                                color = p.periodBadgeFg ?: p.onPrimary,
                                modifier = Modifier
                                    .background(p.periodBadgeBg ?: p.primary, RoundedCornerShape(p.periodBadgeRadius))
                                    .padding(horizontal = 3.dp),
                            )
                            Text(period.startLabel(), fontSize = 10.sp, color = p.muted,
                                modifier = Modifier.padding(top = 1.dp))
                            Text(period.endLabel(), fontSize = 10.sp, color = p.mutedSoft)
                        }
                    }
                }
                weekdays.forEachIndexed { index, _ ->
                    val day = index + 1
                    Column(Modifier.weight(1f)) {
                        state.timetable?.periods?.forEach { _ ->
                            Box(Modifier.height(rowHeight.dp).fillMaxWidth().padding(vertical = 1.dp)
                                .background(p.surfaceSoft, RoundedCornerShape(if (p.isDark) 2.dp else 8.dp))
                                .clickable { onEmptyClick(day) })
                        }
                    }
                }
            }
            // 顶层课程块层：绝对定位绘制在所有空格格之上（硬阴影在 CourseBlock 内绘制，
            // 因此阴影在米纸实底下方、空格格背景上方，与浏览器预览层级一致）；
            // 课程块交错淡入 + 轻微上移（丝滑入场）
            weekdays.forEachIndexed { index, _ ->
                val day = index + 1
                val dayCourses = WeekCalculator.coursesForWeek(state.courses.filter { it.dayOfWeek == day }, week)
                dayCourses.forEachIndexed { i, course ->
                    key("${course.id}:${course.name}:${course.dayOfWeek}:${course.startPeriod}:${course.endPeriod}") {
                          val color = p.coursePalette[course.colorIndex % p.coursePalette.size]
                    AnimatedCourseBlock(
                        course = course,
                        color = color,
                        rowHeight = rowHeight,
                        offsetX = 40.dp + colWidth * index,
                        offsetY = ((course.startPeriod - 1) * rowHeight).dp,
                        blockWidth = colWidth,
                        staggerMs = (index * 2 + i) * 40,
                        onClick = { onCourseClick(course) },
                    )
                      }
                }
            }
        }
    }
}

/** 课程块入场动画：淡入 + 轻微上移，按 staggerMs 交错出现（周切换丝滑感） */
@Composable
private fun AnimatedCourseBlock(
    course: Course,
    color: Color,
    rowHeight: Int,
    offsetX: Dp,
    offsetY: Dp,
    blockWidth: Dp,
    staggerMs: Int,
    onClick: () -> Unit,
) {
    var appear by remember(course.id) { mutableStateOf(false) }
    LaunchedEffect(course.id, course.startPeriod, course.endPeriod, course.name) {
        delay(staggerMs.toLong())
        appear = true
    }
    val progress = animateFloatAsState(
        targetValue = if (appear) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "courseBlockAppear",
    )
    Box(
        Modifier
            .offset(x = offsetX, y = offsetY)
            .width(blockWidth)
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 10.dp.toPx()
            },
    ) {
        CourseBlock(course, color, rowHeight, onClick)
    }
}
