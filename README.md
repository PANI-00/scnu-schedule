# scnu课程表

面向华南师范大学学生的 Android 课程表应用：教务系统一键导课、周课表 / 今日视图、桌面小组件，三套可切换主题。

## 功能

- 课表页：周分页左右滑动、跳转周次、单双周课程、点空格新建课程、长按编辑/删除
- 今日页：今日课程时间线 + 下一节课卡片
- 教务导入：WebView 登录华师正方教务系统，抓取当前学期课表，预览勾选后一键导入
- 作息管理：内置石牌/滨海、大学城/南海两套官方作息，可增删节次、新建作息
- 桌面小组件（Jetpack Glance）：
  - 下一节课 2x1（课程名 + 地点 + 倒计时）
  - 今日课程 4x2（今日课程列表）
  - 周课表 4x3（迷你周网格，含单双周过滤）
  - 下一节倒计时 2x2（精致卡片：日期周次 + 课程 + 地点 + 倒计时）
- 三套主题：奶油珊瑚、午夜机房、老报刊亭，即时切换，小组件同步换肤

## 技术栈

- Kotlin + Jetpack Compose + Material3
- Room（课程 / 作息 / 学期）+ DataStore（主题、学期、激活作息）
- Hilt 依赖注入
- OkHttp + WebView（教务抓取，复用登录会话）
- Jetpack Glance（桌面小组件）
- minSdk 26 / targetSdk 34

## 构建

```bash
# 单元测试
gradle :app:testDebugUnitTest

# 构建 Debug APK
gradle :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/scnu/schedule/
  ui/         主题 / 导航 / 课表 / 今日 / 设置 / 教务导入 / 小组件
  data/       Room 数据库 / DataStore / 仓储 / 正方教务客户端与解析器 / DI
  domain/     领域模型与纯逻辑（周次计算、重叠检测、导入解析接口）
```

`docs/` 下有设计文档与 UI 方案预览（浏览器直接打开 HTML 即可查看）。
