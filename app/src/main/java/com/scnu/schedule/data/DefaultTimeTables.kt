package com.scnu.schedule.data

/**
 * 内置默认作息表（华师官方各校区作息）：
 * 名称 → 节次列表 (periodIndex, startMinute, endMinute)，minute = 距 0 点分钟数。
 * 首个（石牌校区、滨海校区）标记为默认，教务导入激活默认作息时优先。
 */
object DefaultTimeTables {

    val ALL: List<Pair<String, List<Triple<Int, Int, Int>>>> = listOf(
        "石牌校区、滨海校区" to listOf(
            Triple(1, 510, 550), Triple(2, 560, 600), Triple(3, 620, 660), Triple(4, 670, 710),
            Triple(5, 870, 910), Triple(6, 920, 960), Triple(7, 970, 1010), Triple(8, 1020, 1060),
            Triple(9, 1140, 1180), Triple(10, 1190, 1230),
        ),
        "大学城校区、南海校区" to listOf(
            Triple(1, 510, 550), Triple(2, 560, 600), Triple(3, 620, 660), Triple(4, 670, 710),
            Triple(5, 840, 880), Triple(6, 890, 930), Triple(7, 940, 980), Triple(8, 990, 1030),
            Triple(9, 1140, 1180), Triple(10, 1190, 1230),
        ),
    )

    /** 首个内置表（石牌校区、滨海校区），导入防御兜底用。 */
    val FIRST: Pair<String, List<Triple<Int, Int, Int>>> get() = ALL.first()
}
