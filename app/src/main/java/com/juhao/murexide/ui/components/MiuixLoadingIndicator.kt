package com.juhao.murexide.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 风格加载指示器。
 *
 * Miuix 的 `ProgressIndicator` 参数在本仓库没有可核对的稳定签名，这里用 Canvas 画一段
 * 270° 圆弧并匀速旋转，取 `primary` 作为前景色，与 Miuix 的进度语义色一致。
 */
@Composable
fun MiuixLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MiuixTheme.colorScheme.primary,
    strokeWidth: Dp = 3.dp,
    // 传了 progress 就是确定进度：画一段固定角度的圆弧，并可选画轨道
    progress: (() -> Float)? = null,
    trackColor: Color? = null,
) {
    val target = progress?.invoke()?.coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "miuix_loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miuix_loading_rotation"
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(
            width = this.size.width - stroke,
            height = this.size.height - stroke
        )
        if (target == null) {
            // 不确定进度：270° 圆弧匀速旋转
            drawArc(
                color = color,
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        } else {
            // 确定进度：轨道 + 按比例绘制的前景弧
            trackColor?.let {
                drawArc(
                    color = it,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * target,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 应用自有的 Miuix 风格线性进度条（确定进度）。
 *
 * 用于上传/提交这类可计算进度的场景；颜色取 `secondaryContainer` 轨道 + `primary` 前景，
 * 与 Miuix 进度指示器的语义色一致。
 */
@Composable
fun MiuixLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
    trackColor: Color = MiuixTheme.colorScheme.secondaryContainer,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(radius)
        )
        if (clamped > 0f) {
            drawRoundRect(
                color = color,
                size = Size(size.width * clamped, size.height),
                cornerRadius = CornerRadius(radius)
            )
        }
    }
}

/**
 * [MiuixLinearProgress] 的 lambda 重载：兼容 Material3 的 \`progress = { ... }\` 调用形状。
 */
@Composable
fun MiuixLinearProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
    trackColor: Color = MiuixTheme.colorScheme.secondaryContainer
) {
    MiuixLinearProgress(
        progress = progress(),
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

/**
 * 应用自有的 Miuix 风格线性进度条（不确定进度）。
 *
 * 用于刷新、上传等无法计算进度的场景；与 [MiuixLinearProgress] 共用轨道与前景语义色。
 */
@Composable
fun MiuixIndeterminateLinearProgress(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
    trackColor: Color = MiuixTheme.colorScheme.secondaryContainer,
) {
    val transition = rememberInfiniteTransition(label = "miuix_indeterminate_progress")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miuix_indeterminate_progress_fraction"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(radius)
        )
        val barWidth = size.width * 0.35f
        val start = -barWidth + (size.width + barWidth) * fraction
        clipRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height
        ) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x = start, y = 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(radius)
            )
        }
    }
}
