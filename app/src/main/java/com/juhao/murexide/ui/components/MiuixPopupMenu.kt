package com.juhao.murexide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 风格锚定菜单（顶栏「更多」这类操作菜单）。
 *
 * Miuix 的 `OverlayListPopup` / `WindowListPopup` 在本仓库没有可核对的稳定签名，
 * 因此这里用 Foundation 的 [Popup] 承载、内部装 Miuix [Card] 与语义色行，
 * 视觉与 Miuix 弹层（圆角卡片、surfaceContainer 底色、正文样式）保持一致。
 *
 * 位置相对调用处的父布局计算，因此要放在顶栏操作图标所在的同一个 [androidx.compose.foundation.layout.Box] 内，
 * [Alignment.TopEnd] 即"贴着父布局右上角往下偏移"。
 *
 * @param expanded 是否展开（受控）
 * @param onDismissRequest 关闭请求（点击空白、返回键）；调用方负责把 [expanded] 置为 false
 * @param alignment 相对父布局的锚点
 * @param offsetY 相对锚点向下的偏移量（dp）
 * @param content 菜单行，用 [MiuixPopupMenuItem] 或自定义行填充
 */
@Composable
fun MiuixPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offsetY: Int = 8,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    val offset = with(LocalDensity.current) { offsetY.dp.roundToPx() }
    Popup(
        alignment = alignment,
        offset = IntOffset(0, offset),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Card(modifier = modifier.widthIn(min = 168.dp)) {
            Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

/**
 * [MiuixPopupMenu] 的一行。
 *
 * @param tint 文字与图标颜色，默认正文色；危险操作传 [MiuixTheme.colorScheme.error]
 */
@Composable
fun MiuixPopupMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) tint else MiuixTheme.colorScheme.disabledOnSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = if (enabled) tint else MiuixTheme.colorScheme.disabledOnSurface
        )
    }
}
