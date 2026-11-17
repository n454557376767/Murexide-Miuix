package com.juhao.murexide.ui.settings.switchAccount

import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.icons.AutoMirroredIcon

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.juhao.murexide.MainActivity
import com.juhao.murexide.datastore.AccountStorage
import com.juhao.murexide.datastore.UserAccount
import com.juhao.murexide.data.local.LocalCache
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.ui.login.LoginActivity
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun Greeting(
    onBack: () -> Unit,
    isChooseMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val accountStorage = remember(context.applicationContext) {
        AccountStorage.getInstance(context.applicationContext)
    }

    val accounts = accountStorage.userAccountsFlow.collectAsState(initial = emptyList()).value
    val currentAccount = accountStorage.currentTokenFlow.collectAsState(initial = "").value

    MiuixPageScaffold(
        title = if (isChooseMode) "现有账号" else "切换账号",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            if (!isChooseMode) {
                item {
                    BasicComponent(
                        title = "添加账号",
                        startAction = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MiuixTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Add,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        },
                        onClick = { LoginActivity.start(context, true) }
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无账号",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            items(accounts) { account ->
                AccountRow(
                    account = account,
                    isCurrentAccount = account.token == currentAccount,
                    isChooseMode = isChooseMode,
                    onSwitchAccount = {
                        scope.launch {
                            accountStorage.switchAccount(it.id)
                            Toast.makeText(context, "切换成功", Toast.LENGTH_LONG).show()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                    onRemoveAccount = {
                        scope.launch {
                            LocalCache.purgeAccount(it.id)
                            accountStorage.removeAccount(it.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AccountRow(
    account: UserAccount,
    isCurrentAccount: Boolean,
    isChooseMode: Boolean,
    onSwitchAccount: (UserAccount) -> Unit,
    onRemoveAccount: (UserAccount) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val summaryText = buildString {
        append("ID: ${account.id}")
        if (isCurrentAccount && !isChooseMode) append(" · 当前账号")
        if (!account.isValidated) append(" · 账号未验证，切换至此账号验证")
    }

    BasicComponent(
        title = account.username,
        summary = summaryText,
        startAction = {
            if (account.avatar.isNotBlank()) {
                Avatar(url = account.avatar, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                )
            }
        },
        endActions = if (!isCurrentAccount && !isChooseMode) {
            {
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
        } else {
            null
        },
        onClick = {
            if (!isChooseMode) {
                showMenu = true
            } else {
                onSwitchAccount(account)
            }
        }
    )

    MiuixAlertDialog(
        show = showMenu && !isCurrentAccount,
        title = account.username,
        summary = summaryText,
        confirmText = null,
        dismissText = null,
        onDismissRequest = { showMenu = false }
    ) {
        Button(
            onClick = {
                onSwitchAccount(account)
                showMenu = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary()
        ) {
            Text("切换至此账号")
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                showDeleteDialog = true
                showMenu = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("移除账号")
        }
    }

    MiuixAlertDialog(
        show = showDeleteDialog,
        title = "删除账号",
        summary = "确定要删除此账号吗？",
        confirmText = "删除",
        onConfirm = {
            showDeleteDialog = false
            onRemoveAccount(account)
        },
        onDismissRequest = { showDeleteDialog = false }
    )
}
