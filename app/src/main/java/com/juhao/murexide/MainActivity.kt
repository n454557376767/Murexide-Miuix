package com.juhao.murexide

import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AppFilledIcons
import com.juhao.murexide.ui.icons.AnimatedNavigationSymbol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.juhao.murexide.ui.chat.ChatActivity
import com.juhao.murexide.ui.contact.ContactListScreen
import com.juhao.murexide.ui.conversation.ConversationListScreen
import com.juhao.murexide.ui.conversation.CreationActivity
import com.juhao.murexide.ui.login.LoginActivity
import com.juhao.murexide.ui.mine.MineScreen
import com.juhao.murexide.ui.theme.MurexideTheme
import com.juhao.murexide.ui.theme.UiState
import com.juhao.murexide.datastore.SettingsStorage
import com.juhao.murexide.data.ConversationItem
import com.juhao.murexide.data.ConversationKey
import com.juhao.murexide.data.unreadTotal
import com.juhao.murexide.datastore.AccountStorage
import com.juhao.murexide.datastore.UserAccount
import com.juhao.murexide.data.local.LocalCache
import com.juhao.murexide.ui.chat.ChatScreen
import com.juhao.murexide.ui.chat.ChatViewModel
import com.juhao.murexide.ui.components.UnreadCountBadge
import com.juhao.murexide.ui.components.AccountQuickSwitchMenu
import com.juhao.murexide.ui.components.AccountQuickSwitchGlassMenu
import com.juhao.murexide.ui.community.CommunityScreen
import com.juhao.murexide.ui.discover.DiscoverScreen
import com.juhao.murexide.ui.settings.SettingsActivity
import androidx.compose.foundation.combinedClickable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.juhao.murexide.ui.theme.LocalLiquidGlassEnabled
import com.juhao.murexide.ui.theme.LocalLiquidGlassBlur
import com.juhao.murexide.ui.theme.liquidGlass
import com.juhao.murexide.ui.theme.liquidGlassContentColor
import com.juhao.murexide.ui.theme.liquidglass.LiquidBottomTabs
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.juhao.murexide.ui.components.MiuixSurface
import com.juhao.murexide.ui.components.MiuixBadgedBox

private data class NavItem(
    val route: String,
    val title: String,
    val outlineIcon: ImageVector,
    val filledIcon: ImageVector,
)

private val navItems = listOf(
    NavItem("conversations", "消息", AppIcons.ChatBubble, AppFilledIcons.ChatBubble),
    NavItem("contacts", "通讯录", AppIcons.Contacts, AppFilledIcons.Contacts),
    NavItem("community", "社区", AppIcons.Group, AppFilledIcons.Group),
    NavItem("discover", "发现", AppIcons.Explore, AppFilledIcons.Explore),
    NavItem("mine", "我的", AppIcons.Person, AppFilledIcons.Person),
)

private const val TAB_TRANSITION_DURATION_MILLIS = 300

@Composable
private fun HomeNavigationIcon(
    item: NavItem,
    selected: Boolean,
    unreadCount: Int
) {
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val inheritedGlassContentColor = LocalContentColor.current
    val icon: @Composable () -> Unit = {
        AnimatedNavigationSymbol(
            outlineIcon = item.outlineIcon,
            filledIcon = item.filledIcon,
            selected = selected,
            contentDescription = item.title,
            tint = if (selected) {
                MiuixTheme.colorScheme.primary
            } else if (liquidGlassEnabled) {
                inheritedGlassContentColor
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            }
        )
    }

    when (item.route) {
        "conversations" -> {
            MiuixBadgedBox(badge = { UnreadCountBadge(unreadCount) }) {
                icon()
            }
        }

        else -> icon()
    }
}

