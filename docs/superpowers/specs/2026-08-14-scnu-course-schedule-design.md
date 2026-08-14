# 华师课程表 — Android App 设计文档

> 日期：2026-08-14
> 状态：已批准（含桌面小组件）
> 项目目录：`D:\dingding\schedule`

## 1. 目标与范围（MVP）

对标拾光课程表的**核心闭环**，跑通即止。面向华南师范大学学生。

### 1.1 功能清单（本次实现）

- **3 个主页面**（底部 tab，点击切换）：`今日` / `课表` / `我的`
- **课表页**：周分页（左右滑动看上一/下一周）、点击周标题弹「跳转周次」选择器、单双周课程、日期条（周几 + 月/日）、按作息表渲染、点空格新建课程、长按课程编辑/删除
- **今日页**：今日课程时间线 + 下一节课卡片
- **我的页**：主题切换（Claude 默认 / OpenCode TUI）、作息时间管理、教务导入入口
- **教务导入**：WebView 登录华师正方 → 抓取课表 → 解析 → 预览勾选 → 一键建课（自动激活石牌作息）
- **桌面小组件**（Jetpack Glance）：
  - 下一节课 2×1（课程名 + 地点 + 开始时间/倒计时）
  - 今日课程 4×2（今天所有课 + 节次时间段）
  - 周课表 4×3（迷你周网格，含单双周）
  - 主题跟随 App（Claude/OpenCode 各有对应皮肤）；课程变动广播刷新；下一节随时间 tick
- **存储**：Room（课程/作息/学期）+ DataStore（主题、激活作息、学期信息）

### 1.2 明确不做（后置）

课程提醒/勿扰自动开启、节假日数据、ICS/JSON 导入导出、多语言、全局课程管理大表（MVP 用课表页内编辑替代）、教务自动刷新。

### 1.3 技术栈

Kotlin + Jetpack Compose + Material3 · HorizontalPager（周分页）· Room · DataStore · Hilt · OkHttp/JSoup（教务抓取）· WebView（教务登录）· Jetpack Glance（小组件）· minSdk 26（Android 8.0）· compileSdk 34（本机 SDK 已有）

## 2. 架构（方案 A：单模块分层）

```
com.scnu.schedule/
  ScheduleApp / MainActivity     # Hilt；单 Activity + Compose Navigation
  ui/
    theme/          # 两套主题 token：ClaudeTheme + OpenCodeTheme
    navigation/     # 底部 3 tab 路由（今日/课表/我的）
    schedule/       # 课表页：WeekPager / 跳转周次 Sheet / 课程块 / 课程编辑
    today/          # 今日页
    settings/       # 设置页：主题/作息管理/教务入口
    jwxt/           # 教务 WebView 登录流 + 导入预览页
    widget/         # Glance 小组件 3 个
  data/
    db/             # Room：Course/TimeTable/Period/AppState 实体 + DAO
    prefs/          # DataStore：主题、激活作息、学期信息
    repository/     # 仓储实现
    jwxt/           # 正方网络会话 + ZhengFangParser
  domain/
    model/          # Course, TimeTable, Period, WeekPattern, Semester
    logic/          # WeekCalculator（周次/单双周/日期条）、重叠检测
    repo/           # 接口定义
```

要点：单模块、按包分职责；`domain` 不依赖 Android 框架，可单测；`data/jwxt` 与 `ui/jwxt` 分离，解析器可独立替换（对标拾光 shiguang_warehouse）。

## 3. 数据模型

| 实体 | 关键字段 | 说明 |
|---|---|---|
| **Course** | id, name, teacher, location, dayOfWeek(1-7), startPeriod, endPeriod, weekPatternId, colorIndex, timetableId | 一门课；start/endPeriod 对应作息表节次号 |
| **WeekPattern** | id, kind(ALL/ODD/EVEN/CUSTOM), rangeStart, rangeEnd, customWeeks | 上课周模式；`1-16` / `单周` / `双周` / 自定义周集合 |
| **TimeTable** | id, name, isDefault, active | 作息表；**默认内置「石牌校区」10 节** |
| **Period** | id, timetableId, periodIndex, startTime, endTime | 节次时段；可增删改 |
| **Semester** | id, name(2026秋), startDate, totalWeeks(20) | 周次计算基准 |

### 默认作息 = 华师石牌校区（官方文件）

| 节次 | 时段 | 节次 | 时段 |
|---|---|---|---|
| 1 | 8:30–9:10 | 6 | 15:20–16:00 |
| 2 | 9:20–10:00 | 7 | 16:10–16:50 |
| 3 | 10:20–11:00 | 8 | 17:00–17:40 |
| 4 | 11:10–11:50 | 9 | 19:00–19:40 |
| 5 | 14:30–15:10 | 10 | 19:50–20:30 |

