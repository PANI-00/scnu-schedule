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

    @Test
    fun `1 月算上一学年秋学期`() {
        val s = JwxtSemesterResolver.resolve(LocalDate.of(2026, 1, 10))
        assertEquals("2025", s.xnm)
        assertEquals("3", s.xqm)
        assertEquals("2025-2026 第一学期（秋）", s.name)
    }

    @Test
    fun `12 月算本学年秋学期`() {
        val s = JwxtSemesterResolver.resolve(LocalDate.of(2026, 12, 1))
        assertEquals("2026", s.xnm)
        assertEquals("3", s.xqm)
        assertEquals("2026-2027 第一学期（秋）", s.name)
    }

    @Test
    fun `2 月算本学年春季学期`() {
        val s = JwxtSemesterResolver.resolve(LocalDate.of(2026, 2, 1))
        assertEquals("2025", s.xnm)
        assertEquals("12", s.xqm)
        assertEquals("2025-2026 第二学期（春）", s.name)
    }
}
