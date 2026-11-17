package com.juhao.murexide.ui.login

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.juhao.murexide.R
import com.juhao.murexide.ui.components.CapsuleTabBar
import com.juhao.murexide.ui.components.MiuixAlertDialog
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import com.juhao.murexide.ui.components.MiuixTextField
import com.juhao.murexide.ui.settings.switchAccount.SwitchAccountActivity
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class LoginMode { PHONE, EMAIL }

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onTokenLogin: () -> Unit,
    viewModel: LoginViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val phoneState by viewModel.phoneState.collectAsState()

    var loginMode by remember { mutableStateOf(LoginMode.PHONE) }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess((uiState as LoginUiState.Success).token)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "欢迎登录",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        CapsuleTabBar(
            tabs = listOf("手机号登录", "邮箱登录"),
            selectedTabIndex = loginMode.ordinal,
            onTabSelected = { index ->
                loginMode = if (index == 0) LoginMode.PHONE else LoginMode.EMAIL
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        when (loginMode) {
            LoginMode.PHONE -> PhoneLoginContent(
                phone = phone,
                smsCode = smsCode,
                phoneState = phoneState,
                onPhoneChange = { phone = it; viewModel.clearPhoneError() },
                onSmsCodeChange = { smsCode = it; viewModel.clearPhoneError() },
                onSendCode = { viewModel.requestCaptcha() },
                onLogin = { viewModel.phoneLogin(phone.trim(), smsCode.trim()) }
            )
            LoginMode.EMAIL -> EmailLoginContent(
                uiState = uiState,
                onLogin = { email, password -> viewModel.login(email, password) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, SwitchAccountActivity::class.java).apply {
                        putExtra("chooseMode", true)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("现有账号")
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onTokenLogin,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Token登录")
            }
        }
    }

    phoneState.captchaImage?.let { image ->
        CaptchaDialog(
            imageBase64 = image,
            isSending = phoneState.isSendingSms,
            onRefresh = { viewModel.requestCaptcha() },
            onDismiss = { viewModel.dismissCaptcha() },
            onConfirm = { captchaCode ->
                viewModel.submitCaptchaAndSendSms(phone.trim(), captchaCode)
            }
        )
    }
}

@Composable
private fun PhoneLoginContent(
    phone: String,
    smsCode: String,
    phoneState: PhoneLoginState,
    onPhoneChange: (String) -> Unit,
    onSmsCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onLogin: () -> Unit
) {
    val phoneValid = phone.trim().length == 11 && phone.trim().all { it.isDigit() }

    MiuixTextField(
        value = phone,
        onValueChange = { input -> if (input.length <= 11) onPhoneChange(input.filter { it.isDigit() }) },
        placeholder = "手机号",
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixTextField(
            value = smsCode,
            onValueChange = { input -> if (input.length <= 6) onSmsCodeChange(input.filter { it.isDigit() }) },
            placeholder = "短信验证码",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        val canSend = phoneValid && phoneState.countdown == 0 &&
                !phoneState.isCaptchaLoading && !phoneState.isSendingSms
        Button(
            onClick = onSendCode,
            enabled = canSend,
            colors = ButtonDefaults.buttonColors()
        ) {
            if (phoneState.isCaptchaLoading || phoneState.isSendingSms) {
                MiuixLoadingIndicator(
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    color = MiuixTheme.colorScheme.onSecondaryVariant
                )
            } else {
                Text(
                    text = if (phoneState.countdown > 0) "${phoneState.countdown}s" else "获取验证码"
                )
            }
        }
    }

    phoneState.error?.let {
        Text(
            text = it,
            color = MiuixTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            style = MiuixTheme.textStyles.footnote1
        )
    }

    Button(
        onClick = onLogin,
        enabled = !phoneState.isLoggingIn && phoneValid && smsCode.isNotBlank(),
        colors = ButtonDefaults.buttonColorsPrimary(),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (phoneState.isLoggingIn) {
            MiuixLoadingIndicator(
                size = 22.dp,
                strokeWidth = 2.dp,
                color = MiuixTheme.colorScheme.onPrimary
            )
        } else {
            Text(text = "登录", style = MiuixTheme.textStyles.button)
        }
    }
}

@Composable
private fun EmailLoginContent(
    uiState: LoginUiState,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    MiuixTextField(
        value = email,
        onValueChange = { email = it; emailError = null },
        placeholder = "邮箱地址",
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
    emailError?.let { message ->
        Text(
            text = message,
            color = MiuixTheme.colorScheme.error,
            style = MiuixTheme.textStyles.footnote1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }

    MiuixTextField(
        value = password,
        onValueChange = { password = it; passwordError = null },
        placeholder = "密码",
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
    passwordError?.let { message ->
        Text(
            text = message,
            color = MiuixTheme.colorScheme.error,
            style = MiuixTheme.textStyles.footnote1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }

    if (uiState is LoginUiState.Error) {
        Text(
            text = uiState.message,
            color = MiuixTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            style = MiuixTheme.textStyles.footnote1
        )
    }

    Button(
        onClick = {
            var hasError = false
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "邮箱格式不正确"; hasError = true
            }
            if (password.length < 6) {
                passwordError = "密码长度不能少于6位"; hasError = true
            }
            if (!hasError) onLogin(email, password)
        },
        enabled = uiState !is LoginUiState.Loading && email.isNotBlank() && password.isNotBlank(),
        colors = ButtonDefaults.buttonColorsPrimary(),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (uiState is LoginUiState.Loading) {
            MiuixLoadingIndicator(
                size = 22.dp,
                strokeWidth = 2.dp,
                color = MiuixTheme.colorScheme.onPrimary
            )
        } else {
            Text(text = "登录", style = MiuixTheme.textStyles.button)
        }
    }
}

@Composable
private fun CaptchaDialog(
    imageBase64: String,
    isSending: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var captchaCode by remember { mutableStateOf("") }

    val bitmap = remember(imageBase64) {
        runCatching {
            val pure = if (imageBase64.contains(",")) imageBase64.substringAfterLast(",") else imageBase64
            val bytes = Base64.decode(pure, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    MiuixAlertDialog(
        show = true,
        title = "人机验证",
        confirmText = "确定",
        confirmEnabled = captchaCode.isNotBlank() && !isSending,
        onConfirm = { onConfirm(captchaCode.trim()) },
        onDismissRequest = onDismiss
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "验证码图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = "验证码图片加载失败",
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("看不清，换一张")
        }
        Spacer(modifier = Modifier.height(10.dp))
        MiuixTextField(
            value = captchaCode,
            onValueChange = { captchaCode = it },
            placeholder = "请输入图片中的字符"
        )
        if (isSending) {
            Spacer(modifier = Modifier.height(8.dp))
            MiuixLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
        }
    }
}
