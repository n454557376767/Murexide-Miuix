package com.juhao.murexide.ui.conversationdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juhao.murexide.data.GroupMember
import com.juhao.murexide.data.GroupTag
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AutoMirroredIcon
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val tagPalette = listOf(
    "#2196F3", "#E91E63", "#4CAF50", "#FF9800", "#9C27B0", "#00BCD4",
    "#F44336", "#3F51B5", "#8BC34A", "#FFC107", "#795548", "#607D8B"
)

private fun tagColor(color: String): Color {
    val hex = color.trim().ifBlank { "#2196F3" }
    return runCatching {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    }.getOrDefault(Color(0xFF2196F3))
}

@Composable
fun GroupTagScreen(
    isAdmin: Boolean,
    viewModel: GroupTagViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toast.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val activeTag = state.activeTag
    if (activeTag == null) {
        TagListPage(state = state, viewModel = viewModel, onBack = onBack, isAdmin = isAdmin)
    } else {
        TagMemberPage(tag = activeTag, state = state, viewModel = viewModel, isAdmin = isAdmin)
    }

    if (state.showEditor) {
        TagEditorDialog(state = state, viewModel = viewModel)
    }

    val deleteTarget = state.deleteTarget
    if (state.showDeleteConfirm && deleteTarget != null) {
        MiuixAlertDialog(
            show = true,
            title = "删除标签",
            summary = "确定要删除标签「${deleteTarget.tag}」吗？已关联该标签的成员会失去此标签。",
            confirmText = "删除",
            onConfirm = viewModel::confirmDelete,
            onDismissRequest = viewModel::dismissDelete
        )
    }
}

