package com.juhao.murexide.ui.settings

import com.juhao.murexide.ui.icons.AppIcons

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.juhao.murexide.datastore.AccountStorage
import com.juhao.murexide.datastore.UserAccount
import com.juhao.murexide.network.WebSocketManager
import com.juhao.murexide.ui.about.AboutActivity
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.plugin.PluginFunctionActivity
import com.juhao.murexide.plugin.PluginManager
import com.juhao.murexide.plugin.PluginManagerActivity
import com.juhao.murexide.ui.settings.chatbackground.ChatBackgroundActivity
import com.juhao.murexide.ui.settings.userbot.UserBotActivity
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun resolvePluginIcon(name: String?): ImageVector = when (name) {
    "Info" -> AppIcons.Info
    "SmartToy" -> AppIcons.SmartToy
    "Person" -> AppIcons.Person
    "Link" -> AppIcons.Link
    "Image" -> AppIcons.Image
    "Settings" -> AppIcons.Settings
    "Code" -> AppIcons.Code
    "Warning" -> AppIcons.Warning
    else -> AppIcons.Build
}

/**
 * 设置页入口。
 *
 * 搜索改为设置主页顶栏下方的常驻输入框（[MiuixSearchField]），不再使用 Material 的
 * 全屏 `SearchBar`：两步（点图标 → 展开搜索层）变成一步，且整页只有一套 Miuix 组件。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    page: Int = 0,
    highlightId: String? = null,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    BackHandler(enabled = page == 0) {
        onBack()
    }

    if (page == 0) {
        SettingsHomeScreen(
            onBack = onBack,
            onOpenPage = { targetPage ->
                context.startActivity(
                    Intent(context, SettingsActivity::class.java).putExtra("page", targetPage)
                )
            },
            onOpenAbout = {
                context.startActivity(Intent(context, AboutActivity::class.java))
            }
        )
    } else {
        MiuixPageScaffold(
            title = SettingsPageTitles[page] ?: "设置",
            onBack = onBack
        ) { padding ->
            when (page) {
                1 -> AppearanceControl(highlightId, contentPadding = padding)
                2 -> BubbleControl(highlightId, contentPadding = padding)
                3 -> ActionControl(highlightId, contentPadding = padding)
                4 -> UpdateControl(highlightId, contentPadding = padding)
                5 -> WebSocketControl(highlightId, contentPadding = padding)
                6 -> ScreenshotControl(highlightId, contentPadding = padding)
                7 -> AccountControl(highlightId, contentPadding = padding, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun SettingsHomeScreen(
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val accountStorage = remember(context.applicationContext) {
        AccountStorage.getInstance(context.applicationContext)
    }
    val wsManager = remember { WebSocketManager.getInstance() }

    val connected by wsManager.connectionState.collectAsState()
    var currentAccount by remember { mutableStateOf<UserAccount?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    val pluginFunctions by PluginManager.functions.collectAsState()

    LaunchedEffect(Unit) { currentAccount = accountStorage.getCurrentAccount() }

    val results = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            AllSettingsIndex.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.group.contains(query, ignoreCase = true)
            }
        }
    }

    MiuixPageScaffold(
        title = "设置",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenAbout) {
                Icon(
                    imageVector = AppIcons.Info,
                    contentDescription = "关于",
                    tint = MiuixTheme.colorScheme.onBackground
                )
            }
        },
        topBarBottom = {
            MiuixSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索设置",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            if (query.isNotBlank()) {
                if (results.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = AppIcons.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "未找到相关设置",
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                } else {
                    val grouped = results.groupBy { it.group }
                    grouped.forEach { (group, items) ->
                        item(key = "search_header_$group") {
                            SettingsGroupHeader(group)
                        }
                        itemsIndexed(items, key = { _, it -> "search_${it.id}" }) { index, item ->
                            SettingsGroupSurface(
                                isTop = index == 0,
                                isBottom = index == items.size - 1
                            ) {
                                SettingsItem(
                                    icon = item.icon,
                                    title = item.title,
                                    onClick = {
                                        query = ""
                                        context.startActivity(
                                            Intent(context, SettingsActivity::class.java)
                                                .putExtra("page", item.page)
                                                .putExtra("highlight_id", item.id)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                currentAccount?.let { account ->
                    item(key = "account_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(url = account.avatar, size = 64.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = account.username,
                                        style = MiuixTheme.textStyles.title3,
                                        maxLines = 1,
                                        color = MiuixTheme.colorScheme.onSurfaceContainer
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "ID: ${account.id}",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                                    )
                                }
                                Button(onClick = { onOpenPage(7) }) {
                                    Text("账号")
                                }
                            }
                        }
                    }
                }

                item { SettingsGroupHeader("个性化") }
                item {
                    SettingsGroupSurface(isTop = true, isBottom = false) {
                        SettingsItem(
                            icon = AppIcons.Style,
                            title = "应用外观",
                            subtitle = "主题模式、主题颜色、液态玻璃等",
                            onClick = { onOpenPage(1) }
                        )
                    }
                }
                item {
                    SettingsGroupSurface(isTop = false, isBottom = true) {
                        SettingsItem(
                            icon = AppIcons.ChatBubble,
                            title = "消息气泡",
                            subtitle = "自定义气泡透明度、圆角大小、控件显示等",
                            onClick = { onOpenPage(2) }
                        )
                    }
                }

                item { SettingsGroupHeader("功能") }
                item {
                    SettingsGroupSurface(isTop = true, isBottom = false) {
                        SettingsItem(
                            icon = AppIcons.DragClick,
                            title = "行为",
                            subtitle = "大屏模式、截图、通知等",
                            onClick = { onOpenPage(3) }
                        )
                    }
                }
                item {
                    SettingsGroupSurface(isTop = false, isBottom = false) {
                        SettingsItem(
                            icon = AppIcons.Link,
                            title = "WebSocket",
                            subtitle = "连接状态、自动重连开关",
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (connected) Color(0xFF4CAF50)
                                            else MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.4f)
                                        )
                                )
                            },
                            onClick = { onOpenPage(5) }
                        )
                    }
                }
                item {
                    SettingsGroupSurface(isTop = false, isBottom = false) {
                        SettingsItem(
                            icon = AppIcons.SmartToy,
                            title = "用户 Bot",
                            subtitle = "关键词自动回复、自动撤回",
                            onClick = { UserBotActivity.start(context) }
                        )
                    }
                }
                item {
                    SettingsGroupSurface(isTop = false, isBottom = true) {
                        SettingsItem(
                            icon = AppIcons.Image,
                            title = "聊天背景",
                            subtitle = "为聊天设置背景图",
                            onClick = {
                                context.startActivity(Intent(context, ChatBackgroundActivity::class.java))
                            }
                        )
                    }
                }

                item { SettingsGroupHeader("应用") }
                item {
                    SettingsGroupSurface(isTop = true, isBottom = false) {
                        SettingsItem(
                            icon = AppIcons.Build,
                            title = "插件",
                            subtitle = "导入 zip 扩展应用功能",
                            onClick = { PluginManagerActivity.start(context) }
                        )
                    }
                }
                item {
                    SettingsGroupSurface(isTop = false, isBottom = false) {
                        SettingsItem(
                            icon = AppIcons.Update,
                            title = "更新",
                            subtitle = "检查更新、更新频道",
                            onClick = { onOpenPage(4) }
                        )
                    }
                }
                item {
                    SettingsGroupSurface(isTop = false, isBottom = true) {
                        SettingsItem(
                            icon = AppIcons.Person,
                            title = "账号",
                            subtitle = "切换账号、提取 Token、退出登录",
                            onClick = { onOpenPage(7) }
                        )
                    }
                }
                if (pluginFunctions.isNotEmpty()) {
                    item { SettingsGroupHeader("额外功能") }
                    itemsIndexed(
                        pluginFunctions,
                        key = { _, it -> "plugin_fn_${it.id}" }
                    ) { index, entry ->
                        SettingsGroupSurface(
                            isTop = index == 0,
                            isBottom = index == pluginFunctions.lastIndex
                        ) {
                            SettingsItem(
                                icon = resolvePluginIcon(entry.icon),
                                title = entry.title,
                                subtitle = entry.subtitle,
                                onClick = { PluginFunctionActivity.start(context, entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
