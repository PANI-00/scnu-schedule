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
