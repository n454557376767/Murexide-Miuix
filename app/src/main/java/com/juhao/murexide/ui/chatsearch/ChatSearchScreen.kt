package com.juhao.murexide.ui.chatsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juhao.murexide.data.MessageItem
import com.juhao.murexide.ui.components.Avatar
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import com.juhao.murexide.ui.components.MiuixPageScaffold
import com.juhao.murexide.ui.components.MiuixSearchField
import com.juhao.murexide.utils.formatTimestamp
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChatSearchScreen(
    chatName: String,
    viewModel: ChatSearchViewModel,
    onBack: () -> Unit,
    onOpenMessage: (msgId: String, msgSeq: Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last to listState.layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (last, total) ->
                if (total > 0 && last >= total - 3) {
                    viewModel.loadMore()
                }
            }
    }

    MiuixPageScaffold(
        title = "搜索聊天记录",
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Miuix 胶囊搜索框：常驻输入 + 一键清除
            MiuixSearchField(
                value = state.keyword,
                onValueChange = viewModel::onKeywordChange,
                placeholder = "搜索 ${chatName} 中的消息…",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                state.isLoading -> CenterLoading()
                state.error != null -> CenterError(state.error!!, viewModel::retry)
                state.keyword.isBlank() -> CenterHint("输入关键词搜索聊天记录")
                state.results.isEmpty() -> CenterHint("未找到相关消息")
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.results, key = { it.msgId }) { message ->
                            SearchResultItem(
                                message = message,
                                onClick = { onOpenMessage(message.msgId, message.msgSeq) }
                            )
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MiuixLoadingIndicator(size = 24.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    message: MessageItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(url = message.senderAvatar, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderName.ifBlank { "未知用户" },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                    )
                }
                Spacer(Modifier.size(2.dp))
                Text(
                    text = previewText(message),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun previewText(message: MessageItem): String = when (message.contentType) {
    MessageItem.CONTENT_TYPE_TEXT,
    MessageItem.CONTENT_TYPE_MARKDOWN,
    MessageItem.CONTENT_TYPE_HTML -> message.content.ifBlank { "[消息]" }

    MessageItem.CONTENT_TYPE_IMAGE -> "[图片]"
    MessageItem.CONTENT_TYPE_FILE -> "[文件] ${message.fileName.orEmpty()}".trim()
    MessageItem.CONTENT_TYPE_VIDEO -> "[视频]"
    MessageItem.CONTENT_TYPE_AUDIO -> "[语音]"
    MessageItem.CONTENT_TYPE_STICKER -> "[表情]"
    MessageItem.CONTENT_TYPE_POST -> "[文章] ${message.postTitle.orEmpty()}".trim()
    else -> message.getDisplayContent()
}

@Composable
private fun CenterLoading() = Box(Modifier.fillMaxSize(), Alignment.Center) {
    MiuixLoadingIndicator()
}

@Composable
private fun CenterHint(text: String) = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}

@Composable
private fun CenterError(message: String, retry: () -> Unit) = Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Text(
        text = message,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.error
    )
    Spacer(Modifier.size(8.dp))
    Button(
        onClick = retry,
        colors = ButtonDefaults.buttonColorsPrimary()
    ) {
        Text("重试")
    }
}
