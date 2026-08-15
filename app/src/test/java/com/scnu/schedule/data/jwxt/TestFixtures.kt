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
