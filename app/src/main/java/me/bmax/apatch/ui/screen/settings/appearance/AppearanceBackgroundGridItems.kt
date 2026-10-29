package me.bmax.apatch.ui.screen.settings.appearance

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.DualBackgroundSettings
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.SegmentedColumnScope
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.showToast

/**
 * Grid working-card background preferences. They are contributed to the
 * background section's existing group, so the shelf layout is unchanged.
 */
fun SegmentedColumnScope.appearanceBackgroundGridItems(
    showGridCardSettings: Boolean,
    flat: Boolean,
    context: Context,
    scope: CoroutineScope,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
    currentBadgeTextMode: String,
    showCustomBadgeTextDialog: MutableState<Boolean>,
    onSelectGridImage: () -> Unit,
) {
    if (!showGridCardSettings) return

        item(key = "appearance_grid_card_bg") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.GridView,
                title = stringResource(id = R.string.settings_grid_working_card_background),
                summary = if (BackgroundConfig.isGridWorkingCardBackgroundEnabled) stringResource(id = R.string.settings_grid_working_card_background_enabled) else stringResource(id = R.string.settings_grid_working_card_background_summary),
                checked = BackgroundConfig.isGridWorkingCardBackgroundEnabled,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        if (BackgroundConfig.isGridWorkingCardBackgroundEnabled) {
            item(key = "appearance_grid_dim") {
                DualBackgroundSettings(
                    flat = flat,
                    dualDimEnabled = false,
                    onDualDimEnabledChange = {},
                    dim = BackgroundConfig.gridWorkingCardBackgroundDim,
                    onDimChange = { BackgroundConfig.setGridWorkingCardBackgroundDimValue(it) },
                    dayDim = 0f,
                    onDayDimChange = {},
                    nightDim = 0f,
                    onNightDimChange = {},
                    dualOpacityEnabled = BackgroundConfig.isGridDualOpacityEnabled,
                    onDualOpacityEnabledChange = { BackgroundConfig.setGridDualOpacityEnabledState(it) },
                    opacity = BackgroundConfig.gridWorkingCardBackgroundOpacity,
                    onOpacityChange = { BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(it) },
                    dayOpacity = BackgroundConfig.gridWorkingCardBackgroundDayOpacity,
                    onDayOpacityChange = { BackgroundConfig.setGridWorkingCardBackgroundDayOpacityValue(it) },
                    nightOpacity = BackgroundConfig.gridWorkingCardBackgroundNightOpacity,
                    onNightOpacityChange = { BackgroundConfig.setGridWorkingCardBackgroundNightOpacityValue(it) },
                    save = { BackgroundConfig.save(context) },
                    keyPrefix = "grid_card",
                    showDualDim = false,
                )
            }

            item(key = "appearance_grid_select_image") {
                SegmentedValueItem(
        icon = Icons.Outlined.Image,
        title = stringResource(id = R.string.settings_select_background_image),
        summary = if (!BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty()) stringResource(id = R.string.settings_grid_working_card_background_selected) else null,
        onClick = {                                if (PermissionUtils.hasExternalStoragePermission(context)) {
                            try {
                                onSelectGridImage()
                            } catch (e: ActivityNotFoundException) {
                                showToast(context, e.message ?: "")
                            }
                        } else {
                            showToast(context, context.getString(R.string.settings_background_permission_required))
                        }
                    },
    )
            }

            item(key = "appearance_grid_clear_image") {
                val clearGridBackgroundDialog = rememberConfirmDialog(
                    onConfirm = {
                        scope.launch {
                            loadingDialog.show()
                            BackgroundManager.clearGridWorkingCardBackground(context)
                            loadingDialog.hide()
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_grid_working_card_background_cleared))
                        }
                    }
                )
                SegmentedValueItem(
        icon = Icons.Outlined.Delete,
        title = stringResource(id = R.string.settings_clear_grid_working_card_background),
        onClick = {                                clearGridBackgroundDialog.showConfirm(
                            title = context.getString(R.string.settings_clear_grid_working_card_background),
                            content = context.getString(R.string.settings_clear_grid_working_card_background_confirm),
                            markdown = false,
                        )
                    },
    )
            }
        }

        item(key = "appearance_grid_card_check") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.CheckCircle,
                title = stringResource(id = R.string.settings_grid_working_card_hide_check),
                summary = stringResource(id = R.string.settings_grid_working_card_hide_check_summary),
                checked = BackgroundConfig.isGridWorkingCardCheckHidden,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardCheckHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        item(key = "appearance_grid_card_text") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.TextFields,
                title = stringResource(id = R.string.settings_grid_working_card_hide_text),
                summary = stringResource(id = R.string.settings_grid_working_card_hide_text_summary),
                checked = BackgroundConfig.isGridWorkingCardTextHidden,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardTextHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        item(key = "appearance_grid_card_mode") {
            SegmentedSwitchItem(
                icon = Icons.AutoMirrored.Outlined.Label,
                title = stringResource(id = R.string.settings_grid_working_card_hide_mode),
                summary = stringResource(id = R.string.settings_grid_working_card_hide_mode_summary),
                checked = BackgroundConfig.isGridWorkingCardModeHidden,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardModeHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        item(key = "appearance_grid_badge_text", visible = !BackgroundConfig.isGridWorkingCardModeHidden) {
            SegmentedValueItem(
        icon = Icons.Outlined.Badge,
        title = stringResource(id = R.string.settings_custom_badge_text),
        summary = currentBadgeTextMode,
        onClick = { showCustomBadgeTextDialog.value = true },
    )
        }
}
