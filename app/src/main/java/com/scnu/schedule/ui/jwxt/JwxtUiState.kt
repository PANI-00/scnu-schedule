package com.scnu.schedule.ui.jwxt

import com.scnu.schedule.domain.importing.ImportResult

/** 教务导入 UI 状态机。 */
sealed interface JwxtUiState {
    data object Loading : JwxtUiState                  // WebView 初始化
    data object Ready : JwxtUiState                    // 网页就绪，等待用户点「导入当前课表」
    data class Preview(val result: ImportResult) : JwxtUiState
    data object Importing : JwxtUiState
    data class Success(val count: Int) : JwxtUiState
    data class Error(val kind: JwxtErrorKind, val message: String) : JwxtUiState
}

enum class JwxtErrorKind {
    LOGIN_FAILED,      // 登录失效/失败
    NETWORK,           // 断网
    PARSE_FAILED,      // 解析失败
    NOT_ON_CAMPUS,     // 校外访问被拒（校园网限制，需 WebVPN）
    UNKNOWN,
}
