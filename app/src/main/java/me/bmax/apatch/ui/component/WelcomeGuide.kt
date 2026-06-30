package me.bmax.apatch.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import me.bmax.apatch.R

/**
 * 首次启动欢迎引导对话框 —— 4 页分布式指导。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeGuideDialog(
    showDialog: MutableState<Boolean>,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 4

    val pages = remember {
        listOf(
            WelcomePage(
                icon = Icons.Filled.AutoAwesome,
                titleId = R.string.welcome_title_1,
                descId = R.string.welcome_desc_1,
            ),
            WelcomePage(
                icon = Icons.Filled.Palette,
                titleId = R.string.welcome_title_2,
                descId = R.string.welcome_desc_2,
            ),
            WelcomePage(
                icon = Icons.Filled.Tune,
                titleId = R.string.welcome_title_3,
                descId = R.string.welcome_desc_3,
            ),
            WelcomePage(
                icon = Icons.Filled.Settings,
                titleId = R.string.welcome_title_4,
                descId = R.string.welcome_desc_4,
            ),
        )
    }

    BasicAlertDialog(
        onDismissRequest = { /* intentionally no-op: use buttons to dismiss */ },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 页面内容（AnimatedContent 带动画过渡） ──
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        val enterOffset = { w: Int -> dir * w }
                        (slideInHorizontally(tween(300)) { enterOffset(it) } + fadeIn(tween(200)))
                            .togetherWith(
                                slideOutHorizontally(tween(300)) { -enterOffset(it) } + fadeOut(tween(200))
                            )
                    },
                    label = "welcome_page"
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        // ── 图标 ──
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── 标题 ──
                        Text(
                            text = stringResource(page.titleId),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(12.dp))

                        // ── 描述 ──
                        Text(
                            text = stringResource(page.descId),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── 圆点指示器 ──
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(totalPages) { index ->
                        val isActive = index == currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .size(if (isActive) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // ── 底部按钮行 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：“上一步”（第一页隐藏）
                    if (currentPage > 0) {
                        TextButton(onClick = { currentPage-- }) {
                            Icon(
                                Icons.Filled.ChevronLeft,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.welcome_prev))
                        }
                    } else {
                        // 占位保持布局稳定
                        Spacer(Modifier.width(80.dp))
                    }

                    // 中间：跳过（所有页都显示）
                    TextButton(onClick = {
                        showDialog.value = false
                        onDismiss()
                    }) {
                        Text(
                            text = stringResource(R.string.welcome_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 右侧：“下一步” / “开始使用”
                    if (currentPage < totalPages - 1) {
                        TextButton(onClick = { currentPage++ }) {
                            Text(stringResource(R.string.welcome_next))
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(onClick = {
                            showDialog.value = false
                            onDismiss()
                        }) {
                            Text(stringResource(R.string.welcome_got_it))
                        }
                    }
                }
            }
        }
    }

    // 模糊背景效果
    val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
    dialogWindowProvider?.let {
        APDialogBlurBehindUtils.setupWindowBlurListener(it.window)
    }
}

/**
 * 引导页数据结构
 */
private data class WelcomePage(
    val icon: ImageVector,
    val titleId: Int,
    val descId: Int,
)
