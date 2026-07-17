package com.starlore.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppSpecs
import com.starlore.glasense.component.Button
import com.starlore.glasense.core.component.Text
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by remember { viewModel.uiState }
    val isRegisterMode by remember { viewModel.isRegisterMode }

    val scrollState = rememberScrollState()
    val inputShape = RoundedCornerShape(16.dp)

    LaunchedEffect(viewModel.navigationFlow) {
        viewModel.navigationFlow.collect { success ->
            if (success) {
                onAuthSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        // Decorative background nebula orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppColors.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 150.dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppColors.activeTrack.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Form Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "STARLORE",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.primary,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = if (isRegisterMode) "创建您的知识宇宙星宿" else "登录您的知识星图空间",
                fontSize = 14.sp,
                color = AppColors.contentVariant,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // Glassmorphic Card container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.cardBackground.copy(alpha = 0.7f))
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = viewModel.username.value,
                        onValueChange = { viewModel.username.value = it },
                        label = { androidx.compose.material3.Text("用户名 (Username)", color = AppColors.contentVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primary,
                            unfocusedBorderColor = AppColors.scrimMedium,
                            cursorColor = AppColors.primary
                        ),
                        shape = inputShape,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = viewModel.password.value,
                        onValueChange = { viewModel.password.value = it },
                        label = { androidx.compose.material3.Text("密码 (Password)", color = AppColors.contentVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primary,
                            unfocusedBorderColor = AppColors.scrimMedium,
                            cursorColor = AppColors.primary
                        ),
                        shape = inputShape,
                        singleLine = true
                    )

                    AnimatedVisibility(visible = isRegisterMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = viewModel.nickname.value,
                                onValueChange = { viewModel.nickname.value = it },
                                label = { androidx.compose.material3.Text("昵称 (Nickname)", color = AppColors.contentVariant) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.primary,
                                    unfocusedBorderColor = AppColors.scrimMedium,
                                    cursorColor = AppColors.primary
                                ),
                                shape = inputShape,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = viewModel.email.value,
                                onValueChange = { viewModel.email.value = it },
                                label = { androidx.compose.material3.Text("邮箱 (Email)", color = AppColors.contentVariant) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.primary,
                                    unfocusedBorderColor = AppColors.scrimMedium,
                                    cursorColor = AppColors.primary
                                ),
                                shape = inputShape,
                                singleLine = true
                            )
                        }
                    }

                    if (uiState is AuthUiState.Error) {
                        androidx.compose.material3.Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = AppColors.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        action = { viewModel.submit() },
                        enabled = uiState !is AuthUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(AppColors.primary, RoundedCornerShape(25.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(color = AppColors.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                androidx.compose.material3.Text(
                                    text = if (isRegisterMode) "注册 (Register)" else "登录 (Login)",
                                    color = AppColors.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.isRegisterMode.value = !isRegisterMode
                            viewModel.uiState.value = AuthUiState.Idle
                        }
                    ) {
                        androidx.compose.material3.Text(
                            text = if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册",
                            color = AppColors.primary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
