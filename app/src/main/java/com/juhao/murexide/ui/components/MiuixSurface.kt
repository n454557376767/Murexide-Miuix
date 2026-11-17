package com.juhao.murexide.ui.components

import androidx.compose.foundation.LocalContentColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix Surface（替代 Material3 的 \`Surface\` 调用形状）。
 *
 * Miuix 自带 \`Surface\`，但它的参数表在本仓库没有可核对的稳定快照，而项目里的调用点大量使用
 * M3 的参数名（\`shape\` / \`color\` / \`contentColor\` / \`onClick\`）。这里做一个薄门面：
 * 保留这些参数名，视觉统一走 Miuix 语义（默认容器 \`surfaceContainer\`、圆角取 [CardDefaults.CornerRadius]）。
 *
 * @param onClick 非空时整块可点击（等价 M3 的可点击 Surface 重载）
 */
@Composable
fun MiuixSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CardDefaults.CornerRadius),
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
