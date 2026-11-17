package com.juhao.murexide.ui.mine

import com.juhao.murexide.ui.icons.AppIcons

import android.app.Activity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.clickable
import com.juhao.murexide.repository.UserInfo
import com.juhao.murexide.ui.components.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MineScreen(
    token: String,
    innerPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    viewModel: MineViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            val application = LocalContext.current.applicationContext
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MineViewModel(application as android.app.Application, token) as T
            }
        }
    )
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadAndChangeAvatar(context, it)
        }
    }

    val editProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadUserInfo()
        }
    }

    MiuixPageScaffold(
        title = "我的",
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = AppIcons.Settings,
                    contentDescription = "设置",
                    tint = MiuixTheme.colorScheme.onBackground
                )
            }
        }
    ) {
        when (val state = uiState) {
            is MineUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiuixTheme.colorScheme.primary)
                }
            }

            is MineUiState.Error -> {
                EmptyPlaceholder(
                    icon = AppIcons.Warning,
                    title = "加载失败",
                    subtitle = state.message,
                    primaryButton = {
                        Button(
                            onClick = { viewModel.loadUserInfo() },
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("重试")
                        }
                    }
                )
            }

            is MineUiState.Success -> {
                MineContent(
                    innerPadding = innerPadding,
                    userInfo = state.userInfo,
                    onlineDay = state.onlineDay,
                    continuousOnlineDay = state.continuousOnlineDay,
                    scrollState = scrollState,
                    paddingValues = it,
                    introduction = state.introduction,
                    onEditProfileClick = {
                        editProfileLauncher.launch(
                            EditProfileActivity.createIntent(context, token)
                        )
                    },
                    onAvatarEditClick = {
                        avatarLauncher.launch("image/*")
                    }
                )

                if (state.isUploadingAvatar) {
                    UploadProgressDialog(progress = state.uploadProgress)
                }
            }
        }
    }
}

@Composable
fun UploadProgressDialog(progress: Float) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(modifier = Modifier.width(280.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "正在上传头像",
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurfaceContainer
                )
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round,
                    color = MiuixTheme.colorScheme.primary,
                    trackColor = MiuixTheme.colorScheme.secondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }
        }
    }
}

@Composable
private fun MineContent(
    innerPadding: PaddingValues,
    userInfo: UserInfo,
    onlineDay: Int?,
    continuousOnlineDay: Int?,
    introduction: String,
    scrollState: ScrollState,
    paddingValues: PaddingValues,
    onEditProfileClick: () -> Unit,
    onAvatarEditClick: () -> Unit
) {
    var isPhoneVisible by remember { mutableStateOf(false) }
    var isEmailVisible by remember { mutableStateOf(false) }

    val displayPhone = if (isPhoneVisible) {
        userInfo.phone.ifEmpty { "未绑定" }
    } else {
        maskPhoneNumber(userInfo.phone)
    }

    val displayEmail = if (isEmailVisible) {
        userInfo.email.ifEmpty { "未设置" }
    } else {
        maskEmail(userInfo.email)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(paddingValues)
    ) {
        ProfileCard(
            userInfo = userInfo,
            introduction = introduction,
            onEditProfileClick = onEditProfileClick,
            onAvatarEditClick = onAvatarEditClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActivityOverviewCard(
            onlineDay = onlineDay,
            continuousOnlineDay = continuousOnlineDay,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        SettingsGroupHeader("账号信息")
        SettingsGroupSurface(isTop = true, isBottom = false) {
            InfoItem(
                icon = AppIcons.Phone,
                title = "手机号",
                value = displayPhone,
                endIcon = if (isPhoneVisible) AppIcons.Visibility else AppIcons.VisibilityOff,
                onClick = { isPhoneVisible = !isPhoneVisible }
            )
        }
        SettingsGroupSurface(isTop = false, isBottom = false) {
            InfoItem(
                icon = AppIcons.Email,
                title = "邮箱",
                value = displayEmail,
                endIcon = if (isEmailVisible) AppIcons.Visibility else AppIcons.VisibilityOff,
                onClick = { isEmailVisible = !isEmailVisible }
            )
        }
        SettingsGroupSurface(isTop = false, isBottom = true) {
            InfoItem(
                icon = AppIcons.CardGiftcard,
                title = "邀请码",
                value = userInfo.invitationCode.ifEmpty { "未设置" }
            )
        }

        Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
    }
}

@Composable
private fun ActivityOverviewCard(
    onlineDay: Int?,
    continuousOnlineDay: Int?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MiuixTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActivityMetric(
                    label = "在线天数",
                    value = onlineDay,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .width(1.dp)
                        .background(MiuixTheme.colorScheme.dividerLine)
                )

                ActivityMetric(
                    label = "连续在线",
                    value = continuousOnlineDay,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value?.let { "$it 天" } ?: "—",
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ProfileCard(
    userInfo: UserInfo,
    introduction: String,
    onEditProfileClick: () -> Unit,
    onAvatarEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedCoin = remember(userInfo.coin) {
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(userInfo.coin)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onEditProfileClick
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Avatar(
                        url = userInfo.avatarUrl,
                        size = 72.dp,
                        modifier = Modifier.clickable {
                            onAvatarEditClick()
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .offset(x = 3.dp, y = 3.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary)
                            .clickable { onAvatarEditClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "修改头像",
                            modifier = Modifier.size(15.dp),
                            tint = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userInfo.name.ifEmpty { "未知" },
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "ID: ${userInfo.id}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                    )

                    if (introduction.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = introduction,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = "编辑资料",
                    modifier = Modifier.size(22.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStat(
                    icon = AppIcons.Verified,
                    label = "用户等级",
                    value = if (userInfo.isVip) "会员" else "普通用户",
                    modifier = Modifier.weight(1f)
                )

                ProfileStat(
                    icon = AppIcons.MonetizationOn,
                    label = "金币",
                    value = formattedCoin,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MiuixTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun maskPhoneNumber(phone: String): String {
    return if (phone.isEmpty()) {
        "未绑定"
    } else if (phone.length >= 2) {
        phone.first() + "*".repeat(phone.length - 2) + phone.last()
    } else {
        phone
    }
}

fun maskEmail(email: String): String {
    return if (email.isEmpty()) {
        "未设置"
    } else {
        val atIndex = email.indexOf('@')
        if (atIndex > 0) {
            "*".repeat(atIndex) + email.substring(atIndex)
        } else {
            "*".repeat(email.length)
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    endIcon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    SettingsItemCell(
        icon = icon,
        endIcon = endIcon,
        title = title,
        subtitle = value,
        onClick = onClick
    )
}
