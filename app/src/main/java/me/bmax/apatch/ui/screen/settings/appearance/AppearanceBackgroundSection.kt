package me.bmax.apatch.ui.screen.settings.appearance

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberSystemCropLauncher
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceBackgroundSection(
    flat: Boolean,
    highlightKey: String?,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
    showGridCardSettings: Boolean,
    currentBadgeTextMode: String,
    showCustomBadgeTextDialog: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickingType by remember { mutableStateOf<String?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var showCropOptionDialog by remember { mutableStateOf(false) }

    // 裁剪 launcher：将选取的图片交给系统裁剪界面，返回裁剪后的 URI
    // （共用 ui/component/ImageCrop.kt 的实现）
    val cropImageLauncher = rememberSystemCropLauncher(
        cacheName = "background_crop_cache",
    ) { uri: Uri ->
        scope.launch {
            loadingDialog.show()
            val success = when (pickingType) {
                "home" -> BackgroundManager.saveAndApplyHomeBackground(context, uri)
                "kernel" -> BackgroundManager.saveAndApplyKernelBackground(context, uri)
                "superuser" -> BackgroundManager.saveAndApplySuperuserBackground(context, uri)
                "system" -> BackgroundManager.saveAndApplySystemModuleBackground(context, uri)
                "settings" -> BackgroundManager.saveAndApplySettingsBackground(context, uri)
                else -> BackgroundManager.saveAndApplyCustomBackground(context, uri)
            }
            loadingDialog.hide()
            if (success) {
                snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_saved))
                refreshTheme.value = true
            } else {
                snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_error))
            }
            pickingType = null
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 所有壁纸模式都弹窗让用户选裁剪或直接使用
            pendingCropUri = it
            showCropOptionDialog = true
        }
    }

    // 裁剪选项对话框
    if (showCropOptionDialog && pendingCropUri != null) {
        AlertDialog(
            onDismissRequest = {
                showCropOptionDialog = false
                pendingCropUri = null
            },
            title = { Text(text = stringResource(R.string.settings_crop_dialog_title)) },
            text = { Text(text = stringResource(R.string.settings_crop_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCropOptionDialog = false
                    val uri = pendingCropUri!!
                    pendingCropUri = null
                    try {
                        cropImageLauncher.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, context.getString(R.string.settings_crop_not_supported))
                        scope.launch {
                            loadingDialog.show()
                            val success = when (pickingType) {
                                "home" -> BackgroundManager.saveAndApplyHomeBackground(context, uri)
                                "kernel" -> BackgroundManager.saveAndApplyKernelBackground(context, uri)
                                "superuser" -> BackgroundManager.saveAndApplySuperuserBackground(context, uri)
                                "system" -> BackgroundManager.saveAndApplySystemModuleBackground(context, uri)
                                "settings" -> BackgroundManager.saveAndApplySettingsBackground(context, uri)
                                else -> BackgroundManager.saveAndApplyCustomBackground(context, uri)
                            }
                            loadingDialog.hide()
                            if (success) {
                                refreshTheme.value = true
                            }
                            pickingType = null
                        }
                    } catch (e: Exception) {
                        // 源图片 URI 已失效（如文件被删除/回收）时读取会抛 FileNotFoundException
                        showToast(context, context.getString(R.string.settings_custom_background_error))
                        pickingType = null
                    }
                }) {
                    Text(text = stringResource(R.string.settings_crop_dialog_crop))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCropOptionDialog = false
                    val uri = pendingCropUri!!
                    pendingCropUri = null
                    scope.launch {
                        loadingDialog.show()
                        val success = when (pickingType) {
                            "home" -> BackgroundManager.saveAndApplyHomeBackground(context, uri)
                            "kernel" -> BackgroundManager.saveAndApplyKernelBackground(context, uri)
                            "superuser" -> BackgroundManager.saveAndApplySuperuserBackground(context, uri)
                            "system" -> BackgroundManager.saveAndApplySystemModuleBackground(context, uri)
                            "settings" -> BackgroundManager.saveAndApplySettingsBackground(context, uri)
                            else -> BackgroundManager.saveAndApplyCustomBackground(context, uri)
                        }
                        loadingDialog.hide()
                        if (success) {
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_saved))
                            refreshTheme.value = true
                        } else {
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_error))
                        }
                        pickingType = null
                    }
                }) {
                    Text(text = stringResource(R.string.settings_crop_dialog_direct))
                }
            }
        )
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyVideoBackground(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_video_selected))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_error))
                }
            }
        }
    }

    val pickGridImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyGridWorkingCardBackground(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_grid_working_card_background_saved))
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_grid_working_card_background_error))
                }
            }
        }
    }

    SegmentedColumn(title = stringResource(R.string.settings_appearance_background), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_custom_background") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Wallpaper,
                title = stringResource(id = R.string.settings_custom_background),
                summary = if (BackgroundConfig.isCustomBackgroundEnabled) stringResource(id = R.string.settings_custom_background_enabled) else stringResource(id = R.string.settings_custom_background_summary),
                checked = BackgroundConfig.isCustomBackgroundEnabled,
                onCheckedChange = {
                    BackgroundConfig.setCustomBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                    refreshTheme.value = true
                },
            )
        }

        if (BackgroundConfig.isCustomBackgroundEnabled) {
            if (!BackgroundConfig.isVideoBackgroundEnabled) {
                item(key = "appearance_bg_dual_dim") {
                    SegmentedSwitchItem(
                        icon = Icons.Outlined.Contrast,
                        title = stringResource(id = R.string.settings_custom_background_dual_dim),
                        summary = stringResource(id = R.string.settings_custom_background_dual_dim_desc),
                        checked = BackgroundConfig.isDualBackgroundDimEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setDualBackgroundDimEnabledState(it)
                            BackgroundConfig.save(context)
                            refreshTheme.value = true
                        },
                    )
                }

                item(key = "appearance_bg_opacity") {
                    SegmentedSliderItem(
                        title = stringResource(id = R.string.settings_custom_background_opacity),
                        value = BackgroundConfig.customBackgroundOpacity,
                        onValueChange = { BackgroundConfig.setCustomBackgroundOpacityValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_bg_blur") {
                    SegmentedSliderItem(
                        title = stringResource(id = R.string.settings_custom_background_blur),
                        value = BackgroundConfig.customBackgroundBlur,
                        valueRange = 0f..50f,
                        valueFormat = { "${it.toInt()}" },
                        onValueChange = { BackgroundConfig.setCustomBackgroundBlurValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                if (!BackgroundConfig.isDualBackgroundDimEnabled) {
                    item(key = "appearance_bg_dim") {
                        SegmentedSliderItem(
                            title = stringResource(id = R.string.settings_custom_background_dim),
                            value = BackgroundConfig.customBackgroundDim,
                            onValueChange = { BackgroundConfig.setCustomBackgroundDimValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }
                } else {
                    item(key = "appearance_bg_day_dim") {
                        SegmentedSliderItem(
                            title = stringResource(id = R.string.settings_custom_background_day_dim),
                            value = BackgroundConfig.customBackgroundDayDim,
                            onValueChange = { BackgroundConfig.setCustomBackgroundDayDimValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }

                    item(key = "appearance_bg_night_dim") {
                        SegmentedSliderItem(
                            title = stringResource(id = R.string.settings_custom_background_night_dim),
                            value = BackgroundConfig.customBackgroundNightDim,
                            onValueChange = { BackgroundConfig.setCustomBackgroundNightDimValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }
                }
            }

            item(key = "appearance_video_background") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.VideoFile,
                    title = stringResource(id = R.string.settings_video_background),
                    summary = stringResource(id = R.string.settings_video_background_summary),
                    checked = BackgroundConfig.isVideoBackgroundEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setVideoBackgroundEnabledState(it)
                        BackgroundConfig.save(context)
                        refreshTheme.value = true
                    },
                )
            }

            if (BackgroundConfig.isVideoBackgroundEnabled) {
                item(key = "appearance_select_video") {
                    SegmentedValueItem(
            icon = Icons.Outlined.VideoFile,
            title = stringResource(id = R.string.settings_select_video),
            summary = if (!BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) stringResource(id = R.string.settings_video_selected) else null,
            onClick = {                                try {
                                pickVideoLauncher.launch("video/*")
                            } catch (e: ActivityNotFoundException) {
                                showToast(context, e.message ?: "")
                            }
                        },
        )
                }

                if (!BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
                    item(key = "appearance_clear_video") {
                        val clearVideoDialog = rememberConfirmDialog(
                            onConfirm = {
                                scope.launch {
                                    loadingDialog.show()
                                    BackgroundManager.clearVideoBackground(context)
                                    loadingDialog.hide()
                                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_background_image_cleared))
                                    refreshTheme.value = true
                                }
                            }
                        )
                        val clearVideoTitle = stringResource(id = R.string.settings_clear_video_background)
                        val clearVideoConfirm = context.getString(R.string.settings_clear_video_background_confirm)
                        SegmentedValueItem(
            icon = Icons.Outlined.Delete,
            title = clearVideoTitle,
            onClick = {                                    clearVideoDialog.showConfirm(
                                    title = clearVideoTitle,
                                    content = clearVideoConfirm,
                                    markdown = false,
                                )
                            },
        )
                    }
                }

                item(key = "appearance_video_volume") {
                    SegmentedSliderItem(
                        title = stringResource(id = R.string.settings_video_volume),
                        value = BackgroundConfig.videoVolume,
                        onValueChange = { BackgroundConfig.setVideoVolumeValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }
            } else {
                item(key = "appearance_multi_background") {
                    SegmentedSwitchItem(
                        icon = Icons.Outlined.GridView,
                        title = stringResource(id = R.string.settings_multi_background_mode),
                        summary = stringResource(id = R.string.settings_multi_background_mode_summary),
                        checked = BackgroundConfig.isMultiBackgroundEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setMultiBackgroundEnabledState(it)
                            BackgroundConfig.save(context)
                            refreshTheme.value = true
                        },
                    )
                }

                if (BackgroundConfig.isMultiBackgroundEnabled) {
                    item(key = "appearance_multi_background_select") {
                        val multiItems = listOf(
                            Triple(R.string.settings_select_home_background, "home", BackgroundConfig.homeBackgroundUri),
                            Triple(R.string.settings_select_kernel_background, "kernel", BackgroundConfig.kernelBackgroundUri),
                            Triple(R.string.settings_select_superuser_background, "superuser", BackgroundConfig.superuserBackgroundUri),
                            Triple(R.string.settings_select_system_module_background, "system", BackgroundConfig.systemModuleBackgroundUri),
                            Triple(R.string.settings_select_settings_background, "settings", BackgroundConfig.settingsBackgroundUri)
                        )
                        Column {
                            multiItems.forEach { (titleRes, type, uri) ->
                                SegmentedValueItem(
            icon = Icons.Outlined.Image,
            title = stringResource(id = titleRes),
            summary = if (!uri.isNullOrEmpty()) stringResource(id = R.string.settings_background_selected) else null,
            onClick = {                                            if (PermissionUtils.hasExternalStoragePermission(context) &&
                                            PermissionUtils.hasWriteExternalStoragePermission(context)) {
                                            pickingType = type
                                            try {
                                                pickImageLauncher.launch("image/*")
                                            } catch (e: ActivityNotFoundException) {
                                                showToast(context, e.message ?: "")
                                            }
                                        } else {
                                            showToast(context, context.getString(R.string.settings_background_permission_required))
                                        }
                                    },
        )
                            }
                        }
                    }
                } else {
                    item(key = "appearance_select_background") {
                        SegmentedValueItem(
            icon = Icons.Outlined.Image,
            title = stringResource(id = R.string.settings_select_background_image),
            summary = if (!BackgroundConfig.customBackgroundUri.isNullOrEmpty()) stringResource(id = R.string.settings_background_selected) else null,
            onClick = {                                    if (PermissionUtils.hasExternalStoragePermission(context) &&
                                    PermissionUtils.hasWriteExternalStoragePermission(context)) {
                                    pickingType = "default"
                                    try {
                                        pickImageLauncher.launch("image/*")
                                    } catch (e: ActivityNotFoundException) {
                                        showToast(context, e.message ?: "")
                                    }
                                } else {
                                    showToast(context, context.getString(R.string.settings_background_permission_required))
                                }
                            },
        )
                    }

                    if (!BackgroundConfig.customBackgroundUri.isNullOrEmpty()) {
                        item(key = "appearance_clear_background") {
                            val clearBackgroundDialog = rememberConfirmDialog(
                                onConfirm = {
                                    scope.launch {
                                        loadingDialog.show()
                                        BackgroundManager.clearCustomBackground(context)
                                        loadingDialog.hide()
                                        snackBarHost.showSnackbar(message = context.getString(R.string.settings_background_image_cleared))
                                        refreshTheme.value = true
                                    }
                                }
                            )
                            val clearBgTitle = stringResource(id = R.string.settings_clear_background)
                            val clearBgConfirm = context.getString(R.string.settings_clear_background_confirm)
                            SegmentedValueItem(
            icon = Icons.Outlined.Delete,
            title = clearBgTitle,
            onClick = {                                        clearBackgroundDialog.showConfirm(
                                        title = clearBgTitle,
                                        content = clearBgConfirm,
                                        markdown = false,
                                    )
                                },
        )
                        }
                    }
                }
            }
        }

        appearanceBackgroundGridItems(
            showGridCardSettings = showGridCardSettings,
            flat = flat,
            context = context,
            scope = scope,
            snackBarHost = snackBarHost,
            loadingDialog = loadingDialog,
            currentBadgeTextMode = currentBadgeTextMode,
            showCustomBadgeTextDialog = showCustomBadgeTextDialog,
            onSelectGridImage = { pickGridImageLauncher.launch("image/*") },
        )

    }
}
