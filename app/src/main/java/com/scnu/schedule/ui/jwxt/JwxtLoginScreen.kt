package com.scnu.schedule.ui.jwxt

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.scnu.schedule.data.jwxt.ZhengFangClient
import com.scnu.schedule.domain.importing.ImportResult
import com.scnu.schedule.domain.importing.ImportWarning
import com.scnu.schedule.domain.model.Course

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JwxtLoginScreen(
    onBack: () -> Unit,
    onImported: () -> Unit,
    vm: JwxtViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    @SuppressLint("SetJavaScriptEnabled")
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // [VERIFY] 若华师登录页在手机 UA 下显示异常，可换桌面 UA：
            // settings.userAgentString = DESKTOP_UA
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val cookieHeader = CookieManager.getInstance().getCookie(ZhengFangClient.JWXT_BASE)
                    vm.onPageLoaded(url, cookieHeader)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        vm.onWebError(error?.errorCode ?: -1)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        webView.loadUrl(ZhengFangClient.JWXT_BASE)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            // [DECISION] 保留会话 cookie，下次导入免登录；若要"退出登录"再手动清 CookieManager
            webView.removeAllViews()
            webView.destroy()
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("教务导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                if (uiState is JwxtUiState.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
            HorizontalDivider()
            when (val s = uiState) {
                is JwxtUiState.Loading -> BusyPanel("正在打开教务系统…")
                is JwxtUiState.LoginRequired -> HintPanel(
                    title = "请登录华师统一身份认证",
                    body = "在上方网页中使用一卡通账号 + 密码登录（初始密码为身份证后 8 位）。\n登录成功后会自动抓取本学期课表。",
                    action = {
                        TextButton(onClick = { webView.loadUrl(ZhengFangClient.JWXT_BASE) }) {
                            Text("重新加载")
                        }
                    },
                )
                is JwxtUiState.Fetching -> BusyPanel("正在从正方教务抓取课表…")
                is JwxtUiState.Preview -> PreviewPanel(s.result) { chosen -> vm.import(chosen) }
                is JwxtUiState.Importing -> BusyPanel("正在保存课程…")
                is JwxtUiState.Success -> SuccessPanel(s.count, onDone = onImported)
                is JwxtUiState.Error -> ErrorPanel(
                    kind = s.kind,
                    message = s.message,
                    onRetry = { vm.retry() },
                    onReload = { webView.loadUrl(ZhengFangClient.JWXT_BASE) },
                )
            }
        }
    }
}

private val WEEK_CN = listOf("一", "二", "三", "四", "五", "六", "日")

private fun dayLabel(day: Int): String =
    if (day in 1..7) "周${WEEK_CN[day - 1]}" else "周$day"

@Composable
private fun PreviewPanel(result: ImportResult, onConfirm: (List<Course>) -> Unit) {
    val selections = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(result.courses.size) {
        selections.clear()
        result.courses.indices.forEach { selections[it] = true }
    }
    Column(Modifier.fillMaxWidth().height(280.dp)) {
        Text(
            "解析到 ${result.courses.size} 门课（已按周次/节次拆条），勾选后导入",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (result.warnings.isNotEmpty()) {
            WarningBanner(result.warnings)
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(result.courses) { index, course ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selections[index] == true,
                        onCheckedChange = { selections[index] = it },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(course.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${dayLabel(course.dayOfWeek)} 第${course.startPeriod}-${course.endPeriod}节 · ${course.teacher} · ${course.location}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                val chosen = result.courses.filterIndexed { i, _ -> selections[i] == true }
                onConfirm(chosen)
            },
            enabled = selections.values.any { it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text("导入 ${selections.values.count { it }} 门课")
        }
    }
}

@Composable
private fun WarningBanner(warnings: List<ImportWarning>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "${warnings.size} 条数据未能解析：",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        warnings.take(3).forEach {
            Text(
                "· ${it.courseName}：${it.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
        if (warnings.size > 3) {
            Text("…", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun HintPanel(title: String, body: String, action: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        action()
    }
}

@Composable
private fun BusyPanel(text: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
private fun SuccessPanel(count: Int, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("已导入 $count 门课，已激活「石牌校区」作息表。", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onDone, modifier = Modifier.padding(top = 12.dp)) {
            Text("查看课表")
        }
    }
}

@Composable
private fun ErrorPanel(kind: JwxtErrorKind, message: String, onRetry: () -> Unit, onReload: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("导入失败（${kind.name}）", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onRetry) { Text("重试") }
            if (kind == JwxtErrorKind.LOGIN_FAILED || kind == JwxtErrorKind.NOT_ON_CAMPUS) {
                Button(onClick = onReload) { Text("重新加载网页") }
            }
        }
    }
}
