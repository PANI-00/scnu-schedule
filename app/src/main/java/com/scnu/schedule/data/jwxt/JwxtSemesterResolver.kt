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
            // 8-12 月属本学年秋学期；1 月寒假期间仍在上一学年的秋学期里。
            val autumnYear = if (m == 1) today.year - 1 else today.year
            SemesterSelection(
                xnm = autumnYear.toString(),
                xqm = "3",
                name = "$autumnYear-${autumnYear + 1} 第一学期（秋）",
            )
        }
    }
}
