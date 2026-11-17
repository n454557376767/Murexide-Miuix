package com.juhao.murexide.ui.settings.userbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.juhao.murexide.datastore.SettingsStorage
import com.juhao.murexide.ui.theme.MurexideTheme
import kotlinx.coroutines.launch
import com.juhao.murexide.ui.components.MiuixPageScaffold
import top.yukonga.miuix.kmp.basic.Switch

class UserBotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MurexideTheme {
                UserBotScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, UserBotActivity::class.java).apply {
                if (context !is ComponentActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }
}

@Composable
private fun UserBotScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsStorage = remember { SettingsStorage(context) }
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enabled = settingsStorage.getUserBotEnabled()
    }

    MiuixPageScaffold(
        title = "用户 Bot",
        onBack = onBack,
        actions = {
            Switch(
                checked = enabled,
                onCheckedChange = { value ->
                    enabled = value
                    scope.launch { settingsStorage.setUserBotEnabled(value) }
                }
            )
        }
    ) { padding ->
        UserBotPage(
            modifier = Modifier.padding(padding),
            enabled = enabled,
            onEnabledChange = { value ->
                enabled = value
                scope.launch { settingsStorage.setUserBotEnabled(value) }
            }
        )
    }
}