<p align="center">
  <img src="docs/images/scnu-schedule-icon.png" width="128" alt="SCNU Schedule 应用图标" style="vertical-align: middle;">&nbsp;&nbsp;<img src="docs/images/scnu-schedule-wordmark.png" width="360" alt="SCNU Schedule" style="vertical-align: middle; position: relative; top: -4px;">
</p>

<p align="center">
  面向华南师范大学学生的 Android 课程表应用
</p>

<p align="center">
  教务一键导课 · 周课表 / 今日视图 · 桌面小组件 · 三套可切换主题
</p>

<p align="center">
  <a href="https://github.com/PANI-00/scnu-schedule/releases/latest">Release 下载最新 APK</a>
</p>

## 功能

- 课表页：周分页左右滑动、跳转周次、单双周课程、点空格新建课程、长按编辑/删除
- 今日页：今日课程时间线 + 下一节课卡片
- 教务导入：WebView 登录华师正方教务系统，抓取当前学期课表，预览勾选后一键导入
- 作息管理：内置石牌/滨海、大学城/南海两套官方作息，可增删节次、新建作息
- 桌面小组件（Jetpack Glance）：
  - 下一节课 2x1（课程名 + 地点 + 倒计时）
  - 今日课程 4x2（今日课程列表）
  - 周课表 4x3（迷你周网格，含单双周过滤）
  - 下一节倒计时 2x2（日期周次 + 课程 + 地点 + 倒计时）
- 三套主题：奶油珊瑚、午夜机房、老报刊亭，即时切换，小组件同步换肤

## 技术栈

- Kotlin + Jetpack Compose + Material3
- Room（课程 / 作息 / 学期）+ DataStore（主题、学期、激活作息）
- Hilt 依赖注入
- OkHttp + WebView（教务抓取，复用登录会话）
- Jetpack Glance（桌面小组件）
- minSdk 26 / targetSdk 34
