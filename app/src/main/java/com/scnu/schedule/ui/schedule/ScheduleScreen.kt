package com.scnu.schedule.ui.schedule

import android.content.pm.ApplicationInfo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.domain.logic.WeekCalculator
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import com.scnu.schedule.ui.anim.FrameStatsChip
import com.scnu.schedule.ui.theme.LocalAppPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
private val weekdays = listOf("一", "二", "三", "四", "五")

/** 课程块交错入场：相邻块的启动间隔与单个块的动画时长。 */
private const val ENTRANCE_STAGGER_MS = 40
private const val ENTRANCE_ANIM_MS = 320

/** 入场动画总窗口：最后一个块的最长错峰 + 动画时长，之后页内切周直接显示终态。 */
private const val ENTRANCE_WINDOW_MS = 1_200L

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
    // 帧率指示器只在可调试构建出现：release 包不显示
    val appContext = LocalContext.current
    val isDebuggable = remember(appContext) {
        (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    // 入场动画只在「进入课表页」时播放一次：页内左右切周/跳周不再重播。
    // 状态放在页面级（而不是每个课程块内部）：切到今日/我的页时本页会被销毁，
    // remember 随之重置，因此下次回到课表页会重新播放一次入场动画。
    var entranceAnimationDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_WINDOW_MS)
        entranceAnimationDone = true
    }

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
            Spacer(Modifier.width(8.dp))
            if (isDebuggable) FrameStatsChip()
        }

        HorizontalPager(state = pagerState, beyondViewportPageCount = 1) { page ->
            // page index p → week = p + 1
            WeeklyGrid(
                state = state,
                week = page + 1,
                animateBlocks = !entranceAnimationDone,
                onEmptyClick = { day -> creatingDay = day },
                onCourseClick = { course -> editing = course },
            )
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
private fun WeeklyGrid(
    state: ScheduleUiState,
    week: Int,
    animateBlocks: Boolean,
    onEmptyClick: (Int) -> Unit,
    onCourseClick: (Course) -> Unit,
) {
    val p = LocalAppPalette.current
    val rowHeight = 56
    // 本周日期 = weekDates(startDate, week - 1) —— 绝对周号，不是相对偏移
    val weekDates = state.semester?.let { WeekCalculator.weekDates(it.startDate, week - 1) }
    // 每天本周课程：按 (课程表, 周次) 缓存，避免滑动/重组时对 5 天重复 filter + 单双周判断
    val coursesByDay = remember(state.courses, week) {
        (1..5).associateWith { day ->
            WeekCalculator.coursesForWeek(state.courses.filter { it.dayOfWeek == day }, week)
        }
    }

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
        // 课程格子区域：不再使用 BoxWithConstraints（SubcomposeLayout 每页都会跑一次子组合）。
        // 层级很关键：整片空格格作为**底层一次绘制**，课程块放进上层的透明星期列。
        // 若把格子画在各自列里，右边一列的格子会盖住左边列课程块的硬阴影，
        // 阴影只在格子间的 1dp 缝隙露出，看起来就是一排突起（老报刊亭主题尤其明显）。
        val periodCount = state.timetable?.periods?.size ?: 0
        val gridHeight = (rowHeight * periodCount).dp
        val cellRadius = if (p.isDark) 2.dp else 8.dp
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
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
            Box(Modifier.weight(1f).height(gridHeight)) {
                // 底层：5 列 × N 节的空格格，一次绘制 + 一个手势处理器
                // （代替 50 个可点击 Box，大幅减少组合节点；代价：空格格按下无涟漪反馈）
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val cellH = rowHeight.dp.toPx()
                            val gap = 1.dp.toPx()
                            val r = cellRadius.toPx()
                            val colW = size.width / weekdays.size
                            for (col in weekdays.indices) {
                                val x = col * colW
                                repeat(periodCount) { i ->
                                    drawRoundRect(
                                        color = p.surfaceSoft,
                                        topLeft = Offset(x, i * cellH + gap),
                                        size = Size(colW, cellH - gap * 2),
                                        cornerRadius = CornerRadius(r, r),
                                    )
                                }
                            }
                        }
                        .pointerInput(periodCount) {
                            val cellH = rowHeight.dp.toPx()
                            detectTapGestures { offset ->
                                val colW = size.width / weekdays.size.toFloat()
                                val day = (offset.x / colW).toInt() + 1
                                val period = (offset.y / cellH).toInt() + 1
                                if (day in 1..weekdays.size && period in 1..periodCount) onEmptyClick(day)
                            }
                        },
                )
                // 上层：透明的星期列，只承载课程块（硬阴影由 CourseBlock 自己绘制，
                // 上层的列没有背景，因此不会遮挡相邻列的阴影）
                Row(Modifier.fillMaxSize()) {
                    weekdays.forEachIndexed { index, _ ->
                        val day = index + 1
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            coursesByDay[day].orEmpty().forEachIndexed { i, course ->
                                key(course.id) {
                                    val color = p.coursePalette[course.colorIndex % p.coursePalette.size]
                                    AnimatedCourseBlock(
                                        course = course,
                                        color = color,
                                        rowHeight = rowHeight,
                                        offsetY = ((course.startPeriod - 1) * rowHeight).dp,
                                        staggerMs = (index * 2 + i) * ENTRANCE_STAGGER_MS,
                                        animate = animateBlocks,
                                        onClick = { onCourseClick(course) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课程块入场动画：淡入 + 轻微上移，按 staggerMs 交错出现。
 *
 * [animate] 为 false 时（进入课表页的入场窗口已过）直接以终态显示，
 * 因此页内左右切周、跳周都不会重播动画。
 */
@Composable
private fun AnimatedCourseBlock(
    course: Course,
    color: Color,
    rowHeight: Int,
    offsetY: Dp,
    staggerMs: Int,
    animate: Boolean,
    onClick: () -> Unit,
) {
    // 初始值取决于是否需要动画：不需要时直接就是终态，避免任何延迟与过渡
    var appear by remember(course.id) { mutableStateOf(!animate) }
    LaunchedEffect(course.id, animate) {
        if (!animate) {
            appear = true
        } else {
            delay(staggerMs.toLong())
            appear = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (appear) 1f else 0f,
        animationSpec = tween(ENTRANCE_ANIM_MS, easing = FastOutSlowInEasing),
        label = "courseBlockAppear",
    )
    Box(
        Modifier
            .offset(y = offsetY)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 10.dp.toPx()
            },
    ) {
        CourseBlock(course, color, rowHeight, onClick)
    }
}
