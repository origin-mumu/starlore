package com.starlore.app.feature.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberOverscrollEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.Backdrop
import com.starlore.app.theme.AppSpecs
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.GlasensePopup
import com.starlore.app.ui.components.glasense.PopupDirection
import com.starlore.app.ui.components.glasense.PopupState
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.R
import com.starlore.app.data.api.AiMessageRow
import com.starlore.app.data.api.AiSessionRow
import com.starlore.app.data.api.CharacterCard
import com.starlore.app.feature.article.MarkdownText
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.appPageBackground
import com.starlore.app.ui.components.liquid.LiquidGlassButton
import com.starlore.app.ui.components.liquid.liquidGlass
import com.starlore.app.ui.components.liquid.liquidGlassCapsule
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchobotScreen(
    accountSessionKey: Int = 0,
    initialPrompt: String? = null,
    promptToken: Int = 0,
    onBack: (() -> Unit)? = null,
    viewModel: ChatViewModel = koinViewModel(key = "chat-$accountSessionKey")
) {
    val messages = viewModel.messages
    val isSending by remember { viewModel.isSending }
    val isLoadingSession by remember { viewModel.isLoadingSession }
    val inputText = viewModel.inputText
    val selectedCharacterKey by remember { viewModel.selectedCharacterKey }
    val errorMessage by remember { viewModel.errorMessage }
    var showSessions by remember { mutableStateOf(false) }
    var showCharacters by remember { mutableStateOf(false) }
    var characterAnchorBounds by remember { mutableStateOf(Rect.Zero) }

    val listState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val latestMessageLength = messages.lastOrNull()?.let {
        it.content.length + (it.agentTrace?.length ?: 0)
    } ?: 0

    LaunchedEffect(promptToken) {
        initialPrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
            inputText.value = prompt
        }
    }

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

    Box(Modifier.fillMaxSize().appPageBackground()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            if (messages.isEmpty()) {
                EmptyChannel(
                    modifier = Modifier.fillMaxSize(),
                    topPadding = statusBarHeight + 92.dp,
                    bottomPadding = 112.dp,
                    onPrompt = { inputText.value = it }
                )
            } else {
                LazyColumn(
                    state = listState,
                    overscrollEffect = rememberOverscrollEffect(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = statusBarHeight + 92.dp,
                        bottom = 116.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { PinnedIntro() }
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isLoading = isSending && message.id == -2 && message.content.isBlank()
                        )
                    }
                }
            }
        }

        ChannelHeader(
            modifier = Modifier.align(Alignment.TopCenter),
            backdrop = backdrop,
            statusBarHeight = statusBarHeight,
            onBack = onBack,
            onOpenHistory = { showSessions = true }
        )

        ChannelComposer(
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = backdrop,
            value = inputText.value,
            enabled = !isSending && !isLoadingSession,
            canSend = !isSending && inputText.value.trim().isNotEmpty(),
            onValueChange = { inputText.value = it },
            onCharacter = { showCharacters = true },
            onCharacterAnchorChanged = { characterAnchorBounds = it },
            onSend = viewModel::sendMessage
        )

        errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 104.dp),
                action = { TextButton(onClick = viewModel::clearError) { Text("知道了") } }
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

    GlasensePopup(
        popupState = PopupState(showCharacters, characterAnchorBounds),
        onDismiss = { showCharacters = false },
        width = 270.dp,
        direction = PopupDirection.UpLeft,
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(
            "选择助手",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        viewModel.characterCards.forEach { card ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (card.key == selectedCharacterKey) AppColors.primary.copy(alpha = .12f) else Color.Transparent)
                    .clickable {
                        viewModel.selectCharacter(card.key)
                        showCharacters = false
                    }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painterResource(if (card.key == selectedCharacterKey) R.drawable.ic_checkmark_circle else R.drawable.ic_wand_and_rays),
                    null,
                    Modifier.size(18.dp),
                    if (card.key == selectedCharacterKey) AppColors.primary else AppColors.contentVariant
                )
                Column(Modifier.weight(1f)) {
                    Text(card.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(card.description, color = AppColors.contentVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ChannelHeader(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    statusBarHeight: androidx.compose.ui.unit.Dp,
    onBack: (() -> Unit)?,
    onOpenHistory: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(top = statusBarHeight + 8.dp, start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(R.drawable.ic_chevron_forward_compact, "返回", onBack, backdrop, rotate = 180f)
        Row(
            modifier = Modifier.padding(horizontal = 8.dp).weight(1f).height(58.dp)
                .liquidGlassCapsule(backdrop, surfaceColor = AppColors.cardBackground.copy(alpha = .24f))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Starlore AI", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("与你的知识一起思考", color = AppColors.contentVariant, fontSize = 12.sp)
            }
        }
        CircleIconButton(R.drawable.ic_ellipsis, "会话与更多", onOpenHistory, backdrop)
    }
}

@Composable
private fun CircleIconButton(
    icon: Int,
    label: String,
    onClick: (() -> Unit)?,
    backdrop: Backdrop,
    rotate: Float = 0f
) {
    LiquidGlassButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        backdrop = backdrop,
        modifier = Modifier.size(52.dp),
        surfaceColor = AppColors.cardBackground.copy(alpha = .2f)
    ) {
        Icon(
            painterResource(icon), label, Modifier.size(21.dp).graphicsLayer { rotationZ = rotate }, AppColors.content
        )
    }
}

@Composable
private fun EmptyChannel(
    modifier: Modifier,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPrompt: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        overscrollEffect = rememberOverscrollEffect(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PinnedIntro() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(AppColors.cardBackground.copy(alpha = .82f)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("从一个问题开始", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "我可以梳理想法、解读星记，也可以调用多个智能体一起完成复杂问题。",
                    color = AppColors.contentVariant,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                listOf(
                    "帮我整理最近记录的主题",
                    "把一个模糊想法变成文章提纲",
                    "从不同角度分析这个问题"
                ).forEach { prompt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(AppColors.primary.copy(alpha = .08f))
                            .clickable { onPrompt(prompt) }.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prompt, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Icon(painterResource(R.drawable.ic_chevron_forward_compact), null, Modifier.size(16.dp), AppColors.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedIntro() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(AppColors.primary.copy(alpha = .1f)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(painterResource(R.drawable.ic_pin), null, Modifier.size(18.dp), AppColors.primary)
        Column(Modifier.weight(1f)) {
            Text("置顶说明", color = AppColors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("内容仅用于辅助思考，重要结论请自行核对", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChannelComposer(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    value: String,
    enabled: Boolean,
    canSend: Boolean,
    onValueChange: (String) -> Unit,
    onCharacter: () -> Unit,
    onCharacterAnchorChanged: (Rect) -> Unit,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(220)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(52.dp)
            ) {
                LiquidGlassButton(
                    onClick = onCharacter,
                    enabled = enabled,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxSize().onGloballyPositioned { onCharacterAnchorChanged(it.boundsInWindow()) },
                    surfaceColor = AppColors.cardBackground.copy(alpha = .2f)
                ) {
                Icon(painterResource(R.drawable.ic_wand_and_rays), "选择助手", Modifier.size(22.dp), AppColors.primary)
                }
            }
            Row(
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    .liquidGlassCapsule(
                        backdrop,
                        surfaceColor = AppColors.cardBackground.copy(alpha = .24f),
                        tint = Color.Unspecified
                    )
                    .padding(start = 18.dp, top = 5.dp, bottom = 5.dp, end = 5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp).focusRequester(focusRequester),
                    enabled = enabled,
                    textStyle = LocalTextStyle.current.copy(color = AppColors.content, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.primary),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    decorationBox = { inner ->
                        if (value.isEmpty()) Text("写下你的问题…", color = AppColors.contentVariant, fontSize = 15.sp)
                        inner()
                    }
                )
                Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(if (canSend) AppColors.primary else AppColors.scrimNormal)
                        .clickable(enabled = canSend, onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_up), "发送", Modifier.size(19.dp),
                        if (canSend) AppColors.onPrimary else AppColors.contentVariant
                    )
                }
            }
        }
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
    if (isUser) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier.widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                    .background(AppColors.primary).padding(horizontal = 16.dp, vertical = 13.dp)
                    .animateContentSize(animationSpec = tween(200))
            ) {
                Text(text = message.content, color = AppColors.onPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(AppColors.cardBackground.copy(alpha = .84f)).padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f).animateContentSize(animationSpec = tween(200)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    message.agentTrace?.let { trace ->
                        if (trace.isNotEmpty()) {
                            TraceExpandBlock(trace = trace)
                        }
                    }
                    if (isLoading) {
                        AssistantLoadingIndicator()
                    } else {
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
