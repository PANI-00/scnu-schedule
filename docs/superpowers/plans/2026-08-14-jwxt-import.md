# 华师课程表 — Plan 2：教务导入（华师正方 / jwxt）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **前置依赖:** Plan 1（`2026-08-14-core-app.md`）已实现。本计划复用其 `domain/model`（Course/WeekPattern/TimeTable/Period/Semester）、`domain/repo` 接口、Room 实体、`data/repository` 实现、`data/prefs/SettingsDataStore`、`ui/theme` 与 `ui/navigation/ScheduleNavHost`。

**Goal:** 在华师课程表中加入「教务导入」：设置页入口 → WebView 内完成华师统一身份认证 SSO 登录（含验证码）→ 复用 WebView 会话 cookie 请求正方 `kbcx` 课表接口 → `ZhengFangParser` 解析为 `Course` 列表 → 预览勾选 → 一键导入（逐门 upsert 建课 + 自动激活「石牌校区」作息表 + 更新学期信息）。

**Architecture:** 与设计文档 §2/§6 一致：`domain` 层只放纯 Kotlin 的 `ImportResult` 与 `CourseParser` 接口（可单测）；`data/jwxt` 放网络会话与解析实现（`ZhengFangParser`、`ZhengFangClient`、`WebViewCookieJar`、`JwxtImportUseCase`）；`ui/jwxt` 放 WebView 登录屏与状态机（`JwxtLoginScreen` + `JwxtViewModel` + `JwxtUiState`）。登录走 WebView（SSO + 验证码天然支持），抓取走 OkHttp（通过 `WebViewCookieJar` 复用 `CookieManager` 里的会话 cookie），两端解耦、可独立替换。

**Tech Stack:** 沿用 Plan 1（Kotlin 2.0.21 · Compose BOM 2024.10.01 · Material3 · Room 2.6.1 · Hilt 2.52 · coroutines 1.9.0 · minSdk 26 · compileSdk 34 · AGP 8.7.3 · Gradle 8.11.1），新增依赖：**OkHttp 4.12.0**（网络）、**JSoup 1.17.2**（从教务页提取 csrftoken）、`okhttp-mockwebserver 4.12.0`（客户端测试）、`org.json:json 20240303`（JVM 单测里替代 android.jar 的 org.json 桩，否则 `JSONObject` 抛 "Method not mocked"）。

**构建环境事实（务必遵守，同 Plan 1）：**
- Gradle 不在 PATH，用：`export PATH="$PATH:/c/Users/huang/gradle-install/gradle-8.11.1/gradle-8.11.1/bin"`
- `local.properties` 的 `sdk.dir=C:\\Users\\huang\\Android\\Sdk`；JDK 21（Zulu，系统默认）
- 首次拉 OkHttp/JSoup/org.json 需联网；若 google()/mavenCentral() 超时，在 `settings.gradle.kts` 加阿里云镜像（见 Plan 1 Task 1 Step 13）
- 编译命令模板：
```bash
export PATH="$PATH:/c/Users/huang/gradle-install/gradle-8.11.1/gradle-8.11.1/bin"
cd /d/dingding/schedule
gradle :app:assembleDebug
gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.ZhengFangParserTest"
```

**参考源码：** `D:\dingding\shiguangschedule-ref`（拾光 Apache-2.0）。研究结论：
- 拾光的学校适配器**以 JS 形式在 WebView 内运行**（登录后 `evaluateJavascript` 注入适配器脚本，JS 用 XHR 抓课表，再经 `AndroidBridge.saveImportedCourses` 把归一化课程回传 Kotlin）。适配器 JS 是**按学校运行时下载的**，仓库内不包含华师/正方字段解析；`app/src/main/assets/offline_repo/schools/resources/GLOBAL_TOOLS/school.js` 只是桥接 demo（`{name, teacher, position, day, startSection, endSection, weeks[]}` 的课程契约）。
- 归一化课程契约可借鉴：**一行 kbcx 记录 = 一个 Course**（同课程不同节次/不同单双周拆成多条，demo 里「数据结构」出现 5 条即此意）。
- 本计划采用设计文档方案（WebView 登录 + CookieManager 复用 + OkHttp 抓取 + Kotlin 解析），比 JS 适配器更可控、可单测；解析出的字段以**标准正方 `kbcx` 字段**为准（见下方字段字典），全部标 `[VERIFY]`。

> ⚠️ **显著注意事项（端到端验证）**
> 本计划的**自动化测试只覆盖解析器（fixture 驱动）、纯逻辑（登录检测/学期推算）与客户端请求形状（MockWebServer）**。
> 完整链路（WebView 登录 → Cookie 复用 → 抓取 → 解析 → 预览 → 导入）**必须用真实华师一卡通账号 + 校园网（或 WebVPN）在真机上手工验证**。
> 计划中所有 `[VERIFY]` 项（端点路径、表单字段、csrftoken、JSON 字段名、会话 cookie、学期码、周次格式）都依赖这次真机核对，核完把结果反馈回来修正解析器与客户端。

---

## 字段字典（正方 kbcx 接口，标准约定，`[VERIFY]` 待华师真机核对）

端点：`POST https://jwxt.scnu.edu.cn/kbcx/xskbcx_cxXsgrkb.html`（form body：`xnm`=学年、`xqm`=学期码、`kzlx=1`、`xsdm=0`）。返回 `{"kbList":[{...}]}`。

| 字段 | 含义 | 解析到 | 备注 |
|---|---|---|---|
| `kcmc` | 课程名称 | `Course.name` | 缺失时 fallback `jxbmc`（教学班名称） |
| `teaxms` / `xm` / `jsxm` | 教师姓名 | `Course.teacher` | 三个候选取第一个非空 |
| `cdmc` / `ddxx` / `xqmc` | 上课地点 | `Course.location` | 候选按序取非空 |
| `xqj` | 星期几 | `Course.dayOfWeek` | `"1".."7"` 或 `"星期一"/"周三"` |
| `jcs` | 节次 | `startPeriod/endPeriod` | `"1-2"`、`"3"`、`"第1-2节"` |
| `jcjs` | 结束节次（可选） | `endPeriod` | 个别版本与 `jcs` 拆开 |
| `zs` / `zcs` / `zcsm` | 周次字符串 | `WeekPattern` | `"1-16周"`、`"1-16周(单)"`、`"1,3,5,9周"`、`"第1-8周,10-16周"` |
| `zcjs` | 结束周（可选） | `WeekPattern.rangeEnd` | 个别版本 `zs` 只有起始周 |

---

## File Structure

```
D:\dingding\schedule\
  gradle/libs.versions.toml                        [修改] + okhttp/jsoup/mockwebserver/json
  app/build.gradle.kts                             [修改] + 依赖
  app/src/main/AndroidManifest.xml                 [修改] + INTERNET 权限
  app/src/main/java/com/scnu/schedule/
    domain/import/ImportResult.kt                  [新建] ImportWarning + ImportResult（纯 Kotlin）
    domain/import/CourseParser.kt                  [新建] 解析器接口（纯 Kotlin）
    data/jwxt/JwxtExceptions.kt                    [新建] JwxtException 族
    data/jwxt/ZhengFangParser.kt                   [新建] 正方 kbList JSON → Course
    data/jwxt/WebViewCookieJar.kt                  [新建] CookieManager ↔ OkHttp 桥
    data/jwxt/ZhengFangClient.kt                   [新建] kbcx 抓取客户端（复用会话 cookie）
    data/jwxt/JwxtLoginDetector.kt                 [新建] 登录态判定（纯 Kotlin）
    data/jwxt/JwxtSemesterResolver.kt              [新建] 学期 xnm/xqm 推算（纯 Kotlin）
    data/jwxt/JwxtImportUseCase.kt                 [新建] 导入落库（upsert + 激活作息 + 学期）
    data/di/JwxtModule.kt                          [新建] Hilt 绑定
    ui/jwxt/JwxtUiState.kt                         [新建] 状态机 + 错误类型
    ui/jwxt/JwxtViewModel.kt                       [新建] fetch/import/错误编排
    ui/jwxt/JwxtLoginScreen.kt                     [新建] WebView 登录页 + 预览勾选 + 确认
    ui/settings/SettingsScreen.kt                  [修改] 教务导入入口
    ui/navigation/ScheduleNavHost.kt               [修改] jwxt_import 路由
  app/src/test/java/com/scnu/schedule/
    data/jwxt/TestFixtures.kt                      [新建] 读 fixture 的工具
    data/jwxt/InMemoryCookieJar.kt                 [新建] 测试用内存 cookie
    data/jwxt/ZhengFangParserTest.kt               [新建] 解析器 TDD
    data/jwxt/ZhengFangClientTest.kt               [新建] MockWebServer 客户端测试
    data/jwxt/JwxtLoginDetectorTest.kt             [新建] 登录判定单测
    data/jwxt/JwxtSemesterResolverTest.kt          [新建] 学期推算单测
    data/jwxt/JwxtImportUseCaseTest.kt             [新建] 落库逻辑单测（假仓储）
  app/src/test/resources/jwxt/kbList_sample.json   [新建] 正方响应 fixture
```

