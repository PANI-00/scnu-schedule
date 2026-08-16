package com.scnu.schedule.ui.jwxt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.data.jwxt.JwxtImportUseCase
import com.scnu.schedule.domain.importing.CourseParser
import com.scnu.schedule.domain.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 教务导入：手动导入「网页当前显示的课表」。
 *
 * WebView 打开华师正方「个人课表查询」页（xskbcx_cxXskbcxIndex.html?gnmkdm=N2151），
 * 页面内部会请求 kbcx 数据接口；UI 层注入 JS 钩子截获该响应，用户点「导入当前课表」
 * 时把响应文本交给本 ViewModel 解析落库。选哪个学期、是否登录，全由用户在网页内操作，
 * 不再在此处推断（无需猜 xnm/xqm，导入的即为页面显示的课表）。
 */
@HiltViewModel
class JwxtViewModel @Inject constructor(
    private val parser: CourseParser,
    private val importUseCase: JwxtImportUseCase,
) : ViewModel() {

    /** 本次导入的学期名，从页面响应 xsxx 提取，确认导入时使用。 */
    private var pendingSemesterName: String = DEFAULT_SEMESTER_NAME

    private val _uiState = MutableStateFlow<JwxtUiState>(JwxtUiState.Loading)
    val uiState: StateFlow<JwxtUiState> = _uiState.asStateFlow()

    /** WebView 每完成一次主页面加载：空闲态回到 Ready，等用户操作。 */
    fun onPageFinished() {
        val s = _uiState.value
        if (s is JwxtUiState.Preview || s is JwxtUiState.Importing || s is JwxtUiState.Success) return
        _uiState.value = JwxtUiState.Ready
    }

    /** WebView 主框架加载错误（断网 / 校园网限制）。errorCode 为 WebViewClient.ERROR_* 常量。 */
    fun onWebError(errorCode: Int) {
        if (_uiState.value is JwxtUiState.Importing) return
        _uiState.value = JwxtUiState.Error(
            kind = if (errorCode == -2 || errorCode == -6) JwxtErrorKind.NOT_ON_CAMPUS else JwxtErrorKind.NETWORK,
            message = "无法访问教务系统，请确认已连接校园网或通过 WebVPN 访问。",
        )
    }

    /**
     * 用户点「导入当前课表」：解析页面侧抓到的原始响应，进入预览。
     * raw 为 null 表示页面还没抓到数据；以 "ERR:" 开头表示页面侧明确失败。
     */
    fun importFromPage(raw: String?) {
        if (_uiState.value is JwxtUiState.Importing) return
        if (raw == null || raw.isBlank()) {
            _uiState.value = JwxtUiState.Error(
                JwxtErrorKind.PARSE_FAILED,
                "页面还没有课表数据。\n请在网页中登录并进入「个人课表查询」、选好学期，再点导入。",
            )
            return
        }
        if (raw.startsWith("ERR:")) {
            val detail = raw.removePrefix("ERR:")
            _uiState.value = JwxtUiState.Error(
                JwxtErrorKind.PARSE_FAILED,
                if (detail == "no-page") {
                    "没找到课表查询页。\n请确认网页已在「个人课表查询」（有学年/学期选择框），再点导入。"
                } else {
                    "从页面抓取课表失败：$detail\n请确认已登录并进入个人课表查询页后重试。"
                },
            )
            return
        }
        val result = runCatching { parser.parse(raw) }.getOrElse { e ->
            _uiState.value = JwxtUiState.Error(
                JwxtErrorKind.PARSE_FAILED,
                "解析课表失败：${e.message}\n可能是页面返回了登录页/错误页，请确认已登录后重试。",
            )
            return
        }
        pendingSemesterName = semesterNameFromRaw(raw)
        if (result.courses.isEmpty() && result.warnings.isEmpty()) {
            _uiState.value = JwxtUiState.Error(
                JwxtErrorKind.PARSE_FAILED,
                "页面上没有解析到课程（kbList 为空）\n服务器响应：${raw.take(200)}\n" +
                    "可能是所选学期还没排课，换个学期再试。",
            )
        } else {
            _uiState.value = JwxtUiState.Preview(result)
        }
    }

    /** 从预览勾选结果确认导入。 */
    fun import(courses: List<Course>) {
        if (_uiState.value !is JwxtUiState.Preview || courses.isEmpty()) return
        _uiState.value = JwxtUiState.Importing
        viewModelScope.launch {
            try {
                importUseCase.import(courses, pendingSemesterName)
                _uiState.value = JwxtUiState.Success(courses.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.UNKNOWN, "保存失败：${e.message}")
            }
        }
    }

    /** 从错误态回到等待导入。 */
    fun retry() {
        if (_uiState.value is JwxtUiState.Error) {
            _uiState.value = JwxtUiState.Ready
        }
    }

    /** 从页面响应 xsxx 提取学期名（如 "2026-2027 第1学期（秋）"），失败则用通用名。 */
    private fun semesterNameFromRaw(raw: String): String {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return DEFAULT_SEMESTER_NAME
        val xsxx = root.optJSONObject("xsxx") ?: return DEFAULT_SEMESTER_NAME
        // XNMC 即学年范围 "2026-2027"；XQM 为学期码 3/12/16
        val xnmc = xsxx.optString("XNMC").trim()
        val xqm = xsxx.optString("XQM").trim()
        if (xnmc.isBlank() || xqm.isBlank()) return DEFAULT_SEMESTER_NAME
        val term = when (xqm) {
            "3" -> "第1学期（秋）"
            "12" -> "第2学期（春）"
            "16" -> "第3学期（暑）"
            else -> "第$xqm 学期"
        }
        return "$xnmc $term"
    }

    companion object {
        const val DEFAULT_SEMESTER_NAME = "教务导入"
    }
}