@Composable
private fun TagListPage(
    isAdmin: Boolean,
    state: GroupTagUiState,
    viewModel: GroupTagViewModel,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()

    LoadMoreEffect(
        listState = listState,
        itemCount = state.tags.size,
        canLoadMore = state.hasMore && !state.isLoading && !state.isLoadingMore,
        onLoadMore = viewModel::loadMoreTags
    )

    MiuixPageScaffold(
        title = "标签管理",
        onBack = onBack,
        floatingActionButton = {
            if (isAdmin) {
                // Miuix 没有 ExtendedFloatingActionButton，改用带图标的主色按钮
                Button(
                    onClick = { viewModel.openEditor(null) },
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Icon(AppIcons.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新建标签")
                }
            }
        },
        topBarBottom = {
            MiuixSearchField(
                value = state.keyword,
                onValueChange = viewModel::searchTags,
                placeholder = "搜索标签",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.tags.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center
            ) { MiuixLoadingIndicator() }

            state.tags.isEmpty() && state.error != null -> EmptyPlaceholder(
                modifier = Modifier.padding(padding),
                icon = AppIcons.Warning,
                title = "加载失败",
                subtitle = state.error,
                primaryButton = {
                    Button(
                        onClick = viewModel::loadTags,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) { Text("重试") }
                }
            )

            state.tags.isEmpty() -> EmptyPlaceholder(
                modifier = Modifier.padding(padding),
                icon = AppIcons.Tag,
                title = "暂无标签",
                subtitle = "创建标签后可以为群成员添加标识"
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp
                )
            ) {
                items(state.tags, key = { it.id }) { tag ->
                    TagRow(
                        isAdmin = isAdmin,
                        tag = tag,
                        onClick = { viewModel.openTag(tag) },
                        onEdit = { viewModel.openEditor(tag) },
                        onDelete = { viewModel.requestDelete(tag) }
                    )
                }
                if (state.hasMore) {
                    item(key = "tags-load-more") {
                        Box(Modifier.fillMaxWidth().height(52.dp), Alignment.Center) {
                            if (state.isLoadingMore) {
                                MiuixLoadingIndicator(size = 22.dp, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(
    isAdmin: Boolean,
    tag: GroupTag,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember(tag.id) { mutableStateOf(false) }
    // Miuix 卡片行替代 ListItem；颜色点 + 标题 + 描述 + 尾部菜单
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
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tagColor(tag.color))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = tag.tag,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tag.desc.ifBlank { "排序 ${tag.sort}" },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAdmin) {
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.MoreVert,
                            contentDescription = "更多",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                    MiuixPopupMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        MiuixPopupMenuItem(
                            text = "编辑标签",
                            icon = AppIcons.Edit,
                            onClick = {
                                expanded = false
                                onEdit()
                            }
                        )
                        MiuixPopupMenuItem(
                            text = "删除标签",
                            icon = AppIcons.Delete,
                            tint = MiuixTheme.colorScheme.error,
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagMemberPage(
    isAdmin: Boolean,
    tag: GroupTag,
    state: GroupTagUiState,
    viewModel: GroupTagViewModel
) {
    val listState = rememberLazyListState()

    LaunchedEffect(tag.id) { listState.scrollToItem(0) }

    LoadMoreEffect(
        listState = listState,
        itemCount = state.members.size,
        canLoadMore = state.hasMoreMembers &&
            !state.isLoadingMembers &&
            !state.isLoadingMoreMembers,
        onLoadMore = viewModel::loadMoreMembers
    )

    MiuixPageScaffold(
        title = tag.tag,
        subtitle = "已添加 ${state.boundUserIds.size} 人",
        onBack = viewModel::closeTag,
        topBarBottom = {
            MiuixSearchField(
                value = state.memberKeyword,
                onValueChange = viewModel::searchMembers,
                placeholder = "搜索成员",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    ) { padding ->
        when {
            state.isLoadingMembers && state.members.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center
            ) { MiuixLoadingIndicator() }

            state.members.isEmpty() -> EmptyPlaceholder(
                modifier = Modifier.padding(padding),
                icon = AppIcons.Group,
                title = "暂无成员",
                subtitle = "换一个关键词试试"
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
            ) {
                items(state.members, key = { it.userId }) { member ->
                    TagMemberRow(
                        member = member,
                        bound = member.userId in state.boundUserIds,
                        pending = state.pendingUserId == member.userId,
                        onClick = { if (isAdmin) viewModel.toggleMember(member) }
                    )
                }
                if (state.hasMoreMembers) {
                    item(key = "members-load-more") {
                        Box(Modifier.fillMaxWidth().height(52.dp), Alignment.Center) {
                            if (state.isLoadingMoreMembers) {
                                MiuixLoadingIndicator(size = 22.dp, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagMemberRow(
    member: GroupMember,
    bound: Boolean,
    pending: Boolean,
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
            Avatar(url = member.avatarUrl, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        member.permissionLevel == 100 -> "群主"
                        member.permissionLevel >= 2 -> "管理员"
                        else -> "成员"
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
            if (pending) {
                MiuixLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (bound) AppIcons.CheckCircle else AppIcons.CheckBoxOutlineBlank,
                    contentDescription = if (bound) "移除标签" else "添加标签",
                    tint = if (bound) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
        }
    }
}

@Composable
private fun TagEditorDialog(
    state: GroupTagUiState,
    viewModel: GroupTagViewModel
) {
    MiuixAlertDialog(
        show = true,
        title = if (state.draftIsNew) "新建标签" else "编辑标签",
        confirmText = if (state.isSubmitting) "保存中…" else "保存",
        confirmEnabled = !state.isSubmitting,
        onConfirm = viewModel::saveDraft,
        onDismissRequest = { if (!state.isSubmitting) viewModel.closeEditor() }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiuixTextField(
                    value = state.draftName,
                    onValueChange = viewModel::updateDraftName,
                    placeholder = "标签名称",
                    singleLine = true
                )
                MiuixTextField(
                    value = state.draftDesc,
                    onValueChange = viewModel::updateDraftDesc,
                    placeholder = "标签描述",
                    singleLine = true
                )
                MiuixTextField(
                    value = state.draftSort,
                    onValueChange = viewModel::updateDraftSort,
                    placeholder = "排序",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(
                    text = "标签颜色",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tagPalette.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { hex ->
                                val selected = state.draftColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(tagColor(hex))
                                        .clickable { viewModel.updateDraftColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
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
private fun LoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, itemCount, canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (totalItems > 0 && lastVisible >= totalItems - 3) onLoadMore()
        }
    }
}