> 任务排序说明：原拟「解析器 TDD → 客户端 → 登录屏 → 导入流程 → 错误处理/接线」。因 `ZhengFangParser` 编译必须依赖 `ImportResult/CourseParser`，且登录屏引用 ViewModel、ViewModel 引用客户端与用例，本计划按依赖顺序重排为 7 个任务（每任务结束项目可编译、可测）。控制器可在此基础上细化。

---

### Task 1: 依赖 + fixture + 领域类型 + ZhengFangParser（TDD）

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/resources/jwxt/kbList_sample.json`
- Create: `app/src/main/java/com/scnu/schedule/domain/import/ImportResult.kt`
- Create: `app/src/main/java/com/scnu/schedule/domain/import/CourseParser.kt`
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/JwxtExceptions.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/TestFixtures.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/ZhengFangParserTest.kt`
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/ZhengFangParser.kt`

- [x] **Step 1: 版本目录加依赖** `gradle/libs.versions.toml`

在 `[versions]` 追加：
```toml
okhttp = "4.12.0"
jsoup = "1.17.2"
json = "20240303"          # org.json:json，JVM 单测里替代 android.jar 的 org.json 桩
```

在 `[libraries]` 追加：
```toml
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }
json = { group = "org.json", name = "json", version.ref = "json" }
```

- [x] **Step 2: app 依赖** `app/build.gradle.kts` — 在 dependencies 追加

```kotlin
implementation(libs.okhttp)
implementation(libs.jsoup)

testImplementation(libs.okhttp.mockwebserver)
testImplementation(libs.json)
```

> 注意：JVM 单测里若不引入 `org.json:json`，`JSONObject` 会命中 android.jar 桩的 "Method not mocked" 异常，解析器单测必挂。

- [x] **Step 3: 写 fixture** `app/src/test/resources/jwxt/kbList_sample.json`

覆盖：连续周 / 单周 / 双周 / 逗号自定义周 / 分段区间周 / 单节次 / 教师字段变体（teaxms|jsxm|xm）/ 地点变体（cdmc|ddxx）/ 星期变体（数字|中文）/ 缺字段行（应产生 warning）。

```json
{
  "kbList": [
    {
      "kcmc": "高等数学A（1）",
      "teaxms": "王明",
      "cdmc": "第一教学楼101",
      "xqj": "1",
      "jcs": "1-2",
      "zs": "1-16周",
      "xkbz": "1"
    },
    {
      "kcmc": "大学英语（二）",
      "jsxm": "李华",
      "ddxx": "文科楼203",
      "xqj": "星期一",
      "jcs": "3-4",
      "zs": "1-16周(单)",
      "xkbz": "1"
    },
    {
      "kcmc": "线性代数",
      "xm": "张伟",
      "cdmc": "理科楼304",
      "xqj": "2",
      "jcs": "5-6",
      "zs": "1-16周(双)",
      "xkbz": "1"
    },
    {
      "kcmc": "大学物理实验",
      "teaxms": "陈静",
      "cdmc": "实验楼A401",
      "xqj": "周三",
      "jcs": "7",
      "zs": "1,3,5,7,9,11,13,15周",
      "xkbz": "1"
    },
    {
      "kcmc": "形势与政策",
      "teaxms": "刘芳",
      "cdmc": "第一教学楼201",
      "xqj": "5",
      "jcs": "9-10",
      "zs": "第1-8周,10-16周",
      "xkbz": "1"
    },
    {
      "kcmc": "缺失星期字段课程",
      "teaxms": "赵某",
      "cdmc": "某教室",
      "xqj": "",
      "jcs": "1-2",
      "zs": "1-16周",
      "xkbz": "1"
    }
  ]
}
```

- [x] **Step 4: 写领域类型** `domain/import/ImportResult.kt`

```kotlin
package com.scnu.schedule.domain.import

import com.scnu.schedule.domain.model.Course

/** 单条无法解析的记录提示（行级容错，不阻塞整体导入）。 */
data class ImportWarning(
    val courseName: String,
    val message: String,
)

/** 解析产物：成功行 → courses；失败行 → warnings。 */
data class ImportResult(
    val courses: List<Course>,
    val warnings: List<ImportWarning> = emptyList(),
)
```

- [x] **Step 5: 写解析器接口** `domain/import/CourseParser.kt`

```kotlin
package com.scnu.schedule.domain.import

/** 课程源解析器接口：输入原始字符串（JSON/HTML…），输出可导入的课程与警告。 */
interface CourseParser {
    fun parse(raw: String): ImportResult
}
```

- [x] **Step 6: 写异常族** `data/jwxt/JwxtExceptions.kt`

```kotlin
package com.scnu.schedule.data.jwxt

/** 教务导入统一异常基类，便于上层按类分派错误态。 */
open class JwxtException(message: String, cause: Throwable? = null) : Exception(message, cause)

class JwxtLoginExpiredException(message: String) : JwxtException(message)

class JwxtNetworkException(message: String, cause: Throwable? = null) : JwxtException(message, cause)

class JwxtParseException(message: String, cause: Throwable? = null) : JwxtException(message, cause)
```

- [x] **Step 7: 写失败测试（RED）** `ZhengFangParserTest.kt` + `TestFixtures.kt`

`TestFixtures.kt`：
```kotlin
package com.scnu.schedule.data.jwxt