@Composable
private fun HomeNavigationLabel(
    item: NavItem,
    selected: Boolean,
    compact: Boolean = false,
) {
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val inheritedGlassContentColor = LocalContentColor.current

    val visible = compact || selected

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 280,
                easing = LinearOutSlowInEasing
            )
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = 280,
                easing = LinearOutSlowInEasing
            ),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutLinearInEasing
            )
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutLinearInEasing
            ),
            shrinkTowards = Alignment.Top
        )
    ) {
        Text(
            text = item.title,
            style = if (compact) {
                MiuixTheme.textStyles.footnote2.copy(letterSpacing = 0.sp)
            } else {
                LocalTextStyle.current
            },
            color = if (liquidGlassEnabled) {
                inheritedGlassContentColor
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            }
        )
    }
}

/**
 * 底栏长按探测（不消费指针事件）。
 *
 * Miuix 的 `NavigationBarItem` / `FloatingNavigationBarItem` 只暴露 `onClick`，
 * 而"长按『我的』打开账号快速切换菜单"是项目已有交互。这里在容器上观察长按，
 * 不 consume 任何事件，子项自己的点击与无障碍语义保持不变。
 */
private fun Modifier.onLongPressWithoutConsuming(onLongPress: () -> Unit): Modifier =
    pointerInput(onLongPress) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var pressed = true
            val releasedInTime = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (pressed) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    pressed = change?.pressed == true
                }
                true
            }
            if (releasedInTime == null) {
                onLongPress()
                // 等手指抬起，避免同一次长按重复触发
                while (pressed) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    pressed = change.pressed
                }
            }
        }
    }

/**
 * Miuix 底栏：固定 [NavigationBar] 或悬浮 [FloatingNavigationBar]。
 *
 * 选中色用 `primary`，未选中用摘要文字角色；未读数走角标槽位。
 * 玻璃拟态（LiquidGlass）分支仍由 [LiquidBottomTabs] 负责，不在此处叠加。
 */
