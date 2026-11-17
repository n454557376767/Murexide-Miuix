package com.juhao.murexide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 风格提示条宿主。
 *
 * Miuix 的 \`Snackbar\` 在本仓库没有可核对的稳定签名，因此保留 Material3 [SnackbarHost]
 * 作为队列/超时/无障碍的稳定承载，只把它的视觉换成 Miuix：
 * 卡片用 Miuix [Card]（surfaceContainer 容器 + HyperOS 圆角），文字用 Miuix 语义色与字号。
 *
 * 放进 Miuix \`Scaffold\` 的 \`snackbarHost\` 槽位即可，行为与 Material 版一致。
 */
@Composable
fun MiuixSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.visuals.message,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainer
                    )
                    val actionLabel = data.visuals.actionLabel
                    if (actionLabel != null) {
                        Text(
                            text = actionLabel,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clickable { data.performAction() }
                        )
                    }
                }
            }
        }
    }
}