/** 读取 src/test/resources/jwxt/ 下的 fixture，供多个测试共享。 */
object TestFixtures {
    fun read(name: String): String {
        val stream = TestFixtures::class.java.getResourceAsStream("/jwxt/$name")
            ?: throw AssertionError("fixture 不存在: /jwxt/$name")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    val KB_LIST_SAMPLE: String by lazy { read("kbList_sample.json") }
}
```

`ZhengFangParserTest.kt`：
```kotlin
package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZhengFangParserTest {

    private val parser = ZhengFangParser()

    @Test
    fun `解析标准 kbList fixture`() {
        val result = parser.parse(TestFixtures.KB_LIST_SAMPLE)
        assertEquals(5, result.courses.size)
        assertEquals(1, result.warnings.size)

        val gs = result.courses[0]
        assertEquals("高等数学A（1）", gs.name)
        assertEquals("王明", gs.teacher)
        assertEquals("第一教学楼101", gs.location)
        assertEquals(1, gs.dayOfWeek)
        assertEquals(1, gs.startPeriod)
        assertEquals(2, gs.endPeriod)
        assertEquals(WeekKind.ALL, gs.weekPattern.kind)
        assertEquals(1, gs.weekPattern.rangeStart)
        assertEquals(16, gs.weekPattern.rangeEnd)

        val en = result.courses[1]
        assertEquals("大学英语（二）", en.name)
        assertEquals("李华", en.teacher)
        assertEquals("文科楼203", en.location)
        assertEquals(1, en.dayOfWeek)
        assertEquals(WeekKind.ODD, en.weekPattern.kind)
        assertTrue(en.weekPattern.contains(1))
        assertTrue(!en.weekPattern.contains(2))

        val la = result.courses[2]
        assertEquals(WeekKind.EVEN, la.weekPattern.kind)
        assertTrue(la.weekPattern.contains(2))
        assertTrue(!la.weekPattern.contains(1))

        val phy = result.courses[3]
        assertEquals(3, phy.dayOfWeek)
        assertEquals(7, phy.startPeriod)
        assertEquals(7, phy.endPeriod)
        assertEquals(WeekKind.CUSTOM, phy.weekPattern.kind)
        assertEquals(setOf(1, 3, 5, 7, 9, 11, 13, 15), phy.weekPattern.customWeeks)

        val sx = result.courses[4]
        assertEquals(5, sx.dayOfWeek)
        assertEquals(9, sx.startPeriod)
        assertEquals(10, sx.endPeriod)
        assertEquals(WeekKind.CUSTOM, sx.weekPattern.kind)
        assertEquals((1..8).toSet() + (10..16).toSet(), sx.weekPattern.customWeeks)

        assertEquals("缺失星期字段课程", result.warnings[0].courseName)
    }

    @Test
    fun `空 kbList 返回空结果`() {
        val result = parser.parse("""{"kbList":[]}""")
        assertTrue(result.courses.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test(expected = JwxtParseException::class)
    fun `HTML 登录页抛出解析异常`() {
        parser.parse("<html><head><title>登录</title></head></html>")
    }

    @Test(expected = JwxtParseException::class)
    fun `非法 JSON 抛出解析异常`() {
        parser.parse("not json at all")
    }

    @Test
    fun `周次字符串变体`() {
        assertTrue(parseWeeksOnly("第1-16周")!!.let { it.kind == WeekKind.ALL && it.rangeEnd == 16 })
        assertTrue(parseWeeksOnly("1-16周(单周)")!!.kind == WeekKind.ODD)
        assertTrue(parseWeeksOnly("1-16周{双}")!!.kind == WeekKind.EVEN)
        assertTrue(parseWeeksOnly("1-8周,10-16周")!!.kind == WeekKind.CUSTOM)
        assertTrue(parseWeeksOnly("1,3,5,7,9,11周")!!.let {
            it.kind == WeekKind.CUSTOM && it.customWeeks == setOf(1, 3, 5, 7, 9, 11)
        })
        assertTrue(parseWeeksOnly("3周")!!.let { it.kind == WeekKind.ALL && it.rangeStart == 3 && it.rangeEnd == 3 })
    }

    private fun parseWeeksOnly(zs: String): WeekPattern? {
        val json = """{"kbList":[{"kcmc":"X","xqj":"1","jcs":"1","zs":"$zs"}]}"""
        return parser.parse(json).courses.firstOrNull()?.weekPattern
    }

    @Test
    fun `星期字段变体`() {
        assertEquals(1, parseDayOnly("1"))
        assertEquals(1, parseDayOnly("星期一"))
        assertEquals(3, parseDayOnly("周三"))
        assertEquals(7, parseDayOnly("星期日"))
        assertEquals(7, parseDayOnly("7"))
        assertNull(parseDayOnly(""))
    }

    private fun parseDayOnly(xqj: String): Int? {
        val json = """{"kbList":[{"kcmc":"X","xqj":"$xqj","jcs":"1","zs":"1-16周"}]}"""
        return parser.parse(json).courses.firstOrNull()?.dayOfWeek
    }

    @Test
    fun `节次字段变体`() {
        val c1 = parser.parse("""{"kbList":[{"kcmc":"X","xqj":"1","jcs":"第3-4节","zs":"1-16周"}]}""").courses.first()
        assertEquals(3, c1.startPeriod)
        assertEquals(4, c1.endPeriod)

        val c2 = parser.parse("""{"kbList":[{"kcmc":"X","xqj":"1","jcs":"5","jcjs":"6","zs":"1-16周"}]}""").courses.first()
        assertEquals(5, c2.startPeriod)
        assertEquals(6, c2.endPeriod)
    }
}
```

- [x] **Step 8: 运行确认失败（RED）**

Run: `gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.ZhengFangParserTest"`
Expected: 编译失败（`ZhengFangParser` 未定义）。

- [x] **Step 9: 写实现** `data/jwxt/ZhengFangParser.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.import.CourseParser
import com.scnu.schedule.domain.import.ImportResult
import com.scnu.schedule.domain.import.ImportWarning
import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import javax.inject.Inject
import org.json.JSONObject

/**
 * 正方教务 kbcx 课表 JSON 解析器。
 *
 * 端点 kbcx/xskbcx_cxXsgrkb.html 返回 `{"kbList":[{...}]}`。
 * 字段字典见计划头；[VERIFY] 真机核对后如有出入，只改本类与 fixture 即可。
 */
class ZhengFangParser @Inject constructor() : CourseParser {

    override fun parse(raw: String): ImportResult {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            // 登录页 HTML / 错误页 / 乱码 → 视为解析失败，由上层提示重新登录
            throw JwxtParseException("教务响应不是合法 JSON（可能返回了登录页或错误页）", e)
        }
        val kbList = root.optJSONArray("kbList")
        if (kbList == null) {
            return ImportResult(emptyList(), listOf(ImportWarning("", "响应缺少 kbList 字段，请检查接口是否变化")))
        }
        val courses = mutableListOf<Course>()
        val warnings = mutableListOf<ImportWarning>()
        for (i in 0 until kbList.length()) {
            val item = kbList.optJSONObject(i) ?: continue
            parseOne(item, i, warnings)?.let { courses += it }
        }
        return ImportResult(courses, warnings)
    }

    private fun parseOne(item: JSONObject, index: Int, warnings: MutableList<ImportWarning>): Course? {
        val rowLabel = "第 ${index + 1} 行"
        val name = firstNonBlank(item, "kcmc", "jxbmc")
        if (name == null) {
            warnings += ImportWarning(rowLabel, "缺少课程名称(kcmc)")
            return null
        }
        val teacher = firstNonBlank(item, "teaxms", "xm", "jsxm").orEmpty()
        val location = firstNonBlank(item, "cdmc", "ddxx", "xqmc").orEmpty()

        val day = parseDay(item.optString("xqj"), name, warnings)
        val (start, end) = parsePeriods(item.optString("jcs"), item.optString("jcjs"), name, warnings)
        val week = parseWeeks(firstNonBlank(item, "zs", "zcs", "zcsm"), item.optString("zcjs"), name, warnings)

        if (day == null || start == null || end == null || week == null) return null
        return Course(
            name = name,
            teacher = teacher,
            location = location,
            dayOfWeek = day,
            startPeriod = start,
            endPeriod = end,
            weekPattern = week,
        )
    }

    private fun firstNonBlank(item: JSONObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> item.optString(key).trim().takeIf { it.isNotEmpty() } }

    private fun parseDay(raw: String, name: String, warnings: MutableList<ImportWarning>): Int? {
        val t = raw.trim()
        if (t.isEmpty()) {
            warnings += ImportWarning(name, "缺少星期字段(xqj)")
            return null
        }
        t.toIntOrNull()?.let { n ->
            return if (n in 1..7) n else {
                warnings += ImportWarning(name, "星期数值越界: $t")
                null
            }
        }
        Regex("[星期周]([一二三四五六日天])").find(t)?.let { m ->
            return chineseDay(m.groupValues[1])
        }
        chineseDay(t)?.let { return it }
        warnings += ImportWarning(name, "无法解析星期字段: $t")
        return null
    }

    private fun chineseDay(s: String): Int? = when (s) {
        "一" -> 1
        "二" -> 2
        "三" -> 3
        "四" -> 4
        "五" -> 5
        "六" -> 6
        "日", "天" -> 7
        else -> null
    }

    private fun parsePeriods(
        jcsRaw: String,
        jcjsRaw: String,
        name: String,
        warnings: MutableList<ImportWarning>,
    ): Pair<Int?, Int?> {
        val text = jcsRaw.trim().removePrefix("第").removeSuffix("节").trim()
        val nums = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        val start = nums.firstOrNull()
        if (start == null) {
            warnings += ImportWarning(name, "无法解析节次: $jcsRaw")
            return null to null
        }
        val end = nums.getOrNull(1) ?: jcjsRaw.trim().toIntOrNull() ?: start
        return start to end.coerceAtLeast(start)
    }

    private fun parseWeeks(
        zsRaw: String?,
        zcjsRaw: String,
        name: String,
        warnings: MutableList<ImportWarning>,
    ): WeekPattern? {
        val text = zsRaw?.trim().orEmpty()
        if (text.isEmpty()) {
            warnings += ImportWarning(name, "缺少周次字段(zs/zcs/zcsm)")
            return null
        }
        val weeks = extractWeeks(text)
        if (weeks.isEmpty()) {
            zcjsRaw.trim().toIntOrNull()?.let { end -> if (end in 1..60) for (w in 1..end) weeks.add(w) }
        }
        if (weeks.isEmpty()) {
            warnings += ImportWarning(name, "无法解析周次: $text")
            return null
        }
        val min = weeks.minOrNull() ?: return null
        val max = weeks.maxOrNull() ?: return null
        val isOdd = text.contains("单")
        val isEven = text.contains("双")
        return when {
            isOdd -> WeekPattern(WeekKind.ODD, min, max)
            isEven -> WeekPattern(WeekKind.EVEN, min, max)
            weeks.size == max - min + 1 -> WeekPattern(WeekKind.ALL, min, max)
            else -> WeekPattern(WeekKind.CUSTOM, customWeeks = weeks)
        }
    }

    private fun extractWeeks(text: String): LinkedHashSet<Int> {
        val set = LinkedHashSet<Int>()
        var remaining = text
        Regex("(\\d+)\\s*[-~至]\\s*(\\d+)").findAll(text).forEach { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            if (a <= b) for (w in a..b) if (w in 1..60) set.add(w)
            remaining = remaining.replace(m.value, " ")
        }
        Regex("\\d+").findAll(remaining).forEach { m ->
            m.value.toInt().takeIf { it in 1..60 }?.let { set.add(it) }
        }
        return set
    }
}
```

- [x] **Step 10: 运行确认通过（GREEN）**

Run: `gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.ZhengFangParserTest"`
Expected: PASS（9 个测试全绿）。

- [x] **Step 11: Commit**

```bash
git add -A && git commit -m "feat(jwxt): ZhengFangParser 解析正方 kbList + 领域 ImportResult/CourseParser + fixture 测试"
```

---

### Task 2: ZhengFangClient（复用 WebView 会话抓 kbcx）

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/WebViewCookieJar.kt`
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/ZhengFangClient.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/InMemoryCookieJar.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/ZhengFangClientTest.kt`

- [ ] **Step 1: Manifest 加 INTERNET 权限** `app/src/main/AndroidManifest.xml`

在 `<manifest>` 根下、`<application>` 之前加：
```xml
    <uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 2: 写 Cookie 桥** `data/jwxt/WebViewCookieJar.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 桥接 WebView 的 CookieManager 与 OkHttp：
 * OkHttp 请求自动带上 WebView 登录后写入的会话 cookie（JSESSIONID 等），
 * OkHttp 收到的 Set-Cookie 也同步回 CookieManager，保持两端一致。
 */
class WebViewCookieJar : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val manager = CookieManager.getInstance()
        cookies.forEach { cookie ->
            manager.setCookie(url.toString(), cookie.toString(), false)
        }
        manager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = CookieManager.getInstance().getCookie(url.toString())
            ?: return emptyList()
        return header.split(";").mapNotNull { piece ->
            val idx = piece.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val name = piece.substring(0, idx).trim()
            val value = piece.substring(idx + 1).trim()
            runCatching {
                Cookie.Builder()
                    .domain(url.host)
                    .path("/")
                    .name(name)
                    .value(value)
                    .build()
            }.getOrNull()
        }
    }
}
```

- [ ] **Step 3: 写客户端** `data/jwxt/ZhengFangClient.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.import.CourseParser
import com.scnu.schedule.domain.import.ImportResult
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * 正方教务 kbcx 课表接口客户端。
 *
 * 登录在 WebView（SSO + 验证码）完成，本客户端通过 WebViewCookieJar 复用其会话，
 * 直接 POST kbcx 接口拿 JSON 交给 CourseParser。
 *
 * [VERIFY] 以下待真机核对：
 *  - 端点路径 kbcx/xskbcx_cxXsgrkb.html
 *  - 表单字段 xnm/xqm/kzlx/xsdm
 *  - 是否要求 csrftoken 头，以及 token 在首页的提取位置（本实现兼容 hidden input / JS 变量两种）
 */
class ZhengFangClient(
    private val parser: CourseParser,
    private val cookieJar: CookieJar,
    private val baseUrl: String = JWXT_BASE,
) {
    private val base = baseUrl.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .build()

    /** 抓取并解析指定学年/学期的课表。 */
    suspend fun fetchSchedule(xnm: String, xqm: String): ImportResult =
        withContext(Dispatchers.IO) {
            val csrf = fetchCsrfToken()
            val form = FormBody.Builder()
                .add("xnm", xnm)
                .add("xqm", xqm)
                .add("kzlx", "1")
                .add("xsdm", "0")
                .build()
            val request = Request.Builder()
                .url("$base/kbcx/xskbcx_cxXsgrkb.html")
                .header("X-Requested-With", "XMLHttpRequest")
                .apply { csrf?.let { header("csrftoken", it) } }
                .post(form)
                .build()
            try {
                client.newCall(request).execute().use { resp ->
                    when {
                        resp.code == 302 || resp.code == 401 || resp.code == 403 ->
                            throw JwxtLoginExpiredException("登录已失效（HTTP ${resp.code}），请重新登录")
                        !resp.isSuccessful ->
                            throw JwxtNetworkException("教务接口 HTTP ${resp.code}")
                        else -> {
                            val raw = resp.body?.string() ?: throw JwxtParseException("教务接口返回空响应体")
                            parser.parse(raw)
                        }
                    }
                }
            } catch (e: IOException) {
                throw JwxtNetworkException("网络请求失败：${e.message}", e)
            }
        }

    /**
     * 从教务首页提取 csrftoken（新版正方把 token 写进页面 JS 变量或 hidden input）。
     * 若华师不需要该头，此函数返回 null 时请求会跳过 csrftoken 头。
     */
    private fun fetchCsrfToken(): String? = runCatching {
        val request = Request.Builder().url(base).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val html = resp.body?.string() ?: return null
            Jsoup.parse(html).selectFirst("input[name=csrftoken]")?.attr("value")
                ?.takeIf { it.isNotBlank() }
                ?: Regex("csrftoken\\s*=\\s*['\"]([0-9a-fA-F]{32,})['\"]")
                    .find(html)?.groupValues?.get(1)
        }
    }.getOrNull()

