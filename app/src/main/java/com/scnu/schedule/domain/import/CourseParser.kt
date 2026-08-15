package com.scnu.schedule.domain.import

/** 课程源解析器接口：输入原始字符串（JSON/HTML…），输出可导入的课程与警告。 */
interface CourseParser {
    fun parse(raw: String): ImportResult
}
