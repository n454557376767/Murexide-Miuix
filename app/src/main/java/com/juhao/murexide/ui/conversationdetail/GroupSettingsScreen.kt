package com.juhao.murexide.ui.conversationdetail

import com.juhao.murexide.ui.icons.AppIcons

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juhao.murexide.ui.components.Avatar
import com.juhao.murexide.ui.components.CustomItemCell
import com.juhao.murexide.ui.components.MiuixAlertDialog
import com.juhao.murexide.ui.components.MiuixLinearProgress
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import com.juhao.murexide.ui.components.MiuixPageScaffold
import com.juhao.murexide.ui.components.MiuixTextField
import com.juhao.murexide.ui.components.SettingsGroupHeader
import com.juhao.murexide.ui.components.SettingsGroupSurface
import com.juhao.murexide.ui.components.SettingsItem
import com.juhao.murexide.ui.components.SettingsSwitchItem
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val MsgTypeOptions = listOf(
    1 to "文本消息",
    2 to "图片消息",
    3 to "Markdown消息",
    4 to "文件消息",
    5 to "引用消息",
    6 to "帖子消息",
    7 to "表情消息",
    8 to "HTML消息",
    9 to "Bot消息",
    10 to "视频消息",
    11 to "语音消息",
    13 to "语音通话",
    14 to "转账",
    16 to "链接",
    17 to "卡片消息",
    19 to "文件(新)",
    20 to "指令消息",
    21 to "回复消息"
)