    companion object {
        const val JWXT_BASE = "https://jwxt.scnu.edu.cn"
    }
}
```

- [ ] **Step 4: 写测试辅助** `data/jwxt/InMemoryCookieJar.kt`（test source）

```kotlin
package com.scnu.schedule.data.jwxt

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** 测试用内存 cookie 存储（替代 android WebViewCookieJar）。 */
class InMemoryCookieJar : CookieJar {
    private val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store += cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store.filter { c ->
            url.host == c.domain.removePrefix(".") && url.encodedPath.startsWith(c.path)
        }
}
```

- [ ] **Step 5: 写客户端测试** `data/jwxt/ZhengFangClientTest.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZhengFangClientTest {

    @Test
    fun `POST kbcx 接口并解析 kbList`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody("""<html><script>var csrftoken = 'a1b2c3d4e5f60718293a4b5c6d7e8f90';</script></html>"""))
        server.enqueue(MockResponse().setBody(TestFixtures.KB_LIST_SAMPLE))

        val client = ZhengFangClient(
            parser = ZhengFangParser(),
            cookieJar = InMemoryCookieJar(),
            baseUrl = server.url("/").toString().removeSuffix("/"),
        )

        val result = client.fetchSchedule("2026", "3")

        server.takeRequest() // csrftoken 首页 GET
        val kbcxRequest = server.takeRequest()
        assertEquals("/kbcx/xskbcx_cxXsgrkb.html", kbcxRequest.path)
        assertEquals("XMLHttpRequest", kbcxRequest.getHeader("X-Requested-With"))
        assertEquals("a1b2c3d4e5f60718293a4b5c6d7e8f90", kbcxRequest.getHeader("csrftoken"))
        val body = kbcxRequest.body.readUtf8()
        assertTrue(body.contains("xnm=2026"))
        assertTrue(body.contains("xqm=3"))

        assertEquals(5, result.courses.size)
        server.shutdown()
    }

    @Test
    fun `HTTP 401 抛出登录失效异常`() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody("<html>home</html>"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val client = ZhengFangClient(
            parser = ZhengFangParser(),
            cookieJar = InMemoryCookieJar(),
            baseUrl = server.url("/").toString().removeSuffix("/"),
        )

        val thrown = runCatching { client.fetchSchedule("2026", "3") }.exceptionOrNull()
        assertTrue(thrown is JwxtLoginExpiredException)
        server.shutdown()
    }
}
```

- [ ] **Step 6: 运行验证**

Run:
```bash
gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.ZhengFangClientTest"
gradle :app:assembleDebug
```
Expected: 2 个测试 PASS；`BUILD SUCCESSFUL`。

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(jwxt): ZhengFangClient 复用 WebView 会话抓 kbcx + MockWebServer 测试 + INTERNET 权限"
```

