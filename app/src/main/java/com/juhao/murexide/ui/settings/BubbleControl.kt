package com.juhao.murexide.ui.settings

import com.juhao.murexide.ui.icons.AppIcons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.juhao.murexide.data.MessageItem
import com.juhao.murexide.data.MessageTag
import com.juhao.murexide.datastore.SettingsStorage
import com.juhao.murexide.ui.chat.components.MessageBubble
import com.juhao.murexide.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun BubbleControl(
    highlightId: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val settingsStorage = remember { SettingsStorage(context) }
    val scope = rememberCoroutineScope()

    var bubbleCornerRadius by remember { mutableFloatStateOf(16f) }
    val showMyBubbleAvatar by settingsStorage.showMyBubbleAvatarFlow.collectAsState(initial = true)
    val showMsgTags by settingsStorage.showMsgTagsFlow.collectAsState(initial = true)
    var bubbleOpacity by remember { mutableFloatStateOf(0.9f) }

    LaunchedEffect(Unit) {
        bubbleCornerRadius = settingsStorage.getBubbleCornerRadius()
        bubbleOpacity = settingsStorage.getBubbleOpacity()
    }

    val previewMessages = remember {
        listOf(
            MessageItem(
                msgId = "preview_other",
                senderId = "other",
                senderName = "那狗吧",
                senderAvatar = "https://chat-img.jwznb.com/defalut-avatars/Nellie%20Bly.png",
                content = "你好！",
                contentType = MessageItem.CONTENT_TYPE_TEXT,
                timestamp = System.currentTimeMillis() - 60000,
                direction = "left",
                tags = listOf(
                    MessageTag(id = 0, text = "化学式", color = "#66CCFF"),
                    MessageTag(id = 1, text = "Hi", color = "#6DB4FF")
                )
            ),
            MessageItem(
                msgId = "preview_other",
                senderId = "other",
                senderName = "那狗吧",
                senderAvatar = "https://chat-img.jwznb.com/defalut-avatars/Nellie%20Bly.png",
                content = "看看这个气泡效果怎么样？",
                contentType = MessageItem.CONTENT_TYPE_TEXT,
                timestamp = System.currentTimeMillis() - 60000,
                direction = "left"
            ),
            MessageItem(
                msgId = "preview_me",
                senderId = "me",
                senderName = "我",
                senderAvatar = "https://chat-img.jwznb.com/defalut-avatars/Mary%20Roebling.png",
                content = "效果不错！可以调整圆角和透明度",
                contentType = MessageItem.CONTENT_TYPE_TEXT,
                timestamp = System.currentTimeMillis(),
                direction = "right"
            )
        )
    }

    val allIds = remember { listOf(
        "header_preview", "bubble_preview",
        "header_bubble",
        "bubble_corner", "bubble_opacity", "bubble_my_avatar", "bubble_tags"
    ) }

    val listState = rememberLazyListState()
    LaunchedEffect(highlightId, allIds) {
        if (!highlightId.isNullOrBlank()) {
            val index = allIds.indexOf(highlightId)
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item { SettingsGroupHeader("预览") }
        item {
            SettingsGroupSurface(isTop = true, isBottom = true, id = "bubble_preview", highlightId = highlightId) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MessageBubble(
                        message = previewMessages[0],
                        isLastFromSender = true,
                        isFirstFromSender = false,
                        drawAvatar = false,
                        showTags = showMsgTags,
                        showMyBubbleAvatarSetting = showMyBubbleAvatar,
                        bubbleOpacity = bubbleOpacity,
                        bubbleCornerRadius = bubbleCornerRadius,
                        roleLabel = "群主"
                    )
                    MessageBubble(
                        message = previewMessages[1],
                        isLastFromSender = false,
                        isFirstFromSender = true,
                        drawAvatar = true,
                        showMyBubbleAvatarSetting = showMyBubbleAvatar,
                        bubbleOpacity = bubbleOpacity,
                        bubbleCornerRadius = bubbleCornerRadius
                    )
                    MessageBubble(
                        message = previewMessages[2],
                        isLastFromSender = true,
                        isFirstFromSender = true,
                        drawAvatar = true,
                        showMyBubbleAvatarSetting = showMyBubbleAvatar,
                        bubbleOpacity = bubbleOpacity,
                        bubbleCornerRadius = bubbleCornerRadius
                    )
                }
            }
        }

        item { SettingsGroupHeader("气泡样式") }
        item {
            SettingsGroupSurface(isTop = true, isBottom = false, id = "bubble_corner", highlightId = highlightId) {
                SettingsSliderItem(
                    title = "气泡圆角",
                    valueText = "${bubbleCornerRadius.toInt()}dp",
                    value = bubbleCornerRadius,
                    onValueChange = { bubbleCornerRadius = it },
                    onValueChangeFinished = {
                        scope.launch { settingsStorage.setBubbleCornerRadius(bubbleCornerRadius) }
                    },
                    icon = AppIcons.RoundedCorner,
                    valueRange = 0f..24f,
                    steps = 23,
                    startLabel = "0dp",
                    endLabel = "24dp"
                )
            }
        }
        item {
            SettingsGroupSurface(isTop = false, isBottom = false, id = "bubble_opacity", highlightId = highlightId) {
                SettingsSliderItem(
                    title = "气泡不透明度",
                    valueText = "${(bubbleOpacity * 100).toInt()}%",
                    value = bubbleOpacity,
                    onValueChange = { bubbleOpacity = it },
                    onValueChangeFinished = {
                        scope.launch { settingsStorage.setBubbleOpacity(bubbleOpacity) }
                    },
                    icon = AppIcons.Opacity,
                    valueRange = 0.4f..1f,
                    startLabel = "40%",
                    endLabel = "100%"
                )
            }
        }
        item {
            SettingsGroupSurface(isTop = false, isBottom = false, id = "bubble_my_avatar", highlightId = highlightId) {
                SettingsSwitchItem(
                    icon = AppIcons.Face,
                    title = "显示我的头像",
                    subtitle = "在我发送的消息气泡旁显示我的头像",
                    checked = showMyBubbleAvatar,
                    onCheckedChange = { checked ->
                        scope.launch { settingsStorage.setShowMyBubbleAvatar(checked) }
                    }
                )
            }
        }
        item {
            SettingsGroupSurface(isTop = false, isBottom = true, id = "bubble_tags", highlightId = highlightId) {
                SettingsSwitchItem(
                    icon = AppIcons.Tag,
                    title = "显示用户标签",
                    subtitle = "在发送者名称旁显示Ta的标签",
                    checked = showMsgTags,
                    onCheckedChange = { checked ->
                        scope.launch { settingsStorage.setShowMsgTags(checked) }
                    }
                )
            }
        }
    }
}