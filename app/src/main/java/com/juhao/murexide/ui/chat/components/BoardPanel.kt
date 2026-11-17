package com.juhao.murexide.ui.chat.components

import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AutoMirroredIcon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juhao.murexide.data.BoardItem
import com.juhao.murexide.ui.components.CapsuleTabBar
import com.juhao.murexide.ui.components.MarkdownText
import com.juhao.murexide.ui.components.LiteHtmlContent
import com.juhao.murexide.ui.theme.resolvedLiquidGlassContentColor
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BoardPanel(
    boards: List<BoardItem>,
    modifier: Modifier = Modifier,
    onImageClick: ((String) -> Unit)? = null
) {
    if (boards.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { boards.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        if (boards.size > 1) {
            CapsuleTabBar(
                tabs = boards.map { it.botName.ifBlank { "看板" } },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) { page ->
            BoardContent(board = boards[page], onImageClick = onImageClick)
        }
    }
}

@Composable
private fun BoardContent(
    board: BoardItem,
    onImageClick: ((String) -> Unit)?
) {
    val bodyContentColor = resolvedLiquidGlassContentColor(MiuixTheme.colorScheme.onBackground)
    val secondaryContentColor =
        resolvedLiquidGlassContentColor(MiuixTheme.colorScheme.onSurfaceVariantSummary)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AutoMirroredIcon(
                imageVector = AppIcons.VolumeUp,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.width(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = board.botName.ifBlank { "看板" },
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (board.lastUpdateTime > 0) {
                Text(
                    text = formatBoardTime(board.lastUpdateTime),
                    style = MiuixTheme.textStyles.footnote2,
                    color = secondaryContentColor
                )
            }
        }

        SelectionContainer {
            when (board.contentType) {
                BoardItem.CONTENT_TYPE_MARKDOWN -> MarkdownText(
                    markdown = board.content,
                    textColor = bodyContentColor,
                    onImageClick = onImageClick,
                    enableTextSelection = false
                )
                BoardItem.CONTENT_TYPE_HTML -> LiteHtmlContent(
                    htmlContent = board.content,
                    textColor = bodyContentColor,
                    onImageClick = onImageClick
                )
                else -> Text(
                    text = board.content,
                    style = MiuixTheme.textStyles.body2,
                    color = bodyContentColor
                )
            }
        }
    }
}

private fun formatBoardTime(ts: Long): String {
    val millis = if (ts < 1_000_000_000_000L) ts * 1000 else ts
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
}