---

### Task 3: 登录检测 + 学期推算（纯逻辑 TDD）

**Files:**
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/JwxtLoginDetector.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/JwxtLoginDetectorTest.kt`
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/JwxtSemesterResolver.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/JwxtSemesterResolverTest.kt`

- [ ] **Step 1: 写登录检测** `data/jwxt/JwxtLoginDetector.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import java.net.URI

/**
 * 判断 WebView 当前是否已完成教务登录：
 * 页面位于 jwxt.scnu.edu.cn 域、不是登录页，且 CookieManager 里带会话 cookie。
 * [VERIFY] 华师 SSO 回跳后的落地页 URL 与会话 cookie 名称需真机核对。
 */
object JwxtLoginDetector {

    /** 会话 cookie 名称候选（大写），正方新平台为 JSESSIONID，其余兜底。 */
    private val sessionCookieNames: Set<String> =
        setOf("JSESSIONID", "SESSIONID", "MOD_AUTH_CAS", "CONNECT.SID")

    fun isLoggedIn(url: String?, cookieHeader: String?): Boolean {
        if (url.isNullOrBlank() || cookieHeader.isNullOrBlank()) return false
        val host = runCatching { URI(url).host }.getOrNull() ?: return false
        if (!host.endsWith("jwxt.scnu.edu.cn")) return false
        if (isLoginPage(url)) return false
        val names = cookieHeader.split(";").map { it.trim().substringBefore('=').uppercase() }
        return names.any { it in sessionCookieNames }
    }

    fun isLoginPage(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("login") || lower.contains("sso") || lower.contains("authserver")
    }
}
```

- [ ] **Step 2: 写测试** `JwxtLoginDetectorTest.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JwxtLoginDetectorTest {

    @Test
    fun `登录后 jwxt 落地页判定已登录`() {
        assertTrue(
            JwxtLoginDetector.isLoggedIn(
                "https://jwxt.scnu.edu.cn/xtgl/index_initMenu.html",
                "JSESSIONID=ABC123; route=xyz",
            ),
        )
    }

    @Test
    fun `正方登录页即使有 cookie 也判定未登录`() {
        assertFalse(
            JwxtLoginDetector.isLoggedIn(
                "https://jwxt.scnu.edu.cn/xtgl/login_slogin.html",
                "JSESSIONID=ABC123",
            ),
        )
    }

    @Test
    fun `SSO 域判定未登录`() {
        assertFalse(
            JwxtLoginDetector.isLoggedIn(
                "https://sso.scnu.edu.cn/authserver/login",
                "MOD_AUTH_CAS=abc",
            ),
        )
    }

    @Test
    fun `无 cookie 判定未登录`() {
        assertFalse(JwxtLoginDetector.isLoggedIn("https://jwxt.scnu.edu.cn/", null))
    }
}
```

- [ ] **Step 3: 写学期推算** `data/jwxt/JwxtSemesterResolver.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import java.time.LocalDate

/** 教务接口的学年/学期参数。 */
data class SemesterSelection(
    val xnm: String,
    val xqm: String,
    val name: String,
)

/**
 * 按当天日期推算当前教务学期（xnm/xqm 是正方接口的学年/学期码）。
 * 正方通用约定：xqm 3=第一学期(秋)、12=第二学期(春)、16=第三学期(暑假)。
 * [VERIFY] 华师是否沿用该约定需真机核对；若不同只需改这里（后续可加 UI 学期选择器）。
 */
object JwxtSemesterResolver {

    fun resolve(today: LocalDate): SemesterSelection {
        val m = today.monthValue
        return if (m in 2..7) {
            SemesterSelection(
                xnm = (today.year - 1).toString(),
                xqm = "12",
                name = "${today.year - 1}-${today.year} 第二学期（春）",
            )
        } else {
            SemesterSelection(
                xnm = today.year.toString(),
                xqm = "3",
                name = "${today.year}-${today.year + 1} 第一学期（秋）",
            )
        }
    }
}
```

- [ ] **Step 4: 写测试** `JwxtSemesterResolverTest.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class JwxtSemesterResolverTest {

    @Test
    fun `8 月算秋季学期`() {
        val s = JwxtSemesterResolver.resolve(LocalDate.of(2026, 8, 14))
        assertEquals("2026", s.xnm)
        assertEquals("3", s.xqm)
        assertEquals("2026-2027 第一学期（秋）", s.name)
    }

    @Test
    fun `3 月算春季学期`() {
        val s = JwxtSemesterResolver.resolve(LocalDate.of(2026, 3, 5))
        assertEquals("2025", s.xnm)
        assertEquals("12", s.xqm)
        assertEquals("2025-2026 第二学期（春）", s.name)
    }
}
```

- [ ] **Step 5: 运行验证**

Run:
```bash
gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.JwxtLoginDetectorTest"
gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.JwxtSemesterResolverTest"
```
Expected: 4 + 2 个测试 PASS。

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(jwxt): 登录态检测 + 教务学期推算纯逻辑"
```

---

### Task 4: JwxtImportUseCase（导入落库）

**Files:**
- Create: `app/src/main/java/com/scnu/schedule/data/jwxt/JwxtImportUseCase.kt`
- Create: `app/src/test/java/com/scnu/schedule/data/jwxt/JwxtImportUseCaseTest.kt`

- [ ] **Step 1: 写用例** `data/jwxt/JwxtImportUseCase.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Period
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 教务导入落库：
 * 1) 逐门 upsert 课程（id=0 走 Room 插入），colorIndex 循环分配课程色板下标；
 * 2) 激活「石牌校区」作息表（默认表优先）；
 * 3) 学期名/总周数写入 DataStore（保留用户已设的开学日期）。
 *
 * 已知取舍 [DECISION]：MVP 不做去重，重复导入会追加课程行；后续可加"先删当学期课程再导入"。
 */
