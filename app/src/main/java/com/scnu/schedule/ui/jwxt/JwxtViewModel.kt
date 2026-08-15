package com.scnu.schedule.ui.jwxt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scnu.schedule.data.jwxt.JwxtImportUseCase
import com.scnu.schedule.data.jwxt.JwxtLoginDetector
import com.scnu.schedule.data.jwxt.JwxtLoginExpiredException
import com.scnu.schedule.data.jwxt.JwxtNetworkException
import com.scnu.schedule.data.jwxt.JwxtParseException
import com.scnu.schedule.data.jwxt.JwxtSemesterResolver
import com.scnu.schedule.data.jwxt.SemesterSelection
import com.scnu.schedule.data.jwxt.ZhengFangClient
import com.scnu.schedule.domain.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class JwxtViewModel @Inject constructor(
    private val client: ZhengFangClient,
    private val importUseCase: JwxtImportUseCase,
) : ViewModel() {

    private val semesterSelection: SemesterSelection = JwxtSemesterResolver.resolve(LocalDate.now())

    private val _uiState = MutableStateFlow<JwxtUiState>(JwxtUiState.Loading)
    val uiState: StateFlow<JwxtUiState> = _uiState.asStateFlow()

    /** WebView 每完成一次主页面加载回调（带 CookieManager 里的 cookie 头）。 */
    fun onPageLoaded(url: String?, cookieHeader: String?) {
        if (isBusy()) return
        if (JwxtLoginDetector.isLoggedIn(url, cookieHeader)) {
            fetchSchedule()
        } else {
            _uiState.value = JwxtUiState.LoginRequired
        }
    }

    /** WebView 主框架加载错误（断网 / 校园网限制）。errorCode 为 WebViewClient.ERROR_* 常量。 */
    fun onWebError(errorCode: Int) {
        if (isBusy()) return
        _uiState.value = JwxtUiState.Error(
            kind = if (errorCode == -2 || errorCode == -6) JwxtErrorKind.NOT_ON_CAMPUS else JwxtErrorKind.NETWORK,
            message = "无法访问教务系统，请确认已连接校园网或通过 WebVPN 访问。",
        )
    }

    fun fetchSchedule() {
        if (isBusy()) return
        _uiState.value = JwxtUiState.Fetching
        viewModelScope.launch {
            try {
                val result = client.fetchSchedule(semesterSelection.xnm, semesterSelection.xqm)
                if (result.courses.isEmpty() && result.warnings.isEmpty()) {
                    _uiState.value = JwxtUiState.Error(
                        JwxtErrorKind.PARSE_FAILED,
                        "未解析到任何课程，可能是接口字段变化或本学期无课。",
                    )
                } else {
                    _uiState.value = JwxtUiState.Preview(result)
                }
            } catch (e: JwxtLoginExpiredException) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.LOGIN_FAILED, "登录已失效，请重新登录：${e.message}")
            } catch (e: JwxtNetworkException) {
                _uiState.value = JwxtUiState.Error(
                    JwxtErrorKind.NETWORK,
                    "网络请求失败：${e.message}。请确认已连接校园网或 WebVPN。",
                )
            } catch (e: JwxtParseException) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.PARSE_FAILED, "课表解析失败：${e.message}")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.UNKNOWN, "未知错误：${e.message}")
            }
        }
    }

    fun import(courses: List<Course>) {
        if (_uiState.value !is JwxtUiState.Preview || courses.isEmpty()) return
        _uiState.value = JwxtUiState.Importing
        viewModelScope.launch {
            try {
                importUseCase.import(courses, semesterSelection.name)
                _uiState.value = JwxtUiState.Success(courses.size)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = JwxtUiState.Error(JwxtErrorKind.UNKNOWN, "保存失败：${e.message}")
            }
        }
    }

    /** 从错误态返回登录等待（WebView 由 UI 层重载）。 */
    fun retry() {
        if (_uiState.value is JwxtUiState.Error) {
            _uiState.value = JwxtUiState.LoginRequired
        }
    }

    private fun isBusy(): Boolean =
        _uiState.value is JwxtUiState.Fetching ||
            _uiState.value is JwxtUiState.Preview ||
            _uiState.value is JwxtUiState.Importing ||
            _uiState.value is JwxtUiState.Success
}
