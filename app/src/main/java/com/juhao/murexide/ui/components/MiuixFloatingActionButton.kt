package com.juhao.murexide.ui.components

import androidx.compose.foundation.LocalContentColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 悬浮按钮（替代 Material3 [androidx.compose.material3.FloatingActionButton]）。
 *
 * Miuix 的 [top.yukonga.miuix.kmp.basic.FloatingActionButton] 参数表在本仓库没有可核对的快照，
 * 这里保留 M3 的调用形状（onClick / shape / containerColor / contentColor），视觉走 Miuix 语义：
 * 主色容器 + onPrimary 内容 + 一层阴影浮起。
 */
@Composable
fun MiuixFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = MiuixTheme.colorScheme.primary,
    contentColor: Color = MiuixTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** 小号悬浮按钮（40dp），对应 Material3 的 SmallFloatingActionButton。 */
@Composable
fun MiuixSmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    MiuixFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        size = 40.dp,
        content = content
    )
}