class JwxtImportUseCase @Inject constructor(
    private val courseRepo: CourseRepository,
    private val timeTableRepo: TimeTableRepository,
    private val settingsRepo: SettingsRepository,
) {

    suspend fun import(courses: List<Course>, semesterName: String) {
        val timetableId = resolveTimetableId()
        courses.forEachIndexed { index, course ->
            courseRepo.upsert(course.copy(id = 0, colorIndex = index % COLOR_COUNT))
        }
        settingsRepo.setActiveTimeTable(timetableId)
        settingsRepo.setSemester(mergeSemesterName(semesterName))
    }

    private suspend fun mergeSemesterName(name: String): Semester {
        val existing = settingsRepo.semester.first()
        return existing.copy(name = name, totalWeeks = TOTAL_WEEKS)
    }

    private suspend fun resolveTimetableId(): Long {
        val timetables = timeTableRepo.timetables.first()
        timetables.firstOrNull { it.isDefault }?.let { return it.id }
        timetables.firstOrNull()?.let { return it.id }
        // 防御：无作息表时种入石牌默认（正常启动已种子化，理论不触发）
        val tt = TimeTable(
            name = "石牌校区",
            isDefault = true,
            periods = SHIPAI_PERIODS.map { (idx, start, end) ->
                Period(periodIndex = idx, startMinute = start, endMinute = end)
            },
        )
        timeTableRepo.upsert(tt)
        return timeTableRepo.timetables.first()
            .firstOrNull { it.name == "石牌校区" }?.id
            ?: throw JwxtException("作息表创建失败")
    }

    companion object {
        /** 课程色板 8 色（ClaudePalette.coursePalette 长度），按循环取模。 */
        const val COLOR_COUNT = 8

        /** 华师一学期总周数 [VERIFY]。 */
        const val TOTAL_WEEKS = 20

        /** 石牌校区默认 10 节（与 Plan 1 Task 7 种子一致）。 */
        val SHIPAI_PERIODS: List<Triple<Int, Int, Int>> = listOf(
            Triple(1, 510, 550), Triple(2, 560, 600), Triple(3, 620, 660), Triple(4, 670, 710),
            Triple(5, 870, 910), Triple(6, 920, 960), Triple(7, 970, 1010), Triple(8, 1020, 1060),
            Triple(9, 1140, 1180), Triple(10, 1190, 1230),
        )
    }
}
```

- [ ] **Step 2: 写测试（假仓储）** `JwxtImportUseCaseTest.kt`

```kotlin
package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.model.Course
import com.scnu.schedule.domain.model.Semester
import com.scnu.schedule.domain.model.TimeTable
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import com.scnu.schedule.ui.theme.AppThemeType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class JwxtImportUseCaseTest {

    private class FakeCourseRepo : CourseRepository {
        val saved = mutableListOf<Course>()
        private val state = MutableStateFlow<List<Course>>(emptyList())
        override val courses: Flow<List<Course>> = state
        override suspend fun upsert(course: Course) {
            saved += course
            state.value = saved.toList()
        }

        override suspend fun delete(id: Long) {
            saved.removeAll { it.id == id }
            state.value = saved.toList()
        }
    }

    private class FakeTtRepo : TimeTableRepository {
        val state = MutableStateFlow<List<TimeTable>>(
            listOf(TimeTable(id = 1, name = "石牌校区", isDefault = true)),
        )
        override val timetables: Flow<List<TimeTable>> = state
        override suspend fun upsert(timetable: TimeTable) {
            state.value = state.value.filter { it.id != timetable.id } + timetable
        }

        override suspend fun delete(id: Long) {
            state.value = state.value.filter { it.id != id }
        }
    }

    private class FakeSettingsRepo : SettingsRepository {
        val state = MutableStateFlow(Semester(startDate = LocalDate.of(2026, 8, 31)))
        val activeId = MutableStateFlow<Long?>(null)
        override val theme: Flow<AppThemeType> = MutableStateFlow(AppThemeType.CLAUDE)
        override val semester: Flow<Semester> = state
        override val activeTimeTableId: Flow<Long?> = activeId
        override suspend fun setTheme(type: AppThemeType) {}
        override suspend fun setSemester(semester: Semester) { state.value = semester }
        override suspend fun setActiveTimeTable(id: Long?) { activeId.value = id }
    }

    @Test
    fun `导入课程并激活默认作息表与学期名`() = runBlocking {
        val courseRepo = FakeCourseRepo()
        val ttRepo = FakeTtRepo()
        val settingsRepo = FakeSettingsRepo()
        val useCase = JwxtImportUseCase(courseRepo, ttRepo, settingsRepo)

        useCase.import(
            courses = listOf(
                Course(name = "高数", dayOfWeek = 1, startPeriod = 1, endPeriod = 2, weekPattern = WeekPattern(WeekKind.ALL, 1, 16)),
                Course(name = "英语", dayOfWeek = 2, startPeriod = 3, endPeriod = 4, weekPattern = WeekPattern(WeekKind.ODD, 1, 16)),
            ),
            semesterName = "2026-2027 第一学期（秋）",
        )

        assertEquals(2, courseRepo.saved.size)
        assertEquals(0L, courseRepo.saved[0].id)          // id=0 走插入
        assertEquals(0, courseRepo.saved[0].colorIndex)   // 循环取模
        assertEquals(1, courseRepo.saved[1].colorIndex)
        assertEquals(1L, settingsRepo.activeId.value)      // 石牌默认作息
        assertEquals("2026-2027 第一学期（秋）", settingsRepo.state.value.name)
        assertEquals(20, settingsRepo.state.value.totalWeeks)
    }

    @Test
    fun `保留用户开学日期只改学期名`() = runBlocking {
        val settingsRepo = FakeSettingsRepo()
        val useCase = JwxtImportUseCase(FakeCourseRepo(), FakeTtRepo(), settingsRepo)

        useCase.import(emptyList(), "2026-2027 第一学期（秋）")

        assertEquals(LocalDate.of(2026, 8, 31), settingsRepo.state.value.startDate)
        assertEquals("2026-2027 第一学期（秋）", settingsRepo.state.value.name)
    }
}
```

- [ ] **Step 3: 运行验证**

Run: `gradle :app:testDebugUnitTest --tests "com.scnu.schedule.data.jwxt.JwxtImportUseCaseTest"`
Expected: 2 个测试 PASS。

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(jwxt): JwxtImportUseCase 导入落库（upsert+激活石牌作息+学期名）"
```

---

### Task 5: 状态机 + ViewModel + Hilt 接线

**Files:**
- Create: `app/src/main/java/com/scnu/schedule/ui/jwxt/JwxtUiState.kt`
- Create: `app/src/main/java/com/scnu/schedule/ui/jwxt/JwxtViewModel.kt`
- Create: `app/src/main/java/com/scnu/schedule/data/di/JwxtModule.kt`

- [ ] **Step 1: 写状态机** `ui/jwxt/JwxtUiState.kt`

```kotlin
package com.scnu.schedule.ui.jwxt

import com.scnu.schedule.domain.import.ImportResult

/** 教务导入 UI 状态机。 */
sealed interface JwxtUiState {
    data object Loading : JwxtUiState                       // WebView 初始化
    data object LoginRequired : JwxtUiState                 // 等待用户在 WebView 登录
    data object Fetching : JwxtUiState                      // 抓取中
    data class Preview(val result: ImportResult) : JwxtUiState
    data object Importing : JwxtUiState
    data class Success(val count: Int) : JwxtUiState
    data class Error(val kind: JwxtErrorKind, val message: String) : JwxtUiState
}

enum class JwxtErrorKind {
    LOGIN_FAILED,      // 登录失效/失败
    NETWORK,           // 断网
    PARSE_FAILED,      // 解析失败
    NOT_ON_CAMPUS,     // 校外访问被拒（校园网限制，需 WebVPN）
    UNKNOWN,
}
```

