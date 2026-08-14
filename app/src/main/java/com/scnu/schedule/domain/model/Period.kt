package com.scnu.schedule.domain.model

/** 节次时段；startMinute/endMinute = 距 0 点的分钟数 */
data class Period(
    val id: Long = 0,
    val periodIndex: Int,        // 第几节，从 1 开始
    val startMinute: Int,        // e.g. 8:30 -> 510
    val endMinute: Int,          // e.g. 9:10 -> 550
) {
    fun startLabel(): String = "${startMinute / 60}:%02d".format(startMinute % 60)
    fun endLabel(): String = "${endMinute / 60}:%02d".format(endMinute % 60)
}
