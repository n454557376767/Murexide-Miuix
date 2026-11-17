package com.juhao.murexide.ui.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.juhao.murexide.ui.icons.AppIcons
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun VoiceRecordingBar(
    seconds: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    onPickVoiceFile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_recording_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_recording_dot_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer { this.alpha = dotAlpha }
                .background(MiuixTheme.colorScheme.error, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "正在录音",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Text(
                text = formatVoiceDuration(seconds),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPickVoiceFile,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    AppIcons.AudioFile,
                    contentDescription = "选择音频文件",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    AppIcons.Close,
                    contentDescription = "取消录音",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            // FilledIconButton → 主色圆形底 + Miuix IconButton
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (seconds > 0) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = seconds > 0,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        AppIcons.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(20.dp),
                        tint = if (seconds > 0) MiuixTheme.colorScheme.onPrimary
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }
}

private fun formatVoiceDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}