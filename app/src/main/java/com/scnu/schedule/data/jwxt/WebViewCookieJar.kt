package com.scnu.schedule.data.jwxt

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 桥接 WebView 的 CookieManager 与 OkHttp：
 * OkHttp 请求自动带上 WebView 登录后写入的会话 cookie（JSESSIONID 等），
 * OkHttp 收到的 Set-Cookie 也同步回 CookieManager，保持两端一致。
 */
class WebViewCookieJar : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val manager = CookieManager.getInstance()
        cookies.forEach { cookie ->
            manager.setCookie(url.toString(), cookie.toString())
        }
        manager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = CookieManager.getInstance().getCookie(url.toString())
            ?: return emptyList()
        return header.split(";").mapNotNull { piece ->
            val idx = piece.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val name = piece.substring(0, idx).trim()
            val value = piece.substring(idx + 1).trim()
            runCatching {
                Cookie.Builder()
                    .domain(url.host)
                    .path("/")
                    .name(name)
                    .value(value)
                    .build()
            }.getOrNull()
        }
    }
}
