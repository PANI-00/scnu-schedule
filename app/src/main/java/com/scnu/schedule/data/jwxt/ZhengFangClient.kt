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

/**
 * 正方教务 kbcx 课表接口客户端。
 *
 * 登录在 WebView（SSO + 验证码）完成，本客户端通过 WebViewCookieJar 复用其会话，
 * 直接 POST kbcx 接口拿 JSON 交给 CourseParser。
 *
 * 请求契约（2026-08 对齐正方 V9 实测/可运行实现修正）：
 *  - 端点 `kbcx/xskbcx_cxXsgrkb.html` 必须带 `?gnmkdm=N2151`（功能模块码=学生个人课表，
 *    用户在教务页打开的地址 `xskbcx_cxXskbcxIndex.html?gnmkdm=N2151` 中即为该值）
 *  - 表单 `xnm/xqm/kzlx=ck/xsdm=/kclbdm=`：`kzlx` 传 `ck` 而非 `1`，`xsdm` 传空而非 `0`
 *    —— 传错参数服务器返回 `{"kbList":[]}`，App 表现为「未解析到任何课程」
 *  - 数据查询无需 csrftoken 头（登录页 token 与数据接口会话不匹配；可运行参考实现均不携带）
 *  - `Referer` 指向课表查询页，贴近真实浏览器
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

    /** 学生个人课表查询页入口（真实浏览器中用户打开的地址）。 */
    private val scheduleIndexUrl: String = "$base/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=$GNMKDM_SCHEDULE"

    /**
     * 抓取并解析指定学年/学期的课表。
     *
     * 登录过期通过 HTTP 401/403 判定（会话 cookie 被拒时返回）；
     * 客户端 followRedirects(true)，302 会被 OkHttp 自动跟随，若最终跳转到登录页，
     * 会因返回非 JSON 而表现为 [JwxtParseException]。
     * 未登录时端点返回 302 → 登录页 HTML；已登录但参数错误时返回 `{"kbList":[]}`。
     */
    suspend fun fetchSchedule(xnm: String, xqm: String): ImportResult =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("xnm", xnm)
                .add("xqm", xqm)
                .add("kzlx", "ck")
                .add("xsdm", "")
                .add("kclbdm", "")
                .build()
            val request = Request.Builder()
                .url("$base/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=$GNMKDM_SCHEDULE")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", scheduleIndexUrl)
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
                            parser.parse(raw).copy(rawPreview = raw.take(RAW_PREVIEW_CHARS))
                        }
                    }
                }
            } catch (e: IOException) {
                throw JwxtNetworkException("网络请求失败：${e.message}", e)
            }
        }

    companion object {
        const val JWXT_BASE = "https://jwxt.scnu.edu.cn"

        /** 学生「个人课表查询」页：App 的导入 WebView 直接打开这里，用户选学期后导入页面显示的课表。 */
        const val SCHEDULE_INDEX_URL =
            "https://jwxt.scnu.edu.cn/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151&layout=default"

        /** 正方学生个人课表功能模块码（用户浏览器中 `?gnmkdm=N2151`）。 */
        private const val GNMKDM_SCHEDULE = "N2151"

        /** 调试：带进 UI 的原始响应片段长度。 */
        private const val RAW_PREVIEW_CHARS = 500
    }
}
