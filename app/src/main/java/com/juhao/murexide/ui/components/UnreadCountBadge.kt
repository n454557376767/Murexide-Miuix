package com.juhao.murexide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 未读数角标。
 *
 * 用 Miuix 的 `error` 角色而不是主色：Miuix 的 Badge 默认就是这个"需要注意"的角色，
 * 与主题主色（可能被莫奈/自定义色改写）解耦。
 */
@Composable
fun UnreadCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.error)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MiuixTheme.textStyles.footnote2,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onError,
            maxLines = 1
        )
    }
}