- [ ] **Step 2: 写 ViewModel** `ui/jwxt/JwxtViewModel.kt`

```kotlin
package com.scnu.schedule.ui.jwxt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.data.jwxt.JwxtImportUseCase
import com.scnu.schedule.data.jwxt.JwxtLoginDetector
import com.scnu.schedule.data.jwxt.JwxtLoginExpiredException
import com.scnu.schedule.data.jwxt.JwxtNetworkException
import com.scnu.schedule.data.jwxt.JwxtParseException
import com.scnu.schedule.data.jwxt.JwxtSemesterResolver
import com.scnu.schedule.data.jwxt.SemesterSelection
import com.scnu.schedule.data.jwxt.ZhengFangClient
import com.scnu.schedule.domain.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class JwxtViewModel @Inject constructor(
    private val client: ZhengFangClient,
    private val importUseCase: JwxtImportUseCase,
) : ViewModel() {

    private val semesterSelection: SemesterSelection = JwxtSemesterResolver.resolve(LocalDate.now())

    private val _uiState = MutableStateFlow<JwxtUiState>(JwxtUiState.Loading)
    val uiState: StateFlow<JwxtUiState> = _uiState.asStateFlow()

    /** WebView 每完成一次主页面加载回调（带 CookieManager 里的 cookie 头）。 */
    fun onPageLoaded(url: String?, cookieHeader: String?) {
        if (isBusy()) return
        if (JwxtLoginDetector.isLoggedIn(url, cookieHeader)) {
            fetchSchedule()
        } else {
            _uiState.value = JwxtUiState.LoginRequired
        }
    }

    /** WebView 主框架加载错误（断网 / 校园网限制）。errorCode 为 WebViewClient.ERROR_* 常量。 */
    fun onWebError(errorCode: Int) {
        if (isBusy()) return
        _uiState.value = JwxtUiState.Error(
            kind = if (errorCode == -2 || errorCode == -6) JwxtErrorKind.NOT_ON_CAMPUS else JwxtErrorKind.NETWORK,
            message = "无法访问教务系统，请确认已连接校园网或通过 WebVPN 访问。",
        )
    }

    fun fetchSchedule() {
        if (isBusy()) return
        _uiState.value = JwxtUiState.Fetching
        viewModelScope.launch {
            try {
                val result = client.fetchSchedule(semesterSelection.xnm, semesterSelection.xqm)
                if (result.courses.isEmpty() && result.warnings.isEmpty()) {
                    _uiState.value = JwxtUiState.Error(
                        JwxtErrorKind.PARSE_FAILED,
                        "未解析到任何课程，可能是接口字段变化或本学期无课。",
                    )
                } else {
                    _uiState.value = JwxtUiState.Preview(result)
                }
            } catch (e: JwxtLoginExpiredException) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.LOGIN_FAILED, "登录已失效，请重新登录：${e.message}")
            } catch (e: JwxtNetworkException) {
                _uiState.value = JwxtUiState.Error(
                    JwxtErrorKind.NETWORK,
                    "网络请求失败：${e.message}。请确认已连接校园网或 WebVPN。",
                )
            } catch (e: JwxtParseException) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.PARSE_FAILED, "课表解析失败：${e.message}")
            } catch (e: Exception) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.UNKNOWN, "未知错误：${e.message}")
            }
        }
    }

    fun import(courses: List<Course>) {
        if (_uiState.value !is JwxtUiState.Preview || courses.isEmpty()) return
        _uiState.value = JwxtUiState.Importing
        viewModelScope.launch {
            try {
                importUseCase.import(courses, semesterSelection.name)
                _uiState.value = JwxtUiState.Success(courses.size)
            } catch (e: Exception) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.UNKNOWN, "保存失败：${e.message}")
            }
        }
    }

    /** 从错误态返回登录等待（WebView 由 UI 层重载）。 */
    fun retry() {
        if (_uiState.value is JwxtUiState.Error) {
            _uiState.value = JwxtUiState.LoginRequired
        }
    }

    private fun isBusy(): Boolean =
        _uiState.value is JwxtUiState.Fetching ||
            _uiState.value is JwxtUiState.Importing ||
            _uiState.value is JwxtUiState.Success
}
```

- [ ] **Step 3: 写 DI 模块** `data/di/JwxtModule.kt`

```kotlin
package com.scnu.schedule.data.di

import com.scnu.schedule.data.jwxt.WebViewCookieJar
import com.scnu.schedule.data.jwxt.ZhengFangClient
import com.scnu.schedule.data.jwxt.ZhengFangParser
import com.scnu.schedule.data.repository.CourseRepositoryImpl
import com.scnu.schedule.data.repository.SettingsRepositoryImpl
import com.scnu.schedule.data.repository.TimeTableRepositoryImpl
import com.scnu.schedule.domain.import.CourseParser
import com.scnu.schedule.domain.repo.CourseRepository
import com.scnu.schedule.domain.repo.SettingsRepository
import com.scnu.schedule.domain.repo.TimeTableRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.CookieJar

@Module
@InstallIn(SingletonComponent::class)
abstract class JwxtModule {

    @Binds
    abstract fun bindCourseParser(impl: ZhengFangParser): CourseParser

    // 注意：Plan 1（core-app）Task 8 的 DataModule 未提供仓储接口绑定，
    // 而 ScheduleViewModel 等直接注入接口，此处补齐。若实现 Plan 1 时已加过，
    // 则删除下面三条重复绑定，避免 Hilt 重复。
    @Binds
    @Singleton
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    @Singleton
    abstract fun bindTimeTableRepository(impl: TimeTableRepositoryImpl): TimeTableRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideCookieJar(): CookieJar = WebViewCookieJar()

        @Provides
        @Singleton
        fun provideZhengFangClient(parser: CourseParser, cookieJar: CookieJar): ZhengFangClient =
            ZhengFangClient(parser, cookieJar)
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `gradle :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`（若提示仓储接口重复绑定，按 Step 3 注释删对应 `@Binds`）。

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(jwxt): 导入状态机 ViewModel + 错误分类 + Hilt 接线"
```

---

### Task 6: JwxtLoginScreen（WebView 登录 + 预览勾选 + 确认）

**Files:**
- Create: `app/src/main/java/com/scnu/schedule/ui/jwxt/JwxtLoginScreen.kt`

- [ ] **Step 1: 写登录页** `ui/jwxt/JwxtLoginScreen.kt`

