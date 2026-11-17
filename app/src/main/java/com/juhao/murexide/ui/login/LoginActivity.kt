package com.juhao.murexide.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.juhao.murexide.MainActivity
import com.juhao.murexide.datastore.AccountStorage
import com.juhao.murexide.datastore.UserAccount
import com.juhao.murexide.repository.AuthRepository
import com.juhao.murexide.ui.theme.MurexideTheme
import com.juhao.murexide.utils.DeviceIdProvider
import kotlinx.coroutines.launch
import com.juhao.murexide.ui.components.MiuixAlertDialog
import com.juhao.murexide.ui.components.MiuixTextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

class LoginActivity : ComponentActivity() {
    private val accountStorage by lazy { AccountStorage.getInstance(this) }
    private val authRepository by lazy { AuthRepository() }
    private val deviceId by lazy { DeviceIdProvider.get(this) }
    private lateinit var loginViewModel: LoginViewModel
    private var isCompletingLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isAddMode = intent.getBooleanExtra("addMode", false)
        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(deviceId)
        )[LoginViewModel::class.java]

        setContent {
            var showTokenDialog by remember { mutableStateOf(false) }

            MurexideTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.background)
                ) {
                    LoginScreen(
                        onLoginSuccess = { token ->
                            completeLogin(token, isAddMode, "登录成功")
                        },
                        onTokenLogin = {
                            showTokenDialog = true
                        },
                        viewModel = loginViewModel
                    )

                    if (showTokenDialog) {
                        TokenInputDialog(
                            onDismiss = { showTokenDialog = false },
                            onConfirm = { token ->
                                showTokenDialog = false
                                completeLogin(token.trim(), isAddMode, "Token登录成功")
                            }
                        )
                    }
                }
            }
        }
    }

    private fun completeLogin(token: String, isAddMode: Boolean, successMessage: String) {
        if (isCompletingLogin) return
        isCompletingLogin = true

        lifecycleScope.launch {
            val userInfoResult = authRepository.getUserInfo(token)
            if (userInfoResult.isFailure) {
                isCompletingLogin = false
                loginViewModel.resetLoginResult()
                Toast.makeText(
                    this@LoginActivity,
                    userInfoResult.exceptionOrNull()?.message ?: "登录凭据无效",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val userInfo = userInfoResult.getOrThrow()
            try {
                accountStorage.upsertAccount(
                    account = UserAccount(
                        username = userInfo.name,
                        avatar = userInfo.avatarUrl,
                        id = userInfo.id,
                        token = token,
                        isValidated = true
                    ),
                    makeCurrent = !isAddMode
                )
            } catch (error: Exception) {
                isCompletingLogin = false
                loginViewModel.resetLoginResult()
                Toast.makeText(
                    this@LoginActivity,
                    error.message ?: "保存账号失败",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            Toast.makeText(
                this@LoginActivity,
                if (isAddMode) "账号已添加" else successMessage,
                Toast.LENGTH_SHORT
            ).show()
            if (!isAddMode) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            }
            finish()
        }
    }

    companion object {
        fun start(context: Context, isAddMode: Boolean = false) {
            val intent = Intent(context, LoginActivity::class.java).apply {
                putExtra("addMode", isAddMode)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun TokenInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var token by remember { mutableStateOf("") }

    MiuixAlertDialog(
        show = true,
        title = "Token 登录",
        confirmText = "登录",
        confirmEnabled = token.isNotBlank(),
        onConfirm = { onConfirm(token) },
        onDismissRequest = onDismiss
    ) {
        MiuixTextField(
            value = token,
            onValueChange = { token = it },
            placeholder = "请输入 Token"
        )
    }
}
