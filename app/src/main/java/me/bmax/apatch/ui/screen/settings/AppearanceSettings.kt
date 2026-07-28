package me.bmax.apatch.ui.screen.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import me.bmax.apatch.ui.component.ColorGenerationModeSelector
import me.bmax.apatch.ui.component.SliderSettingCard
import me.bmax.apatch.ui.component.SliderStyleConfig
import me.bmax.apatch.ui.component.ColorStandardSelector
import me.bmax.apatch.ui.component.ColorStylePicker
import me.bmax.apatch.ui.theme.ColorGenerationMode
import me.bmax.apatch.ui.theme.ColorStandard
import me.bmax.apatch.ui.theme.ColorStyle
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SwitchIconState
import me.bmax.apatch.ui.component.FilePickerDialog
import me.bmax.apatch.ui.component.DualBackgroundSettings

import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.component.ThemeColorPicker
import me.bmax.apatch.ui.component.ThemeMode
import me.bmax.apatch.ui.component.ThemeModeSelector
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.FontConfig
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.BottomBarIconConfig
import me.bmax.apatch.util.ui.FloatingBarConfig
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsContent(
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    onNavigateToThemeStore: () -> Unit,
    onNavigateToApiMarketplace: () -> Unit,
    flat: Boolean = false,
    highlightKey: String? = null,
    themeStoreMode: String? = null,
    onThemeStoreModeChanged: ((String) -> Unit)? = null,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    var pickingType by remember { mutableStateOf<String?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var showCropOptionDialog by remember { mutableStateOf(false) }

    // 裁剪 launcher：将选取的图片交给系统裁剪界面，返回裁剪后的 URI
    val cropImageLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContract<Uri, Uri?>() {
            override fun createIntent(context: Context, input: Uri): Intent {
                val tempFile = File(context.cacheDir, "background_crop_cache").apply {
                    parentFile?.mkdirs()
                    delete()
                    createNewFile()
                    deleteOnExit()
                }

                context.contentResolver.openInputStream(input)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val tempUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                return Intent("com.android.camera.action.CROP").apply {
                    setDataAndType(tempUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    putExtra("crop", "true")

                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    putExtra("aspectX", screenWidth)
                    putExtra("aspectY", screenHeight)
                    putExtra("outputX", screenWidth)
                    putExtra("outputY", screenHeight)

                    putExtra("return-data", false)
                    putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                return if (resultCode == Activity.RESULT_OK) intent?.data else null
            }
        }
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = when (pickingType) {
                    "home" -> BackgroundManager.saveAndApplyHomeBackground(context, it)
                    "kernel" -> BackgroundManager.saveAndApplyKernelBackground(context, it)
                    "superuser" -> BackgroundManager.saveAndApplySuperuserBackground(context, it)
                    "system" -> BackgroundManager.saveAndApplySystemModuleBackground(context, it)
                    "settings" -> BackgroundManager.saveAndApplySettingsBackground(context, it)
                    else -> BackgroundManager.saveAndApplyCustomBackground(context, it)
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

    val pickFontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = FontConfig.saveFontFile(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_font_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_font_error))
                }
            }
        }
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

    var pendingExportMetadata by remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val showExportDialog = remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportMetadata by remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val showImportDialog = remember { mutableStateOf(false) }
    val showFilePicker = remember { mutableStateOf(false) }

    val importThemeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loadingDialog.show()
                val metadata = ThemeManager.readThemeMetadata(context, uri)
                loadingDialog.hide()
                if (metadata != null) {
                    pendingImportUri = uri
                    pendingImportMetadata = metadata
                    showImportDialog.value = true
                } else {
                    loadingDialog.show()
                    val success = ThemeManager.importTheme(context, uri)
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(
                        message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                    )
                }
            }
        }
    }

    val isNightModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var nightModeFollowSys by remember { mutableStateOf(prefs.getBoolean("night_mode_follow_sys", true)) }
    var nightModeEnabled by remember { mutableStateOf(prefs.getBoolean("night_mode_enabled", true)) }
    val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var useSystemDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_system_color_theme", false)) }
    var customFontEnabled by remember { mutableStateOf(FontConfig.isCustomFontEnabled) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)

    var customColorScheme by remember { mutableStateOf(prefs.getString("custom_color", "indigo")) }
    var amoledTheme by remember { mutableStateOf(prefs.getBoolean("amoled_theme", false)) }
    var colorGenerationMode by remember { mutableStateOf(ColorGenerationMode.fromKey(prefs.getString("color_generation_mode", "classic"))) }
    var colorStandard by remember { mutableStateOf(ColorStandard.fromName(prefs.getString("color_standard", "MD3_2021"))) }
    var colorStyle by remember { mutableStateOf(ColorStyle.fromName(prefs.getString("color_style", "TONAL_SPOT"))) }

    var currentStyle by remember { mutableStateOf(prefs.getString("home_layout_style", "circle")) }

    if (refreshThemeObserver) {
        nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
        nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
        useSystemDynamicColor = prefs.getBoolean("use_system_color_theme", true)
        customFontEnabled = FontConfig.isCustomFontEnabled
        customColorScheme = prefs.getString("custom_color", "indigo")
        amoledTheme = prefs.getBoolean("amoled_theme", false)
        colorGenerationMode = ColorGenerationMode.fromKey(prefs.getString("color_generation_mode", "classic"))
        colorStandard = ColorStandard.fromName(prefs.getString("color_standard", "MD3_2021"))
        colorStyle = ColorStyle.fromName(prefs.getString("color_style", "TONAL_SPOT"))
        currentStyle = prefs.getString("home_layout_style", "circle")
    }

    val isDarkTheme = if (nightModeFollowSys) isSystemInDarkTheme() else nightModeEnabled
    val themeMode = if (nightModeFollowSys) ThemeMode.SYSTEM else if (nightModeEnabled) ThemeMode.DARK else ThemeMode.LIGHT
    val isStatsLayout = currentStyle == "stats"
    var statsTopLayout by remember { mutableStateOf(prefs.getString("stats_top_layout", "list") ?: "list") }
    val statsTopLayoutListLabel = stringResource(id = R.string.settings_stats_top_layout_list)
    val statsTopLayoutGridLabel = stringResource(id = R.string.settings_stats_top_layout_grid)
    val statsTopLayoutValue = if (statsTopLayout == "grid") statsTopLayoutGridLabel else statsTopLayoutListLabel
    var showStatsTopLayoutDialog by remember { mutableStateOf(false) }

    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    var currentNavMode by remember { mutableStateOf(prefs.getString("nav_mode", "floating") ?: "floating") }
    val navSchemeLabel = when (currentNavMode) {
        "rail" -> stringResource(R.string.settings_nav_mode_rail)
        "bottom" -> stringResource(R.string.settings_nav_mode_bottom)
        "floating" -> stringResource(R.string.settings_nav_mode_floating)
        else -> stringResource(R.string.settings_nav_mode_auto)
    }
    var showNavSchemeDialog by remember { mutableStateOf(false) }

    val isFloatingNav = currentNavMode == "floating"
    var floatingAutoHide by remember { mutableStateOf(prefs.getBoolean("floating_auto_hide", true)) }
    var floatingSwipeHide by remember { mutableStateOf(prefs.getBoolean("floating_swipe_hide", true)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "night_mode_follow_sys" -> nightModeFollowSys = prefs.getBoolean(key, true)
                "night_mode_enabled" -> nightModeEnabled = prefs.getBoolean(key, true)
                "use_system_color_theme" -> useSystemDynamicColor = prefs.getBoolean(key, false)
                "custom_color" -> customColorScheme = prefs.getString(key, "indigo")
                "amoled_theme" -> amoledTheme = prefs.getBoolean(key, false)
                "color_generation_mode" -> colorGenerationMode = ColorGenerationMode.fromKey(prefs.getString(key, "classic"))
                "color_standard" -> colorStandard = ColorStandard.fromName(prefs.getString(key, "MD3_2021"))
                "color_style" -> colorStyle = ColorStyle.fromName(prefs.getString(key, "TONAL_SPOT"))
                "home_layout_style" -> currentStyle = prefs.getString(key, "circle")
                "stats_top_layout" -> statsTopLayout = prefs.getString(key, "list") ?: "list"
                "show_nav_apm" -> showNavApm = prefs.getBoolean(key, true)
                "show_nav_kpm" -> showNavKpm = prefs.getBoolean(key, true)
                "show_nav_superuser" -> showNavSuperUser = prefs.getBoolean(key, true)
                "nav_mode" -> currentNavMode = prefs.getString(key, "floating") ?: "floating"
                "floating_auto_hide" -> floatingAutoHide = prefs.getBoolean(key, true)
                "floating_swipe_hide" -> floatingSwipeHide = prefs.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isKernelSuStyle = currentStyle == "kernelsu"
    val showGridCardSettings = isKernelSuStyle || (isStatsLayout && statsTopLayout == "grid")
    val isListStyle = currentStyle != "kernelsu" && currentStyle != "focus" && !(isStatsLayout && statsTopLayout == "grid")
    // Focus布局样式（FocusUI），该样式下4个卡片支持独立壁纸
    val isFocusStyle = currentStyle == "focus"

    val badgeTextModes = listOf(
        stringResource(R.string.settings_custom_badge_text_full_half),
        stringResource(R.string.settings_custom_badge_text_lkm),
        stringResource(R.string.settings_custom_badge_text_gki),
        stringResource(R.string.settings_custom_badge_text_n_gki),
        stringResource(R.string.settings_custom_badge_text_oki),
        stringResource(R.string.settings_custom_badge_text_built_in)
    )
    val currentBadgeTextModeIndex = BackgroundConfig.customBadgeTextMode
    val currentBadgeTextMode = badgeTextModes.getOrElse(currentBadgeTextModeIndex) { badgeTextModes[0] }
    val showCustomBadgeTextDialog = remember { mutableStateOf(false) }

    val showHomeLayoutChooseDialog = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        SplicedColumnGroup(title = stringResource(R.string.settings_appearance_night_mode), flat = flat, highlightKey = highlightKey) {
            if (isNightModeSupported) {
                item(key = "appearance_theme_mode") {
                    ThemeModeSelector(
                        selectedMode = themeMode,
                        onModeSelected = { mode ->
                            when (mode) {
                                ThemeMode.LIGHT -> {
                                    nightModeFollowSys = false
                                    nightModeEnabled = false
                                    prefs.edit().putBoolean("night_mode_follow_sys", false).putBoolean("night_mode_enabled", false).apply()
                                }
                                ThemeMode.DARK -> {
                                    nightModeFollowSys = false
                                    nightModeEnabled = true
                                    prefs.edit().putBoolean("night_mode_follow_sys", false).putBoolean("night_mode_enabled", true).apply()
                                }
                                ThemeMode.SYSTEM -> {
                                    nightModeFollowSys = true
                                    prefs.edit().putBoolean("night_mode_follow_sys", true).apply()
                                }
                            }
                            refreshTheme.value = true
                        },
                        flat = flat,
                        bare = true,
                    )
                }
            }

            item(key = "appearance_theme_color") {
                ThemeColorPicker(
                    selectedColorKey = customColorScheme ?: "indigo",
                    onColorSelected = { key ->
                        prefs.edit().putString("custom_color", key).putBoolean("use_system_color_theme", false).apply()
                        customColorScheme = key
                        useSystemDynamicColor = false
                        refreshTheme.value = true
                    },
                    isDarkTheme = isDarkTheme,
                    flat = flat,
                    isDynamicColorSupported = isDynamicColorSupport,
                    isDynamicColorEnabled = useSystemDynamicColor,
                    onDynamicColorSelected = {
                        prefs.edit().putBoolean("use_system_color_theme", true).apply()
                        useSystemDynamicColor = true
                        refreshTheme.value = true
                    },
                    bare = true,
                )
            }

            // Color generation mode & style pickers
            item(key = "appearance_color_generation_mode") {
                ColorGenerationModeSelector(
                    selectedMode = colorGenerationMode,
                    onModeSelected = { mode ->
                        colorGenerationMode = mode
                        prefs.edit().putString("color_generation_mode", mode.key).apply()
                        refreshTheme.value = true
                    },
                    flat = flat,
                    bare = true,
                )
            }

            if (colorGenerationMode == ColorGenerationMode.CUSTOM) {
                item(key = "appearance_color_standard") {
                    ColorStandardSelector(
                        selectedStandard = colorStandard,
                        onStandardSelected = { standard ->
                            colorStandard = standard
                            prefs.edit().putString("color_standard", standard.name).apply()
                            refreshTheme.value = true
                        },
                        flat = flat,
                        bare = true,
                    )
                }

                item(key = "appearance_color_style") {
                    ColorStylePicker(
                        selectedStyle = colorStyle,
                        onStyleSelected = { style ->
                            colorStyle = style
                            prefs.edit().putString("color_style", style.name).apply()
                            refreshTheme.value = true
                        },
                        flat = flat,
                        bare = true,
                    )
                }
            }

            if (isDarkTheme) {
                item(key = "appearance_amoled_theme") {
                    val isWallpaperEnabled = BackgroundConfig.isCustomBackgroundEnabled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = amoledTheme,
                                onValueChange = {
                                    if (!isWallpaperEnabled) {
                                        amoledTheme = it
                                        prefs.edit().putBoolean("amoled_theme", it).apply()
                                        refreshTheme.value = true
                                    }
                                },
                                role = Role.Switch,
                                enabled = !isWallpaperEnabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.DarkMode, contentDescription = null, tint = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_amoled_theme),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_amoled_theme_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            )
                        }
                        ExpressiveSwitch(
                            checked = amoledTheme,
                            onCheckedChange = null,
                            enabled = !isWallpaperEnabled,
                        )
                    }
                }
            }

            item(key = "appearance_switch_icon") {
                var showSwitchIcon by remember { mutableStateOf(SwitchIconState.showIcon) }
                ToggleSettingCard(
                    icon = Icons.Filled.ToggleOn,
                    flat = flat,
                    title = stringResource(R.string.settings_switch_icon),
                    description = stringResource(R.string.settings_switch_icon_desc),
                    checked = showSwitchIcon,
                    onCheckedChange = {
                        showSwitchIcon = it
                        SwitchIconState.showIcon = it
                        prefs.edit().putBoolean("show_switch_icon", it).apply()
                    },
                )
            }

            item(key = "appearance_discrete_slider") {
                var isDiscreteSlider by remember { mutableStateOf(SliderStyleConfig.isDiscrete) }
                ToggleSettingCard(
                    icon = Icons.Filled.Segment,
                    flat = flat,
                    title = stringResource(R.string.settings_discrete_slider),
                    description = stringResource(R.string.settings_discrete_slider_desc),
                    checked = isDiscreteSlider,
                    onCheckedChange = {
                        isDiscreteSlider = it
                        SliderStyleConfig.isDiscrete = it
                        prefs.edit().putBoolean("discrete_slider", it).apply()
                    },
                )
            }
        }

        SplicedColumnGroup(title = stringResource(R.string.settings_appearance_layout), flat = flat, highlightKey = highlightKey) {
            item(key = "appearance_home_layout") {
                ExpressiveCard(flat = flat, onClick = { showHomeLayoutChooseDialog.value = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_home_layout_style),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(homeLayoutStyleToString(currentStyle.toString())),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            item(key = "appearance_stats_top_layout", visible = isStatsLayout) {
                ExpressiveCard(flat = flat, onClick = { showStatsTopLayoutDialog = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_stats_top_layout),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = statsTopLayoutValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            if (kPatchReady) {
                item(key = "appearance_nav_layout") {
                    var expanded by remember { mutableStateOf(false) }
                    val rotationState by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "ArrowRotation",
                    )
                    ExpressiveCard(flat = flat, onClick = { expanded = !expanded }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.settings_nav_layout_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_nav_layout_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.rotate(rotationState),
                            )
                        }
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = stringResource(id = R.string.settings_show_apm),
                                summary = null,
                                checked = showNavApm,
                                onCheckedChange = {
                                    showNavApm = it
                                    prefs.edit().putBoolean("show_nav_apm", it).apply()
                                },
                            )
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = stringResource(id = R.string.settings_show_kpm),
                                summary = null,
                                checked = showNavKpm,
                                onCheckedChange = {
                                    showNavKpm = it
                                    prefs.edit().putBoolean("show_nav_kpm", it).apply()
                                },
                            )
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = stringResource(id = R.string.settings_show_superuser),
                                summary = null,
                                checked = showNavSuperUser,
                                onCheckedChange = {
                                    showNavSuperUser = it
                                    prefs.edit().putBoolean("show_nav_superuser", it).apply()
                                },
                            )
                        }
                    }
                }
            }

            item(key = "appearance_nav_scheme") {
                ExpressiveCard(flat = flat, onClick = { showNavSchemeDialog = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_nav_scheme),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = navSchemeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            if (isFloatingNav) {
                item(key = "appearance_navbar_glass") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.AutoAwesome,
                        title = stringResource(id = R.string.settings_navbar_glass_effect),
                        description = stringResource(id = R.string.settings_navbar_glass_effect_summary),
                        checked = BackgroundConfig.isNavBarGlassEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setNavBarGlassEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                if (BackgroundConfig.isNavBarGlassEnabled) {
                    item(key = "appearance_navbar_glass_blur") {
                        SliderSettingCard(
                            flat = flat,
                            title = stringResource(id = R.string.settings_navbar_glass_blur_strength),
                            value = BackgroundConfig.navBarGlassBlurStrength,
                            onValueChange = { BackgroundConfig.setNavBarGlassBlurStrengthValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }

                    item(key = "appearance_navbar_glass_transparency") {
                        SliderSettingCard(
                            flat = flat,
                            title = stringResource(id = R.string.settings_navbar_glass_transparency),
                            value = BackgroundConfig.navBarGlassTransparency,
                            onValueChange = { BackgroundConfig.setNavBarGlassTransparencyValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }

                    item(key = "appearance_navbar_glass_highlight") {
                        SliderSettingCard(
                            flat = flat,
                            title = stringResource(id = R.string.settings_navbar_glass_highlight_strength),
                            value = BackgroundConfig.navBarGlassHighlightStrength,
                            onValueChange = { BackgroundConfig.setNavBarGlassHighlightStrengthValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }

                    item(key = "appearance_navbar_glass_specular") {
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.LensBlur,
                            title = stringResource(id = R.string.settings_navbar_glass_specular),
                            description = stringResource(id = R.string.settings_navbar_glass_specular_summary),
                            checked = BackgroundConfig.isNavBarGlassSpecularEnabled,
                            onCheckedChange = {
                                BackgroundConfig.setNavBarGlassSpecularEnabledState(it)
                                BackgroundConfig.save(context)
                            },
                        )
                    }

                    item(key = "appearance_navbar_glass_glow") {
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.Grain,
                            title = stringResource(id = R.string.settings_navbar_glass_inner_glow),
                            description = stringResource(id = R.string.settings_navbar_glass_inner_glow_summary),
                            checked = BackgroundConfig.isNavBarGlassInnerGlowEnabled,
                            onCheckedChange = {
                                BackgroundConfig.setNavBarGlassInnerGlowEnabledState(it)
                                BackgroundConfig.save(context)
                            },
                        )
                    }

                    item(key = "appearance_navbar_glass_border") {
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.BorderStyle,
                            title = stringResource(id = R.string.settings_navbar_glass_border),
                            description = stringResource(id = R.string.settings_navbar_glass_border_summary),
                            checked = BackgroundConfig.isNavBarGlassBorderEnabled,
                            onCheckedChange = {
                                BackgroundConfig.setNavBarGlassBorderEnabledState(it)
                                BackgroundConfig.save(context)
                            },
                        )
                    }
                }

                // ---- 紧凑圆角风格 ----
                // 仅非毛玻璃模式显示（毛玻璃已有独立的外观控制）
                if (!BackgroundConfig.isNavBarGlassEnabled) {
                    item(key = "appearance_compact_rounded_bar") {
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.RoundedCorner,
                            title = stringResource(id = R.string.settings_compact_rounded_bar),
                            description = stringResource(id = R.string.settings_compact_rounded_bar_summary),
                            checked = FloatingBarConfig.isCompactRoundedStyle,
                            onCheckedChange = { enabled ->
                                FloatingBarConfig.isCompactRoundedStyle = enabled
                                FloatingBarConfig.save(context)
                            },
                        )
                    }
                }

                item(key = "appearance_floating_auto_hide") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.VisibilityOff,
                        title = stringResource(id = R.string.settings_floating_auto_hide),
                        description = stringResource(id = R.string.settings_floating_auto_hide_summary),
                        checked = floatingAutoHide,
                        onCheckedChange = {
                            floatingAutoHide = it
                            prefs.edit().putBoolean("floating_auto_hide", it).apply()
                        },
                    )
                }

                item(key = "appearance_floating_swipe_hide") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.Swipe,
                        title = stringResource(id = R.string.settings_floating_swipe_hide),
                        description = stringResource(id = R.string.settings_floating_swipe_hide_summary),
                        checked = floatingSwipeHide,
                        onCheckedChange = {
                            floatingSwipeHide = it
                            prefs.edit().putBoolean("floating_swipe_hide", it).apply()
                        },
                    )
                }
            }

            item(key = "appearance_nav_custom_icons") {
                val customNavIconsEnabled = remember { mutableStateOf(prefs.getBoolean("nav_icon_custom_enabled", false)) }
                var editingDestName by remember { mutableStateOf<String?>(null) }
                // Observe config revision so the previews below refresh immediately after pick/clear.
                val iconRevision by BottomBarIconConfig.revision.collectAsStateWithLifecycle()
                val iconPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    val dest = editingDestName ?: return@rememberLauncherForActivityResult
                    if (uri != null) {
                        BottomBarIconConfig.setCustomIconUri(dest, uri.toString())
                        scope.launch {
                            snackBarHost.showSnackbar(context.getString(R.string.nav_icon_set))
                        }
                    }
                    editingDestName = null
                }

                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.Image,
                    title = stringResource(R.string.settings_nav_custom_icons),
                    description = stringResource(R.string.settings_nav_custom_icons_summary),
                    checked = customNavIconsEnabled.value,
                    onCheckedChange = {
                        customNavIconsEnabled.value = it
                        BottomBarIconConfig.isEnabled = it
                    }
                )

                if (customNavIconsEnabled.value) {
                    Spacer(Modifier.height(8.dp))
                    val navDestinations = listOf(
                        Triple("Home", R.string.nav_icon_home, Icons.Filled.Home),
                        Triple("KModule", R.string.nav_icon_kpm, Icons.Filled.Archive),
                        Triple("SuperUser", R.string.nav_icon_superuser, Icons.Filled.AdminPanelSettings),
                        Triple("AModule", R.string.nav_icon_apm, Icons.Filled.Extension),
                        Triple("Settings", R.string.nav_icon_settings, Icons.Filled.Settings),
                    )

                    Column {
                        navDestinations.forEach { (destName, labelRes, defaultIcon) ->
                            val customUri = remember(iconRevision, destName) { prefs.getString("nav_icon_$destName", null) }
                            ExpressiveCard(
                                flat = flat,
                                onClick = {
                                    editingDestName = destName
                                    try { iconPickerLauncher.launch("image/*") } catch (_: Throwable) {}
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (customUri != null) {
                                        AsyncImage(
                                            model = customUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = defaultIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(labelRes),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            if (customUri != null) stringResource(R.string.nav_icon_custom_selected)
                                            else stringResource(R.string.nav_icon_default),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (customUri != null) {
                                        IconButton(
                                            onClick = {
                                                BottomBarIconConfig.setCustomIconUri(destName, null)
                                                scope.launch {
                                                    snackBarHost.showSnackbar(context.getString(R.string.nav_icon_cleared))
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Filled.Close, stringResource(R.string.nav_icon_clear), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            item(key = "appearance_list_card_badge", visible = isListStyle) {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.LabelOff,
                    title = stringResource(id = R.string.settings_list_card_hide_status_badge),
                    description = stringResource(id = R.string.settings_list_card_hide_status_badge_summary),
                    checked = BackgroundConfig.isListWorkingCardModeHidden,
                    onCheckedChange = {
                        BackgroundConfig.setListWorkingCardModeHiddenState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            item(key = "appearance_custom_badge_text_list", visible = isListStyle && !BackgroundConfig.isListWorkingCardModeHidden) {
                ExpressiveCard(flat = flat, onClick = { showCustomBadgeTextDialog.value = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_custom_badge_text),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = currentBadgeTextMode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            item(key = "appearance_advanced_title") {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.Title,
                    title = stringResource(id = R.string.settings_advanced_title_style),
                    description = if (BackgroundConfig.isAdvancedTitleStyleEnabled) stringResource(id = R.string.settings_advanced_title_style_enabled) else stringResource(id = R.string.settings_advanced_title_style_summary),
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
                    SliderSettingCard(
                        flat = flat,
                        title = stringResource(id = R.string.settings_title_image_day_opacity),
                        value = BackgroundConfig.titleImageDayOpacity,
                        onValueChange = { BackgroundConfig.setTitleImageDayOpacityValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_title_night_opacity") {
                    SliderSettingCard(
                        flat = flat,
                        title = stringResource(id = R.string.settings_title_image_night_opacity),
                        value = BackgroundConfig.titleImageNightOpacity,
                        onValueChange = { BackgroundConfig.setTitleImageNightOpacityValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_title_image_dim") {
                    SliderSettingCard(
                        flat = flat,
                        title = stringResource(id = R.string.settings_title_image_dim),
                        value = BackgroundConfig.titleImageDim,
                        onValueChange = { BackgroundConfig.setTitleImageDimValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_title_image_offset_x") {
                    SliderSettingCard(
                        flat = flat,
                        title = stringResource(id = R.string.settings_title_image_offset_x),
                        value = BackgroundConfig.titleImageOffsetX,
                        valueRange = -1f..1f,
                        onValueChange = { BackgroundConfig.setTitleImageOffsetXValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_select_title_image") {
                    ExpressiveCard(
                        flat = flat,
                        onClick = {
                            if (PermissionUtils.hasExternalStoragePermission(context)) {
                                try {
                                    pickTitleImageLauncher.launch("image/*")
                                } catch (e: ActivityNotFoundException) {
                                    showToast(context, e.message ?: "")
                                }
                            } else {
                                showToast(context, "请先授予存储权限才能选择标题图片")
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(id = R.string.settings_select_title_image), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) {
                                    Text(text = stringResource(id = R.string.settings_title_image_selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
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
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                clearTitleImageDialog.showConfirm(
                                    title = context.getString(R.string.settings_clear_title_image),
                                    content = context.getString(R.string.settings_clear_title_image_confirm),
                                    markdown = false,
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(text = stringResource(id = R.string.settings_clear_title_image), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        SplicedColumnGroup(title = stringResource(R.string.settings_appearance_background), flat = flat, highlightKey = highlightKey) {
            item(key = "appearance_custom_background") {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.Wallpaper,
                    title = stringResource(id = R.string.settings_custom_background),
                    description = if (BackgroundConfig.isCustomBackgroundEnabled) stringResource(id = R.string.settings_custom_background_enabled) else stringResource(id = R.string.settings_custom_background_summary),
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
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.Contrast,
                            title = stringResource(id = R.string.settings_custom_background_dual_dim),
                            description = stringResource(id = R.string.settings_custom_background_dual_dim_desc),
                            checked = BackgroundConfig.isDualBackgroundDimEnabled,
                            onCheckedChange = {
                                BackgroundConfig.setDualBackgroundDimEnabledState(it)
                                BackgroundConfig.save(context)
                                refreshTheme.value = true
                            },
                        )
                    }

                    item(key = "appearance_bg_opacity") {
                        SliderSettingCard(
                            flat = flat,
                            title = stringResource(id = R.string.settings_custom_background_opacity),
                            value = BackgroundConfig.customBackgroundOpacity,
                            onValueChange = { BackgroundConfig.setCustomBackgroundOpacityValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }

                    item(key = "appearance_bg_blur") {
                        SliderSettingCard(
                            flat = flat,
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
                            SliderSettingCard(
                                flat = flat,
                                title = stringResource(id = R.string.settings_custom_background_dim),
                                value = BackgroundConfig.customBackgroundDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                            )
                        }
                    } else {
                        item(key = "appearance_bg_day_dim") {
                            SliderSettingCard(
                                flat = flat,
                                title = stringResource(id = R.string.settings_custom_background_day_dim),
                                value = BackgroundConfig.customBackgroundDayDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundDayDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                            )
                        }

                        item(key = "appearance_bg_night_dim") {
                            SliderSettingCard(
                                flat = flat,
                                title = stringResource(id = R.string.settings_custom_background_night_dim),
                                value = BackgroundConfig.customBackgroundNightDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundNightDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                            )
                        }
                    }
                }

                item(key = "appearance_video_background") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.VideoFile,
                        title = stringResource(id = R.string.settings_video_background),
                        description = stringResource(id = R.string.settings_video_background_summary),
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
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                try {
                                    pickVideoLauncher.launch("video/*")
                                } catch (e: ActivityNotFoundException) {
                                    showToast(context, e.message ?: "")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(imageVector = Icons.Filled.VideoFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = R.string.settings_select_video), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    if (!BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
                                        Text(text = stringResource(id = R.string.settings_video_selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
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
                            ExpressiveCard(
                                flat = flat,
                                onClick = {
                                    clearVideoDialog.showConfirm(
                                        title = clearVideoTitle,
                                        content = clearVideoConfirm,
                                        markdown = false,
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(text = clearVideoTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    item(key = "appearance_video_volume") {
                        SliderSettingCard(
                            flat = flat,
                            title = stringResource(id = R.string.settings_video_volume),
                            value = BackgroundConfig.videoVolume,
                            onValueChange = { BackgroundConfig.setVideoVolumeValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }
                } else {
                    item(key = "appearance_multi_background") {
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.GridView,
                            title = stringResource(id = R.string.settings_multi_background_mode),
                            description = stringResource(id = R.string.settings_multi_background_mode_summary),
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
                                    ExpressiveCard(
                                        flat = flat,
                                        onClick = {
                                            if (PermissionUtils.hasExternalStoragePermission(context) &&
                                                PermissionUtils.hasWriteExternalStoragePermission(context)) {
                                                pickingType = type
                                                try {
                                                    pickImageLauncher.launch("image/*")
                                                } catch (e: ActivityNotFoundException) {
                                                    showToast(context, e.message ?: "")
                                                }
                                            } else {
                                                showToast(context, "请先授予存储权限才能选择背景图片")
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                            Spacer(Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = stringResource(id = titleRes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                                if (!uri.isNullOrEmpty()) {
                                                    Text(text = stringResource(id = R.string.settings_background_selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "appearance_select_background") {
                            ExpressiveCard(
                                flat = flat,
                                onClick = {
                                    if (PermissionUtils.hasExternalStoragePermission(context) &&
                                        PermissionUtils.hasWriteExternalStoragePermission(context)) {
                                        pickingType = "default"
                                        try {
                                            pickImageLauncher.launch("image/*")
                                        } catch (e: ActivityNotFoundException) {
                                            showToast(context, e.message ?: "")
                                        }
                                    } else {
                                        showToast(context, "请先授予存储权限才能选择背景图片")
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = stringResource(id = R.string.settings_select_background_image), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                        if (!BackgroundConfig.customBackgroundUri.isNullOrEmpty()) {
                                            Text(text = stringResource(id = R.string.settings_background_selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
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
                                ExpressiveCard(
                                    flat = flat,
                                    onClick = {
                                        clearBackgroundDialog.showConfirm(
                                            title = clearBgTitle,
                                            content = clearBgConfirm,
                                            markdown = false,
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Text(text = clearBgTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showGridCardSettings) {
                item(key = "appearance_grid_card_bg") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.GridView,
                        title = stringResource(id = R.string.settings_grid_working_card_background),
                        description = if (BackgroundConfig.isGridWorkingCardBackgroundEnabled) stringResource(id = R.string.settings_grid_working_card_background_enabled) else stringResource(id = R.string.settings_grid_working_card_background_summary),
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
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                if (PermissionUtils.hasExternalStoragePermission(context)) {
                                    try {
                                        pickGridImageLauncher.launch("image/*")
                                    } catch (e: ActivityNotFoundException) {
                                        showToast(context, e.message ?: "")
                                    }
                                } else {
                                    showToast(context, "请先授予存储权限才能选择背景图片")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = R.string.settings_select_background_image), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    if (!BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty()) {
                                        Text(text = stringResource(id = R.string.settings_grid_working_card_background_selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
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
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                clearGridBackgroundDialog.showConfirm(
                                    title = context.getString(R.string.settings_clear_grid_working_card_background),
                                    content = context.getString(R.string.settings_clear_grid_working_card_background_confirm),
                                    markdown = false,
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(text = stringResource(id = R.string.settings_clear_grid_working_card_background), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                item(key = "appearance_grid_card_check") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.CheckCircle,
                        title = stringResource(id = R.string.settings_grid_working_card_hide_check),
                        description = stringResource(id = R.string.settings_grid_working_card_hide_check_summary),
                        checked = BackgroundConfig.isGridWorkingCardCheckHidden,
                        onCheckedChange = {
                            BackgroundConfig.setGridWorkingCardCheckHiddenState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                item(key = "appearance_grid_card_text") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.TextFields,
                        title = stringResource(id = R.string.settings_grid_working_card_hide_text),
                        description = stringResource(id = R.string.settings_grid_working_card_hide_text_summary),
                        checked = BackgroundConfig.isGridWorkingCardTextHidden,
                        onCheckedChange = {
                            BackgroundConfig.setGridWorkingCardTextHiddenState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                item(key = "appearance_grid_card_mode") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.Label,
                        title = stringResource(id = R.string.settings_grid_working_card_hide_mode),
                        description = stringResource(id = R.string.settings_grid_working_card_hide_mode_summary),
                        checked = BackgroundConfig.isGridWorkingCardModeHidden,
                        onCheckedChange = {
                            BackgroundConfig.setGridWorkingCardModeHiddenState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                item(key = "appearance_grid_badge_text", visible = !BackgroundConfig.isGridWorkingCardModeHidden) {
                    ExpressiveCard(flat = flat, onClick = { showCustomBadgeTextDialog.value = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.settings_custom_badge_text),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = currentBadgeTextMode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }

        }

        // FocusUI card wallpapers are separate from page-level and multi-background settings.
        if (isFocusStyle) {
            SplicedColumnGroup(
                title = stringResource(R.string.focus_card_background_title),
                flat = flat,
                highlightKey = highlightKey,
            ) {
                item(key = "appearance_focus_card_background_enabled") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.Wallpaper,
                        title = stringResource(R.string.settings_focus_card_background),
                        description = if (BackgroundConfig.isFocusCardBackgroundEnabled) {
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
                        ExpressiveCard(
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
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
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
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(id = R.string.focus_card_background_clear),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(24.dp)
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

        if (showCustomBadgeTextDialog.value) {
            AlertDialog(
                onDismissRequest = { showCustomBadgeTextDialog.value = false },
                title = { Text(stringResource(id = R.string.settings_custom_badge_text)) },
                text = {
                    Column {
                        Text(
                            stringResource(id = R.string.settings_custom_badge_text_summary),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        badgeTextModes.forEachIndexed { index, mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        BackgroundConfig.setCustomBadgeTextModeValue(index)
                                        BackgroundConfig.save(context)
                                        showCustomBadgeTextDialog.value = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = index == currentBadgeTextModeIndex,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = mode)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomBadgeTextDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        SplicedColumnGroup(title = stringResource(R.string.settings_appearance_banner), flat = flat, highlightKey = highlightKey) {
            item(key = "appearance_banner") {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.Campaign,
                    title = stringResource(id = R.string.apm_enable_module_banner),
                    description = stringResource(id = R.string.apm_enable_module_banner_summary),
                    checked = BackgroundConfig.isBannerEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            if (BackgroundConfig.isBannerEnabled) {
                item(key = "appearance_folk_banner") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.Image,
                        title = stringResource(id = R.string.apm_enable_folk_banner),
                        description = stringResource(id = R.string.apm_enable_folk_banner_summary),
                        checked = BackgroundConfig.isFolkBannerEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setFolkBannerEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                if (BackgroundConfig.isFolkBannerEnabled) {
                    item(key = "appearance_banner_api_mode") {
                        ToggleSettingCard(
                            flat = flat,
                            icon = Icons.Filled.Api,
                            title = stringResource(id = R.string.apm_banner_api_mode),
                            description = stringResource(id = R.string.apm_banner_api_mode_summary),
                            checked = BackgroundConfig.isBannerApiModeEnabled,
                            onCheckedChange = {
                                BackgroundConfig.setBannerApiModeEnabledState(it)
                                BackgroundConfig.save(context)
                            },
                        )
                    }

                    if (BackgroundConfig.isBannerApiModeEnabled) {
                        item(key = "appearance_banner_api_source") {
                            val showBannerApiConfigDialog = remember { mutableStateOf(false) }
                            val apiSourceSummary = if (BackgroundConfig.bannerApiSource.isNotBlank()) {
                                if (BackgroundConfig.bannerApiSource.startsWith("/")) {
                                    context.getString(R.string.apm_banner_local_dir_configured)
                                } else {
                                    context.getString(R.string.apm_banner_api_url_configured)
                                }
                            } else {
                                context.getString(R.string.apm_banner_api_source_not_configured)
                            }

                            Column {
                                ExpressiveCard(flat = flat, onClick = { showBannerApiConfigDialog.value = true }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(imageVector = Icons.Filled.Api, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(id = R.string.apm_banner_api_source),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = apiSourceSummary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                            )
                                        }
                                    }
                                }

                                if (showBannerApiConfigDialog.value) {
                                    BannerApiConfigDialog(
                                        showDialog = showBannerApiConfigDialog,
                                        currentSource = BackgroundConfig.bannerApiSource,
                                        onConfirm = { newSource ->
                                            BackgroundConfig.setBannerApiSourceValue(newSource)
                                            BackgroundConfig.save(context)
                                        },
                                        onClearCache = {
                                            scope.launch {
                                                loadingDialog.show()
                                                me.bmax.apatch.ui.screen.BannerApiService.clearAllCache(context)
                                                loadingDialog.hide()
                                                showToast(context, context.getString(R.string.apm_banner_cache_cleared))
                                            }
                                        }
                                    )
                                }

                                ExpressiveCard(flat = flat, onClick = { onNavigateToApiMarketplace() }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.apm_api_marketplace_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "appearance_banner_opacity") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.Opacity,
                        title = stringResource(id = R.string.settings_banner_custom_opacity),
                        description = stringResource(id = R.string.settings_banner_custom_opacity_summary),
                        checked = BackgroundConfig.isBannerCustomOpacityEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setBannerCustomOpacityEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                if (BackgroundConfig.isBannerCustomOpacityEnabled) {
                    item(key = "appearance_banner_opacity_slider") {
                        SliderSettingCard(
                            flat = flat,
                            title = stringResource(id = R.string.settings_banner_opacity),
                            value = BackgroundConfig.bannerCustomOpacity,
                            onValueChange = { BackgroundConfig.setBannerCustomOpacityValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                        )
                    }
                }
            }
        }

        SplicedColumnGroup(title = stringResource(R.string.settings_appearance_font), flat = flat, highlightKey = highlightKey) {
            item(key = "appearance_custom_font") {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.FormatSize,
                    title = stringResource(id = R.string.settings_custom_font),
                    description = if (customFontEnabled) {
                        if (FontConfig.customFontFilename != null) stringResource(id = R.string.settings_font_selected) else stringResource(id = R.string.settings_custom_font_enabled)
                    } else {
                        stringResource(id = R.string.settings_custom_font_summary)
                    },
                    checked = customFontEnabled,
                    onCheckedChange = {
                        customFontEnabled = it
                        FontConfig.setCustomFontEnabledState(it)
                        FontConfig.save(context)
                        refreshTheme.value = true
                    },
                )
            }

            if (FontConfig.isCustomFontEnabled) {
                item(key = "appearance_select_font") {
                    ExpressiveCard(
                        flat = flat,
                        onClick = {
                            try {
                                pickFontLauncher.launch("*/*")
                            } catch (e: ActivityNotFoundException) {
                                showToast(context, e.message ?: "")
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(text = stringResource(id = R.string.settings_select_font_file), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                if (FontConfig.customFontFilename != null) {
                    item(key = "appearance_clear_font") {
                        val clearFontDialog = rememberConfirmDialog(
                            onConfirm = {
                                FontConfig.clearFont(context)
                                refreshTheme.value = true
                                scope.launch {
                                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_font_cleared))
                                }
                            }
                        )
                        val clearFontTitle = stringResource(id = R.string.settings_clear_font)
                        val clearFontConfirm = context.getString(R.string.settings_clear_font_confirm)
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                clearFontDialog.showConfirm(
                                    title = clearFontTitle,
                                    content = clearFontConfirm,
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(text = clearFontTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        SplicedColumnGroup(title = stringResource(R.string.settings_appearance_theme), flat = flat, highlightKey = highlightKey) {
            item(key = "appearance_theme_store") {
                ExpressiveCard(flat = flat, onClick = { onNavigateToThemeStore() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.theme_store_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            if (themeStoreMode == "compat") {
                                Text(
                                    text = stringResource(R.string.theme_mode_compat_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item(key = "appearance_theme_store_mode") {
                val modeName = when (themeStoreMode) {
                    "compat" -> stringResource(R.string.theme_mode_compat)
                    else -> stringResource(R.string.theme_mode_builtin)
                }
                val showModeSwitchDialog = remember { mutableStateOf(false) }
                ExpressiveCard(flat = flat, onClick = { showModeSwitchDialog.value = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_theme_mode),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.theme_mode_current, modeName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showModeSwitchDialog.value) {
                    BasicAlertDialog(
                        onDismissRequest = { showModeSwitchDialog.value = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(30.dp),
                            color = AlertDialogDefaults.containerColor,
                            tonalElevation = AlertDialogDefaults.TonalElevation,
                            modifier = Modifier.width(320.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = stringResource(R.string.theme_mode_switch_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = stringResource(R.string.theme_mode_switch_msg),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val builtinLabel = stringResource(R.string.theme_mode_builtin_label)
                                val compatLabel = stringResource(R.string.theme_mode_compat_label)
                                listOf("builtin" to builtinLabel, "compat" to compatLabel).forEach { (mode, label) ->
                                    Surface(
                                        onClick = {
                                            prefs.edit { putString("theme_mode", mode) }
                                            onThemeStoreModeChanged?.invoke(mode)
                                            showModeSwitchDialog.value = false
                                            scope.launch {
                                                snackBarHost.showSnackbar(
                                                    context.getString(R.string.theme_mode_switched, label)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (themeStoreMode == mode) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = themeStoreMode == mode,
                                                onClick = null
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    if (mode == "compat") stringResource(R.string.theme_mode_compat_desc)
                                                    else stringResource(R.string.theme_mode_builtin_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }

                                TextButton(
                                    onClick = { showModeSwitchDialog.value = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "appearance_save_theme") {
                ExpressiveCard(flat = flat, onClick = { showExportDialog.value = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(text = stringResource(id = R.string.settings_save_theme), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item(key = "appearance_import_theme") {
                ExpressiveCard(flat = flat, onClick = { showFilePicker.value = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(text = stringResource(id = R.string.settings_import_theme), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item(key = "appearance_reset_theme") {
                val resetThemeDialog = rememberConfirmDialog(
                    onConfirm = {
                        scope.launch {
                            loadingDialog.show()
                            val success = ThemeManager.resetTheme(context)
                            loadingDialog.hide()
                            snackBarHost.showSnackbar(
                                message = if (success) context.getString(R.string.settings_theme_reset) else context.getString(R.string.settings_theme_reset_failed)
                            )
                        }
                    }
                )
                val resetThemeTitle = stringResource(id = R.string.settings_reset_theme)
                val resetThemeConfirm = context.getString(R.string.settings_reset_theme_confirm)
                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        resetThemeDialog.showConfirm(
                            title = resetThemeTitle,
                            content = resetThemeConfirm,
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(text = resetThemeTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        }

    if (showHomeLayoutChooseDialog.value) {
        HomeLayoutChooseDialog(showHomeLayoutChooseDialog) { selectedLayout ->
            currentStyle = selectedLayout
            refreshTheme.value = true
        }
    }

    if (showNavSchemeDialog) {
        NavModeChooseDialog(
            showDialog = remember { mutableStateOf(true) }.apply { value = showNavSchemeDialog },
            currentMode = currentNavMode,
            onModeSelected = { mode ->
                currentNavMode = mode
                prefs.edit().putString("nav_mode", mode).apply()
                showNavSchemeDialog = false
            },
            onDismiss = { showNavSchemeDialog = false }
        )
    }

    if (showStatsTopLayoutDialog) {
        StatsTopLayoutChooseDialog(
            showDialog = remember { mutableStateOf(true) }.apply { value = showStatsTopLayoutDialog },
            currentMode = statsTopLayout,
            onModeSelected = { mode ->
                statsTopLayout = mode
                prefs.edit().putString("stats_top_layout", mode).apply()
                showStatsTopLayoutDialog = false
            },
            onDismiss = { showStatsTopLayoutDialog = false }
        )
    }

    if (showExportDialog.value) {
        ThemeExportDialog(
            showDialog = showExportDialog,
            onConfirm = { metadata ->
                pendingExportMetadata = metadata
                scope.launch {
                    loadingDialog.show()
                    try {
                        val exportDir = java.io.File("/storage/emulated/0/Download/FolkPatch/Themes/")
                        if (!exportDir.exists()) {
                            exportDir.mkdirs()
                        }
                        val safeName = metadata.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        val fileName = "$safeName.fpt"
                        val file = java.io.File(exportDir, fileName)
                        val uri = Uri.fromFile(file)
                        val success = ThemeManager.exportTheme(context, uri, metadata)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_saved) + ": ${file.absolutePath}" else context.getString(R.string.settings_theme_save_failed)
                        )
                    } catch (e: Exception) {
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(message = context.getString(R.string.settings_theme_save_failed) + ": ${e.message}")
                    }
                    pendingExportMetadata = null
                }
            }
        )
    }

    if (showImportDialog.value && pendingImportMetadata != null) {
        ThemeImportDialog(
            showDialog = showImportDialog,
            metadata = pendingImportMetadata!!,
            onConfirm = {
                pendingImportUri?.let { uri ->
                    scope.launch {
                        loadingDialog.show()
                        val success = ThemeManager.importTheme(context, uri)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                        )
                        pendingImportUri = null
                        pendingImportMetadata = null
                    }
                }
            }
        )
    }

    if (showFilePicker.value) {
        FilePickerDialog(
            onDismissRequest = { showFilePicker.value = false },
            onFileSelected = { file ->
                showFilePicker.value = false
                val uri = Uri.fromFile(file)
                scope.launch {
                    loadingDialog.show()
                    val metadata = ThemeManager.readThemeMetadata(context, uri)
                    loadingDialog.hide()
                    if (metadata != null) {
                        pendingImportUri = uri
                        pendingImportMetadata = metadata
                        showImportDialog.value = true
                    } else {
                        loadingDialog.show()
                        val success = ThemeManager.importTheme(context, uri)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun colorNameToString(colorName: String): Int {
    return colorsList().find { it.name == colorName }?.nameId ?: R.string.blue_theme
}

private data class APColor(
    val name: String, @param:StringRes val nameId: Int
)

private fun colorsList(): List<APColor> {
    return listOf(
        APColor("amber", R.string.amber_theme),
        APColor("blue_grey", R.string.blue_grey_theme),
        APColor("blue", R.string.blue_theme),
        APColor("brown", R.string.brown_theme),
        APColor("cyan", R.string.cyan_theme),
        APColor("deep_orange", R.string.deep_orange_theme),
        APColor("deep_purple", R.string.deep_purple_theme),
        APColor("green", R.string.green_theme),
        APColor("indigo", R.string.indigo_theme),
        APColor("light_blue", R.string.light_blue_theme),
        APColor("light_green", R.string.light_green_theme),
        APColor("lime", R.string.lime_theme),
        APColor("orange", R.string.orange_theme),
        APColor("pink", R.string.pink_theme),
        APColor("purple", R.string.purple_theme),
        APColor("red", R.string.red_theme),
        APColor("sakura", R.string.sakura_theme),
        APColor("teal", R.string.teal_theme),
        APColor("yellow", R.string.yellow_theme),
        APColor("ink_wash", R.string.ink_wash_theme),
    )
}

@Composable
private fun homeLayoutStyleToString(style: String): Int {
    return when (style) {
        "kernelsu" -> R.string.settings_home_layout_grid
        "focus" -> R.string.settings_home_layout_focus
        "sign" -> R.string.settings_home_layout_sign
        "circle" -> R.string.settings_home_layout_circle
        "dashboard_ui" -> R.string.settings_home_layout_dashboard_pro
        "stats" -> R.string.settings_home_layout_stats
        else -> R.string.settings_home_layout_default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(colorsList(), key = { it.name }) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(it.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("custom_color", it.name) }
                            refreshTheme.value = true
                        })
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutChooseDialog(showDialog: MutableState<Boolean>, onLayoutSelected: (String) -> Unit) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_home_layout_style),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val currentStyle = prefs.getString("home_layout_style", "circle")
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_default)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "default",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "default").apply()
                                onLayoutSelected("default")
                                showDialog.value = false
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_grid)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "kernelsu",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "kernelsu").apply()
                                onLayoutSelected("kernelsu")
                                showDialog.value = false
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_focus)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "focus",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "focus").apply()
                                onLayoutSelected("focus")
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_sign)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "sign",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "sign").apply()
                                onLayoutSelected("sign")
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_circle)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "circle",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "circle").apply()
                                onLayoutSelected("circle")
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_dashboard_pro)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "dashboard_ui",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "dashboard_ui").apply()
                                onLayoutSelected("dashboard_ui")
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_stats)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "stats",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "stats").apply()
                                onLayoutSelected("stats")
                                showDialog.value = false
                            }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeExportDialog(
    showDialog: MutableState<Boolean>,
    onConfirm: (ThemeManager.ThemeMetadata) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("phone") }
    var version by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.theme_export_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.theme_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.theme_type),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { type = "phone" }
                    ) {
                        RadioButton(
                            selected = type == "phone",
                            onClick = { type = "phone" }
                        )
                        Text(
                            text = stringResource(R.string.theme_type_phone),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { type = "tablet" }
                            .padding(start = 16.dp)
                    ) {
                        RadioButton(
                            selected = type == "tablet",
                            onClick = { type = "tablet" }
                        )
                        Text(
                            text = stringResource(R.string.theme_type_tablet),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text(stringResource(R.string.theme_version)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.theme_author)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.theme_description)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                showDialog.value = false
                                onConfirm(
                                    ThemeManager.ThemeMetadata(
                                        name = name,
                                        type = type,
                                        version = version,
                                        author = author,
                                        description = description
                                    )
                                )
                            }
                        },
                        enabled = name.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.theme_export_action))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeImportDialog(
    showDialog: MutableState<Boolean>,
    metadata: ThemeManager.ThemeMetadata,
    onConfirm: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.theme_import_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.theme_import_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.theme_info),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(text = "${stringResource(R.string.theme_name)}: ${metadata.name}")
                        Text(text = "${stringResource(R.string.theme_type)}: ${if (metadata.type == "tablet") stringResource(R.string.theme_type_tablet) else stringResource(R.string.theme_type_phone)}")
                        if (metadata.version.isNotEmpty()) {
                            Text(text = "${stringResource(R.string.theme_version)}: ${metadata.version}")
                        }
                        if (metadata.author.isNotEmpty()) {
                            Text(text = "${stringResource(R.string.theme_author)}: ${metadata.author}")
                        }
                        if (metadata.description.isNotEmpty()) {
                            Text(
                                text = "${stringResource(R.string.theme_description)}: ${metadata.description}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(onClick = {
                        showDialog.value = false
                        onConfirm()
                    }) {
                        Text(stringResource(R.string.theme_import_action))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavModeChooseDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_nav_scheme),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_floating)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "floating",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("floating")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_auto)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "auto",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("auto")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_bottom)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "bottom",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("bottom")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_rail)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "rail",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("rail")
                            }
                        )
                    }
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTopLayoutChooseDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_stats_top_layout),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_stats_top_layout_list)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "list",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("list")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_stats_top_layout_grid)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "grid",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("grid")
                            }
                        )
                    }
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerApiConfigDialog(
    showDialog: MutableState<Boolean>,
    currentSource: String,
    onConfirm: (String) -> Unit,
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    var sourceText by remember { mutableStateOf(currentSource) }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.apm_banner_api_config_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.apm_banner_api_config_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    label = { Text(stringResource(R.string.apm_banner_api_source)) },
                    placeholder = { Text(stringResource(R.string.apm_banner_api_source_hint), style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (sourceText.isNotEmpty()) {
                            IconButton(onClick = { sourceText = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.apm_banner_api_examples_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.apm_banner_api_examples),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearCache()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.apm_banner_clear_cache))
                    }
                    Button(
                        onClick = {
                            onConfirm(sourceText)
                            showDialog.value = false
                            showToast(context, context.getString(R.string.apm_banner_api_source_saved))
                        },
                        enabled = sourceText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
