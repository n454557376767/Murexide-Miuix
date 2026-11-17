package com.juhao.murexide.ui.components

import com.juhao.murexide.ui.icons.AppIcons

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Switch
import com.juhao.murexide.ui.theme.liquidglass.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun StyledButton(
    onClick: () -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    tint: Color = Color(0xFF0088FF),
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    if (backdrop == null) {
        // 无玻璃背景时用 Miuix 按钮，液态玻璃路径仍走自绘实现
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColorsPrimary(),
            content = content
        )
    } else {
        val animationScope = rememberCoroutineScope()

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope
            )
        }

        val interactive = enabled && isInteractive

        val effectiveTint = if (enabled) tint else Color.Gray

        Row(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = if (interactive) {
                        {
                            val width = size.width
                            val height = size.height

                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                        (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                        (height / width).fastCoerceAtMost(1f)
                        }
                    } else {
                        null
                    },
                    onDrawSurface = {
                        if (effectiveTint.isSpecified) {
                            drawRect(effectiveTint, blendMode = BlendMode.Hue)
                            drawRect(effectiveTint.copy(alpha = 0.75f))
                        }
                        if (surfaceColor.isSpecified) {
                            drawRect(surfaceColor)
                        }
                    }
                )
                .clickable(
                    interactionSource = null,
                    indication = if (interactive) null else LocalIndication.current,
                    role = Role.Button,
                    enabled = enabled,
                    onClick = onClick
                )
                .then(
                    if (interactive) {
                        Modifier
                            .then(interactiveHighlight.modifier)
                            .then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    }
                )
                .height(48f.dp)
                .padding(horizontal = 16f.dp),
            horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun StyledSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Miuix Switch：默认外观已带 HyperOS 的滑块/轨道语义色，不再叠加 thumbContent
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
fun ExpressiveDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MenuDefaults.standaloneGroupShape,
        content = content
    )
}
