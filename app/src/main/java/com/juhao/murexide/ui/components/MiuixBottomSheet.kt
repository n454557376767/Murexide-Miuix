package com.juhao.murexide.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 风格底部弹层外壳。
 *
 * 为什么不是 Miuix 的 `OverlayBottomSheet` / `WindowBottomSheet`：
 * 这两个组件在本仓库没有可核对的稳定签名（无网络、无构件缓存可查），而弹层涉及手势、
 * 返回键和窗口内边距，签名猜错会直接编译失败。这里先用 Material3 的 [ModalBottomSheet]
 * 作为承载（它在这方面是稳定契约），把外观换成 Miuix 语义：
 * 容器取 `surfaceContainer`、去掉 M3 的拖拽把手、圆角与 Miuix Card 一致。
 *
 * 迁移到 Miuix 原生弹层时只需要改这一个文件，调用点不用动。
 *
 * @param onDismissRequest 关闭请求；调用方负责把对应的显示状态置为 false
 * @param skipPartiallyExpanded 是否跳过半展开态，默认直接全展开
 * @param content 弹层内容，自行处理 `navigationBarsPadding()` / `imePadding()`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuixBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        containerColor = MiuixTheme.colorScheme.surfaceContainer,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        content = content
    )
}
