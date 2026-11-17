package com.juhao.murexide.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juhao.murexide.data.BotItem
import com.juhao.murexide.data.InstructionItem
import com.juhao.murexide.ui.components.CapsuleTabBar
import com.juhao.murexide.ui.components.MiuixLoadingIndicator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstructionPanel(
    bots: List<BotItem>,
    instructions: List<InstructionItem>,
    isLoading: Boolean,
    onInstructionClick: (InstructionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        if (isLoading && bots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MiuixLoadingIndicator(size = 24.dp)
            }
            return@Column
        }

        if (bots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "暂无指令",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            return@Column
        }

        val pagerState = rememberPagerState(pageCount = { bots.size })
        val scope = rememberCoroutineScope()

        if (bots.size > 1) {
            CapsuleTabBar(
                tabs = bots.map { it.name },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            val bot = bots[page]
            val botInstructions = instructions.filter { it.botId == bot.id }
            InstructionListPage(
                instructions = botInstructions,
                onItemClick = onInstructionClick
            )
        }
    }
}

@Composable
private fun InstructionListPage(
    instructions: List<InstructionItem>,
    onItemClick: (InstructionItem) -> Unit
) {
    if (instructions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "暂无指令",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(instructions, key = { it.id }) { item ->
            InstructionRow(item = item, onClick = { onItemClick(item) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun InstructionRow(
    item: InstructionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MiuixTheme.colorScheme.onBackground
            )
            if (item.desc.isNotEmpty()) {
                Text(
                    text = item.desc,
                    style = MiuixTheme.textStyles.footnote2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        InstructionTypeTag(type = item.type)
    }
}

@Composable
private fun InstructionTypeTag(type: Int) {
    val label = when (type) {
        2 -> "直发"
        5 -> "自定义"
        else -> "普通"
    }
    // 标签底色走 secondaryContainer，与 Miuix 状态标签的容器/前景配对一致
    Box(
        modifier = Modifier
            .background(MiuixTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSecondaryContainer
        )
    }
}
