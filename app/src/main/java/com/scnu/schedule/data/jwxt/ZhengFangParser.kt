package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.importing.CourseParser
import com.scnu.schedule.domain.importing.ImportResult
import com.scnu.schedule.domain.importing.ImportWarning
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
        val week = parseWeeks(
            firstNonBlank(item, "zcd", "zs", "zcs", "zcsm"),
            item.optString("zcjs"),
            name,
            warnings,
        )

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
