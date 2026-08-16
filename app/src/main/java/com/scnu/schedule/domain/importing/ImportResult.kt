package com.scnu.schedule.domain.importing

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
    /** 调试用：抓取到的原始响应片段（客户端填入）。空结果时由 UI 展示以定位接口问题。 */
    val rawPreview: String = "",
)
