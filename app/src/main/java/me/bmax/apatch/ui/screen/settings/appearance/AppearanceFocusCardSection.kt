package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.DualBackgroundSettings
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceFocusCardSection(
    flat: Boolean,
    highlightKey: String?,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 当前正在选择壁纸的Focus卡片ID（用于区分选中的图片应保存到哪个卡片）
    var pickingFocusCardId by remember { mutableStateOf<String?>(null) }

    // FocusUI卡片壁纸选择器：根据 pickingFocusCardId 将选中的图片保存到对应卡片
    val pickFocusCardImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val cardId = pickingFocusCardId
        if (uri != null && cardId != null) {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyFocusCardBackground(context, cardId, uri)
                loadingDialog.hide()
                snackBarHost.showSnackbar(
                    message = if (success) context.getString(R.string.focus_card_background_saved)
                        else context.getString(R.string.focus_card_background_error)
                )
            }
        }
    }

    SegmentedColumn(
        title = stringResource(R.string.focus_card_background_title),
        flat = flat,
        highlightKey = highlightKey,
    ) {
        item(key = "appearance_focus_card_background_enabled") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Wallpaper,
                title = stringResource(R.string.settings_focus_card_background),
                summary = if (BackgroundConfig.isFocusCardBackgroundEnabled) {
                    stringResource(R.string.settings_focus_card_background_enabled)
                } else {
                    stringResource(R.string.settings_focus_card_background_summary)
                },
                checked = BackgroundConfig.isFocusCardBackgroundEnabled,
                onCheckedChange = {
                    BackgroundConfig.setFocusCardBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        if (BackgroundConfig.isFocusCardBackgroundEnabled) {
        item(key = "appearance_focus_card_dual_background") {
            DualBackgroundSettings(
                flat = flat,
                dualDimEnabled = BackgroundConfig.isFocusCardDualDimEnabled,
                onDualDimEnabledChange = { BackgroundConfig.setFocusCardDualDimEnabledState(it) },
                dim = BackgroundConfig.focusCardBgDim,
                onDimChange = { BackgroundConfig.setFocusCardBgDimValue(it) },
                dayDim = BackgroundConfig.focusCardBgDayDim,
                onDayDimChange = { BackgroundConfig.setFocusCardBgDayDimValue(it) },
                nightDim = BackgroundConfig.focusCardBgNightDim,
                onNightDimChange = { BackgroundConfig.setFocusCardBgNightDimValue(it) },
                dualOpacityEnabled = BackgroundConfig.isFocusCardDualOpacityEnabled,
                onDualOpacityEnabledChange = { BackgroundConfig.setFocusCardDualOpacityEnabledState(it) },
                opacity = BackgroundConfig.focusCardBgOpacity,
                onOpacityChange = { BackgroundConfig.setFocusCardBgOpacityValue(it) },
                dayOpacity = BackgroundConfig.focusCardBgDayOpacity,
                onDayOpacityChange = { BackgroundConfig.setFocusCardBgDayOpacityValue(it) },
                nightOpacity = BackgroundConfig.focusCardBgNightOpacity,
                onNightOpacityChange = { BackgroundConfig.setFocusCardBgNightOpacityValue(it) },
                save = { BackgroundConfig.save(context) },
                keyPrefix = "focus_card",
                dualDimTitle = stringResource(R.string.settings_focus_card_dual_dim),
                dualDimDescription = stringResource(R.string.settings_focus_card_dual_dim_desc),
                opacityTitle = stringResource(R.string.settings_focus_card_opacity),
                dayDimTitle = stringResource(R.string.settings_focus_card_day_dim),
                nightDimTitle = stringResource(R.string.settings_focus_card_night_dim),
                dualOpacityTitle = stringResource(R.string.settings_focus_card_dual_opacity),
                dualOpacityDescription = stringResource(R.string.settings_focus_card_dual_opacity_desc),
                dayOpacityTitle = stringResource(R.string.settings_focus_card_day_opacity),
                nightOpacityTitle = stringResource(R.string.settings_focus_card_night_opacity),
            )
        }

        // 4个卡片的配置清单：卡片ID -> 名称字符串资源
        val focusCards = listOf(
            BackgroundConfig.FOCUS_CARD_KERNEL to R.string.settings_focus_card_kernel,
            BackgroundConfig.FOCUS_CARD_APP to R.string.settings_focus_card_app,
            BackgroundConfig.FOCUS_CARD_DEVICE to R.string.settings_focus_card_device,
            BackgroundConfig.FOCUS_CARD_STORAGE to R.string.settings_focus_card_storage,
        )

        focusCards.forEach { (cardId, nameRes) ->
            // 当前卡片是否已设置壁纸
            val hasWallpaper = BackgroundConfig.getFocusCardBgUri(cardId) != null

            // 每个卡片一个设置项：点击卡片主体选择壁纸，右侧叉叉图标清除已有壁纸
            item(key = "appearance_focus_card_$cardId") {
                // 清除壁纸确认对话框（点击叉叉后弹出）
                val clearFocusBgDialog = rememberConfirmDialog(
                    onConfirm = {
                        scope.launch {
                            loadingDialog.show()
                            BackgroundManager.clearFocusCardBackground(context, cardId)
                            loadingDialog.hide()
                            snackBarHost.showSnackbar(message = context.getString(R.string.focus_card_background_cleared))
                        }
                    }
                )
                TonalCard(
                    flat = flat,
                    // 点击卡片主体：调起系统图片选择器，保存到对应卡片
                    onClick = {
                        if (PermissionUtils.hasExternalStoragePermission(context)) {
                            try {
                                pickingFocusCardId = cardId
                                pickFocusCardImageLauncher.launch("image/*")
                            } catch (e: ActivityNotFoundException) {
                                showToast(context, e.message ?: "")
                            }
                        } else {
                            showToast(context, context.getString(R.string.focus_card_permission_required))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(FolkDimens.Size16),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(FolkDimens.Size24))
                        Spacer(Modifier.width(FolkDimens.Size16))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = nameRes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (hasWallpaper) stringResource(id = R.string.settings_focus_card_wallpaper_selected) else stringResource(id = R.string.settings_select_background_image),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        // 右侧叉叉清除按钮：仅在已设置壁纸时显示，点击弹出确认对话框
                        // （嵌套clickable会消费点击事件，不会触发卡片的选图onClick）
                        if (hasWallpaper) {
                            Spacer(Modifier.width(FolkDimens.Size8))
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(id = R.string.focus_card_background_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(FolkDimens.Size24)
                                    .clickable {
                                        clearFocusBgDialog.showConfirm(
                                            title = context.getString(R.string.focus_card_background_clear),
                                            content = context.getString(R.string.focus_card_background_clear_confirm),
                                            markdown = false,
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