@Composable
private fun MiuixBottomNavigationBar(
    currentRoute: String?,
    unreadCount: Int,
    floating: Boolean,
    showAccountMenu: Boolean,
    accounts: List<UserAccount>,
    currentAccountId: String,
    hazeState: HazeState,
    onNavigate: (String) -> Unit,
    onLongPress: () -> Unit,
    onDismissAccountMenu: () -> Unit,
) {
    val itemColors = NavigationBarDefaults.navigationBarItemColors(
        unselectedContentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        selectedContentColor = MiuixTheme.colorScheme.primary,
    )
    val barModifier = Modifier.onLongPressWithoutConsuming(onLongPress)

    if (floating) {
        Box(modifier = barModifier.fillMaxWidth()) {
            FloatingNavigationBar(horizontalOutSidePadding = 20.dp) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    FloatingNavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = if (selected) item.filledIcon else item.outlineIcon,
                        label = item.title,
                        colors = itemColors,
                        badge = if (item.route == "conversations" && unreadCount > 0) {
                            { UnreadCountBadge(unreadCount) }
                        } else {
                            null
                        },
                    )
                }
            }
            AccountQuickSwitchMenu(
                expanded = showAccountMenu,
                accounts = accounts,
                hazeState = hazeState,
                currentAccountId = currentAccountId,
                onDismissRequest = onDismissAccountMenu
            )
        }
    } else {
        Box(modifier = barModifier.fillMaxWidth()) {
            NavigationBar {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = if (selected) item.filledIcon else item.outlineIcon,
                        label = item.title,
                        colors = itemColors,
                        badge = if (item.route == "conversations" && unreadCount > 0) {
                            { UnreadCountBadge(unreadCount) }
                        } else {
                            null
                        },
                    )
                }
            }
            AccountQuickSwitchMenu(
                expanded = showAccountMenu,
                accounts = accounts,
                hazeState = hazeState,
                currentAccountId = currentAccountId,
                onDismissRequest = onDismissAccountMenu
            )
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSlideDirection():
        AnimatedContentTransitionScope.SlideDirection {
    val initialIndex = navItems.indexOfFirst { it.route == initialState.destination.route }
    val targetIndex = navItems.indexOfFirst { it.route == targetState.destination.route }
    return if (targetIndex > initialIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val accountStorage = AccountStorage.getInstance(this)

        lifecycleScope.launch {
            val account = accountStorage.getCurrentUserInfo()
            if (account == null || account.token.isEmpty()) {
                LoginActivity.start(this@MainActivity)
                finish()
                return@launch
            }

            LocalCache.setActiveAccount(account.id)

            setContent {
                MurexideTheme {
                    MiuixSurface(modifier = Modifier.fillMaxSize()) {
                        MainScreen(account)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(account: UserAccount) {
    val themeColor by UiState.themeColor
    val token = account.token
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }
    val liquidGlassEnabled = LocalLiquidGlassEnabled.current
    val liquidBackdrop = if (liquidGlassEnabled) rememberLayerBackdrop() else null
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settingsStorage = remember { SettingsStorage(context) }
    val bigScreenEnabled by settingsStorage.bigScreenFlow.collectAsState(initial = true)
    val floatBottomBarEnabled by settingsStorage.isFloatBottomBarEnabledFlow.collectAsState(initial = true)

    val isBigScreen = LocalConfiguration.current.screenWidthDp >= 600

    var currentConversation by remember { mutableStateOf<ConversationItem?>(null) }
    val cachedConversations by LocalCache.observeConversations(account.id)
        .collectAsState(initial = emptyList())
    val unreadCount = cachedConversations.unreadTotal(
        currentConversation?.takeIf { isBigScreen && bigScreenEnabled }
            ?.let { ConversationKey(it.chatId, it.chatType) }
    )

    var isContactNewMessagesVisible by remember { mutableStateOf(false) }

    var showAccountMenu by remember { mutableStateOf(false) }
    val accountStorage = remember(context.applicationContext) {
        AccountStorage.getInstance(context.applicationContext)
    }
    val loggedInAccounts by accountStorage.userAccountsFlow.collectAsState(initial = emptyList())

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val useNavigationRail = bigScreenEnabled && isBigScreen
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NavigationSuiteScaffold(
        layoutType = if (useNavigationRail) {
            NavigationSuiteType.NavigationRail
        } else {
            // 底部导航由 Miuix 的 NavigationBar / FloatingNavigationBar 接管，
            // NavigationSuiteScaffold 只保留大屏 NavigationRail 分支。
            NavigationSuiteType.None
        },
        navigationSuiteColors = if (themeColor == "WHITE") {
            NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MiuixTheme.colorScheme.surfaceContainer,
                navigationRailContainerColor = MiuixTheme.colorScheme.surfaceContainer
            )
        } else {
            NavigationSuiteDefaults.colors()
        },
        navigationSuiteItems = {
            navItems.forEach { item ->
                val selected = currentRoute == item.route
                item(
                    icon = {
                        when (item.route) {
                            "mine" -> {
                                Box(
                                    modifier = Modifier.combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { navigateTo(item.route) },
                                        onLongClick = { showAccountMenu = true }
                                    )
                                ) {
                                    AnimatedNavigationSymbol(
                                        outlineIcon = item.outlineIcon,
                                        filledIcon = item.filledIcon,
                                        selected = selected,
                                        contentDescription = item.title
                                    )
                                    AccountQuickSwitchMenu(
                                        expanded = showAccountMenu && !liquidGlassEnabled,
                                        accounts = loggedInAccounts,
                                        hazeState = hazeState,
                                        currentAccountId = account.id,
                                        onDismissRequest = { showAccountMenu = false }
                                    )
                                }
                            }

                            "conversations" -> {
                                MiuixBadgedBox(badge = { UnreadCountBadge(unreadCount) }) {
                                    AnimatedNavigationSymbol(
                                        outlineIcon = item.outlineIcon,
                                        filledIcon = item.filledIcon,
                                        selected = selected,
                                        contentDescription = item.title
                                    )
                                }
                            }

                            else -> {
                                AnimatedNavigationSymbol(
                                    outlineIcon = item.outlineIcon,
                                    filledIcon = item.filledIcon,
                                    selected = selected,
                                    contentDescription = item.title
                                )
                            }
                        }
                    },
                    label = { HomeNavigationLabel(item, selected) },
                    selected = selected,
                    onClick = { navigateTo(item.route) }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (!useNavigationRail) {
                    if (floatBottomBarEnabled && liquidGlassEnabled && liquidBackdrop != null) {
                        LiquidBottomTabs(
                            selectedTabIndex = navItems.indexOfFirst { it.route == currentRoute }
                                .coerceAtLeast(0),
                            onTabSelected = { index -> navigateTo(navItems[index].route) },
                            backdrop = liquidBackdrop,
                            tabsCount = navItems.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 10.dp),
                            onTabLongClick = { index ->
                                if (navItems[index].route == "mine") showAccountMenu = true
                            },
                            onTabLongClickLabel = { index ->
                                if (navItems[index].route == "mine") "打开账号菜单" else null
                            },
                        ) { index, selected, overlayPass ->
                            val item = navItems[index]
                            if (item.route == "mine") {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(
                                            2.dp,
                                            Alignment.CenterVertically
                                        )
                                    ) {
                                        HomeNavigationIcon(
                                            item = item,
                                            selected = selected,
                                            unreadCount = unreadCount
                                        )
                                        HomeNavigationLabel(item, selected, compact = true)
                                    }
                                    if (!overlayPass) {
                                        AccountQuickSwitchMenu(
                                            expanded = showAccountMenu && !liquidGlassEnabled,
                                            accounts = loggedInAccounts,
                                            hazeState = hazeState,
                                            currentAccountId = account.id,
                                            onDismissRequest = { showAccountMenu = false }
                                        )
                                    }
                                }
                            } else {
                                HomeNavigationIcon(
                                    item = item,
                                    selected = selected,
                                    unreadCount = unreadCount
                                )
                                HomeNavigationLabel(item, selected, compact = true)
                            }
                        }
                    } else {
                        MiuixBottomNavigationBar(
                            currentRoute = currentRoute,
                            unreadCount = unreadCount,
                            floating = floatBottomBarEnabled,
                            showAccountMenu = showAccountMenu && !liquidGlassEnabled,
                            accounts = loggedInAccounts,
                            currentAccountId = account.id,
                            hazeState = hazeState,
                            onNavigate = navigateTo,
                            onLongPress = { showAccountMenu = true },
                            onDismissAccountMenu = { showAccountMenu = false },
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "conversations",
                    modifier = Modifier
                        .then(
                            if (useNavigationRail && liquidGlassEnabled) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        )
                        .hazeSource(hazeState)
                        .then(
                            if (liquidBackdrop != null) {
                                Modifier.layerBackdrop(liquidBackdrop)
                            } else {
                                Modifier
                            }
                        ),
                    enterTransition = {
                        slideIntoContainer(
                            towards = tabSlideDirection(),
                            animationSpec = tween(TAB_TRANSITION_DURATION_MILLIS)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = tabSlideDirection(),
                            animationSpec = tween(TAB_TRANSITION_DURATION_MILLIS)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = tabSlideDirection(),
                            animationSpec = tween(TAB_TRANSITION_DURATION_MILLIS)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = tabSlideDirection(),
                            animationSpec = tween(TAB_TRANSITION_DURATION_MILLIS)
                        )
                    }
                ) {
                    composable("conversations") {
                        Row(modifier = Modifier.fillMaxSize()) {
                            ConversationListScreen(
                                modifier = Modifier
                                    .weight(if (isBigScreen && bigScreenEnabled) 0.4f else 1f)
                                    .fillMaxHeight(),
                                token = token,
                                accountId = account.id,
                                innerPadding = innerPadding,
                                bigScreenMode = isBigScreen && bigScreenEnabled,
                                currentConversation = if (isBigScreen && bigScreenEnabled) currentConversation else null,
                                onConversationClick = { conversation ->
                                    if (isBigScreen && bigScreenEnabled) {
                                        currentConversation = conversation
                                    } else {
                                        ChatActivity.start(
                                            context = context,
                                            chatId = conversation.chatId,
                                            chatType = conversation.chatType,
                                            chatName = conversation.displayName,
                                            chatAvatar = conversation.avatarUrl,
                                            initialUnreadCount = conversation.unreadMessage
                                        )
                                    }
                                },
                                onCreateClick = { kind -> CreationActivity.start(context, kind) }
                            )

                            if (isBigScreen && bigScreenEnabled) {
                                VerticalDivider()
                                if (currentConversation != null) {
                                    BackHandler {
                                        currentConversation = null
                                    }
                                    key(currentConversation!!.chatId) {
                                        ChatScreen(
                                            modifier = Modifier
                                                .weight(0.7f)
                                                .fillMaxHeight(),
                                            chatAvatar = currentConversation!!.avatarUrl,
                                            chatName = currentConversation!!.name,
                                            chatType = currentConversation!!.chatType,
                                            chatId = currentConversation!!.chatId,
                                            onBackClick = { currentConversation = null },
                                            onOpenConversation = { target ->
                                                currentConversation = target.toConversationItem()
                                            },
                                            bigScreenMode = true,
                                            backUnreadCount = unreadCount,
                                            viewModel = viewModel(
                                                key = "chat_" + currentConversation!!.chatId,
                                                factory = object : ViewModelProvider.Factory {
                                                    @Suppress("UNCHECKED_CAST")
                                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                                        return ChatViewModel(
                                                            token = token,
                                                            chatId = currentConversation!!.chatId,
                                                            chatType = currentConversation!!.chatType,
                                                            accountStorage = accountStorage,
                                                            currentUserId = account.id,
                                                            currentUserName = account.username,
                                                            currentUserAvatar = account.avatar
                                                        ) as T
                                                    }
                                                }
                                            )
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.ChatBubble,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .alpha(0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    composable("contacts") {
                        ContactListScreen(
                            token = token,
                            innerPadding = innerPadding,
                            onNewMessagesVisibilityChanged = { isVisible ->
                                isContactNewMessagesVisible = isVisible
                            },
                            onContactClick = { contact ->
                                ChatActivity.start(
                                    context = context,
                                    chatId = contact.chatId,
                                    chatType = contact.chatType,
                                    chatName = contact.remark ?: contact.name,
                                    chatAvatar = contact.avatarUrl
                                )
                            }
                        )
                    }

                    composable("community") {
                        CommunityScreen(
                            token = token,
                            innerPadding = innerPadding
                        )
                    }

                    composable("discover") {
                        DiscoverScreen(
                            token = token,
                            innerPadding = innerPadding
                        )
                    }

                    composable("mine") {
                        MineScreen(
                            token = token,
                            innerPadding = innerPadding,
                            onSettingsClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
        if (liquidGlassEnabled && liquidBackdrop != null) {
            AccountQuickSwitchGlassMenu(
                expanded = showAccountMenu,
                accounts = loggedInAccounts,
                currentAccountId = account.id,
                backdrop = liquidBackdrop,
                onDismissRequest = { showAccountMenu = false },
                cardAlignment = if (useNavigationRail) Alignment.BottomStart else Alignment.BottomEnd,
                cardPadding = if (useNavigationRail) {
                    PaddingValues(start = 8.dp, bottom = navigationBarInset + 12.dp)
                } else {
                    PaddingValues(end = 20.dp, bottom = navigationBarInset + 82.dp)
                }
            )
        }
    }
}
