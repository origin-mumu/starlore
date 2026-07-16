package com.starlore.app.feature.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.theme.AppSpecs
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.R
import com.starlore.app.data.api.AiMessageRow
import com.starlore.app.data.api.AiSessionRow
import com.starlore.app.data.api.CharacterCard
import com.starlore.app.feature.article.MarkdownText
import com.starlore.app.theme.AppColors
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchobotScreen(
    accountSessionKey: Int = 0,
    viewModel: ChatViewModel = koinViewModel(key = "chat-$accountSessionKey")
) {
    val messages = viewModel.messages
    val isSending by remember { viewModel.isSending }
    val isLoadingSession by remember { viewModel.isLoadingSession }
    val inputText = viewModel.inputText
    val selectedCharacterKey by remember { viewModel.selectedCharacterKey }
    val quota by remember { viewModel.quota }
    val errorMessage by remember { viewModel.errorMessage }
    var showSessions by remember { mutableStateOf(false) }
    var showCharacters by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val latestMessageLength = messages.lastOrNull()?.let {
        it.content.length + (it.agentTrace?.length ?: 0)
    } ?: 0

    // Follow both new messages and incremental SSE updates.
    LaunchedEffect(messages.size, latestMessageLength) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            // Messages area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarHeight + 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sparkle_viewfinder),
                                contentDescription = null,
                                tint = AppColors.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "向 Echobot 提问以启动多智能体协作计划...",
                                color = AppColors.contentVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = statusBarHeight + 56.dp + 12.dp,
                            bottom = 12.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                isLoading = isSending && message.id == -2 && message.content.isBlank()
                            )
                        }
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val characterName = viewModel.characterCards
                    .firstOrNull { it.key == selectedCharacterKey }?.name ?: "默认助手"
                AssistChip(
                    onClick = { showCharacters = true },
                    label = { Text(characterName, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_character),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    enabled = !isSending
                )
                quota?.let {
                    Text(
                        text = if (it.isAdmin) "无限额度" else "剩余 ${it.remaining}/${it.dailyLimit}",
                        color = AppColors.contentVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 100.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText.value,
                    onValueChange = { inputText.value = it },
                    placeholder = { androidx.compose.material3.Text("写下你的思考...", color = AppColors.contentVariant) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.cardBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = AppColors.cardBackground.copy(alpha = 0.3f),
                        focusedBorderColor = AppColors.primary,
                        unfocusedBorderColor = AppColors.scrimMedium,
                        cursorColor = AppColors.primary
                    ),
                    maxLines = 4,
                    enabled = !isSending && !isLoadingSession
                )

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = !isSending && inputText.value.trim().isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (inputText.value.trim().isNotEmpty() && !isSending) AppColors.primary else AppColors.scrimNormal)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_up),
                        contentDescription = "Send",
                        tint = if (inputText.value.trim().isNotEmpty() && !isSending) AppColors.onPrimary else AppColors.contentVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Overlay top app bar with blurred glass texture
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(statusBarHeight + 56.dp)
        ) {
            val isSmallTitleVisible by remember {
                derivedStateOf {
                    if (messages.isEmpty()) true else listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10
                }
            }
            GlasenseDynamicSmallTitle(
                modifier = Modifier.fillMaxSize(),
                title = "智能助手",
                statusBarHeight = statusBarHeight,
                isVisible = isSmallTitleVisible,
                backdrop = backdrop,
                surfaceColor = backgroundColor
            ) {}
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSessions = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clock_cycle),
                        contentDescription = "会话记录",
                        tint = AppColors.content,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = viewModel::newSession, enabled = !isSending) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_plus),
                        contentDescription = "新建会话",
                        tint = AppColors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 116.dp),
                action = {
                    TextButton(onClick = viewModel::clearError) { Text("知道了") }
                }
            ) { Text(message) }
        }
    }

    if (showSessions) {
        SessionHistorySheet(
            sessions = viewModel.sessions,
            activeSessionId = viewModel.activeSessionId.value,
            isBusy = isSending,
            onDismiss = { showSessions = false },
            onNewSession = {
                viewModel.newSession()
                showSessions = false
            },
            onSelectSession = {
                viewModel.selectSession(it)
                showSessions = false
            },
            onDeleteSession = viewModel::deleteSession
        )
    }

    if (showCharacters) {
        CharacterPickerSheet(
            cards = viewModel.characterCards,
            selectedKey = selectedCharacterKey,
            onDismiss = { showCharacters = false },
            onSelect = {
                viewModel.selectCharacter(it)
                showCharacters = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionHistorySheet(
    sessions: List<AiSessionRow>,
    activeSessionId: Int?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onNewSession: () -> Unit,
    onSelectSession: (Int) -> Unit,
    onDeleteSession: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.pageBackground) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("会话记录", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onNewSession, enabled = !isBusy) { Text("＋ 新会话") }
        }
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Text("还没有历史会话", color = AppColors.contentVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (session.id == activeSessionId) AppColors.primary.copy(alpha = 0.12f)
                                else AppColors.cardBackground.copy(alpha = 0.65f)
                            )
                            .clickable(enabled = !isBusy) { onSelectSession(session.id) }
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                session.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (session.id == activeSessionId) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                session.updatedAt.take(16).replace('T', ' '),
                                color = AppColors.contentVariant,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { onDeleteSession(session.id) }, enabled = !isBusy) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash),
                                contentDescription = "删除会话",
                                tint = AppColors.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterPickerSheet(
    cards: List<CharacterCard>,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.pageBackground) {
        Text(
            "选择助手",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cards.forEach { card ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (card.key == selectedKey) AppColors.primary.copy(alpha = 0.12f)
                            else AppColors.cardBackground.copy(alpha = 0.65f)
                        )
                        .clickable { onSelect(card.key) }
                        .padding(16.dp)
                ) {
                    Text(card.name, fontWeight = FontWeight.Bold)
                    Text(card.description, color = AppColors.contentVariant, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
fun MessageBubble(message: AiMessageRow, isLoading: Boolean = false) {
    val isUser = message.role == "user"
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .align(if (isUser) Alignment.TopEnd else Alignment.TopStart)
                .fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Label removed as requested

            // Bubble shape & background
            val bubbleShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        if (isUser) AppColors.primary.copy(alpha = 0.15f) else AppColors.cardBackground.copy(alpha = 0.5f)
                    )
                    .then(
                        if (!isUser) Modifier.glasenseHighlight(bubbleShape) else Modifier
                    )
                    .padding(14.dp)
                    .animateContentSize(animationSpec = tween(200))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reasoning traces (observability expansion block!) displayed ABOVE the final answer
                    if (!isUser) {
                        message.agentTrace?.let { trace ->
                            if (trace.isNotEmpty()) {
                                TraceExpandBlock(trace = trace)
                            }
                        }
                    }

                    if (isLoading) {
                        AssistantLoadingIndicator()
                    } else {
                        // Answer text (Markdown rendered!)
                        MarkdownText(text = message.content)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantLoadingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = AppColors.primary,
            trackColor = AppColors.scrimMedium
        )
        Text(
            text = "正在生成回答",
            color = AppColors.contentVariant,
            fontSize = 13.sp
        )
    }
}

enum class StepStatus {
    PENDING, RUNNING, SUCCESS, FAILED
}

data class TraceStep(
    val title: String,
    val description: String,
    val status: StepStatus,
    val iconId: Int
)

fun parseTraceSteps(trace: String): List<TraceStep> {
    val steps = mutableListOf<TraceStep>()
    if (trace.isEmpty()) return steps

    // 1. Planner Step
    val hasPlanStart = trace.contains("规划中") || trace.contains("Planner")
    if (hasPlanStart) {
        val summary = if (trace.contains("Planner 规划计划:")) {
            trace.substringAfter("Planner 规划计划:").substringBefore("\n\n").trim()
        } else {
            "正在规划任务计划..."
        }
        val isDone = trace.contains("Planner 规划计划:")
        steps.add(
            TraceStep(
                title = "任务规划 (Planner)",
                description = summary,
                status = if (isDone) StepStatus.SUCCESS else StepStatus.RUNNING,
                iconId = R.drawable.ic_document
            )
        )
    }

    // 2. Executor Step
    val hasExecutor = trace.contains("Executor")
    if (hasExecutor) {
        val execLines = trace.lines().filter { it.contains("Executor 执行子任务") || it.contains("子任务完成反馈") }
        val lastLine = execLines.lastOrNull() ?: ""
        val desc = if (lastLine.contains("子任务完成反馈")) {
            "子任务执行完成"
        } else if (lastLine.contains("Executor 执行子任务:")) {
            "执行中: " + lastLine.substringAfter("Executor 执行子任务:").trim()
        } else {
            "子任务开始执行..."
        }
        val isDone = trace.contains("Reviewer") || trace.contains("done")
        steps.add(
            TraceStep(
                title = "执行阶段 (Executor)",
                description = desc,
                status = if (isDone) StepStatus.SUCCESS else StepStatus.RUNNING,
                iconId = R.drawable.ic_bolt
            )
        )
    }

    // 3. Reviewer Step
    val hasReviewer = trace.contains("Reviewer")
    if (hasReviewer) {
        val reviewerText = trace.substringAfter("Reviewer 审查决策:").substringBefore("\n\n").trim()
        val isPass = reviewerText.contains("PASS")
        val desc = if (isPass) "决策: PASS" else "决策: FAIL\n${reviewerText.replace("FAIL", "").trim()}"
        steps.add(
            TraceStep(
                title = "结果审核 (Reviewer)",
                description = desc,
                status = if (isPass) StepStatus.SUCCESS else StepStatus.FAILED,
                iconId = if (isPass) R.drawable.ic_checkmark_circle else R.drawable.ic_xmark_bold_circle_fill
            )
        )
    } else if (hasExecutor && !trace.contains("done")) {
        steps.add(
            TraceStep(
                title = "结果审核 (Reviewer)",
                description = "等待子任务完成后开始审查",
                status = StepStatus.PENDING,
                iconId = R.drawable.ic_clock
            )
        )
    }

    return steps
}

@Composable
fun FlowchartItem(
    step: TraceStep,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            val tintColor = when (step.status) {
                StepStatus.SUCCESS -> AppColors.primary
                StepStatus.RUNNING -> Color(0xFFF57C00) // Orange 400
                StepStatus.FAILED -> AppColors.error
                StepStatus.PENDING -> AppColors.contentVariant
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(step.iconId),
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (!isLast) {
                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(AppColors.scrimMedium)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        ) {
            Text(
                text = step.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when (step.status) {
                    StepStatus.FAILED -> AppColors.error
                    else -> AppColors.content
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = step.description,
                fontSize = 11.sp,
                color = AppColors.contentVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun TraceExpandBlock(trace: String) {
    var isExpanded by remember { mutableStateOf(true) }
    val steps = remember(trace) { parseTraceSteps(trace) }

    if (steps.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.scrimNormal)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mini_analytics),
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isExpanded) "收起推理链路 (Hide Trace)" else "查看推理链路 (Show Reasoning Trace)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.primary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.scrimLight)
                    .padding(12.dp)
            ) {
                Column {
                    steps.forEachIndexed { index, step ->
                        FlowchartItem(step = step, isLast = index == steps.size - 1)
                    }
                }
            }
        }
    }
}
