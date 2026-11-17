package com.juhao.murexide.ui.components

import com.juhao.murexide.ui.icons.AppIcons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 兼容旧调用点保留；Miuix 分组容器（[Card]）自带形状，不再需要手工拼接圆角。 */
@Deprecated("Miuix 的 Card / Preference 组件自带形状，新代码不要再使用手工圆角")
fun settingsItemShape(isTop: Boolean, isBottom: Boolean): Shape {
    val cornerRadius = 24.dp
    val smallRadius = 4.dp
    return when {
        isTop && isBottom -> RoundedCornerShape(cornerRadius)
        isTop -> RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = smallRadius,
            bottomEnd = smallRadius
        )
        isBottom -> RoundedCornerShape(
            topStart = smallRadius,
            topEnd = smallRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
        else -> RoundedCornerShape(smallRadius)
    }
}

/** 分组标题：Miuix [SmallTitle]。 */
@Composable
fun SettingsGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    SmallTitle(text = title, modifier = modifier)
}

/**
 * 分组容器。
 *
 * Miuix 的 [Card] 只能自成一卡，而项目里同一分组被拆成多个 `SettingsGroupSurface` 段
 * （`isTop=true` → 中间段 → `isBottom=true`）。这里按段位拼接圆角，段间不留间隙，
 * 整组视觉上仍然是一张连续的 Miuix 卡片；行样式、行高、按压反馈由内部的
 * [BasicComponent] / [ArrowPreference] 负责。
 *
 * 外圆角直接取 [CardDefaults.CornerRadius]，与真正的 [Card] 保持一致。
 */
@Composable
fun SettingsGroupSurface(
    isTop: Boolean = true,
    isBottom: Boolean = true,
    id: String? = null,
    highlightId: String? = null,
    content: @Composable () -> Unit
) {
    val isTarget = id != null && id == highlightId
    var visible by remember(isTarget) { mutableStateOf(isTarget) }

    LaunchedEffect(isTarget) {
        if (isTarget) {
            visible = true
            delay(1800)
            visible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "settings_highlight_alpha"
    )

    val radius = CardDefaults.CornerRadius
    val shape = when {
        isTop && isBottom -> RoundedCornerShape(radius)
        isTop -> RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        isBottom -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = radius,
            bottomEnd = radius
        )
        else -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = if (isBottom) 12.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MiuixTheme.colorScheme.surfaceContainer)
        ) {
            content()
        }
        // 设置项定位高亮
        if (alpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { this.alpha = alpha }
                    .clip(shape)
                    .background(Color(0x33FFC107))
            )
        }
    }
}

@Composable
fun SettingsCustomItem(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            content()
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/** 可点击的设置行（带 HyperOS 尾部箭头）。
 *
 * 有图标时用 [BasicComponent] + 尾部箭头（`ArrowPreference` 没有 `startAction` 插槽）；
 * 无图标时直接用 [ArrowPreference]，由组件自己提供行样式与按压反馈。
 */
@Composable
fun SettingsItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    isEnabled: Boolean = true,
    isDestructive: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    if (icon == null) {
        ArrowPreference(
            title = title,
            summary = subtitle,
            enabled = isEnabled,
            onClick = onClick
        )
    } else {
        BasicComponent(
            title = title,
            summary = subtitle,
            enabled = isEnabled,
            startAction = { PreferenceIcon(icon, title) },
            endActions = {
                trailingContent?.invoke()
                PreferenceArrow()
            },
            onClick = onClick
        )
    }
}

/** 静态设置行（无箭头）：Miuix [BasicComponent]。 */
@Composable
fun SettingsItemCell(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    isEnabled: Boolean = true,
    isDestructive: Boolean = false,
    endIcon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    BasicComponent(
        title = title,
        summary = subtitle,
        enabled = isEnabled,
        startAction = if (icon != null) {
            { PreferenceIcon(icon, title) }
        } else null,
        endActions = if (endIcon != null) {
            { PreferenceIcon(endIcon, null) }
        } else null,
        onClick = onClick
    )
}

/** 开关设置行：Miuix 行容器 + Miuix [Switch]（整行点击与开关本身都可切换）。 */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
    isDestructive: Boolean = false
) {
    BasicComponent(
        title = title,
        summary = subtitle,
        enabled = isEnabled,
        startAction = if (icon != null) {
            { PreferenceIcon(icon, title) }
        } else null,
        endActions = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled
            )
        },
        onClick = { if (isEnabled) onCheckedChange(!checked) }
    )
}

/** 下拉设置行：Miuix [OverlayDropdownPreference]（需要页面 Scaffold 作为弹层宿主）。 */
@Composable
fun SettingsDropdownItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onOptionSelected: (String) -> Unit,
    isEnabled: Boolean = true
) {
    val selectedIndex = remember(options, selectedValue) {
        options.indexOfFirst { it.first == selectedValue }.coerceAtLeast(0)
    }
    // OverlayDropdownPreference 无 startAction 插槽，行首图标暂不显示
    OverlayDropdownPreference(
        title = title,
        summary = subtitle,
        items = options.map { it.second },
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            options.getOrNull(index)?.let { onOptionSelected(it.first) }
        },
        enabled = isEnabled
    )
}

/** 设置行前置图标。 */
@Composable
private fun PreferenceIcon(
    icon: ImageVector,
    contentDescription: String?
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp),
        tint = MiuixTheme.colorScheme.onSurfaceContainer
    )
}

/** 设置行尾部箭头（与 ArrowPreference 一致的视觉）。 */
@Composable
private fun PreferenceArrow() {
    Icon(
        imageVector = AppIcons.NavigateNext,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantActions
    )
}

@Composable
fun CustomItemCell(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val alpha = if (isEnabled) 1f else 0.38f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .then(
                if (onClick != null && isEnabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
