package com.juhao.murexide.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 风格滑块。
 *
 * Miuix 公开的 `Slider` 参数在本仓库没有可核对的稳定签名，因此这里用 Compose
 * Foundation 原语自行绘制；颜色全部走 Miuix 的滑块语义 token
 * （激活轨道 `primary`、未激活轨道 `sliderBackground`、滑块 `onPrimary`、
 * 禁用态 `disabledPrimarySlider`），并补齐 `progressBarRangeInfo` / `setProgress`
 * 无障碍语义，保证 TalkBack 与系统无障碍服务可用。
 *
 * 受控组件：滑块位置始终由 [value] 决定，拖动时通过 [onValueChange] 回传。
 */
@Composable
fun MiuixSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    thumbColor: Color = Color.Unspecified,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

    val start = valueRange.start
    val end = valueRange.endInclusive
    val low = minOf(start, end)
    val high = maxOf(start, end)
    val span = (end - start).takeIf { it != 0f } ?: 1f
    val fraction = ((value - start) / span).coerceIn(0f, 1f)

    fun snap(raw: Float): Float {
        val coerced = raw.coerceIn(low, high)
        if (steps <= 0) return coerced
        val stepSize = span / (steps + 1)
        return (start + ((coerced - start) / stepSize).toInt() * stepSize).coerceIn(low, high)
    }

    val resolvedActive = when {
        !enabled -> MiuixTheme.colorScheme.disabledPrimarySlider
        activeColor != Color.Unspecified -> activeColor
        else -> MiuixTheme.colorScheme.primary
    }
    val resolvedInactive = if (inactiveColor != Color.Unspecified) {
        inactiveColor
    } else {
        MiuixTheme.colorScheme.sliderBackground
    }
    val resolvedThumb = when {
        !enabled -> MiuixTheme.colorScheme.disabledPrimarySlider
        thumbColor != Color.Unspecified -> thumbColor
        else -> MiuixTheme.colorScheme.onPrimary
    }

    val gestures = if (enabled) {
        Modifier.pointerInput(start, end, steps) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val widthPx = size.width.toFloat().coerceAtLeast(1f)

                fun update(x: Float) {
                    val f = (x / widthPx).coerceIn(0f, 1f)
                    currentOnValueChange(snap(start + f * span))
                }

                update(down.position.x)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    update(change.position.x)
                    change.consume()
                }
                currentOnFinished?.invoke()
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value,
                    range = valueRange,
                    steps = steps
                )
                if (enabled) {
                    setProgress { target ->
                        currentOnValueChange(snap(target))
                        currentOnFinished?.invoke()
                        true
                    }
                }
            }
            .then(gestures)
    ) {
        val thumbRadius = 10.dp.toPx()
        val trackStroke = 4.dp.toPx()
        val centerY = size.height / 2f
        val usable = (size.width - thumbRadius * 2f).coerceAtLeast(1f)
        val thumbX = thumbRadius + usable * fraction

        drawRoundRect(
            color = resolvedInactive,
            topLeft = Offset(thumbRadius, centerY - trackStroke / 2f),
            size = Size(usable, trackStroke),
            cornerRadius = CornerRadius(trackStroke / 2f)
        )
        drawRoundRect(
            color = resolvedActive,
            topLeft = Offset(thumbRadius, centerY - trackStroke / 2f),
            size = Size((usable * fraction).coerceAtLeast(0f), trackStroke),
            cornerRadius = CornerRadius(trackStroke / 2f)
        )
        drawCircle(
            color = resolvedThumb,
            radius = thumbRadius,
            center = Offset(thumbX, centerY)
        )
    }
}

/**
 * 设置行 + 滑块：图标 / 标题 + 当前值胶囊 + 滑块 + 两端刻度文案。
 *
 * 统一设置页里各处手写滑块行的间距、字号与配色。
 */
@Composable
fun SettingsSliderItem(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    startLabel: String? = null,
    endLabel: String? = null,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) {
                        MiuixTheme.colorScheme.onSurfaceContainer
                    } else {
                        MiuixTheme.colorScheme.disabledOnSurface
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1,
                color = if (enabled) {
                    MiuixTheme.colorScheme.onSurfaceContainer
                } else {
                    MiuixTheme.colorScheme.disabledOnSurface
                }
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MiuixTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueText,
                    style = MiuixTheme.textStyles.footnote2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished
        )
        if (startLabel != null || endLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = startLabel.orEmpty(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
                Text(
                    text = endLabel.orEmpty(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
        }
    }
}
