package me.bmax.apatch.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Eject
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.ramcosta.composedestinations.generated.destinations.NavigationLayoutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.UmountConfigScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.LocalEnableFloatingBottomBar
import me.bmax.apatch.util.APatchKeyHelper
import me.bmax.apatch.util.PageScaleUtils
import me.bmax.apatch.util.LauncherIconUtils
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.isGlobalNamespaceEnabled
import me.bmax.apatch.util.isHideServiceEnabled
import me.bmax.apatch.util.isMagicMountEnabled
import me.bmax.apatch.util.outputStream
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.setGlobalNamespaceEnabled
import me.bmax.apatch.util.setHideServiceEnabled
import me.bmax.apatch.util.setMagicMountEnabled
import me.bmax.apatch.util.VisualConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun SettingScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
    var isGlobalNamespaceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var isMagicMountEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var isHideServiceEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    var bSkipStoreSuperKey by rememberSaveable {
        mutableStateOf(APatchKeyHelper.shouldSkipStoreSuperKey())
    }
    if (kPatchReady && aPatchReady) {
        isGlobalNamespaceEnabled = isGlobalNamespaceEnabled()
        isMagicMountEnabled = isMagicMountEnabled()
        isHideServiceEnabled = isHideServiceEnabled()
    }

    val showResetSuPathDialog = remember { mutableStateOf(false) }
    val showLogBottomSheet = remember { mutableStateOf(false) }
    val showClearKeyDialog = rememberSaveable { mutableStateOf(false) }
    val showScaleDialog = remember { mutableStateOf(false) }

    val loadingDialog = rememberLoadingDialog()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val logSavedMessage = stringResource(R.string.log_saved)
    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                loadingDialog.show()
                uri.outputStream().use { output ->
                    getBugreportFile(context).inputStream().use {
                        it.copyTo(output)
                    }
                }
                loadingDialog.hide()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, logSavedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings),
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = {
            ResetSUPathDialog(showResetSuPathDialog)
            LogBottomSheet(
                showLogBottomSheet,
                scope,
                exportBugreportLauncher,
                loadingDialog,
                context
            )
        }
    )
    { paddingValues ->
        val prefs = APApplication.sharedPreferences
        var sliderValue by rememberSaveable { mutableFloatStateOf(PageScaleUtils.currentScale) }

        val languages = stringArrayResource(id = R.array.languages)
        val languagesValues = stringArrayResource(id = R.array.languages_values)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguageTag = if (currentLocales.isEmpty) null
        else currentLocales.get(0)?.toLanguageTag()
        val langInitialIndex = if (currentLanguageTag == null) 0
        else languagesValues.indexOf(currentLanguageTag).let { if (it >= 0) it else 0 }
        var langSelectedIndex by remember { mutableStateOf(langInitialIndex) }

        var themeMode by rememberSaveable {
            mutableIntStateOf(prefs.getInt("color_mode", 0))
        }
        val themeItems = listOf(
            stringResource(id = R.string.settings_theme_mode_monet_system),
            stringResource(id = R.string.settings_theme_mode_monet_light),
            stringResource(id = R.string.settings_theme_mode_monet_dark),
            stringResource(id = R.string.settings_theme_mode_system),
            stringResource(id = R.string.settings_theme_mode_light),
            stringResource(id = R.string.settings_theme_mode_dark),
        )

        var enableFloatingBottomBar by rememberSaveable {
            mutableStateOf(VisualConfig.enableFloatingBottomBar)
        }

        val homeLayoutItems = listOf(
            stringResource(id = R.string.settings_home_layout_list),
            stringResource(id = R.string.settings_home_layout_default)
        )
        val homeLayoutValues = listOf("list", "default")
        var currentHomeLayout by rememberSaveable { mutableStateOf(prefs.getString("home_layout_style", "list") ?: "list") }
        val homeLayoutIndex = homeLayoutValues.indexOf(currentHomeLayout).let { if (it == -1) 0 else it }

        val floatingBottomPadding = if (LocalEnableFloatingBottomBar.current) 88.dp else 0.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + floatingBottomPadding + 12.dp
            )
        ) {
            // --- Section: Customization ---
            item {
                SmallTitle(text = stringResource(R.string.settings_section_customization))
            }
            item {
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                    SuperDropdown(
                        title = stringResource(id = R.string.settings_theme),
                        summary = stringResource(id = R.string.settings_theme_summary),
                        items = themeItems,
                        selectedIndex = themeMode,
                        startAction = {
                            Icon(
                                Icons.Rounded.Palette,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onSelectedIndexChange = { index ->
                            prefs.edit { putInt("color_mode", index) }
                            themeMode = index
                        }
                    )

                    AnimatedVisibility(visible = themeMode in 3..5) {
                        val colorItems = listOf(
                            stringResource(id = R.string.settings_key_color_default),
                            stringResource(id = R.string.color_red),
                            stringResource(id = R.string.color_pink),
                            stringResource(id = R.string.color_purple),
                            stringResource(id = R.string.color_deep_purple),
                            stringResource(id = R.string.color_indigo),
                            stringResource(id = R.string.color_blue),
                            stringResource(id = R.string.color_cyan),
                            stringResource(id = R.string.color_teal),
                            stringResource(id = R.string.color_green),
                            stringResource(id = R.string.color_yellow),
                            stringResource(id = R.string.color_amber),
                            stringResource(id = R.string.color_orange),
                            stringResource(id = R.string.color_brown),
                            stringResource(id = R.string.color_blue_grey),
                            stringResource(id = R.string.color_sakura),
                        )
                        val colorValues = listOf(
                            0,
                            Color(0xFFF44336).toArgb(),
                            Color(0xFFE91E63).toArgb(),
                            Color(0xFF9C27B0).toArgb(),
                            Color(0xFF673AB7).toArgb(),
                            Color(0xFF3F51B5).toArgb(),
                            Color(0xFF2196F3).toArgb(),
                            Color(0xFF00BCD4).toArgb(),
                            Color(0xFF009688).toArgb(),
                            Color(0xFF4FAF50).toArgb(),
                            Color(0xFFFFEB3B).toArgb(),
                            Color(0xFFFFC107).toArgb(),
                            Color(0xFFFF9800).toArgb(),
                            Color(0xFF795548).toArgb(),
                            Color(0xFF607D8F).toArgb(),
                            Color(0xFFFF9CA8).toArgb(),
                        )
                        var keyColorIndex by rememberSaveable {
                            mutableIntStateOf(
                                colorValues.indexOf(VisualConfig.keyColor).takeIf { it >= 0 } ?: 0
                            )
                        }
                        SuperDropdown(
                            title = stringResource(id = R.string.settings_key_color),
                            summary = stringResource(id = R.string.settings_key_color_summary),
                            items = colorItems,
                            selectedIndex = keyColorIndex,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onSelectedIndexChange = { index ->
                                VisualConfig.keyColor = colorValues[index]
                                keyColorIndex = index
                            }
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var enableBlur by rememberSaveable {
                            mutableStateOf(VisualConfig.enableBlur)
                        }
                        SuperSwitch(
                            title = stringResource(id = R.string.settings_enable_blur),
                            summary = stringResource(id = R.string.settings_enable_blur_summary),
                            checked = enableBlur,
                            startAction = {
                                Icon(
                                    Icons.Rounded.BlurOn,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onCheckedChange = {
                                VisualConfig.enableBlur = it
                                enableBlur = VisualConfig.enableBlur
                            }
                        )
                    }

                    SuperSwitch(
                        title = stringResource(id = R.string.settings_floating_bottom_bar),
                        summary = stringResource(id = R.string.settings_floating_bottom_bar_summary),
                        checked = enableFloatingBottomBar,
                        startAction = {
                            Icon(
                                Icons.Rounded.Dock,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = {
                            VisualConfig.enableFloatingBottomBar = it
                            enableFloatingBottomBar = it
                        }
                    )

                    AnimatedVisibility(
                        visible = enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        var enableLiquidGlass by rememberSaveable {
                            mutableStateOf(VisualConfig.enableLiquidGlass)
                        }
                        SuperSwitch(
                            title = stringResource(id = R.string.settings_enable_liquid_glass),
                            summary = stringResource(id = R.string.settings_enable_liquid_glass_summary),
                            checked = enableLiquidGlass,
                            startAction = {
                                Icon(
                                    Icons.Rounded.AutoFixHigh,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onCheckedChange = {
                                VisualConfig.enableLiquidGlass = it
                                enableLiquidGlass = VisualConfig.enableLiquidGlass
                            }
                        )
                    }

                    SuperDropdown(
                        title = stringResource(id = R.string.settings_home_layout_style),
                        summary = stringResource(id = R.string.settings_home_layout_style_summary),
                        items = homeLayoutItems,
                        selectedIndex = homeLayoutIndex,
                        startAction = {
                            Icon(
                                Icons.Rounded.Dashboard,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onSelectedIndexChange = { index ->
                            prefs.edit { putString("home_layout_style", homeLayoutValues[index]) }
                            currentHomeLayout = homeLayoutValues[index]
                        }
                    )

                    SuperArrow(
                        title = stringResource(id = R.string.settings_nav_layout_title),
                        summary = stringResource(id = R.string.settings_nav_layout_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.Menu,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onClick = {
                            navigator.navigate(NavigationLayoutScreenDestination)
                        }
                    )

                    SuperArrow(
                        title = stringResource(id = R.string.settings_page_scale),
                        summary = stringResource(id = R.string.settings_page_scale_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.AspectRatio,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        endActions = {
                            Text(
                                text = "${(sliderValue * 100).toInt()}%",
                                color = colorScheme.onSurfaceVariantActions
                            )
                        },
                        onClick = { showScaleDialog.value = !showScaleDialog.value },
                        holdDownState = showScaleDialog.value,
                        bottomAction = {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    PageScaleUtils.setScale(sliderValue)
                                },
                                valueRange = 0.8f..1.1f,
                                showKeyPoints = true,
                                keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                magnetThreshold = 0.01f,
                                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            )
                        }
                    )
                    ScaleDialog(
                        showDialog = showScaleDialog.value,
                        onDismissRequest = { showScaleDialog.value = false },
                        scaleState = { PageScaleUtils.currentScale },
                        onScaleChange = {
                            PageScaleUtils.setScale(it)
                            sliderValue = it
                        }
                    )

                    SuperDropdown(
                        title = stringResource(R.string.settings_app_language),
                        summary = stringResource(R.string.settings_app_language_summary),
                        items = languages.toList(),
                        selectedIndex = langSelectedIndex,
                        startAction = {
                            Icon(
                                Icons.Rounded.Language,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onSelectedIndexChange = { newIndex ->
                            langSelectedIndex = newIndex
                            if (newIndex == 0) {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.getEmptyLocaleList()
                                )
                            } else {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(
                                        languagesValues[newIndex]
                                    )
                                )
                            }
                        }
                    )

                    var useAltIcon by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("use_alt_icon", false))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.settings_alt_icon),
                        summary = stringResource(id = R.string.settings_alt_icon_summary),
                        checked = useAltIcon,
                        startAction = {
                            Icon(
                                Icons.Rounded.SwapHoriz,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = {
                            prefs.edit { putBoolean("use_alt_icon", it) }
                            LauncherIconUtils.updateLauncherState(context)
                            useAltIcon = it
                        }
                    )
                }
            }

            // --- Section: Kernel Patch ---
            item {
                SmallTitle(text = stringResource(R.string.settings_section_kernel))
            }
            item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                    if (kPatchReady) {
                        val clearKeyDialogTitle = stringResource(id = R.string.clear_super_key)
                        val clearKeyDialogContent =
                            stringResource(id = R.string.settings_clear_super_key_dialog)
                        SuperArrow(
                            title = stringResource(R.string.clear_super_key),
                            summary = stringResource(R.string.clear_super_key_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.VpnKey,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onClick = { showClearKeyDialog.value = true }
                        )
                        if (showClearKeyDialog.value) {
                            SuperDialog(
                                show = showClearKeyDialog.value,
                                title = clearKeyDialogTitle,
                                summary = clearKeyDialogContent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        stringResource(id = android.R.string.cancel),
                                        onClick = { showClearKeyDialog.value = false },
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(20.dp))
                                    TextButton(
                                        stringResource(id = android.R.string.ok),
                                        onClick = {
                                            APatchKeyHelper.clearConfigKey()
                                            APApplication.superKey = ""
                                            showClearKeyDialog.value = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                    )
                                }
                            }
                        }
                    }

                    SuperSwitch(
                        title = stringResource(id = R.string.settings_donot_store_superkey),
                        summary = stringResource(id = R.string.settings_donot_store_superkey_summary),
                        checked = bSkipStoreSuperKey,
                        startAction = {
                            Icon(
                                Icons.Rounded.Lock,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = {
                            bSkipStoreSuperKey = it
                            APatchKeyHelper.setShouldSkipStoreSuperKey(bSkipStoreSuperKey)
                        }
                    )

                    if (kPatchReady && aPatchReady) {
                        SuperSwitch(
                            title = stringResource(id = R.string.settings_global_namespace_mode),
                            summary = stringResource(id = R.string.settings_global_namespace_mode_summary),
                            checked = isGlobalNamespaceEnabled,
                            startAction = {
                                Icon(
                                    Icons.Rounded.AccountTree,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onCheckedChange = {
                                setGlobalNamespaceEnabled(
                                    if (isGlobalNamespaceEnabled) "0" else "1"
                                )
                                isGlobalNamespaceEnabled = it
                            }
                        )
                    }

                    if (kPatchReady) {
                        SuperArrow(
                            title = stringResource(R.string.setting_reset_su_path),
                            summary = stringResource(R.string.setting_reset_su_path_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Restore,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onClick = { showResetSuPathDialog.value = true }
                        )
                    }
                }
            }

            // --- Section: Functions ---
            if (kPatchReady && aPatchReady) {
                item {
                    SmallTitle(text = stringResource(R.string.settings_section_module))
                }
                item {
                Card(modifier = Modifier.fillMaxWidth()) {
                        SuperSwitch(
                            title = stringResource(id = R.string.settings_magic_mount),
                            summary = stringResource(id = R.string.settings_magic_mount_summary),
                            checked = isMagicMountEnabled,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Extension,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onCheckedChange = {
                                setMagicMountEnabled(it)
                                isMagicMountEnabled = it
                            }
                        )

                        SuperSwitch(
                            title = stringResource(id = R.string.settings_hide_service),
                            summary = stringResource(id = R.string.settings_hide_service_summary),
                            checked = isHideServiceEnabled,
                            startAction = {
                                Icon(
                                    Icons.Rounded.VisibilityOff,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onCheckedChange = {
                                setHideServiceEnabled(it)
                                isHideServiceEnabled = it
                            }
                        )

                        SuperArrow(
                            title = stringResource(id = R.string.settings_umount_service),
                            summary = stringResource(id = R.string.settings_umount_service_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Eject,
                                    null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            onClick = {
                                navigator.navigate(UmountConfigScreenDestination)
                            }
                        )
                    }
                }
            }

            // --- Section: Behavior ---
            item {
                SmallTitle(text = stringResource(R.string.settings_section_general))
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    var stayOnPage by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("apm_action_stay_on_page", true))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.settings_apm_stay_on_page),
                        summary = stringResource(id = R.string.settings_apm_stay_on_page_summary),
                        checked = stayOnPage,
                        startAction = {
                            Icon(
                                Icons.AutoMirrored.Rounded.OpenInNew,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = {
                            prefs.edit { putBoolean("apm_action_stay_on_page", it) }
                            stayOnPage = it
                        }
                    )

                    var apmSortEnabled by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("apm_sort_enabled", true))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.settings_apm_sorting),
                        summary = stringResource(id = R.string.settings_apm_sorting_summary),
                        checked = apmSortEnabled,
                        startAction = {
                            Icon(
                                Icons.AutoMirrored.Rounded.Sort,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = {
                            prefs.edit { putBoolean("apm_sort_enabled", it) }
                            apmSortEnabled = it
                        }
                    )

                    var enableWebDebugging by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("enable_web_debugging", false))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.enable_web_debugging),
                        summary = stringResource(id = R.string.enable_web_debugging_summary),
                        checked = enableWebDebugging,
                        startAction = {
                            Icon(
                                Icons.Rounded.BugReport,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = { isChecked ->
                            enableWebDebugging = isChecked
                            APApplication.sharedPreferences.edit {
                                putBoolean("enable_web_debugging", isChecked)
                            }
                        }
                    )

                    var installConfirm by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("apm_install_confirm_enabled", true))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.settings_apm_install_confirm),
                        summary = stringResource(id = R.string.settings_apm_install_confirm_summary),
                        checked = installConfirm,
                        startAction = {
                            Icon(
                                Icons.Rounded.VerifiedUser,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = {
                            prefs.edit { putBoolean("apm_install_confirm_enabled", it) }
                            installConfirm = it
                        }
                    )

                    var hideAboutCard by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("hide_about_card", false))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.hide_about_card),
                        summary = stringResource(id = R.string.hide_about_card_summary),
                        checked = hideAboutCard,
                        startAction = {
                            Icon(
                                Icons.Rounded.HideImage,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = { isChecked ->
                            hideAboutCard = isChecked
                            prefs.edit { putBoolean("hide_about_card", isChecked) }
                        }
                    )

                    var checkUpdate by rememberSaveable {
                        mutableStateOf(prefs.getBoolean("check_update", true))
                    }
                    SuperSwitch(
                        title = stringResource(id = R.string.settings_check_update),
                        summary = stringResource(id = R.string.settings_check_update_summary),
                        checked = checkUpdate,
                        startAction = {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onCheckedChange = { isChecked ->
                            checkUpdate = isChecked
                            prefs.edit { putBoolean("check_update", isChecked) }
                        }
                    )
                }
            }

            // --- Section: Logs ---
            item {
                SmallTitle(text = stringResource(R.string.settings_section_about))
            }
            item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                    SuperArrow(
                        title = stringResource(R.string.send_log),
                        summary = stringResource(R.string.send_log_summary),
                        startAction = {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        },
                        onClick = {
                            showLogBottomSheet.value = true
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun LogBottomSheet(
    showLogBottomSheet: MutableState<Boolean>,
    scope: CoroutineScope,
    exportBugreportLauncher: ActivityResultLauncher<String>,
    loadingDialog: LoadingDialogHandle,
    context: Context
) {
    SuperDialog(
        show = showLogBottomSheet.value,
        title = "Save Log",
        onDismissRequest = { showLogBottomSheet.value = false }
    ) {
        Column {
            SuperArrow(
                title = stringResource(R.string.save_log),
                onClick = {
                    scope.launch {
                        val formatter =
                            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                        val current = LocalDateTime.now().format(formatter)
                        exportBugreportLauncher.launch("APatch_bugreport_${current}.tar.gz")
                        showLogBottomSheet.value = false
                    }
                }
            )
            SuperArrow(
                title = stringResource(R.string.send_log),
                onClick = {
                    scope.launch {
                        val bugreport = loadingDialog.withLoading {
                            withContext(Dispatchers.IO) {
                                getBugreportFile(context)
                            }
                        }

                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                            bugreport
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            setDataAndType(uri, "application/gzip")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.send_log)
                            )
                        )
                        showLogBottomSheet.value = false
                    }
                }
            )
        }
    }
}

@Composable
fun ResetSUPathDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    var suPath by remember { mutableStateOf(Natives.suPath()) }

    val suPathChecked: (path: String) -> Boolean = {
        it.startsWith("/") && it.trim().length > 1
    }

    SuperDialog(
        show = showDialog.value,
        title = stringResource(R.string.setting_reset_su_path),
        onDismissRequest = { showDialog.value = false }
    ) {
        TextField(
            value = suPath,
            onValueChange = { suPath = it },
            label = stringResource(R.string.setting_reset_su_new_path),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                stringResource(id = android.R.string.cancel),
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(id = android.R.string.ok),
                onClick = {
                    showDialog.value = false
                    val success = Natives.resetSuPath(suPath)
                    Toast.makeText(
                        context,
                        if (success) R.string.success else R.string.failure,
                        Toast.LENGTH_SHORT
                    ).show()
                    rootShellForResult("echo $suPath > ${APApplication.SU_PATH_FILE}")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = suPathChecked(suPath)
            )
        }
    }
}
