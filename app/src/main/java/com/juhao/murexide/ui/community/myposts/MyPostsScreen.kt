package com.juhao.murexide.ui.community.myposts

import com.juhao.murexide.ui.icons.AppIcons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.juhao.murexide.ui.community.PostCard
import com.juhao.murexide.ui.community.detail.PostDetailActivity
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import com.juhao.murexide.ui.components.MiuixPageScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    onBackClick: () -> Unit,
    viewModel: MyPostsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MiuixPageScaffold(
        title = "我的文章",
        onBack = onBackClick
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MiuixLoadingIndicator(size = 36.dp)
                    }
                }

                uiState.posts.isEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Box(
                                Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.error ?: "你还没有发布文章",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(uiState.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onLikeClick = { },
                                onCollectClick = { },
                                onPostClick = { PostDetailActivity.start(context, post.id) }
                            )
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MiuixLoadingIndicator(size = 28.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
