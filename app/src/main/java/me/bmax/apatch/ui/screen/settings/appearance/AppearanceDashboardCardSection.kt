package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.DualBackgroundSettings
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceDashboardCardSection(
    flat: Boolean,
    highlightKey: String?,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showOptions by remember { mutableStateOf(false) }
    val clearBackgroundDialog = rememberConfirmDialog(
        onConfirm = {
            BackgroundManager.clearDashboardCardBackground(context)
            showToast(context, context.getString(R.string.dashboard_card_background_cleared))
        }
    )

    val pickDashboardCardImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyDashboardCardBackground(context, it)
                loadingDialog.hide()
                snackBarHost.showSnackbar(
                    if (success) context.getString(R.string.dashboard_card_background_saved)
                    else context.getString(R.string.dashboard_card_background_error)
                )
            }
        }
    }

    SegmentedColumn(
        title = stringResource(R.string.dashboard_card_background_title),
        flat = flat,
        highlightKey = highlightKey,
    ) {
        item(key = "appearance_dashboard_card_background_enabled") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Wallpaper,
                title = stringResource(R.string.settings_dashboard_card_background),
                summary = if (BackgroundConfig.isDashboardCardBackgroundEnabled) {
                    stringResource(R.string.settings_dashboard_card_background_enabled)
                } else {
                    stringResource(R.string.settings_dashboard_card_background_summary)
                },
                checked = BackgroundConfig.isDashboardCardBackgroundEnabled,
                onCheckedChange = {
                    BackgroundConfig.setDashboardCardBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        if (BackgroundConfig.isDashboardCardBackgroundEnabled) {
            item(key = "appearance_dashboard_card_dual_background") {
                DualBackgroundSettings(
                    flat = flat,
                    dualDimEnabled = BackgroundConfig.isDashboardCardDualDimEnabled,
                    onDualDimEnabledChange = { BackgroundConfig.setDashboardCardDualDimEnabledState(it) },
                    dim = BackgroundConfig.dashboardCardBgDim,
                    onDimChange = { BackgroundConfig.setDashboardCardBgDimValue(it) },
                    dayDim = BackgroundConfig.dashboardCardBgDayDim,
                    onDayDimChange = { BackgroundConfig.setDashboardCardBgDayDimValue(it) },
                    nightDim = BackgroundConfig.dashboardCardBgNightDim,
                    onNightDimChange = { BackgroundConfig.setDashboardCardBgNightDimValue(it) },
                    dualOpacityEnabled = BackgroundConfig.isDashboardCardDualOpacityEnabled,
                    onDualOpacityEnabledChange = { BackgroundConfig.setDashboardCardDualOpacityEnabledState(it) },
                    opacity = BackgroundConfig.dashboardCardBgOpacity,
                    onOpacityChange = { BackgroundConfig.setDashboardCardBgOpacityValue(it) },
                    dayOpacity = BackgroundConfig.dashboardCardBgDayOpacity,
                    onDayOpacityChange = { BackgroundConfig.setDashboardCardBgDayOpacityValue(it) },
                    nightOpacity = BackgroundConfig.dashboardCardBgNightOpacity,
                    onNightOpacityChange = { BackgroundConfig.setDashboardCardBgNightOpacityValue(it) },
                    save = { BackgroundConfig.save(context) },
                    keyPrefix = "dashboard_card",
                    dualDimTitle = stringResource(R.string.settings_dashboard_card_dual_dim),
                    dualDimDescription = stringResource(R.string.settings_dashboard_card_dual_dim_desc),
                    opacityTitle = stringResource(R.string.settings_dashboard_card_opacity),
                    dayDimTitle = stringResource(R.string.settings_dashboard_card_day_dim),
                    nightDimTitle = stringResource(R.string.settings_dashboard_card_night_dim),
                    dualOpacityTitle = stringResource(R.string.settings_dashboard_card_dual_opacity),
                    dualOpacityDescription = stringResource(R.string.settings_dashboard_card_dual_opacity_desc),
                    dayOpacityTitle = stringResource(R.string.settings_dashboard_card_day_opacity),
                    nightOpacityTitle = stringResource(R.string.settings_dashboard_card_night_opacity),
                )
            }

            item(key = "appearance_dashboard_card_select") {
                TonalCard(
                    flat = flat,
                    onClick = { showOptions = true },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(FolkDimens.Size16),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(FolkDimens.Size16))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_select_background_image),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (BackgroundConfig.dashboardCardBgUri != null) {
                                Text(
                                    text = stringResource(R.string.settings_background_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOptions) {
        BackgroundOptionsDialog(
            showDialog = showOptions,
            onDismiss = { showOptions = false },
            title = stringResource(R.string.dashboard_card_background_title),
            selectLabel = stringResource(R.string.settings_select_background_image),
            clearLabel = stringResource(R.string.dashboard_card_background_clear),
            hasExisting = BackgroundConfig.dashboardCardBgUri != null,
            onSelectImage = {
                if (PermissionUtils.hasExternalStoragePermission(context)) {
                    try {
                        pickDashboardCardImageLauncher.launch("image/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                } else {
                    showToast(context, context.getString(R.string.focus_card_permission_required))
                }
            },
            onClearImage = {
                clearBackgroundDialog.showConfirm(
                    title = context.getString(R.string.dashboard_card_background_clear),
                    content = context.getString(R.string.dashboard_card_background_clear_confirm),
                    markdown = false,
                )
            },
        )
    }
}