可自由增删节次、修改时段；存为独立 TimeTable，设置页可新建多份。

### 周次计算（WeekCalculator）

- 当前周 = `floor((今天 − 学期开学日期) / 7) + 1`；当前周奇偶 = 单/双
- 某课程当前周是否上课：`weekPattern.contains(当前周)`
- 日期条：本周周一~周日真实日期，随周分页（偏移 ±n 周）联动

## 4. 主题系统

- `AppTheme(Claude | OpenCode)` 包装 MaterialTheme；每套主题定义：配色 / 字体 / 圆角 / 间距 + **课程色板**（课程块彩色左描边色序）
- 主题切换存 DataStore，即时生效；小组件同读该偏好

### Claude 奶油珊瑚（默认，依据 design-library/Claude DESIGN.md）

- 奶油底 `#faf9f5`、surface-card `#efe9de`、hairline `#e6dfd8`
- 主色珊瑚 `#cc785c`（active `#a9583e`），墨字 `#141413`，muted `#6c6a64`
- 语义：teal `#5db8a6`、amber `#e8a55a`、success `#5db872`、error `#c64545`
- 标题衬线（Noto Serif SC），正文人文无衬线（Noto Sans SC）；圆角 8/12/16/pill；阴影克制
- 课程块 = 奶油卡片 + 3px 彩色左描边；单/双周为细描边小标签

### OpenCode TUI（依据 design-library/opencode.ai DESIGN.md）

- 近黑终端底 `#201d1d`、elevated `#302c2c`、hairline `#3a3a38`、on-dark `#fdfcfc`、mute `#9a9898`
- 语义：success `#30d158`、accent `#007aff`、warning `#ff9f0a`、danger `#ff3b30`
- 全等宽字体（JetBrains Mono 近似 Berkeley Mono）；圆角 0–4px；无阴影
- 课程块 = 终端面板：1px 细边 + 左侧彩色描边；课程名带方括号 `[高数]`；教室灰色

## 5. 课表页交互

- 默认显示**当前周**；`HorizontalPager` 无限分页，左右滑动 ±1 周
- 顶栏周标题「第 3 周 ⌄」点击 → 底部弹层「跳转周次」：周次网格（W1~W20），当前周高亮，含学期总周数
- 日期条：`一 08/10 … 五 08/14`，**不写「今天」字样**；当前日仅下划线弱提示
- 无页点；周切换 = 左右滑动；页面切换 = 底部 tab 点击
- 单/双周课程在对应周显示，非本周课程不渲染；课程块显示课程名 + 教室 + 单/双标签

## 6. 教务导入（正方 WebView）

1. 设置页「教务导入」→ WebView 加载 `jwxt.scnu.edu.cn`（统一身份认证 SSO）
2. 用户在 WebView 内完成登录（验证码/滑块天然支持）
3. 登录成功 → CookieManager 共享会话 → 请求正方 `kbcx` 课表接口 → 拿 JSON
4. `ZhengFangParser` 解析为 Course 列表 → 预览页勾选 → 导入（自动建课程 + 激活石牌作息）

**风险预案**：正方 API 端点 / SSO 流程细节需真机实测；校外校园网限制（需 WebVPN）→ 分态错误提示。

## 7. 桌面小组件（Glance）

- **下一节课 2×1**：最近一节未开始的课；课程名 + 地点 + 开始时间；随系统时间刷新
- **今日课程 4×2**：今日全部课程列表（节次 + 名称 + 地点）
- **周课表 4×3**：迷你周网格；仅渲染当前周有课（含单双周过滤）
- 数据：Glance `provideGlance` 直读 Room + DataStore（主题）
- 刷新：课程/作息/学期变动 → 发广播更新；下一节组件按分钟 tick
- 主题：跟随 App 当前主题（Claude/OpenCode）；空态显示引导文案

## 8. 错误处理

- 教务导入：登录失败 / 验证码 / 断网 / 解析失败 → 分态提示 + 重试
- 课程重叠：保存时检测冲突并提示
- 空课表：引导页提示去教务导入或手动新建
- 小组件：数据缺失显示占位，不崩溃

## 9. 测试

- 单测（domain）：WeekCalculator（周次/单双周/日期条）、WeekPattern.contains、ZhengFangParser（样例 JSON）
- 测试：Room DAO、主题切换、Glance 渲染冒烟
