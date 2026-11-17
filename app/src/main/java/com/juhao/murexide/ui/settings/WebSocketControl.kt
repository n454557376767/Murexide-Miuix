package com.juhao.murexide.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.juhao.murexide.datastore.SettingsStorage
import com.juhao.murexide.network.WebSocketManager
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.ui.icons.AppIcons
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WebSocketControl(
    highlightId: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val settingsStorage = remember { SettingsStorage(context) }
    val scope = rememberCoroutineScope()
    val wsManager = remember { WebSocketManager.getInstance() }

    val connected by wsManager.connectionState.collectAsState()
    var wsEnabled by remember { mutableStateOf(true) }
    var batteryOptimizationIgnored by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    LaunchedEffect(Unit) { wsEnabled = settingsStorage.getWsEnabled() }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allIds = listOf("header_ws", "ws_status", "header_ws_settings", "ws_enable", "ws_battery", "ws_reconnect")
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
        item { SettingsGroupHeader("状态") }
        item {
            SettingsGroupSurface(isTop = true, isBottom = true, id = "ws_status", highlightId = highlightId) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (connected) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.error
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (connected) AppIcons.Check else AppIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (connected) {
                                MiuixTheme.colorScheme.onPrimary
                            } else {
                                MiuixTheme.colorScheme.onError
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (connected) "连接正常" else "连接已断开",
                        style = MiuixTheme.textStyles.body1,
                        color = if (connected) {
                            MiuixTheme.colorScheme.onSurfaceContainer
                        } else {
                            MiuixTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (connected) "WebSocket 已建立，可接收实时消息" else "正在等待重新连接",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                    )
                }
            }
        }

        item { SettingsGroupHeader("WebSocket") }
        item {
            SettingsGroupSurface(isTop = true, isBottom = false, id = "ws_enable", highlightId = highlightId) {
                SettingsSwitchItem(
                    icon = AppIcons.Link,
                    title = "启用 WebSocket",
                    subtitle = "关闭后将不再连接，重启应用生效",
                    checked = wsEnabled,
                    onCheckedChange = { enabled ->
                        wsEnabled = enabled
                        scope.launch { settingsStorage.setWsEnabled(enabled) }
                    }
                )
            }
        }
        item {
            SettingsGroupSurface(isTop = false, isBottom = false, id = "ws_battery", highlightId = highlightId) {
                SettingsSwitchItem(
                    icon = AppIcons.BatteryPlus,
                    title = "忽略电池优化",
                    subtitle = "忽略电池优化以减少后台被杀概率",
                    checked = batteryOptimizationIgnored,
                    isEnabled = !batteryOptimizationIgnored,
                    onCheckedChange = { enabled ->
                        requestIgnoreBatteryOptimizations(context, enabled)
                    }
                )
            }
        }
        item {
            SettingsGroupSurface(isTop = false, isBottom = true, id = "ws_reconnect", highlightId = highlightId) {
                SettingsItem(
                    icon = AppIcons.Refresh,
                    title = "重启连接",
                    isEnabled = wsEnabled,
                    subtitle = "立即断开并重新建立 WebSocket 连接",
                    onClick = { wsManager.manualReconnect() }
                )
            }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context, enable: Boolean) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (enable) {
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            runCatching {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }.onFailure {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    } else {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}