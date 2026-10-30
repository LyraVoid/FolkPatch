@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkColors

import me.bmax.apatch.ui.theme.tokens.FolkDimens

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

@Composable
fun MagiskStyleCard(
    title: String,
    icon: ImageVector,
    actionText: String,
    showAction: Boolean,
    actionEnabled: Boolean = true,
    isWallpaperMode: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardId: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // FocusUI卡片壁纸状态
    val cardBgUri = if (cardId != null && BackgroundConfig.isFocusCardBackgroundEnabled) {
        BackgroundConfig.getFocusCardBgUri(cardId)
    } else null
    val hasCardWallpaper = !cardBgUri.isNullOrEmpty()
    // Match APatchTheme: manual light/dark mode must not be replaced by the
    // device configuration when the app is not following the system.
    val prefs = APApplication.sharedPreferences
    val isDarkTheme = if (prefs.getBoolean("night_mode_follow_sys", false)) {
        isSystemInDarkTheme()
    } else {
        prefs.getBoolean("night_mode_enabled", true)
    }
    val cardBgDim = BackgroundConfig.getEffectiveFocusCardBgDim(isDarkTheme)
    val cardBgOpacity = BackgroundConfig.getEffectiveFocusCardBgOpacity(isDarkTheme)

    // 长按对话框状态
    var showBgOptionsDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (cardId != null) {
                scope.launch {
                    val success = BackgroundManager.saveAndApplyFocusCardBackground(context, cardId, it)
                    if (success) {
                        showToast(context, R.string.focus_card_background_saved)
                    } else {
                        showToast(context, R.string.focus_card_background_error)
                    }
                }
            }
        }
    }

    // 清除确认对话框
    val clearBgConfirmDialog = rememberConfirmDialog(
        onConfirm = {
            if (cardId != null) {
                scope.launch {
                    BackgroundManager.clearFocusCardBackground(context, cardId)
                    showToast(context, context.getString(R.string.focus_card_background_cleared))
                }
            }
        }
    )

    // 卡片壁纸模式下的内容颜色
    val contentColor = if (hasCardWallpaper) FolkColors.MediaContent else MaterialTheme.colorScheme.onSurface
    val subContentColor = if (hasCardWallpaper) FolkColors.MediaContent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (cardId != null && BackgroundConfig.isFocusCardBackgroundEnabled) {
                    Modifier.pointerInput(cardId) {
                        detectTapGestures(
                            onLongPress = { showBgOptionsDialog = true }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = FolkShape.Corner20,
        colors = CardDefaults.cardColors(
            containerColor = if (hasCardWallpaper) FolkColors.Transparent
                else LocalFolkWallpaperTokens.current?.group?.fill
                    ?: FolkTheme.palette.groupedSurface,
            contentColor = contentColor
        )
    ) {
        // 外层Box包裹内容，壁纸层通过 matchParentSize 精确填充内容尺寸
        // 注意：不能使用 fillMaxSize，因为卡片是 wrap-content 的，fillMaxSize 会导致尺寸堙缩为0
        Box {
            // 卡片壁纸层（作为背景，匹配内容尺寸）
            if (hasCardWallpaper) {
                // 配置支持GIF/动图的ImageLoader
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()

                // 壁纸图片：matchParentSize 使其精确填充卡片内容区域
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(cardBgUri)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                            .alpha(cardBgOpacity)
                )
                // 暗度层，保证文字在壁纸上的可读性
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(FolkColors.MediaScrim.copy(alpha = cardBgDim))
                )
            }

            // 卡片内容层（决定卡片的实际尺寸）
            Column(
                modifier = Modifier.padding(FolkDimens.Size16)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(FolkDimens.Size32),
                        tint = if (hasCardWallpaper) FolkColors.MediaContent else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(FolkDimens.Size16))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (showAction) {
                        Button(
                            onClick = onActionClick,
                            enabled = actionEnabled,
                            // Material 3 Expressive's medium size: a taller, roomier
                            // primary action than the compact default.
                            contentPadding = ButtonDefaults.MediumContentPadding,
                            colors = ButtonDefaults.buttonColors()
                        ) {
                            Text(text = actionText)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FolkDimens.Size16),
                    color = if (hasCardWallpaper) FolkColors.MediaContent.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Content Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
                ) {
                    CompositionLocalProvider(
                        LocalFocusCardWallpaper provides hasCardWallpaper,
                        LocalFocusCardSubColor provides subContentColor
                    ) {
                        content()
                    }
                }
            }
        }
    }

    // 长按背景选项对话框
    if (cardId != null && BackgroundConfig.isFocusCardBackgroundEnabled) {
        BackgroundOptionsDialog(
            showDialog = showBgOptionsDialog,
            onDismiss = { showBgOptionsDialog = false },
            title = stringResource(R.string.focus_card_background_title),
            selectLabel = stringResource(R.string.settings_select_background_image),
            clearLabel = stringResource(R.string.focus_card_background_clear),
            hasExisting = hasCardWallpaper,
            onSelectImage = {
                if (PermissionUtils.hasExternalStoragePermission(context)) {
                    try {
                        pickImageLauncher.launch("image/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                } else {
                    showToast(context, context.getString(R.string.focus_card_permission_required))
                }
            },
            onClearImage = {
                clearBgConfirmDialog.showConfirm(
                    title = context.getString(R.string.focus_card_background_clear),
                    content = context.getString(R.string.focus_card_background_clear_confirm),
                    markdown = false,
                )
            },
        )
    }
}
