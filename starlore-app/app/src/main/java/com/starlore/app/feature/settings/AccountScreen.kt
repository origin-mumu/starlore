package com.starlore.app.feature.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.R
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.feature.settings.util.SettingsViewModel
import com.starlore.app.theme.AppButtonColors
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppSpecs
import com.starlore.app.theme.harmonize
import com.starlore.app.ui.components.glasense.GlasenseButton
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.ui.components.packed.ConfigInfoHeader
import com.starlore.app.ui.components.packed.ConfigTextField
import com.starlore.app.ui.components.packed.TopBarSpacer
import com.starlore.glasense.component.ListRowAccessory
import com.starlore.glasense.component.ListStack
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.core.component.VGap
import com.starlore.glasense.theme.tokens.Slate500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class EditField {
    NONE, NICKNAME, BIO, EMAIL, LOCATION, WEBSITE, GITHUB, PASSWORD
}

@Composable
fun AccountScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    // Current state for user info
    var nickname by remember { mutableStateOf(SettingsManager.userNickname) }
    var bio by remember { mutableStateOf(SettingsManager.userBio) }
    var email by remember { mutableStateOf(SettingsManager.userEmail) }
    var location by remember { mutableStateOf(SettingsManager.userLocation) }
    var website by remember { mutableStateOf(SettingsManager.userWebsite) }
    var github by remember { mutableStateOf(SettingsManager.userGithub) }

    // Dialog trigger states
    var activeEditField by remember { mutableStateOf(EditField.NONE) }
    var isUpdating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Sync from server on launch
    LaunchedEffect(Unit) {
        settingsViewModel.fetchUserProfile {
            nickname = SettingsManager.userNickname
            bio = SettingsManager.userBio
            email = SettingsManager.userEmail
            location = SettingsManager.userLocation
            website = SettingsManager.userWebsite
            github = SettingsManager.userGithub
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

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Perform profile update api call
    fun saveField(field: EditField, newValue: String) {
        isUpdating = true
        val targetNickname = if (field == EditField.NICKNAME) newValue else nickname
        val targetBio = if (field == EditField.BIO) newValue else bio
        val targetEmail = if (field == EditField.EMAIL) newValue else email
        val targetLocation = if (field == EditField.LOCATION) newValue else location
        val targetWebsite = if (field == EditField.WEBSITE) newValue else website
        val targetGithub = if (field == EditField.GITHUB) newValue else github

        settingsViewModel.updateProfile(
            nickname = targetNickname.trim(),
            bio = targetBio.trim(),
            email = targetEmail.trim(),
            location = targetLocation.trim(),
            website = targetWebsite.trim(),
            github = targetGithub.trim(),
            onSuccess = {
                isUpdating = false
                when (field) {
                    EditField.NICKNAME -> nickname = targetNickname
                    EditField.BIO -> bio = targetBio
                    EditField.EMAIL -> email = targetEmail
                    EditField.LOCATION -> location = targetLocation
                    EditField.WEBSITE -> website = targetWebsite
                    EditField.GITHUB -> github = targetGithub
                    else -> {}
                }
                activeEditField = EditField.NONE
                Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                isUpdating = false
                Toast.makeText(context, "更新失败: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ListStack(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight + 24.dp)
        ) {
            TopBarSpacer()

            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = harmonize(Slate500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_character),
                    title = "账户管理",
                    info = "罗列并管理您的个人账户和密码基本安全信息"
                )
            }

            item { VGap(24.dp) }

            // Section 1: Basic Info List
            Section(header = { "个人资料" }) {
                Row(
                    onClick = { activeEditField = EditField.NICKNAME },
                    accessory = ListRowAccessory.Chevron,
                    trailing = { Text(nickname.ifEmpty { "未设置" }, color = AppColors.contentVariant) }
                ) {
                    Text("修改昵称")
                }
                Row(
                    onClick = { activeEditField = EditField.BIO },
                    accessory = ListRowAccessory.Chevron,
                    trailing = {
                        val displayBio = bio.ifEmpty { "未设置" }
                        Text(
                            text = if (displayBio.length > 12) displayBio.take(12) + "..." else displayBio,
                            color = AppColors.contentVariant
                        )
                    }
                ) {
                    Text("修改个性签名")
                }
                Row(
                    onClick = { activeEditField = EditField.EMAIL },
                    accessory = ListRowAccessory.Chevron,
                    trailing = { Text(email.ifEmpty { "未设置" }, color = AppColors.contentVariant) }
                ) {
                    Text("修改邮箱")
                }
                Row(
                    onClick = { activeEditField = EditField.LOCATION },
                    accessory = ListRowAccessory.Chevron,
                    trailing = { Text(location.ifEmpty { "未设置" }, color = AppColors.contentVariant) }
                ) {
                    Text("修改所在地")
                }
                Row(
                    onClick = { activeEditField = EditField.WEBSITE },
                    accessory = ListRowAccessory.Chevron,
                    trailing = {
                        val displayUrl = website.ifEmpty { "未设置" }
                        Text(
                            text = if (displayUrl.length > 15) displayUrl.take(15) + "..." else displayUrl,
                            color = AppColors.contentVariant
                        )
                    }
                ) {
                    Text("修改个人网站")
                }
                Row(
                    onClick = { activeEditField = EditField.GITHUB },
                    accessory = ListRowAccessory.Chevron,
                    trailing = { Text(github.ifEmpty { "未设置" }, color = AppColors.contentVariant) }
                ) {
                    Text("修改GitHub")
                }
            }

            // Section 2: Password Security List
            Section(header = { "账号安全" }) {
                Row(
                    onClick = { activeEditField = EditField.PASSWORD },
                    accessory = ListRowAccessory.Chevron
                ) {
                    Text("修改密码")
                }
            }
        }

        // Show individual modal dialogs on click
        when (activeEditField) {
            EditField.NICKNAME -> {
                EditDialog(
                    title = "修改昵称",
                    initialValue = nickname,
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { saveField(EditField.NICKNAME, it) }
                )
            }
            EditField.BIO -> {
                EditDialog(
                    title = "修改个性签名",
                    initialValue = bio,
                    singleLine = false,
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { saveField(EditField.BIO, it) }
                )
            }
            EditField.EMAIL -> {
                EditDialog(
                    title = "修改邮箱",
                    initialValue = email,
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { saveField(EditField.EMAIL, it) }
                )
            }
            EditField.LOCATION -> {
                EditDialog(
                    title = "修改所在地",
                    initialValue = location,
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { saveField(EditField.LOCATION, it) }
                )
            }
            EditField.WEBSITE -> {
                EditDialog(
                    title = "修改个人网站",
                    initialValue = website,
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { saveField(EditField.WEBSITE, it) }
                )
            }
            EditField.GITHUB -> {
                EditDialog(
                    title = "修改GitHub",
                    initialValue = github,
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { saveField(EditField.GITHUB, it) }
                )
            }
            EditField.PASSWORD -> {
                PasswordEditDialog(
                    isUpdating = isUpdating,
                    onDismiss = { activeEditField = EditField.NONE },
                    onConfirm = { oldPass, newPass, confirmPass ->
                        if (newPass.isEmpty()) {
                            Toast.makeText(context, "新密码不能为空", Toast.LENGTH_SHORT).show()
                            return@PasswordEditDialog
                        }
                        if (newPass != confirmPass) {
                            Toast.makeText(context, "新密码与确认密码不一致", Toast.LENGTH_SHORT).show()
                            return@PasswordEditDialog
                        }
                        // Mock update delay since API doesn't support changing password
                        coroutineScope.launch {
                            isUpdating = true
                            delay(800)
                            isUpdating = false
                            activeEditField = EditField.NONE
                            Toast.makeText(context, "修改密码失败：服务器暂未开放此接口", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            EditField.NONE -> {}
        }

        // Small animated title bar
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "账户管理",
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = AppColors.pageBackground
        ) {}

        GlasenseBackButton(
            onClick = { activity?.finish() },
            backdrop = backdrop,
            modifier = Modifier
                .padding(top = statusBarHeight, start = 12.dp)
                .size(48.dp)
                .align(Alignment.TopStart)
        )

        // Show individual custom modal dialogs as screen overlay
        if (activeEditField != EditField.NONE) {
            BackHandler(enabled = true) {
                if (!isUpdating) activeEditField = EditField.NONE
            }

            when (activeEditField) {
                EditField.NICKNAME -> {
                    EditDialog(
                        title = "修改昵称",
                        initialValue = nickname,
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { saveField(EditField.NICKNAME, it) }
                    )
                }
                EditField.BIO -> {
                    EditDialog(
                        title = "修改个性签名",
                        initialValue = bio,
                        singleLine = false,
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { saveField(EditField.BIO, it) }
                    )
                }
                EditField.EMAIL -> {
                    EditDialog(
                        title = "修改邮箱",
                        initialValue = email,
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { saveField(EditField.EMAIL, it) }
                    )
                }
                EditField.LOCATION -> {
                    EditDialog(
                        title = "修改所在地",
                        initialValue = location,
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { saveField(EditField.LOCATION, it) }
                    )
                }
                EditField.WEBSITE -> {
                    EditDialog(
                        title = "修改个人网站",
                        initialValue = website,
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { saveField(EditField.WEBSITE, it) }
                    )
                }
                EditField.GITHUB -> {
                    EditDialog(
                        title = "修改GitHub",
                        initialValue = github,
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { saveField(EditField.GITHUB, it) }
                    )
                }
                EditField.PASSWORD -> {
                    PasswordEditDialog(
                        isUpdating = isUpdating,
                        onDismiss = { activeEditField = EditField.NONE },
                        onConfirm = { oldPass, newPass, confirmPass ->
                            if (newPass.isEmpty()) {
                                Toast.makeText(context, "新密码不能为空", Toast.LENGTH_SHORT).show()
                                return@PasswordEditDialog
                            }
                            if (newPass != confirmPass) {
                                Toast.makeText(context, "新密码与确认密码不一致", Toast.LENGTH_SHORT).show()
                                return@PasswordEditDialog
                            }
                            coroutineScope.launch {
                                isUpdating = true
                                delay(800)
                                isUpdating = false
                                activeEditField = EditField.NONE
                                Toast.makeText(context, "修改密码失败：服务器暂未开放此接口", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
                EditField.NONE -> {}
            }
        }
    }
}

@Composable
private fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AppColors.pageBackground.copy(alpha = 0.5f), AppSpecs.cardShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.primary),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AppColors.content,
                fontSize = 15.sp
            ),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        color = AppColors.contentVariant,
                        fontSize = 15.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun EditDialog(
    title: String,
    initialValue: String,
    singleLine: Boolean = true,
    isUpdating: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }
    val density = LocalDensity.current
    val screenWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isUpdating) onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(screenWidth - 48.dp * 2)
                .clip(AppSpecs.dialogShape)
                .background(AppColors.cardBackground.copy(alpha = 0.95f))
                .glasenseHighlight(AppSpecs.dialogCorner)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary
                )

                DialogTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = "请输入${title.replace("修改", "")}",
                    singleLine = singleLine
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isUpdating
                    ) {
                        Text("取消", color = AppColors.contentVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onConfirm(textValue) },
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = AppColors.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("确定", color = AppColors.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordEditDialog(
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val density = LocalDensity.current
    val screenWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isUpdating) onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(screenWidth - 48.dp * 2)
                .clip(AppSpecs.dialogShape)
                .background(AppColors.cardBackground.copy(alpha = 0.95f))
                .glasenseHighlight(AppSpecs.dialogCorner)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "修改密码",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("当前密码", fontSize = 12.sp, color = AppColors.contentVariant)
                    DialogTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        placeholder = "输入旧密码进行验证",
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("新密码", fontSize = 12.sp, color = AppColors.contentVariant)
                    DialogTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = "输入新密码",
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("确认新密码", fontSize = 12.sp, color = AppColors.contentVariant)
                    DialogTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "再次输入新密码",
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isUpdating
                    ) {
                        Text("取消", color = AppColors.contentVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onConfirm(oldPassword, newPassword, confirmPassword) },
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = AppColors.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("确定", color = AppColors.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
