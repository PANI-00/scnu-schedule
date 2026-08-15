package com.scnu.schedule.data.jwxt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JwxtLoginDetectorTest {

    @Test
    fun `登录后 jwxt 落地页判定已登录`() {
        assertTrue(
            JwxtLoginDetector.isLoggedIn(
                "https://jwxt.scnu.edu.cn/xtgl/index_initMenu.html",
                "JSESSIONID=ABC123; route=xyz",
            ),
        )
    }

    @Test
    fun `正方登录页即使有 cookie 也判定未登录`() {
        assertFalse(
            JwxtLoginDetector.isLoggedIn(
                "https://jwxt.scnu.edu.cn/xtgl/login_slogin.html",
                "JSESSIONID=ABC123",
            ),
        )
    }

    @Test
    fun `SSO 域判定未登录`() {
        assertFalse(
            JwxtLoginDetector.isLoggedIn(
                "https://sso.scnu.edu.cn/authserver/login",
                "MOD_AUTH_CAS=abc",
            ),
        )
    }

    @Test
    fun `无 cookie 判定未登录`() {
        assertFalse(JwxtLoginDetector.isLoggedIn("https://jwxt.scnu.edu.cn/", null))
    }

    @Test
    fun `host 大小写不敏感仍判定已登录`() {
        assertTrue(
            JwxtLoginDetector.isLoggedIn(
                "https://Jwxt.SCNU.edu.cn/xtgl/index_initMenu.html",
                "JSESSIONID=ABC123",
            ),
        )
    }

    @Test
    fun `恶意前缀域不匹配教务域`() {
        assertFalse(
            JwxtLoginDetector.isLoggedIn(
                "https://eviljwxt.scnu.edu.cn/xtgl/index_initMenu.html",
                "JSESSIONID=ABC123",
            ),
        )
    }

    @Test
    fun `会话 cookie 值为空判定未登录`() {
        assertFalse(
            JwxtLoginDetector.isLoggedIn(
                "https://jwxt.scnu.edu.cn/xtgl/index_initMenu.html",
                "JSESSIONID=; route=xyz",
            ),
        )
    }
}
