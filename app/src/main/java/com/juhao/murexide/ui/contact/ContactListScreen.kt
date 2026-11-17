package com.juhao.murexide.ui.contact

import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AutoMirroredIcon

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juhao.murexide.data.ContactGroup
import com.juhao.murexide.data.ContactItem
import com.juhao.murexide.data.ContactRequestItem
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.ui.theme.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class ContactTab(
    val title: String,
    val chatType: Int,
    val icon: ImageVector,
    val emptyTitle: String,
    val emptyDescription: String
)

private val ContactTabs = listOf(
    ContactTab("好友", 1, AppIcons.People, "还没有好友", "添加好友后会显示在这里"),
    ContactTab("群聊", 2, AppIcons.Groups, "还没有加入群聊", "加入的群聊会显示在这里"),
    ContactTab("机器人", 3, AppIcons.SmartToy, "还没有机器人", "添加的机器人会显示在这里")
)

private val ContactIndexCellSize = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    token: String,
    innerPadding: PaddingValues,
    onContactClick: (ContactItem) -> Unit,
    onNewMessagesVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: ContactViewModel = remember(token) { ContactViewModel(token) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewMessages by rememberSaveable { mutableStateOf(false) }

    val setNewMessagesVisible: (Boolean) -> Unit = { visible ->
        showNewMessages = visible
        onNewMessagesVisibilityChanged(visible)
    }

    BackHandler(enabled = showNewMessages) {
        setNewMessagesVisible(false)
    }

    LaunchedEffect(showNewMessages) {
        onNewMessagesVisibilityChanged(showNewMessages)
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUserMessage()
    }

    AnimatedContent(
        targetState = showNewMessages,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 300),
                    initialOffsetX = { fullWidth -> fullWidth }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 220, delayMillis = 80)
                )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300),
                    targetOffsetX = { fullWidth -> -fullWidth / 4 }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 160)
                ))
            } else {
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = 300),
                    initialOffsetX = { fullWidth -> -fullWidth / 4 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 220, delayMillis = 80)
                )) togetherWith (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300),
                    targetOffsetX = { fullWidth -> fullWidth }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 160)
                ))
            }
        },
        label = "contact_page_transition"
    ) { newMessagesVisible ->
        if (newMessagesVisible) {
            NewMessagesScreen(
                uiState = uiState,
                innerPadding = innerPadding,
                snackbarHostState = snackbarHostState,
                onBack = { setNewMessagesVisible(false) },
                onRefresh = { viewModel.loadRequests() },
                onRespond = viewModel::respondToRequest
            )
        } else {
            ContactDirectoryScreen(
                uiState = uiState,
                innerPadding = innerPadding,
                snackbarHostState = snackbarHostState,
                onRefresh = viewModel::refreshAll,
                onOpenNewMessages = {
                    setNewMessagesVisible(true)
                    if (!uiState.isRequestsLoading) {
                        viewModel.loadRequests(showLoading = uiState.requests.isEmpty())
                    }
                },
                onContactClick = onContactClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDirectoryScreen(
    uiState: ContactUiState,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onOpenNewMessages: () -> Unit,
    onContactClick: (ContactItem) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val friendListState = rememberLazyListState()
    val groupListState = rememberLazyListState()
    val botListState = rememberLazyListState()
    val currentTab = ContactTabs[selectedTabIndex]
    val listState = when (selectedTabIndex) {
        0 -> friendListState
        1 -> groupListState
        else -> botListState
    }

    MiuixPageScaffold(
        title = "通讯录",
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(innerPadding)
            )
        },
        actions = {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = AppIcons.Refresh,
                        contentDescription = "刷新通讯录",
                        tint = MiuixTheme.colorScheme.onBackground
                    )
                }
            }
        },
        topBarBottom = {
            CapsuleTabBar(
                tabs = ContactTabs.map { it.title },
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    ) { paddingValues ->
        ContactTabContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            innerPadding = innerPadding,
            groups = uiState.contactGroups,
            tab = currentTab,
            listState = listState,
            isLoading = uiState.isLoading,
            error = uiState.error,
            pendingRequestCount = uiState.pendingRequestCount,
            isRequestsLoading = uiState.isRequestsLoading,
            onRetry = onRefresh,
            onOpenNewMessages = onOpenNewMessages,
            onContactClick = onContactClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactTabContent(
    modifier: Modifier,
    innerPadding: PaddingValues,
    groups: List<ContactGroup>,
    tab: ContactTab,
    listState: LazyListState,
    isLoading: Boolean,
    error: String?,
    pendingRequestCount: Int,
    isRequestsLoading: Boolean,
    onRetry: () -> Unit,
    onOpenNewMessages: () -> Unit,
    onContactClick: (ContactItem) -> Unit
) {
    val contacts = remember(groups, tab.chatType) {
        groups.asSequence()
            .filter { it.chatType == tab.chatType }
            .flatMap { it.contacts.asSequence() }
            .toList()
    }
    val sections = remember(contacts) { buildContactSections(contacts) }
    val showNewMessageShortcut = tab.chatType == 1
    val headerIndices = remember(sections, showNewMessageShortcut) {
        var itemIndex = if (showNewMessageShortcut) 1 else 0
        buildMap {
            sections.forEach { section ->
                put(section.initial, itemIndex)
                itemIndex += section.contacts.size + 1
            }
        }
    }
    val currentSection by remember(listState, headerIndices) {
        derivedStateOf {
            headerIndices
                .filterValues { it <= listState.firstVisibleItemIndex }
                .maxByOrNull { it.value }
                ?.key
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (showNewMessageShortcut) {
                item(key = "new_messages", contentType = "shortcut") {
                    NewMessageShortcut(
                        pendingCount = pendingRequestCount,
                        isLoading = isRequestsLoading,
                        onClick = onOpenNewMessages
                    )
                }
            }

            if (sections.isEmpty()) {
                item(key = "contact_state", contentType = "state") {
                    when {
                        isLoading -> LoadingState(Modifier.fillMaxSize())

                        error != null -> EmptyPlaceholder(
                            icon = AppIcons.Warning,
                            title = "加载失败",
                            subtitle = error,
                            primaryButton = {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColorsPrimary()
                                ) {
                                    Text("重试")
                                }
                            }
                        )

                        else -> EmptyPlaceholder(
                            icon = tab.icon,
                            title = tab.emptyTitle,
                            subtitle = tab.emptyDescription
                        )
                    }
                }
            } else {
                sections.forEach { section ->
                    stickyHeader(key = "section_${tab.chatType}_${section.initial}") {
                        ContactSectionHeader(section.initial)
                    }
                    items(
                        items = section.contacts,
                        key = { "${it.chatType}:${it.chatId}" },
                        contentType = { "contact" }
                    ) { contact ->
                        Column {
                            ContactItemRow(
                                contact = contact,
                                onClick = { onContactClick(contact) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 78.dp, end = 32.dp)
                            )
                        }
                    }
                }
            }
        }

        if (sections.isNotEmpty()) {
            AlphabetFastScroller(
                modifier = Modifier.fillMaxSize(),
                headerIndices = headerIndices,
                listState = listState,
                currentSection = currentSection
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewMessageShortcut(
    pendingCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "新消息"
                stateDescription = if (pendingCount > 0) {
                    "有 $pendingCount 条待处理申请或邀请"
                } else {
                    "没有待处理申请或邀请"
                }
            },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.PersonAdd,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (pendingCount > 0) {
                    UnreadCountBadge(
                        count = pendingCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "新消息",
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        pendingCount > 0 -> "$pendingCount 条申请或邀请待处理"
                        isLoading -> "正在检查新消息…"
                        else -> "好友与群聊申请、邀请"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        }
    }
}

@Composable
private fun ContactSectionHeader(initial: Char) {
    SmallTitle(text = initial.toString())
}

@Composable
private fun ContactItemRow(
    contact: ContactItem,
    onClick: () -> Unit
) {
    val displayName = contactDisplayName(contact)
    val supportingText = when {
        !contact.remark.isNullOrBlank() && contact.name.isNotBlank() && contact.remark != contact.name ->
            contact.name
        contact.chatType == 2 -> "群聊 · ${contact.chatId}"
        contact.chatType == 3 -> "机器人 · ${contact.chatId}"
        else -> "ID: ${contact.chatId}"
    }

    BasicComponent(
        title = displayName,
        summary = supportingText,
        startAction = {
            Avatar(
                url = contact.avatarUrl,
                size = 48.dp
            )
        },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlphabetFastScroller(
    modifier: Modifier,
    headerIndices: Map<Char, Int>,
    listState: LazyListState,
    currentSection: Char?
) {
    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    val selectLetter: (Char) -> Unit = { requested ->
        closestAvailableInitial(requested, headerIndices.keys)?.let { target ->
            activeLetter = target
            headerIndices[target]?.let { itemIndex ->
                scrollJob?.cancel()
                scrollJob = scope.launch { listState.scrollToItem(itemIndex) }
            }
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = activeLetter != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter?.toString().orEmpty(),
                    style = MiuixTheme.textStyles.title2,
                    color = MiuixTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(ContactIndexCellSize * ContactIndexLetters.size.toFloat())
                .width(30.dp)
                .pointerInput(headerIndices) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun selectAt(y: Float) {
                            val index = ((y / size.height.coerceAtLeast(1)) * ContactIndexLetters.size)
                                .toInt()
                                .coerceIn(ContactIndexLetters.indices)
                            selectLetter(ContactIndexLetters[index])
                        }

                        selectAt(down.position.y)
                        down.consume()
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            pressed = change.pressed
                            if (pressed) {
                                selectAt(change.position.y)
                                change.consume()
                            }
                        }
                        activeLetter = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactIndexLetters.forEach { letter ->
                val isSelected = (activeLetter ?: currentSection) == letter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "$letter 索引"
                            onClick(label = "跳转到 $letter") {
                                selectLetter(letter)
                                activeLetter = null
                                true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(ContactIndexCellSize)
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        color = MiuixTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            color = if (isSelected) {
                                MiuixTheme.colorScheme.onPrimary
                            } else {
                                MiuixTheme.colorScheme.primary
                            },
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessagesScreen(
    uiState: ContactUiState,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRespond: (Int, Boolean) -> Unit
) {
    val requests = remember(uiState.requests) {
        uiState.requests.sortedWith(
            compareByDescending<ContactRequestItem> { it.isPending }
                .thenByDescending { it.invitedAt }
        )
    }

    MiuixPageScaffold(
        title = "新消息",
        onBack = onBack,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(innerPadding)
            )
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !uiState.isRequestsLoading) {
                Icon(
                    imageVector = AppIcons.Refresh,
                    contentDescription = "刷新新消息",
                    tint = MiuixTheme.colorScheme.onBackground
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isRequestsLoading && requests.isEmpty() -> LoadingState(Modifier.fillMaxSize())
                uiState.requestsError != null && requests.isEmpty() -> EmptyPlaceholder(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    icon = AppIcons.Warning,
                    title = "加载失败",
                    subtitle = uiState.requestsError,
                    primaryButton = {
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("重试")
                        }
                    }
                )
                requests.isEmpty() -> EmptyPlaceholder(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    icon = AppIcons.Inbox,
                    title = "暂无新消息",
                    subtitle = "好友和群聊申请、邀请会显示在这里"
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = paddingValues,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isRequestsLoading) {
                        item(key = "request_loading") {
                            MiuixIndeterminateLinearProgress(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                    if (uiState.requestsError != null) {
                        item(key = "request_error") {
                            InlineError(message = uiState.requestsError, onRetry = onRefresh)
                        }
                    }
                    items(
                        items = requests,
                        key = { it.requestId },
                        contentType = { "request" }
                    ) { request ->
                        RequestSurface(
                            request = request,
                            isProcessing = request.requestId in uiState.processingRequestIds,
                            onRespond = { accept -> onRespond(request.requestId, accept) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestSurface(
    request: ContactRequestItem,
    isProcessing: Boolean,
    onRespond: (Boolean) -> Unit
) {
    val description = buildList {
        request.contextName?.let(::add)
        add(request.typeLabel)
    }.joinToString(" · ")

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                RequestAvatar(
                    url = request.displayAvatarUrl,
                    type = when {
                        request.sourceType == 2 || request.targetType == 2 -> 2
                        request.sourceType == 3 || request.targetType == 3 -> 3
                        else -> 1
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = request.displayName,
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!request.isPending) {
                            RequestStatusPill(request.resultLabel)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (request.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = request.note,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (request.invitedAtText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = request.invitedAtText,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                        )
                    }
                }
            }

            if (request.isPending) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "处理中…",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                        )
                    } else {
                        Button(
                            onClick = { onRespond(false) },
                            colors = ButtonDefaults.buttonColors()
                        ) {
                            Text("拒绝")
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { onRespond(true) },
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("同意")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestAvatar(url: String, type: Int) {
    if (url.isNotBlank()) {
        Avatar(url = url, size = 52.dp)
        return
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (type) {
                2 -> AppIcons.Groups
                3 -> AppIcons.SmartToy
                else -> AppIcons.People
            },
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun RequestStatusPill(label: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ContainedLoadingIndicator()
    }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.errorContainer,
            contentColor = MiuixTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onErrorContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text("重试")
            }
        }
    }
}