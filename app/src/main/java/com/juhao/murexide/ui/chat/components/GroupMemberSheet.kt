package com.juhao.murexide.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juhao.murexide.data.GroupMember
import com.juhao.murexide.ui.components.Avatar
import com.juhao.murexide.ui.components.MiuixBottomSheet
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import com.juhao.murexide.ui.components.MiuixSearchField
import com.juhao.murexide.ui.icons.AppIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GroupMemberSheet(
    title: String,
    members: List<GroupMember>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onMemberClick: (GroupMember) -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String = "",
    onSearchChange: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, members.size, hasMore, isLoading) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            Pair(lastVisible, info.totalItemsCount)
        }.collect { (lastVisible, total) ->
            if (hasMore && !isLoading && total > 0 && lastVisible >= total - 5) {
                onLoadMore()
            }
        }
    }

    MiuixBottomSheet(
        onDismissRequest = onDismiss,
        skipPartiallyExpanded = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (onSearchChange != null) {
                MiuixSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "搜索成员",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (members.isEmpty() && isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    MiuixLoadingIndicator()
                }
            } else if (members.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "没有匹配的成员" else "暂无成员",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(members, key = { it.userId }) { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMemberClick(member) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Avatar(url = member.avatarUrl, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${member.userId}",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            when (member.permissionLevel) {
                                100 -> RoleBadge("群主")
                                2 -> RoleBadge("管理员")
                            }
                        }
                    }

                    if (isLoading && members.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                MiuixLoadingIndicator(size = 24.dp, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String) {
    // 角色标签：容器/前景走 Miuix 的 secondaryContainer 配对
    Box(
        modifier = Modifier
            .background(
                MiuixTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSecondaryContainer
        )
    }
}