```kotlin
package com.scnu.schedule.ui.jwxt

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.data.jwxt.ZhengFangClient
import com.scnu.schedule.domain.import.ImportResult
import com.scnu.schedule.domain.import.ImportWarning
import com.scnu.schedule.domain.model.Course

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JwxtLoginScreen(
    onBack: () -> Unit,
    onImported: () -> Unit,
    vm: JwxtViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    @SuppressLint("SetJavaScriptEnabled")
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // [VERIFY] 若华师登录页在手机 UA 下显示异常，可换桌面 UA：
            // settings.userAgentString = DESKTOP_UA
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val cookieHeader = CookieManager.getInstance().getCookie(ZhengFangClient.JWXT_BASE)
                    vm.onPageLoaded(url, cookieHeader)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        vm.onWebError(error?.errorCode ?: -1)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        webView.loadUrl(ZhengFangClient.JWXT_BASE)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            // [DECISION] 保留会话 cookie，下次导入免登录；若要"退出登录"再手动清 CookieManager
            webView.removeAllViews()
            webView.destroy()
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("教务导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                if (uiState is JwxtUiState.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
            HorizontalDivider()
            when (val s = uiState) {
                is JwxtUiState.Loading -> BusyPanel("正在打开教务系统…")
                is JwxtUiState.LoginRequired -> HintPanel(
                    title = "请登录华师统一身份认证",
                    body = "在上方网页中使用一卡通账号 + 密码登录（初始密码为身份证后 8 位）。\n登录成功后会自动抓取本学期课表。",
                    action = {
                        TextButton(onClick = { webView.loadUrl(ZhengFangClient.JWXT_BASE) }) {
                            Text("重新加载")
                        }
                    },
                )
                is JwxtUiState.Fetching -> BusyPanel("正在从正方教务抓取课表…")
                is JwxtUiState.Preview -> PreviewPanel(s.result) { chosen -> vm.import(chosen) }
                is JwxtUiState.Importing -> BusyPanel("正在保存课程…")
                is JwxtUiState.Success -> SuccessPanel(s.count, onDone = onImported)
                is JwxtUiState.Error -> ErrorPanel(
                    kind = s.kind,
                    message = s.message,
                    onRetry = { vm.retry() },
                    onReload = { webView.loadUrl(ZhengFangClient.JWXT_BASE) },
                )
            }
        }
    }
}

private val WEEK_CN = listOf("一", "二", "三", "四", "五", "六", "日")

private fun dayLabel(day: Int): String =
    if (day in 1..7) "周${WEEK_CN[day - 1]}" else "周$day"

@Composable
private fun PreviewPanel(result: ImportResult, onConfirm: (List<Course>) -> Unit) {
    val selections = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(result.courses.size) {
        selections.clear()
        result.courses.indices.forEach { selections[it] = true }
    }
    Column(Modifier.fillMaxWidth().height(280.dp)) {
        Text(
            "解析到 ${result.courses.size} 门课（已按周次/节次拆条），勾选后导入",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (result.warnings.isNotEmpty()) {
            WarningBanner(result.warnings)
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(result.courses) { index, course ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selections[index] == true,
                        onCheckedChange = { selections[index] = it },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(course.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${dayLabel(course.dayOfWeek)} 第${course.startPeriod}-${course.endPeriod}节 · ${course.teacher} · ${course.location}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                val chosen = result.courses.filterIndexed { i, _ -> selections[i] == true }
                onConfirm(chosen)
            },
            enabled = selections.values.any { it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text("导入 ${selections.values.count { it }} 门课")
        }
    }
}

@Composable
private fun WarningBanner(warnings: List<ImportWarning>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "${warnings.size} 条数据未能解析：",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        warnings.take(3).forEach {
            Text(
                "· ${it.courseName}：${it.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
        if (warnings.size > 3) {
            Text("…", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun HintPanel(title: String, body: String, action: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        action()
    }
}

@Composable
private fun BusyPanel(text: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
private fun SuccessPanel(count: Int, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("已导入 $count 门课，已激活「石牌校区」作息表。", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onDone, modifier = Modifier.padding(top = 12.dp)) {
            Text("查看课表")
        }
    }
}

@Composable
private fun ErrorPanel(kind: JwxtErrorKind, message: String, onRetry: () -> Unit, onReload: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("导入失败（${kind.name}）", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onRetry) { Text("重试") }
            if (kind == JwxtErrorKind.LOGIN_FAILED || kind == JwxtErrorKind.NOT_ON_CAMPUS) {
                Button(onClick = onReload) { Text("重新加载网页") }
            }
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `gradle :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

> 已知取舍 [DECISION]：旋转屏幕等配置变化会重建 WebView 并重新加载登录页（MVP 可接受）。如需保留登录态，可后续用 `rememberSaveable` + ViewModel 持有 URL。

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(jwxt): 教务导入 WebView 登录页 + 预览勾选 + 确认导入"
```

---

### Task 7: 设置页入口 + 导航接线 + 端到端验证清单

**Files:**
- Modify: `app/src/main/java/com/scnu/schedule/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/scnu/schedule/ui/navigation/ScheduleNavHost.kt`

- [ ] **Step 1: 设置页加入口** `ui/settings/SettingsScreen.kt`

函数签名加 `onJwxtImport` 回调：
```kotlin
@Composable
fun SettingsScreen(
    onJwxtImport: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
```

在「作息时间」段之后追加：
```kotlin
        Text("教务导入", fontSize = 14.sp, color = p.muted, modifier = Modifier.padding(top = 20.dp))
        Text(
            "从华师教务系统导入本学期课表 →",
            fontSize = 15.sp,
            color = p.primary,
            modifier = Modifier.clickable(onClick = onJwxtImport),
        )
```

- [ ] **Step 2: 导航接线** `ui/navigation/ScheduleNavHost.kt`

import：
```kotlin
import com.scnu.schedule.ui.jwxt.JwxtLoginScreen
```

NavHost 里把 `composable("settings")` 改为传回调，并加 `jwxt_import` 路由：
```kotlin
composable("settings") { SettingsScreen(onJwxtImport = { nav.navigate("jwxt_import") }) }
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
```

- [ ] **Step 3: 编译 + 全量测试**

Run:
```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`；全部单测 PASS。

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(ui/settings): 教务导入入口 + jwxt_import 路由"
```

---

## 端到端手工验证清单（需要真实华师账号，必须人工执行）

计划内自动化测试**只覆盖**：解析器（fixture）、纯逻辑（登录检测/学期推算）、客户端请求形状（MockWebServer）、落库顺序（假仓储）。
完整链路必须在**真机 + 校园网（或 WebVPN）+ 真实华师一卡通账号**下验证：

1. 设置页 → 「教务导入」→ WebView 打开 `jwxt.scnu.edu.cn`，重定向到统一身份认证 SSO。
2. 用一卡通账号 + 密码登录（可能含验证码/滑块）→ 自动跳回教务首页。
3. 确认自动进入「解析到 N 门课」预览；勾选/全选 → 导入。
4. 回课表页确认课程已渲染（含单双周过滤）、「石牌校区」作息被激活、学期名已更新。
5. 逐项记录并反馈 `[VERIFY]` 结论（据此修正代码）：
   - kbcx 端点路径与表单字段（`xnm/xqm/kzlx/xsdm`）是否与 Task 2 一致；
   - 是否要求 `csrftoken` 头，token 在首页 HTML 的提取位置；
   - 返回 JSON 字段名（`kcmc/teaxms/xm/cdmc/ddxx/xqj/jcs/zs` 等）与 fixture 是否一致；
   - 会话 cookie 名称（`JSESSIONID`?）与登录成功判定 URL；
   - 学期码 `xqm`（3=秋 / 12=春）是否与华师实际一致；
   - 周次字符串写法（`1-16周`、单双周、分段周）是否被 `ZhengFangParser` 正确覆盖。

---

## 风险与决策记录

| 项 | 类型 | 说明 |
|---|---|---|
| kbcx 端点/字段/csrftoken/学期码/周次格式 | `[VERIFY]` | 依赖真机核对；改动只影响 `ZhengFangClient`/`ZhengFangParser`/fixture |
| 会话 cookie 名与登录落地页 | `[VERIFY]` | 影响 `JwxtLoginDetector`；不中则登录检测不触发抓取 |
| 重复导入去重 | `[DECISION]` | MVP 直接追加（id=0 插入）；后续可加「删除当学期课程再导入」 |
| 预览粒度 | `[DECISION]` | 一行 kbcx 记录 = 一个 Course，与拾光 demo 一致 |
| WebView cookie 保留 | `[DECISION]` | 离开导入页不清 cookie，便于再次导入 |
| 配置变化重建 WebView | `[DECISION]` | 旋转会重新加载登录页，MVP 可接受 |
| 仓储接口 Hilt 绑定 | `[DECISION]` | Plan 1 未含 `@Binds`，JwxtModule 补齐；若已存在则删重复项 |
| 校外访问 | `[DECISION]` | 主框架 `ERROR_HOST_LOOKUP/-2`、`ERROR_CONNECT/-6` 映射为 NOT_ON_CAMPUS，文案提示校园网/WebVPN |
