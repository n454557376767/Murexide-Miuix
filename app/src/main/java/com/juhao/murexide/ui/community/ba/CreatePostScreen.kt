package com.juhao.murexide.ui.community.ba

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.juhao.murexide.ui.components.*
import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.utils.QiniuUploader
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CreatePostScreen(
    baName: String,
    onClose: () -> Unit,
    onPublished: () -> Unit,
    viewModel: CreatePostViewModel,
    postId: Int? = null,
    title: String,
    content: String,
    contentType: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isEditMode = postId != null

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var uploading by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }

    var contentFieldValue by remember {
        mutableStateOf(TextFieldValue(content, TextRange(content.length)))
    }
    var contentInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onTitleChange(title)
        viewModel.onContentChange(content)
        viewModel.onContentTypeChange(contentType)
        viewModel.onEditModeChange(isEditMode)
        if (isEditMode) {
            viewModel.onPostIdChange(postId)
        }
    }

    LaunchedEffect(Unit) {
        if (!contentInitialized) {
            contentFieldValue = TextFieldValue(content, TextRange(content.length))
            contentInitialized = true
        }
    }

    LaunchedEffect(uiState.contentType) {
        if (uiState.contentType != 2) {
            isPreviewMode = false
        }
    }

    LaunchedEffect(uiState.published) {
        if (uiState.published) onPublished()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    fun insertAtCursor(text: String) {
        val start = contentFieldValue.selection.start.coerceIn(0, contentFieldValue.text.length)
        val end = contentFieldValue.selection.end.coerceIn(0, contentFieldValue.text.length)
        val newText = contentFieldValue.text.replaceRange(start, end, text)
        contentFieldValue = TextFieldValue(newText, TextRange(start + text.length))
        viewModel.onContentChange(newText)
        focusRequester.requestFocus()
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        if (viewModel.token.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("请先登录") }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            uploading = true
            QiniuUploader(
                context = context,
                userToken = viewModel.token,
                uploadType = 1
            ).uploadFromUri(context, uri)
                .onSuccess { response ->
                    insertAtCursor("![](https://chat-img.jwznb.com/${response.key})")
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(error.message ?: "图片上传失败")
                }
            uploading = false
        }
    }

    MiuixPageScaffold(
        modifier = Modifier.imePadding(),
        title = when {
            isEditMode -> "编辑文章"
            baName.isNotEmpty() -> "发布到 $baName"
            else -> "发布文章"
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(AppIcons.Close, contentDescription = "关闭")
            }
        },
        snackbarHost = { MiuixSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // 原来的 BottomAppBar + FloatingActionButton 换成 Miuix 行内操作条 + 主色发布按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.contentType == 2) {
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) AppIcons.Edit else AppIcons.Visibility,
                            contentDescription = if (isPreviewMode) "编辑" else "预览",
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                    if (!isPreviewMode) {
                        IconButton(onClick = { insertAtCursor("# ") }) {
                            Icon(
                                imageVector = AppIcons.Tag,
                                contentDescription = "标题",
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { insertAtCursor("**粗体**") }) {
                            Icon(
                                imageVector = AppIcons.FormatBold,
                                contentDescription = "粗体",
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { insertAtCursor("[]()") }) {
                            Icon(
                                imageVector = AppIcons.Link,
                                contentDescription = "链接",
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !uploading
                        ) {
                            if (uploading) {
                                MiuixLoadingIndicator(size = 18.dp, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = AppIcons.Image,
                                    contentDescription = "插入图片",
                                    tint = MiuixTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { if (!uiState.isPublishing) viewModel.publish() },
                    enabled = !uiState.isPublishing,
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    if (uiState.isPublishing) {
                        MiuixLoadingIndicator(
                            size = 18.dp,
                            strokeWidth = 2.dp,
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("发布")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            MiuixTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = "标题",
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "内容格式",
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(12.dp))
                // 原来的 FilterChip 换成 Miuix 按钮：选中态用主色，未选中用次级色
                Button(
                    onClick = { viewModel.onContentTypeChange(1) },
                    colors = if (uiState.contentType == 1) ButtonDefaults.buttonColorsPrimary()
                    else ButtonDefaults.buttonColors()
                ) {
                    Text("文本")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.onContentTypeChange(2) },
                    colors = if (uiState.contentType == 2) ButtonDefaults.buttonColorsPrimary()
                    else ButtonDefaults.buttonColors()
                ) {
                    Text("Markdown")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (uiState.contentType == 2 && isPreviewMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (uiState.content.isBlank()) {
                            Text(
                                text = "暂无内容",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                            )
                        } else {
                            MarkdownText(
                                markdown = uiState.content,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                MiuixTextField(
                    value = contentFieldValue,
                    onValueChange = { newValue ->
                        contentFieldValue = newValue
                        viewModel.onContentChange(newValue.text)
                    },
                    placeholder = if (uiState.contentType == 2) "内容（支持 Markdown）" else "内容",
                    singleLine = false,
                    minHeight = 240,
                    focusRequester = focusRequester,
                    modifier = Modifier.heightIn(min = 240.dp)
                )
            }
        }
    }
}
