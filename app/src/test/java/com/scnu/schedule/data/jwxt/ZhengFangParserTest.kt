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