@Composable
fun GroupSettingsScreen(
    viewModel: GroupSettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showMsgTypeDialog by remember { mutableStateOf(false) }
    var showGroupCodeDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.uploadAvatar(context, uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GroupSettingsViewModel.GroupSettingsEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is GroupSettingsViewModel.GroupSettingsEvent.Saved -> onBack()
            }
        }
    }

    MiuixPageScaffold(
        title = "群设置",
        onBack = onBack,
        actions = {
            // 保存中走 Miuix 加载指示器，避免重复提交
            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MiuixLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = { viewModel.save() }) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = "保存",
                        tint = MiuixTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    MiuixLoadingIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(
                        url = uiState.avatarUrl,
                        size = 88.dp,
                        modifier = Modifier.clickable(enabled = !uiState.isUploadingAvatar) {
                            imagePicker.launch("image/*")
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isUploadingAvatar) "上传中…" else "点击更换群头像",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center
                    )
                    if (uiState.isUploadingAvatar) {
                        // 上传进度改为 Miuix 线性进度条，跟随主题语义色
                        MiuixLinearProgress(
                            progress = uiState.uploadProgress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .width(120.dp)
                                .padding(top = 8.dp)
                        )
                    }
                }

                SettingsGroupHeader("基本信息")
                SettingsGroupSurface(isTop = true, isBottom = false) {
                    CustomItemCell {
                        MiuixTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            placeholder = "群名称",
                            singleLine = true
                        )
                    }
                }
                SettingsGroupSurface(isTop = false, isBottom = true) {
                    CustomItemCell {
                        MiuixTextField(
                            value = uiState.introduction,
                            onValueChange = viewModel::updateIntroduction,
                            placeholder = "群简介",
                            singleLine = false,
                            minHeight = 72
                        )
                    }
                }

                SettingsGroupHeader("群聊设置")
                SettingsGroupSurface(isTop = true, isBottom = false) {
                    SettingsSwitchItem(
                        icon = AppIcons.HowToReg,
                        title = "进群免审核",
                        subtitle = "开启后新成员无需验证直接进群",
                        checked = uiState.directJoin,
                        onCheckedChange = viewModel::toggleDirectJoin
                    )
                }
                SettingsGroupSurface(isTop = false, isBottom = false) {
                    SettingsSwitchItem(
                        icon = AppIcons.History,
                        title = "新成员可见历史消息",
                        checked = uiState.historyMsg,
                        onCheckedChange = viewModel::toggleHistoryMsg
                    )
                }
                SettingsGroupSurface(isTop = false, isBottom = false) {
                    SettingsSwitchItem(
                        icon = AppIcons.Lock,
                        title = "群聊私有",
                        subtitle = "开启后需要验证才能加入",
                        checked = uiState.isPrivate,
                        onCheckedChange = viewModel::togglePrivate
                    )
                }
                SettingsGroupSurface(isTop = false, isBottom = true) {
                    SettingsSwitchItem(
                        icon = AppIcons.VisibilityOff,
                        title = "隐藏群成员",
                        checked = uiState.hideGroupMembers,
                        onCheckedChange = viewModel::toggleHideMembers
                    )
                }

                if (uiState.isOwner) {
                    SettingsGroupHeader("群云盘")
                    SettingsGroupSurface(isTop = true, isBottom = true) {
                        SettingsSwitchItem(
                            icon = AppIcons.Folder,
                            title = "禁止成员上传到群云盘",
                            subtitle = "开启后仅群主可上传文件",
                            checked = uiState.denyMembersUploadToGroupDisk,
                            isEnabled = !uiState.isSavingDenyUpload,
                            onCheckedChange = viewModel::toggleDenyMembersUploadToGroupDisk
                        )
                    }
                }

                if (uiState.canManage) {
                    SettingsGroupHeader("高级设置")
                    SettingsGroupSurface(isTop = true, isBottom = !uiState.isOwner) {
                        SettingsItem(
                            icon = AppIcons.TextFields,
                            title = "消息类型限制",
                            subtitle = if (uiState.limitedMsgType.isEmpty()) {
                                "未限制消息类型"
                            } else {
                                "已限制 ${uiState.limitedMsgType.size} 种消息类型"
                            },
                            onClick = { showMsgTypeDialog = true }
                        )
                    }
                    if (uiState.isOwner) {
                        SettingsGroupSurface(isTop = false, isBottom = true) {
                            SettingsItem(
                                icon = AppIcons.Key,
                                title = "群口令",
                                subtitle = uiState.groupCode.ifBlank { "设置后可用口令搜索加入群聊" },
                                onClick = { showGroupCodeDialog = true }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showMsgTypeDialog) {
        var selected by remember(uiState.limitedMsgType) {
            mutableStateOf(uiState.limitedMsgType)
        }
        MiuixAlertDialog(
            show = true,
            title = "限制群内消息类型",
            summary = "勾选后，群内成员将无法发送对应类型的消息",
            confirmText = if (uiState.isSavingMsgTypeLimit) "保存中…" else "保存",
            confirmEnabled = !uiState.isSavingMsgTypeLimit,
            onConfirm = {
                showMsgTypeDialog = false
                viewModel.saveMsgTypeLimit(selected)
            },
            onDismissRequest = {
                if (!uiState.isSavingMsgTypeLimit) showMsgTypeDialog = false
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                MsgTypeOptions.forEach { (type, label) ->
                    val checked = type in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isSavingMsgTypeLimit) {
                                selected = if (checked) selected - type else selected + type
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 勾选态用 Miuix 主色圆点 + 勾，未勾选用容器色，与列表选择态一致
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (checked) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.surfaceContainerHigh
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Icon(
                                    imageVector = AppIcons.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MiuixTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }

    if (showGroupCodeDialog) {
        var code by remember(uiState.groupCode) {
            mutableStateOf(uiState.groupCode)
        }
        MiuixAlertDialog(
            show = true,
            title = "群口令",
            summary = "留空表示删除现有口令",
            confirmText = if (uiState.isSavingGroupCode) "保存中…" else "保存",
            confirmEnabled = !uiState.isSavingGroupCode,
            onConfirm = {
                showGroupCodeDialog = false
                viewModel.saveGroupCode(code)
            },
            onDismissRequest = {
                if (!uiState.isSavingGroupCode) showGroupCodeDialog = false
            }
        ) {
            MiuixTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = "群口令（为空删除）",
                enabled = !uiState.isSavingGroupCode,
                singleLine = true
            )
        }
    }
}
