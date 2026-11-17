package com.juhao.murexide.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AutoMirroredIcon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 迁移到 Miuix 的页面外壳：Miuix [Scaffold] + [TopAppBar]。
 *
 * 需要位于 [MiuixTheme] 之下（由 `MurexideTheme` 在应用根部统一提供，页面内不要再嵌套主题）。
 *
 * @param title 顶栏标题（Miuix 的标题是 String，不支持任意 composable）
 * @param subtitle 顶栏副标题，空字符串表示不显示
 * @param onBack 非空时显示默认返回按钮；与 [navigationIcon] 同时提供时以 [navigationIcon] 为准
 * @param navigationIcon 自定义顶栏前导按钮（关闭、禁用返回等）
 * @param actions 顶栏右侧操作区
 * @param bottomBar 底部栏（如输入框、操作条）
 * @param floatingActionButton 悬浮按钮
 * @param snackbarHost 提示条宿主，走 Scaffold 的 snackbar 槽位
 * @param containerColor 页面底色，默认使用 Miuix 的 `background`
 * @param content 页面内容，需消费传入的 [PaddingValues]
 */
@Composable
fun MiuixPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = {
                    when {
                        navigationIcon != null -> navigationIcon()
                        onBack != null -> IconButton(onClick = onBack) {
                            AutoMirroredIcon(
                                imageVector = AppIcons.ArrowBack,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        content = content,
    )
}
