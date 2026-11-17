package com.juhao.murexide.ui.sticker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.juhao.murexide.data.StickerPack
import com.juhao.murexide.data.resolveStickerUrl
import com.juhao.murexide.datastore.AccountStorage
import com.juhao.murexide.ui.icons.AppIcons
import com.juhao.murexide.ui.components.*
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickerPackManagerScreen(
    viewModel: StickerPackManagerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val snackbarHostState = remember { SnackbarHostState() }
    val accountStorage = remember { AccountStorage.getInstance(context) }
    var currentUserId by remember { mutableStateOf("") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var packToRemove by remember { mutableStateOf<StickerPack?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        accountStorage.getCurrentUserId()?.let { currentUserId = it }
    }

    val listState = rememberLazyListState()
    val packsState = rememberUpdatedState(state.packs)
    var draggingPackId by remember { mutableStateOf<Long?>(null) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    var overlayTop by remember { mutableFloatStateOf(0f) }
    var scrollSpeed by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val touchOffsetY = remember { floatArrayOf(0f) }
    val dragIndex = remember { intArrayOf(-1) }
    val dragItemHeight = remember { floatArrayOf(0f) }
    val edgePx = remember(density) { with(density) { 64.dp.toPx() } }
    val maxSpeedPx = remember(density) { with(density) { 900.dp.toPx() } }
    val maxStepPx = remember(density) { with(density) { 36.dp.toPx() } }

    fun clampToViewport(y: Float): Float {
        val layout = listState.layoutInfo
        val top = layout.beforeContentPadding.toFloat()
        val bottom = (layout.viewportEndOffset - layout.afterContentPadding).toFloat()
        return y.coerceIn(top, bottom)
    }

    fun refreshScrollSpeed(y: Float) {
        val layout = listState.layoutInfo
        val top = layout.beforeContentPadding.toFloat()
        val bottom = (layout.viewportEndOffset - layout.afterContentPadding).toFloat()
        if (bottom <= top) {
            scrollSpeed = 0f
            return
        }
        scrollSpeed = when {
            y < top + edgePx && listState.canScrollBackward ->
                -maxSpeedPx * ((top + edgePx - y) / edgePx).coerceIn(0f, 1f)
            y > bottom - edgePx && listState.canScrollForward ->
                maxSpeedPx * ((y - (bottom - edgePx)) / edgePx).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun reorderByOffset() {
        val id = draggingPackId ?: return
        val from = dragIndex[0]
        if (from < 0) return
        val height = dragItemHeight[0]
        if (height <= 0f) return
        val steps = (dragOffsetY / height).roundToInt()
        if (steps == 0) return
        val total = packsState.value.size
        val to = (from + steps).coerceIn(0, total - 1)
        if (to == from) {
            dragOffsetY -= steps * height
            return
        }
        viewModel.move(from, to)
        dragIndex[0] = to
        dragOffsetY -= steps * height
    }

    LaunchedEffect(draggingPackId) {
        if (draggingPackId == null) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive) {
            val frame = withFrameNanos { it }
            val dt = ((frame - last) / 1_000_000_000f).coerceIn(0f, 0.034f)
            last = frame
            val speed = scrollSpeed
            if (speed != 0f) {
                val step = (speed * dt).coerceIn(-maxStepPx, maxStepPx)
                listState.scrollBy(step)
                reorderByOffset()
            }
        }
    }

    MiuixPageScaffold(
        title = "管理表情包",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(AppIcons.Add, contentDescription = "新建表情包")
            }
        },
        snackbarHost = { MiuixSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.packs.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        MiuixLoadingIndicator()
                    }
                }

                state.error != null && state.packs.isEmpty() -> EmptyPlaceholder(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    icon = AppIcons.Warning,
                    title = "加载失败",
                    subtitle = state.error,
                    primaryButton = {
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) { Text("重试") }
                    }
                )

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = state.packs,
                            key = { _, pack -> pack.id }
                        ) { _, pack ->
                            val isDragging = draggingPackId == pack.id
                            StickerPackRow(
                                pack = pack,
                                isDragging = false,
                                dragOffsetY = 0f,
                                onClick = {
                                    if (draggingPackId == null) {
                                        StickerDetailActivity.start(
                                            context = context,
                                            packId = pack.id,
                                            token = viewModel.token,
                                            currentUserId = currentUserId
                                        )
                                    }
                                },
                                onRemove = { packToRemove = pack },
                                modifier = Modifier
                                    .then(
                                        if (!isDragging) Modifier.animateItem()
                                        else Modifier
                                    )
                                    .graphicsLayer { alpha = if (isDragging) 0f else 1f }
                                    .pointerInput(pack.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { start ->
                                                val info = listState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.key == pack.id }
                                                    ?: return@detectDragGesturesAfterLongPress
                                                val index = packsState.value.indexOfFirst { it.id == pack.id }
                                                if (index < 0) return@detectDragGesturesAfterLongPress
                                                draggingPackId = pack.id
                                                touchOffsetY[0] = start.y
                                                dragItemHeight[0] = info.size.toFloat().coerceAtLeast(1f)
                                                dragIndex[0] = index
                                                dragOffsetY = 0f
                                                fingerY = clampToViewport(info.offset + start.y)
                                                overlayTop = fingerY - touchOffsetY[0]
                                                refreshScrollSpeed(fingerY)
                                            },
                                            onDrag = { change, dragAmount ->
                                                if (draggingPackId != pack.id) {
                                                    return@detectDragGesturesAfterLongPress
                                                }
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val info = listState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.key == pack.id }
                                                val rawFingerY = if (info != null) {
                                                    info.offset + change.position.y
                                                } else {
                                                    fingerY + dragAmount.y
                                                }
                                                fingerY = clampToViewport(rawFingerY)
                                                overlayTop = fingerY - touchOffsetY[0]
                                                refreshScrollSpeed(fingerY)
                                                reorderByOffset()
                                            },
                                            onDragEnd = {
                                                scrollSpeed = 0f
                                                draggingPackId = null
                                                dragIndex[0] = -1
                                                dragOffsetY = 0f
                                                viewModel.persistSort()
                                            },
                                            onDragCancel = {
                                                scrollSpeed = 0f
                                                draggingPackId = null
                                                dragIndex[0] = -1
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            val draggingPack = draggingPackId?.let { id ->
                state.packs.firstOrNull { it.id == id }
            }
            if (draggingPack != null) {
                val topPaddingPx = with(density) { innerPadding.calculateTopPadding().toPx() }
                StickerPackRow(
                    pack = draggingPack,
                    isDragging = true,
                    dragOffsetY = 0f,
                    onClick = {},
                    onRemove = {},
                    interactive = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .offset {
                            IntOffset(0, (overlayTop + topPaddingPx).roundToInt())
                        }
                )
            }
        }
    }

    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        MiuixAlertDialog(
            show = true,
            title = "新建表情包",
            confirmText = "创建",
            confirmEnabled = newName.isNotBlank() && !state.isSubmitting,
            onConfirm = {
                viewModel.createPack(newName) {
                    showCreateDialog = false
                }
            },
            onDismissRequest = { showCreateDialog = false }
        ) {
            MiuixTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "表情包名称",
                singleLine = true
            )
        }
    }

    packToRemove?.let { pack ->
        MiuixAlertDialog(
            show = true,
            title = "移除表情包",
            summary = "确定要从收藏中移除「${pack.name}」吗？",
            confirmText = "移除",
            onConfirm = {
                viewModel.removePack(pack.id)
                packToRemove = null
            },
            onDismissRequest = { packToRemove = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerPackRow(
    pack: StickerPack,
    isDragging: Boolean,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true
) {
    val context = LocalContext.current
    val coverUrl = pack.stickerItems.firstOrNull()?.url

    val imageRequest = remember(coverUrl) {
        ImageRequest.Builder(context)
            .data(coverUrl?.let { resolveStickerUrl(it) })
            .apply {
                if (coverUrl?.contains("jwznb.com") == true) {
                    setHeader("Referer", "https://myapp.jwznb.com")
                }
            }
            .build()
    }

    // Miuix 卡片行：Card 自带 surfaceContainer 容器与 HyperOS 圆角；
    // 拖拽时的强调只在需要时叠一层阴影，颜色仍走同一套语义 token
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer { translationY = dragOffsetY }
            .then(if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(12.dp)) else Modifier)
            .then(
                if (interactive) Modifier.combinedClickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = AppIcons.Mood,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceContainerVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${pack.stickerItems.size} 个表情",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant
                )
            }

            if (interactive) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = "移除",
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.error
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.error
                    )
                }
            }

            Icon(
                imageVector = AppIcons.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        }
    }
}