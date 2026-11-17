package com.juhao.murexide.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 应用自有的 Miuix 对话框外壳。
 *
 * 用 [WindowDialog] 而不是 `OverlayDialog`：设置项里的确认框可能出现在 Activity、
 * BottomSheet 或其他没有 Miuix `Scaffold` 祖先的位置，Window 变体不依赖页面宿主。
 *
 * 这里刻意不传 Miuix 对话框的 `title`/`summary` 等可选参数，标题与正文用主题文字自行排版，
 * 一旦上游对话框签名变化，只需要改这一个适配层。
 *
 * @param show 是否显示（受控）
 * @param title 标题
 * @param onDismissRequest 关闭请求（返回键、点击遮罩）；调用方负责把 [show] 置为 false
 * @param summary 可选说明文字
 * @param confirmText 确认按钮文案；为 null 时不显示确认按钮
 * @param onConfirm 确认回调；为 null 时点击确认只关闭对话框
 * @param dismissText 取消按钮文案；为 null 时不显示取消按钮
 * @param content 额外内容（表单、滑块等），默认用纵向布局
 */
@Composable
fun MiuixAlertDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    confirmText: String? = "确定",
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    dismissText: String? = "取消",
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    WindowDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground
            )
            if (!summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
            if (content != null) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
            if (dismissText != null || confirmText != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissText != null) {
                        Button(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.buttonColors()
                        ) {
                            Text(dismissText)
                        }
                    }
                    if (dismissText != null && confirmText != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (confirmText != null) {
                        Button(
                            onClick = {
                                onConfirm?.invoke()
                                if (onConfirm == null) onDismissRequest()
                            },
                            enabled = confirmEnabled,
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text(confirmText)
                        }
                    }
                }
            }
        }
    }
}
