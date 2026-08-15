package com.scnu.schedule.data.jwxt

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** 测试用内存 cookie 存储（替代 android WebViewCookieJar）。 */
class InMemoryCookieJar : CookieJar {
    private val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store += cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store.filter { c ->
            url.host == c.domain.removePrefix(".") && url.encodedPath.startsWith(c.path)
        }
}
