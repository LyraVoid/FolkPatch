package me.bmax.apatch.ui.screen.settings.appearance

import androidx.compose.material.icons.automirrored.outlined.LabelOff
import androidx.compose.material.icons.automirrored.outlined.ViewList
import android.content.ActivityNotFoundException
import android.net.Uri
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.ui.screen.settings.appearance.homeLayoutStyleToString
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.LayoutVariant
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceLayoutSection(
    flat: Boolean,
    highlightKey: String?,
    kPatchReady: Boolean,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
    currentStyle: String?,
    isStatsLayout: Boolean,
    statsTopLayoutValue: String,
    onShowStatsTopLayoutDialog: () -> Unit,
    navSchemeLabel: String,
    onShowNavSchemeDialog: () -> Unit,
    isFloatingNav: Boolean,
    floatingAutoHide: Boolean,
    onFloatingAutoHideChange: (Boolean) -> Unit,
    floatingSwipeHide: Boolean,
    onFloatingSwipeHideChange: (Boolean) -> Unit,
    showNavApm: Boolean,
    onShowNavApmChange: (Boolean) -> Unit,
    showNavKpm: Boolean,
    onShowNavKpmChange: (Boolean) -> Unit,
    showNavSuperUser: Boolean,
    onShowNavSuperUserChange: (Boolean) -> Unit,
    isListStyle: Boolean,
    isDefaultStyle: Boolean,
    currentBadgeTextMode: String,
    showCustomBadgeTextDialog: MutableState<Boolean>,
    showHomeLayoutChooseDialog: MutableState<Boolean>,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var layoutVariant by remember {
        mutableStateOf(LayoutVariant.fromValue(prefs.getString("layout_variant", LayoutVariant.M3E)))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            if (key == "layout_variant") {
                layoutVariant = LayoutVariant.fromValue(
                    preferences.getString("layout_variant", LayoutVariant.M3E)
                )
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

        val pickTitleImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyTitleImage(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_error))
                }
            }
        }
    }

    SegmentedColumn(title = stringResource(R.string.settings_appearance_layout), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_layout_variant") {
            var showDialog by remember { mutableStateOf(false) }
            SegmentedValueItem(
                icon = Icons.Outlined.ViewAgenda,
                title = stringResource(R.string.settings_layout_variant),
                summary = stringResource(
                    if (layoutVariant == LayoutVariant.TRADITIONAL) {
                        R.string.settings_layout_variant_traditional
                    } else {
                        R.string.settings_layout_variant_m3e
                    }
                ),
                onClick = { showDialog = true },
            )
            if (showDialog) {
                ExpressiveDialog(onDismissRequest = { showDialog = false }) {
                    Column(Modifier.padding(FolkDimens.Size24)) {
                        Text(
                            text = stringResource(R.string.settings_layout_variant),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = FolkDimens.Size16),
                        )
                        LayoutVariant.supported.forEach { variant ->
                            SegmentedSelectableItem(
                                title = stringResource(
                                    if (variant == LayoutVariant.TRADITIONAL) {
                                        R.string.settings_layout_variant_traditional
                                    } else {
                                        R.string.settings_layout_variant_m3e
                                    }
                                ),
                                selected = layoutVariant == variant,
                                onClick = {
                                    prefs.edit().putString("layout_variant", variant).apply()
                                    showDialog = false
                                },
                            )
                        }
                    }
                }
            }
        }

        item(key = "appearance_home_layout") {
            SegmentedValueItem(
            icon = Icons.Outlined.Dashboard,
            title = stringResource(id = R.string.settings_home_layout_style),
            summary = stringResource(homeLayoutStyleToString(currentStyle.toString())),
            onClick = { showHomeLayoutChooseDialog.value = true },
        )
        }

        item(key = "appearance_stats_top_layout", visible = isStatsLayout) {
            SegmentedValueItem(
            icon = Icons.Outlined.GridView,
            title = stringResource(id = R.string.settings_stats_top_layout),
            summary = statsTopLayoutValue,
            onClick = { onShowStatsTopLayoutDialog() },
        )
        }

        appearanceNavItems(
            kPatchReady = kPatchReady,
            isFloatingNav = isFloatingNav,
            flat = flat,
            context = context,
            prefs = prefs,
            showNavApm = showNavApm,
            onShowNavApmChange = onShowNavApmChange,
            showNavKpm = showNavKpm,
            onShowNavKpmChange = onShowNavKpmChange,
            showNavSuperUser = showNavSuperUser,
            onShowNavSuperUserChange = onShowNavSuperUserChange,
            navSchemeLabel = navSchemeLabel,
            onShowNavSchemeDialog = onShowNavSchemeDialog,
            floatingAutoHide = floatingAutoHide,
            onFloatingAutoHideChange = onFloatingAutoHideChange,
            floatingSwipeHide = floatingSwipeHide,
            onFloatingSwipeHideChange = onFloatingSwipeHideChange,
        )

        appearanceNavCustomIconsItem(
            flat = flat,
            context = context,
            prefs = prefs,
            scope = scope,
            snackBarHost = snackBarHost,
        )

        item(key = "appearance_list_card_badge", visible = isListStyle) {
            SegmentedSwitchItem(
                icon = Icons.AutoMirrored.Outlined.LabelOff,
                title = stringResource(id = R.string.settings_list_card_hide_status_badge),
                summary = stringResource(id = R.string.settings_list_card_hide_status_badge_summary),
                checked = BackgroundConfig.isListWorkingCardModeHidden,
                onCheckedChange = {
                    BackgroundConfig.setListWorkingCardModeHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        item(key = "appearance_custom_badge_text_list", visible = isListStyle && !BackgroundConfig.isListWorkingCardModeHidden) {
            SegmentedValueItem(
            icon = Icons.Outlined.Badge,
            title = stringResource(id = R.string.settings_custom_badge_text),
            summary = currentBadgeTextMode,
            onClick = { showCustomBadgeTextDialog.value = true },
        )
        }

        item(key = "appearance_list_info_icons", visible = isDefaultStyle) {
            var showListInfoIcons by remember { mutableStateOf(prefs.getBoolean("list_info_show_icons", false)) }
            SegmentedSwitchItem(
                icon = Icons.AutoMirrored.Outlined.ViewList,
                title = stringResource(id = R.string.settings_list_info_show_icons),
                summary = stringResource(id = R.string.settings_list_info_show_icons_summary),
                checked = showListInfoIcons,
                onCheckedChange = {
                    showListInfoIcons = it
                    prefs.edit().putBoolean("list_info_show_icons", it).apply()
                    refreshTheme.value = true
                },
            )
        }

        item(key = "appearance_advanced_title") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Title,
                title = stringResource(id = R.string.settings_advanced_title_style),
                summary = if (BackgroundConfig.isAdvancedTitleStyleEnabled) stringResource(id = R.string.settings_advanced_title_style_enabled) else stringResource(id = R.string.settings_advanced_title_style_summary),
                checked = BackgroundConfig.isAdvancedTitleStyleEnabled,
                onCheckedChange = {
                    BackgroundConfig.setAdvancedTitleStyleEnabledState(it)
                    BackgroundConfig.save(context)
                    refreshTheme.value = true
                },
            )
        }

        if (BackgroundConfig.isAdvancedTitleStyleEnabled) {
            item(key = "appearance_title_day_opacity") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_title_image_day_opacity),
                    value = BackgroundConfig.titleImageDayOpacity,
                    onValueChange = { BackgroundConfig.setTitleImageDayOpacityValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_title_night_opacity") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_title_image_night_opacity),
                    value = BackgroundConfig.titleImageNightOpacity,
                    onValueChange = { BackgroundConfig.setTitleImageNightOpacityValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_title_image_dim") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_title_image_dim),
                    value = BackgroundConfig.titleImageDim,
                    onValueChange = { BackgroundConfig.setTitleImageDimValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_title_image_offset_x") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_title_image_offset_x),
                    value = BackgroundConfig.titleImageOffsetX,
                    valueRange = -1f..1f,
                    onValueChange = { BackgroundConfig.setTitleImageOffsetXValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_select_title_image") {
                SegmentedValueItem(
            icon = Icons.Outlined.Image,
            title = stringResource(id = R.string.settings_select_title_image),
            summary = if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) stringResource(id = R.string.settings_title_image_selected) else null,
            onClick = {                            if (PermissionUtils.hasExternalStoragePermission(context)) {
                            try {
                                pickTitleImageLauncher.launch("image/*")
                            } catch (e: ActivityNotFoundException) {
                                showToast(context, e.message ?: "")
                            }
                        } else {
                            showToast(context, context.getString(R.string.settings_title_image_permission_required))
                        }
                    },
        )
            }

            if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) {
                item(key = "appearance_clear_title_image") {
                    val clearTitleImageDialog = rememberConfirmDialog(
                        onConfirm = {
                            scope.launch {
                                loadingDialog.show()
                                BackgroundManager.clearTitleImage(context)
                                loadingDialog.hide()
                                snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_cleared))
                                refreshTheme.value = true
                            }
                        }
                    )
                    SegmentedValueItem(
            icon = Icons.Outlined.Delete,
            title = stringResource(id = R.string.settings_clear_title_image),
            onClick = {                                clearTitleImageDialog.showConfirm(
                                title = context.getString(R.string.settings_clear_title_image),
                                content = context.getString(R.string.settings_clear_title_image_confirm),
                                markdown = false,
                            )
                        },
        )
                }
            }
        }
    }
}
