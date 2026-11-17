package com.juhao.murexide.ui.conversationdetail

import com.juhao.murexide.ui.icons.AppIcons

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
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
import com.juhao.murexide.ui.components.SettingsItemCell
import com.juhao.murexide.ui.components.SettingsSwitchItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BotSettingsScreen(
    viewModel: BotSettingsViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit = onBack
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.uploadAvatar(context, uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BotSettingsViewModel.BotSettingsEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is BotSettingsViewModel.BotSettingsEvent.Saved -> onSaved()
            }
        }
    }

    MiuixPageScaffold(
        title = "机器人设置",
        onBack = onBack,
        actions = {
            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MiuixLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
                }
            } else if (uiState.original != null) {
                IconButton(
                    onClick = { viewModel.save() },
                    enabled = !uiState.isUploadingAvatar
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = "保存",
                        tint = MiuixTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        val loadError = uiState.loadError
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { MiuixLoadingIndicator() }

            loadError != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = loadError,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = viewModel::load,
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) { Text("重试") }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
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
                        text = if (uiState.isUploadingAvatar) "上传中…" else "点击更换机器人头像",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center
                    )
                    if (uiState.isUploadingAvatar) {
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
                            placeholder = "机器人名称",
                            singleLine = true
                        )
                    }
                }
                SettingsGroupSurface(isTop = false, isBottom = false) {
                    CustomItemCell {
                        MiuixTextField(
                            value = uiState.introduction,
                            onValueChange = viewModel::updateIntroduction,
                            placeholder = "机器人简介",
                            singleLine = false,
                            minHeight = 72
                        )
                    }
                }
                SettingsGroupSurface(isTop = false, isBottom = true) {
                    SettingsSwitchItem(
                        icon = AppIcons.Lock,
                        title = "私有机器人",
                        subtitle = "开启后不在机器人商店展示",
                        checked = uiState.isPrivate,
                        onCheckedChange = viewModel::togglePrivate
                    )
                }

                SettingsGroupHeader("消息订阅")
                SettingsGroupSurface(isTop = true, isBottom = false) {
                    CustomItemCell {
                        // 原来的 FilterChip 换成 Miuix 按钮：选中态主色、未选中次级色
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.updateSubscribeType(0) },
                                colors = if (uiState.subscribeType == 0) {
                                    ButtonDefaults.buttonColorsPrimary()
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) { Text("URL 订阅") }
                            Button(
                                onClick = { viewModel.updateSubscribeType(1) },
                                colors = if (uiState.subscribeType == 1) {
                                    ButtonDefaults.buttonColorsPrimary()
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) { Text("WSS 订阅") }
                        }
                    }
                }
                SettingsGroupSurface(isTop = false, isBottom = true) {
                    CustomItemCell {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiuixTextField(
                                value = uiState.link,
                                onValueChange = viewModel::updateLink,
                                placeholder = if (uiState.subscribeType == 1) {
                                    "订阅地址：wss://..."
                                } else {
                                    "订阅地址：https://..."
                                },
                                singleLine = true
                            )
                            if (uiState.original?.linkStop == true) {
                                Text(
                                    text = "当前订阅地址已被停用",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                SettingsGroupHeader("机器人设置 JSON")
                SettingsGroupSurface(isTop = true, isBottom = true) {
                    CustomItemCell {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiuixTextField(
                                value = uiState.settingJson,
                                onValueChange = viewModel::updateSettingJson,
                                placeholder = "settingJson（[]）",
                                singleLine = false,
                                minHeight = 96
                            )
                            Text(
                                text = "群内机器人设置表单，需为 JSON 数组",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                SettingsGroupHeader("Token")
                SettingsGroupSurface(isTop = true, isBottom = false) {
                    SettingsItemCell(
                        icon = AppIcons.Key,
                        endIcon = if (uiState.tokenVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                        title = if (uiState.tokenVisible) uiState.botToken else maskToken(uiState.botToken),
                        subtitle = "点击显示 / 隐藏",
                        onClick = viewModel::toggleTokenVisible
                    )
                }
                SettingsGroupSurface(isTop = false, isBottom = false) {
                    SettingsItemCell(
                        icon = AppIcons.ContentCopy,
                        title = "复制 Token",
                        isEnabled = uiState.botToken.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("bot token", uiState.botToken))
                                )
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                SettingsGroupSurface(isTop = false, isBottom = true) {
                    SettingsItemCell(
                        icon = AppIcons.Refresh,
                        title = if (uiState.isResettingToken) "重置中…" else "重置 Token",
                        subtitle = "旧 Token 将立即失效",
                        isDestructive = true,
                        isEnabled = !uiState.isResettingToken,
                        onClick = { viewModel.showResetConfirm(true) }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (uiState.showResetConfirm) {
        MiuixAlertDialog(
            show = true,
            title = "重置 Token？",
            summary = "重置后旧 Token 会立即失效，使用旧 Token 的服务需要同步更新。",
            confirmText = "重置",
            onConfirm = viewModel::resetToken,
            onDismissRequest = { viewModel.showResetConfirm(false) }
        )
    }
}

private fun maskToken(token: String): String = when {
    token.isEmpty() -> "（无）"
    token.length <= 8 -> "••••••••"
    else -> token.take(4) + "••••••••" + token.takeLast(4)
}
