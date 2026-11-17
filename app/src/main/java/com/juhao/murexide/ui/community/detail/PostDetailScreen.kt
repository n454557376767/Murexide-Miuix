package com.juhao.murexide.ui.community.detail

import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AutoMirroredIcon

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juhao.murexide.data.*
import com.juhao.murexide.datastore.AccountStorage
import com.juhao.murexide.repository.ConversationRepository
import com.juhao.murexide.ui.chat.ChatActivity
import com.juhao.murexide.ui.conversationdetail.ConversationDetailActivity
import com.juhao.murexide.ui.community.ba.BaDetailActivity
import com.juhao.murexide.ui.community.ba.CreatePostActivity
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.ui.theme.LocalLiquidGlassBlur
import com.juhao.murexide.ui.theme.LocalLiquidGlassEnabled
import com.juhao.murexide.ui.theme.liquidglass.glassControl
import com.juhao.murexide.ui.theme.UiState
import com.juhao.murexide.ui.theme.UiCache
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PostDetailScreen(
    onBackClick: () -> Unit,
    viewModel: PostDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var showShareSheet by remember { mutableStateOf(false) }
    val isSharing by viewModel.isSharing.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInputSheet by remember { mutableStateOf(false) }
    var replyToComment by remember { mutableStateOf<CommentItem?>(null) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMoreComments && !uiState.isLoadingComments) {
            viewModel.loadMoreComments()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBackClick()
        }
    }

    val hazeState = remember { HazeState() }
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val themeColor by UiState.themeColor
    val topBarBackgroundColor = if (themeColor == "WHITE") {
        MiuixTheme.colorScheme.surfaceContainer
    } else {
        MiuixTheme.colorScheme.surface
    }
    val liquidBackdrop = if (liquidGlassEnabled) {
        rememberLayerBackdrop {
            drawRect(topBarBackgroundColor)
            drawContent()
        }
    } else {
        null
    }

    // Miuix 对话框：WindowDialog 承载，不依赖页面 Scaffold 宿主
    MiuixAlertDialog(
        show = showDeleteDialog,
        title = "确认删除",
        summary = "确定要删除此文章吗？此操作不可撤销。",
        confirmText = "删除",
        onConfirm = {
            viewModel.deletePost()
            showDeleteDialog = false
        },
        onDismissRequest = { showDeleteDialog = false }
    )
    
    if (showShareSheet) {
        PostShareSheet(
            isSharing = isSharing,
            onDismiss = { if (!isSharing) showShareSheet = false },
            onShare = { targets ->
                viewModel.sharePost(targets) { success, error ->
                    if (success) {
                        showShareSheet = false
                        Toast.makeText(context, "已分享", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, error ?: "分享失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showInputSheet) {
        CommentInputSheet(
            replyTo = replyToComment,
            onDismiss = {
                showInputSheet = false
                replyToComment = null
            },
            onSend = { text ->
                val parentId = replyToComment?.id ?: 0
                viewModel.submitComment(text, commentId = parentId) { success ->
                    if (success) {
                        showInputSheet = false
                        replyToComment = null
                        Toast.makeText(context, "评论成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "评论失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            FloatingTopBar(
                hazeState = hazeState,
                liquidBackdrop = liquidBackdrop,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        AutoMirroredIcon(AppIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                avatar = {
                    uiState.post?.let { post ->
                        Avatar(url = post.senderAvatar, alwaysCircle = true, size = 40.dp)
                    }
                },
                title = {
                    uiState.post?.let { post ->
                        Column {
                            Text(
                                text = post.senderNickname,
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = post.createTimeText,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1
                            )
                        }
                    }
                },
                onTitleClick = {
                    uiState.post?.let { post ->
                        ConversationDetailActivity.start(
                            context = context,
                            chatId = post.senderId,
                            chatType = 1,
                            chatName = post.senderNickname,
                            chatAvatar = post.senderAvatar
                        )
                    }
                },
                actions = {
                    Box {
                        MiuixPopupMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            MiuixPopupMenuItem(
                                text = "复制原文",
                                icon = AppIcons.ContentCopy,
                                onClick = {
                                    showMenu = false
                                    val content = uiState.post?.content ?: ""
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("post", content))
                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                }
                            )
                            MiuixPopupMenuItem(
                                text = "分享",
                                icon = AppIcons.Share,
                                onClick = {
                                    showMenu = false
                                    showShareSheet = true
                                }
                            )
                            if (uiState.isManageEnabled) {
                                MiuixPopupMenuItem(
                                    text = "编辑文章",
                                    icon = AppIcons.Edit,
                                    onClick = {
                                        showMenu = false
                                        CreatePostActivity.startEdit(
                                            context = context,
                                            content = uiState.post?.content ?: "",
                                            contentType = uiState.post?.contentType ?: 1,
                                            postId = uiState.post?.id ?: return@MiuixPopupMenuItem,
                                            title = uiState.post?.title ?: ""
                                        )
                                    }
                                )
                                MiuixPopupMenuItem(
                                    text = "删除文章",
                                    icon = AppIcons.Delete,
                                    tint = MiuixTheme.colorScheme.error,
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showMenu = true }) {
                            AutoMirroredIcon(AppIcons.MoreVert, contentDescription = "更多")
                        }
                    }
                }
            )
        },
        bottomBar = {
            CommentBottomBar(
                hazeState = hazeState,
                liquidBackdrop = liquidBackdrop,
                enabled = uiState.post != null,
                isLiked = uiState.post?.isLiked == 1,
                isCollected = uiState.post?.isCollected == 1,
                likeCount = uiState.post?.likeNum ?: 0,
                commentCount = uiState.post?.commentNum ?: 0,
                collectCount = uiState.post?.collectNum ?: 0,
                onInputClick = {
                    replyToComment = null
                    showInputSheet = true
                },
                onLikeClick = { viewModel.toggleLike() },
                onCommentClick = {
                    scope.launch { listState.animateScrollToItem(2) }
                },
                onCollectClick = { viewModel.toggleCollect() }
            )
        }
    ) { innerPadding ->
        val post = uiState.post
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (liquidBackdrop != null) {
                        Modifier
                            .layerBackdrop(liquidBackdrop)
                            .hazeSource(hazeState)
                    } else {
                        Modifier.hazeSource(hazeState)
                    }
                )
        ) {
            when {
                uiState.isLoadingDetail && post == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        MiuixLoadingIndicator()
                    }
                }

                post == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "post_header_${post.id}") {
                            PostHeader(post = post, ba = uiState.ba, viewModel = viewModel)
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "评论",
                                style = MiuixTheme.textStyles.title4,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (uiState.comments.isEmpty() && !uiState.isLoadingComments) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无评论",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                            }
                        }

                        items(uiState.comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                onReplyClick = {
                                    replyToComment = comment
                                    showInputSheet = true
                                }
                            )
                        }

                        if (uiState.isLoadingComments) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MiuixLoadingIndicator(size = 28.dp)
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
private fun CommentBottomBar(
    hazeState: HazeState,
    liquidBackdrop: Backdrop?,
    enabled: Boolean,
    isLiked: Boolean,
    isCollected: Boolean,
    likeCount: Int,
    commentCount: Int,
    collectCount: Int,
    onInputClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCollectClick: () -> Unit
) {
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val liquidGlassBlur = LocalLiquidGlassBlur.current
    val controlSize = 48.dp
    val buttonShape = CircleShape
    val barColor = MiuixTheme.colorScheme.surfaceContainer
    val barStyle = HazeMaterials.thin().then {
        blurRadius(32.dp)
        noiseFactor(0f)
    }
    val useGlassBar = liquidGlassEnabled && liquidBackdrop != null
    val glassSurfaceColor = barColor.copy(alpha = 0.8f)

    @Composable
    fun Modifier.glassControl(
        shape: Shape,
        isInteractive: Boolean = true
    ): Modifier = glassControl(shape, liquidBackdrop, hazeState, useGlassBar, isInteractive)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            barColor.copy(alpha = 0.6f),
                            barColor.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(controlSize)
                    .glassControl(RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = enabled, onClick = onInputClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    AppIcons.Edit,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "说点什么…",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Row(
                modifier = Modifier
                    .height(controlSize)
                    .glassControl(buttonShape),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarAction(
                    icon = if (isLiked) AppIcons.Favorite else AppIcons.FavoriteBorder,
                    count = likeCount,
                    tint = if (isLiked) MiuixTheme.colorScheme.error
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    enabled = enabled,
                    onClick = onLikeClick
                )
                BottomBarAction(
                    icon = AppIcons.ChatBubble,
                    count = commentCount,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    enabled = enabled,
                    onClick = onCommentClick
                )
                BottomBarAction(
                    icon = if (isCollected) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                    count = collectCount,
                    tint = if (isCollected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    enabled = enabled,
                    onClick = onCollectClick
                )
            }
        }
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    count: Int,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = count.toString(),
            style = MiuixTheme.textStyles.footnote2,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun CommentInputSheet(
    replyTo: CommentItem?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val canSend = text.isNotBlank()

    MiuixBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (replyTo != null) {
                Text(
                    text = "回复 @${replyTo.senderNickname}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            // 多行输入：用应用自有的 Miuix 输入框（secondaryContainer 容器 + Miuix 文字/光标语义色）
            MiuixTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = if (replyTo != null) "回复 @${replyTo.senderNickname}…" else "说点什么…",
                singleLine = false,
                minHeight = 120,
                modifier = Modifier
                    .heightIn(min = 120.dp, max = 260.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (canSend) {
                        onSend(text.trim())
                    }
                },
                enabled = canSend,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(8.dp)
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
private fun PostHeader(post: PostDetail, ba: BaDetail?, viewModel: PostDetailViewModel) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Text(
            text = post.title,
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        if (post.contentType == 2) {
            MarkdownText(markdown = post.content, modifier = Modifier.fillMaxWidth())
        } else {
            SelectionContainer {
                Text(text = post.content, style = MiuixTheme.textStyles.body2)
            }
        }

        ba?.let {
            Spacer(Modifier.height(12.dp))
            BaCard(ba = it)
        }

        post.group?.takeIf { it.groupId.isNotEmpty() }?.let { group ->
            Spacer(Modifier.height(12.dp))
            GroupCard(group)
        }
    }
}

@Composable
private fun BaCard(ba: BaDetail) {
    val context = LocalContext.current
    // Miuix 分组卡片：Card 自带 surfaceContainer 容器与圆角，不再手工拼 Surface + 阴影色
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { BaDetailActivity.start(context, ba.id) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(url = ba.avatar, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ba.name,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer
                )
                Text(
                    text = "${ba.memberNum} 关注 · ${ba.postNum} 文章",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        }
    }
}

@Composable
private fun GroupCard(group: GroupInfo) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                ChatActivity.start(
                    context = context,
                    chatId = group.groupId,
                    chatType = 2,
                    chatName = group.name,
                    chatAvatar = group.avatarUrl
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(url = group.avatarUrl, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer
                )
                Text(
                    text = "${group.headcount} 人",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    onReplyClick: () -> Unit,
    isNested: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isNested) 48.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 4.dp
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Avatar(url = comment.senderAvatar, size = if (isNested) 24.dp else 32.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.senderNickname,
                    style = if (isNested) {
                        MiuixTheme.textStyles.footnote1
                    } else {
                        MiuixTheme.textStyles.body2
                    },
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.createTimeText,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.content,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "回复",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onReplyClick)
                )
            }
            if (comment.likeNum > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (comment.isLiked == "1") AppIcons.Favorite else AppIcons.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (comment.isLiked == "1") MiuixTheme.colorScheme.error
                        else MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = comment.likeNum.toString(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }

        if (comment.replies.isNotEmpty()) {
            comment.replies.forEach { reply ->
                CommentRow(
                    comment = reply,
                    onReplyClick = onReplyClick,
                    isNested = true
                )
            }
        }
    }
}

@Composable
private fun PostShareSheet(
    isSharing: Boolean,
    onDismiss: () -> Unit,
    onShare: (List<ForwardTarget>) -> Unit
) {
    val context = LocalContext.current
    val accountStorage = remember { AccountStorage.getInstance(context) }
    val conversationRepository = remember { ConversationRepository() }

    var isLoading by remember { mutableStateOf(true) }
    var targets by remember { mutableStateOf<List<ForwardTarget>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        val token = runCatching { accountStorage.getCurrentToken() }.getOrNull()
        if (token == null) {
            isLoading = false
            return@LaunchedEffect
        }
        val cached = UiCache.conversation.value
        val cachedSticky = UiCache.stickyConversations.value
        val merged = if (cached.isNotEmpty() || cachedSticky.isNotEmpty()) {
            mergeForwardTargets(cached, cachedSticky)
        } else {
            val convs = conversationRepository.getConversationList(token).getOrNull().orEmpty()
            val sticky = conversationRepository.getStickyList(token).getOrNull().orEmpty()
            mergeForwardTargets(convs, sticky)
        }
        targets = merged
        isLoading = false
    }

    val filtered = remember(targets, query) {
        if (query.isBlank()) targets
        else targets.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.chatId.contains(query, ignoreCase = true)
        }
    }

    MiuixBottomSheet(
        onDismissRequest = { if (!isSharing) onDismiss() },
        skipPartiallyExpanded = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "分享到会话",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            MiuixSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索会话",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) { MiuixLoadingIndicator() }

                filtered.isEmpty() -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "暂无可分享的会话" else "未找到相关会话",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filtered, key = { "${it.chatType}:${it.chatId}" }) { target ->
                        val selected = target.chatId in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = !isSharing) {
                                    selectedIds = if (selected) {
                                        selectedIds - target.chatId
                                    } else {
                                        selectedIds + target.chatId
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Avatar(url = target.avatarUrl, size = 44.dp)
                            Text(
                                text = target.displayName,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.surfaceContainerHigh
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = "已选择",
                                        modifier = Modifier.size(15.dp),
                                        tint = MiuixTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    enabled = !isSharing,
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.weight(1f)
                ) { Text("取消") }
                Button(
                    onClick = {
                        val selected = targets.filter { it.chatId in selectedIds }
                        if (selected.isNotEmpty()) onShare(selected)
                    },
                    enabled = selectedIds.isNotEmpty() && !isSharing,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSharing) {
                        MiuixLoadingIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("分享")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}