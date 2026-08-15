package com.scnu.schedule.data.jwxt

import java.net.URI

/**
 * 判断 WebView 当前是否已完成教务登录：
 * 页面位于 jwxt.scnu.edu.cn 域、不是登录页，且 CookieManager 里带会话 cookie。
 * [VERIFY] 华师 SSO 回跳后的落地页 URL 与会话 cookie 名称需真机核对。
 */
object JwxtLoginDetector {

    /** 会话 cookie 名称候选（大写），正方新平台为 JSESSIONID，其余兜底。 */
    private val sessionCookieNames: Set<String> =
        setOf("JSESSIONID", "SESSIONID", "MOD_AUTH_CAS", "CONNECT.SID")

    fun isLoggedIn(url: String?, cookieHeader: String?): Boolean {
        if (url.isNullOrBlank() || cookieHeader.isNullOrBlank()) return false
        val host = runCatching { URI(url).host }.getOrNull() ?: return false
        if (!host.endsWith("jwxt.scnu.edu.cn")) return false
        if (isLoginPage(url)) return false
        val names = cookieHeader.split(";").map { it.trim().substringBefore('=').uppercase() }
        return names.any { it in sessionCookieNames }
    }

    fun isLoginPage(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("login") || lower.contains("sso") || lower.contains("authserver")
    }
}
