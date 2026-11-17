package com.juhao.murexide.ui.chat.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juhao.murexide.repository.EditHistoryItem
import com.juhao.murexide.repository.MessageRepository
import com.juhao.murexide.ui.components.MiuixAlertDialog
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EditHistoryDialog(
    token: String,
    msgId: String,
    onDismiss: () -> Unit
) {
    val repository = MessageRepository()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var historyList by remember { mutableStateOf<List<EditHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }

    fun getEditHistory(
        loadMore: Boolean = false
    ) {
        scope.launch {
            if (!hasMore) return@launch
            isLoading = true
            repository.getMessageEditHistory(
                token = token,
                page = currentPage,
                msgId = msgId
            ).onSuccess { list ->
                isLoading = false
                if (list.isEmpty()) {
                    hasMore = false
                    return@onSuccess
                }
                historyList = if (loadMore) {
                    historyList + list
                } else {
                    list
                }
            }.onFailure {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        getEditHistory()
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleItem to layoutInfo.totalItemsCount
        }.collect { (lastVisibleItem, totalItems) ->
            if (
                totalItems > 0 &&
                lastVisibleItem >= totalItems - 3
            ) {
                currentPage++
                getEditHistory(true)
            }
        }
    }

    MiuixAlertDialog(
        show = true,
        title = "编辑历史",
        confirmText = "确定",
        onConfirm = onDismiss,
        onDismissRequest = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            items(historyList) { item ->
                Item(item)
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        MiuixLoadingIndicator(size = 24.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(
    data: EditHistoryItem
) {
    val timestampDisplay = remember(data.msgTime) {
        try {
            val date = Date(data.msgTime)
            val now = Date()

            val todayCalendar = Calendar.getInstance().apply {
                time = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            when {
                date.after(todayCalendar.time) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                date.after(Date(todayCalendar.timeInMillis - 86400000)) -> "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            ""
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.getOldText(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainer
                )
                Text(
                    text = "编辑时间：$timestampDisplay",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
        }
    }
}