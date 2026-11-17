package com.juhao.murexide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 角标（替代 Material3 [androidx.compose.material3.Badge] 的调用形状）。
 *
 * Miuix 的 [top.yukonga.miuix.kmp.basic.Badge] 参数表在本仓库没有可核对的稳定快照，
 * 而项目里大量调用使用 M3 的 \`containerColor\` / \`contentColor\` 命名；
 * 这里保留该命名，视觉走 Miuix 的错误色语义（HyperOS 未读角标即红底白字）。
 */
@Composable
fun MiuixBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.error,
    contentColor: Color = MiuixTheme.colorScheme.onError,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(content = content ?: {})
        }
    }
}

/**
 * 把角标叠在内容右上角，等价 Material3 [androidx.compose.material3.BadgedBox]。
 */
@Composable
fun MiuixBadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        Box(modifier = Modifier.align(Alignment.TopEnd), content = badge)
    }
}
