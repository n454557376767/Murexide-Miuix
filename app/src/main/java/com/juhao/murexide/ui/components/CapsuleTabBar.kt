package com.juhao.murexide.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS 风格的胶囊分段标签栏（应用自有组件）。
 *
 * 颜色沿用 Miuix `TabRow` 的语义配对：容器 `surfaceContainer`，
 * 选中项 `surface` + `onBackground`，未选中 `onSurfaceVariantSummary`；
 * 不写死色值，跟随浅色/深色/莫奈主题变化。
 */
@Composable
fun CapsuleTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return

    val selectedIndex = selectedTabIndex.coerceIn(tabs.indices)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val backgroundColor by animateColorAsState(
                    targetValue = if (selected) {
                        MiuixTheme.colorScheme.surface
                    } else {
                        MiuixTheme.colorScheme.surfaceContainer
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "capsule tab background"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) {
                        MiuixTheme.colorScheme.onBackground
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "capsule tab text color"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(index) }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
