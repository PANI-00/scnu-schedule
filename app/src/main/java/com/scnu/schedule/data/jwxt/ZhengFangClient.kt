package com.scnu.schedule.data.jwxt

import com.scnu.schedule.domain.importing.CourseParser
import com.scnu.schedule.domain.importing.ImportResult
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * 正方教务 kbcx 课表接口客户端。
 *
 * 登录在 WebView（SSO + 验证码）完成，本客户端通过 WebViewCookieJar 复用其会话，
 * 直接 POST kbcx 接口拿 JSON 交给 CourseParser。
 *
 * [VERIFY] 以下待真机核对：
 *  - 端点路径 kbcx/xskbcx_cxXsgrkb.html
 *  - 表单字段 xnm/xqm/kzlx/xsdm
 *  - 是否要求 csrftoken 头，以及 token 在首页的提取位置（本实现兼容 hidden input / JS 变量两种）
 */
class ZhengFangClient(
    private val parser: CourseParser,
    private val cookieJar: CookieJar,
    private val baseUrl: String = JWXT_BASE,
) {
    private val base = baseUrl.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .build()

    /**
     * 抓取并解析指定学年/学期的课表。
     *
     * 登录过期通过 HTTP 401/403 判定（会话 cookie 被拒时返回）；
     * 客户端 followRedirects(true)，302 会被 OkHttp 自动跟随，若最终跳转到登录页，
     * 会因返回非 JSON 而表现为 [JwxtParseException]。
     * [VERIFY] 真实过期行为待真机核对。
     */
    suspend fun fetchSchedule(xnm: String, xqm: String): ImportResult =
        withContext(Dispatchers.IO) {
            val csrf = fetchCsrfToken()
            val form = FormBody.Builder()
                .add("xnm", xnm)
                .add("xqm", xqm)
                .add("kzlx", "1")
                .add("xsdm", "0")
                .build()
            val request = Request.Builder()
                .url("$base/kbcx/xskbcx_cxXsgrkb.html")
                .header("X-Requested-With", "XMLHttpRequest")
                .apply { csrf?.let { header("csrftoken", it) } }
                .post(form)
                .build()
            try {
                client.newCall(request).execute().use { resp ->
                    when {
                        resp.code == 401 || resp.code == 403 ->
                            throw JwxtLoginExpiredException("登录已失效（HTTP ${resp.code}），请重新登录")
                        !resp.isSuccessful ->
                            throw JwxtNetworkException("教务接口 HTTP ${resp.code}")
                        else -> {
                            val raw = resp.body?.string() ?: throw JwxtParseException("教务接口返回空响应体")
                            parser.parse(raw)
                        }
                    }
                }
            } catch (e: IOException) {
                throw JwxtNetworkException("网络请求失败：${e.message}", e)
            }
        }

    /**
     * 从教务首页提取 csrftoken（新版正方把 token 写进页面 JS 变量或 hidden input）。
     * 若华师不需要该头，此函数返回 null 时请求会跳过 csrftoken 头。
     */
    private fun fetchCsrfToken(): String? = runCatching {
        val request = Request.Builder().url(base).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val html = resp.body?.string() ?: return null
            Jsoup.parse(html).selectFirst("input[name=csrftoken]")?.attr("value")
                ?.takeIf { it.isNotBlank() }
                ?: Regex("csrftoken\\s*=\\s*['\"]([0-9a-fA-F]{32,})['\"]")
                    .find(html)?.groupValues?.get(1)
        }
    }.getOrNull()

    companion object {
        const val JWXT_BASE = "https://jwxt.scnu.edu.cn"
    }
}
