package me.bmax.apatch.ui.screen.settings

import androidx.compose.ui.Modifier
import androidx.compose.material.icons.automirrored.outlined.List
import me.bmax.apatch.util.ui.showToast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import kotlinx.coroutines.launch
import com.ramcosta.composedestinations.generated.destinations.LanguagePickerScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.UpdateDialog
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.util.*
import me.bmax.apatch.ui.screen.settings.general.*
import me.bmax.apatch.util.LauncherIconUtils

@Composable
fun GeneralSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    currentSELinuxMode: String,
    onSELinuxModeChange: (String) -> Unit,
    isGlobalNamespaceEnabled: Boolean,
    namespaceLoaded: Boolean,
    onGlobalNamespaceChange: (Boolean) -> Unit,
    isMagicMountEnabled: Boolean,
    onMagicMountChange: (Boolean) -> Unit,
    snackBarHost: SnackbarHostState,
    flat: Boolean = false,
    highlightKey: String? = null,
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val languageTitle = stringResource(id = R.string.settings_app_language)
    val languageValue = remember {
        val locale = AppCompatDelegate.getApplicationLocales()[0]
        if (locale == null) {
            context.getString(R.string.system_default)
        } else {
            val languageTag = locale.toLanguageTag()
            val languages = context.resources.getStringArray(R.array.languages)
            val languagesValues = context.resources.getStringArray(R.array.languages_values)

            // Prefer an exact match, then a bare language-code match
            // (e.g. "id" for "id-ID"), otherwise fall back to the raw tag.
            var index = languagesValues.indexOf(languageTag)
            if (index < 0) {
                index = languagesValues.indexOf(languageTag.substringBefore('-'))
            }
            if (index >= 0) languages[index] else languageTag
        }
    }

    val updateTitle = stringResource(id = R.string.settings_check_update)

    val autoUpdateTitle = stringResource(id = R.string.settings_auto_update_check)
    val autoUpdateSummary = stringResource(id = R.string.settings_auto_update_check_summary)

    val globalNamespaceTitle = stringResource(id = R.string.settings_global_namespace_mode)
    val globalNamespaceSummary = stringResource(id = R.string.settings_global_namespace_mode_summary)

    val magicMountTitle = stringResource(id = R.string.settings_magic_mount)
    val magicMountSummary = stringResource(id = R.string.settings_magic_mount_summary)

    val selinuxModeTitle = stringResource(id = R.string.settings_selinux_mode)
    val selinuxModeSummary = stringResource(id = R.string.settings_selinux_mode_summary)
    val selinuxModeValue = when (currentSELinuxMode) {
        "Enforcing" -> stringResource(R.string.settings_selinux_mode_enforcing)
        "Permissive" -> stringResource(R.string.settings_selinux_mode_permissive)
        else -> stringResource(R.string.home_selinux_status_unknown)
    }

    val resetSuPathTitle = stringResource(id = R.string.setting_reset_su_path)

    val launcherIconTitle = stringResource(id = R.string.settings_launcher_icon)
    var launcherIconStyle by remember {
        mutableStateOf(
            prefs.getString(LauncherIconUtils.PREF_ICON_STYLE, null)
                ?: if (prefs.getBoolean("use_alt_icon", false)) LauncherIconUtils.ICON_STYLE_APATCH
                else LauncherIconUtils.ICON_STYLE_ANIME
        )
    }
    val launcherIconSummary = when (launcherIconStyle) {
        LauncherIconUtils.ICON_STYLE_GEOMETRY -> stringResource(R.string.launcher_icon_style_geometry)
        LauncherIconUtils.ICON_STYLE_APATCH -> stringResource(R.string.launcher_icon_style_apatch)
        else -> stringResource(R.string.launcher_icon_style_anime)
    }

    val appTitleTitle = stringResource(id = R.string.settings_app_title)
    var currentAppTitle by remember { mutableStateOf(prefs.getString("app_title", "folkpatch") ?: "folkpatch") }
    val appTitleLabel = when (currentAppTitle) {
        "custom" -> remember { prefs.getString("custom_app_title", "FolkPatch") } ?: stringResource(R.string.app_title_custom)
        "fpatch" -> stringResource(R.string.app_title_fpatch)
        "apatch_folk" -> stringResource(R.string.app_title_apatch_folk)
        "apatchx" -> stringResource(R.string.app_title_apatchx)
        "apatch" -> stringResource(R.string.app_title_apatch)
        "kernelpatch" -> stringResource(R.string.app_title_kernelpatch)
        "kernelsu" -> stringResource(R.string.app_title_kernelsu)
        "supersu" -> stringResource(R.string.app_title_supersu)
        "folksu" -> stringResource(R.string.app_title_fpatch)
        "superuser" -> stringResource(R.string.app_title_superuser)
        "superpatch" -> stringResource(R.string.app_title_superpatch)
        "magicpatch" -> stringResource(R.string.app_title_magicpatch)
        else -> stringResource(R.string.app_title_folkpatch)
    }

    val customAppTitleTitle = stringResource(id = R.string.settings_custom_app_title)
    var currentCustomAppTitle by remember { mutableStateOf(prefs.getString("custom_app_title", "FolkPatch") ?: "FolkPatch") }

    val desktopAppNameTitle = stringResource(id = R.string.desktop_app_name)
    var currentDesktopAppName by remember { mutableStateOf(prefs.getString("desktop_app_name", "FolkPatch") ?: "FolkPatch") }

    val dpiTitle = stringResource(id = R.string.settings_app_dpi)
    val currentDpiVal = DPIUtils.currentDpi
    val dpiValue = if (currentDpiVal == DPIUtils.DEFAULT_DPI) stringResource(id = R.string.system_default) else "${DPIUtils.getDpiFriendlyName(currentDpiVal)} ($currentDpiVal DPI)"

    val logTitle = stringResource(id = R.string.send_log)

    val cleanStorageTitle = stringResource(id = R.string.settings_clear_cache)
    val cleanStorageSummary = stringResource(id = R.string.settings_clean_storage_summary)

    val predictiveBackTitle = stringResource(id = R.string.settings_predictive_back)
    val predictiveBackSummary = stringResource(id = R.string.settings_predictive_back_summary)

    val appListLoadingSchemeTitle = stringResource(id = R.string.settings_app_list_loading_scheme)
    var currentScheme by remember { mutableStateOf(prefs.getString("app_list_loading_scheme", "root_service") ?: "root_service") }
    val currentSchemeLabel = if (currentScheme == "root_service") stringResource(R.string.app_list_loading_scheme_root_service) else stringResource(R.string.app_list_loading_scheme_package_manager)
    val newAppProfileTitle = stringResource(id = R.string.settings_new_app_profile_mode)

    val blockUpdateTitle = stringResource(id = R.string.settings_block_kernelpatch_update)
    val blockUpdateSummary = stringResource(id = R.string.settings_block_kernelpatch_update_summary)

    val blockApUpdateTitle = stringResource(id = R.string.settings_block_androidpatch_update)
    val blockApUpdateSummary = stringResource(id = R.string.settings_block_androidpatch_update_summary)

    val showUpdateDialog = remember { mutableStateOf(false) }
    val showResetSuPathDialog = remember { mutableStateOf(false) }
    val showCleanStorageDialog = remember { mutableStateOf(false) }
    val showAppTitleDialog = remember { mutableStateOf(false) }
    val showCustomAppTitleDialog = remember { mutableStateOf(false) }
    val showDesktopAppNameDialog = remember { mutableStateOf(false) }
    val showDpiDialog = remember { mutableStateOf(false) }
    val showAppListLoadingSchemeDialog = remember { mutableStateOf(false) }
    val showNewAppProfileModeDialog = remember { mutableStateOf(false) }
    val showSELinuxModeDialog = remember { mutableStateOf(false) }

    val showLauncherIconDialog = remember { mutableStateOf(false) }
    var autoUpdateCheck by remember { mutableStateOf(prefs.getBoolean("auto_update_check", true)) }
    var blockUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, false)) }
    var blockApUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, false)) }
    var predictiveBackEnabled by remember { mutableStateOf(prefs.getBoolean("predictive_back_enabled", true)) }

    val newAppProfileEnabledTitle = stringResource(id = R.string.settings_new_app_profile_enabled)
    val newAppProfileEnabledSummary = stringResource(id = R.string.settings_new_app_profile_enabled_summary)
    var newAppProfileEnabled by remember {
        mutableStateOf(prefs.getBoolean(APApplication.PREF_NEW_APP_PROFILE_ENABLED, false))
    }
    var newAppProfileMode by remember {
        mutableIntStateOf(prefs.getInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0))
    }
    LaunchedEffect(Unit) {
        newAppProfileMode = loadNewAppProfileMode(prefs)
    }
    val currentNewAppProfileLabel = when (newAppProfileMode) {
        1 -> stringResource(R.string.settings_new_app_profile_root)
        2 -> stringResource(R.string.settings_new_app_profile_exclude)
        else -> stringResource(R.string.settings_new_app_profile_normal)
    }

    SegmentedSection(title = stringResource(R.string.settings_section_general_basics)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        item(key = "general_language") {
            SegmentedValueItem(
                icon = Icons.Outlined.Translate,
                title = languageTitle,
                summary = languageValue,
                modifier = Modifier.navOrigin(color = folkGroupColor()),
                onClick = { navigator.navigate(LanguagePickerScreenDestination) },
            )
        }

        item(key = "general_check_update") {
            SegmentedNavigationItem(
                icon = Icons.Outlined.Update,
                title = updateTitle,
                onClick = {
                    scope.launch {
                        loadingDialog.show()
                        val hasUpdate = UpdateChecker.checkUpdate()
                        loadingDialog.hide()
                        if (hasUpdate) {
                            showUpdateDialog.value = true
                        } else {
                            showToast(context, R.string.update_latest)
                        }
                    }
                },
            )
        }

        item(key = "general_auto_update") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Autorenew,
                title = autoUpdateTitle,
                summary = autoUpdateSummary,
                checked = autoUpdateCheck,
                onCheckedChange = {
                    autoUpdateCheck = it
                    prefs.edit { putBoolean("auto_update_check", it) }
                },
            )
        }

        item(key = "general_block_kp_update") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Block,
                title = blockUpdateTitle,
                summary = blockUpdateSummary,
                checked = blockUpdateChecked,
                onCheckedChange = {
                    blockUpdateChecked = it
                    prefs.edit { putBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, it) }
                },
            )
        }

        item(key = "general_block_ap_update") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Block,
                title = blockApUpdateTitle,
                summary = blockApUpdateSummary,
                checked = blockApUpdateChecked,
                onCheckedChange = {
                    blockApUpdateChecked = it
                    prefs.edit { putBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, it) }
                },
            )
        }

     }
    }

    GeneralInterfaceSection(
        flat = flat,
        highlightKey = highlightKey,
        predictiveBackTitle = predictiveBackTitle,
        predictiveBackSummary = predictiveBackSummary,
        predictiveBackEnabled = predictiveBackEnabled,
        onPredictiveBackEnabledChange = { predictiveBackEnabled = it },
    )

    if (kPatchReady) {
    SegmentedSection(title = stringResource(R.string.settings_section_general_root)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        item(key = "general_new_app_profile_enabled", visible = kPatchReady) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.AppRegistration,
                title = newAppProfileEnabledTitle,
                summary = newAppProfileEnabledSummary,
                checked = newAppProfileEnabled,
                onCheckedChange = {
                    if (it) {
                        val targetMode = prefs.getInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0)
                        val result = runCatching { Natives.setNewAppProfileMode(targetMode) }.getOrDefault(-1L)
                        if (result == 0L) {
                            newAppProfileEnabled = true
                            prefs.edit { putBoolean(APApplication.PREF_NEW_APP_PROFILE_ENABLED, true) }
                        } else {
                            newAppProfileEnabled = false
                            showToast(
                                context,
                                context.getString(R.string.settings_new_app_profile_update_failed, result.toString())
                            )
                        }
                    } else {
                        runCatching { Natives.setNewAppProfileMode(0) }
                        newAppProfileMode = 0
                        newAppProfileEnabled = false
                        prefs.edit {
                            putBoolean(APApplication.PREF_NEW_APP_PROFILE_ENABLED, false)
                            putInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0)
                        }
                    }
                },
            )
        }

        item(key = "general_new_app_profile", visible = kPatchReady && newAppProfileEnabled) {
            SegmentedValueItem(
                icon = Icons.Outlined.SettingsApplications,
                title = newAppProfileTitle,
                summary = currentNewAppProfileLabel,
                onClick = { showNewAppProfileModeDialog.value = true },
            )
        }

        item(key = "general_app_list_scheme", visible = kPatchReady) {
            SegmentedValueItem(
                icon = Icons.AutoMirrored.Outlined.List,
                title = appListLoadingSchemeTitle,
                summary = currentSchemeLabel,
                onClick = { showAppListLoadingSchemeDialog.value = true },
            )
        }

        item(key = "general_selinux_mode", visible = kPatchReady && aPatchReady) {
            SegmentedValueItem(
                icon = Icons.Outlined.Security,
                title = selinuxModeTitle,
                summary = stringResource(R.string.settings_selinux_current_mode, selinuxModeValue),
                onClick = { showSELinuxModeDialog.value = true },
            )
        }

        item(key = "general_global_namespace", visible = kPatchReady && aPatchReady) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Public,
                title = globalNamespaceTitle,
                summary = globalNamespaceSummary,
                checked = isGlobalNamespaceEnabled,
                enabled = namespaceLoaded,
                onCheckedChange = {
                    setGlobalNamespaceEnabled(if (isGlobalNamespaceEnabled) "0" else "1")
                    onGlobalNamespaceChange(it)
                },
            )
        }

     }
    }

    if (kPatchReady && aPatchReady) {
        GeneralMountSection(
            flat = flat,
            highlightKey = highlightKey,
            magicMountTitle = magicMountTitle,
            magicMountSummary = magicMountSummary,
            isMagicMountEnabled = isMagicMountEnabled,
            onMagicMountChange = onMagicMountChange,
            resetSuPathTitle = resetSuPathTitle,
            showResetSuPathDialog = showResetSuPathDialog,
        )

    }
    }
    GeneralIdentitySection(
        flat = flat,
        highlightKey = highlightKey,
        launcherIconTitle = launcherIconTitle,
        launcherIconSummary = launcherIconSummary,
        showLauncherIconDialog = showLauncherIconDialog,
        appTitleTitle = appTitleTitle,
        appTitleLabel = appTitleLabel,
        currentAppTitle = currentAppTitle,
        showAppTitleDialog = showAppTitleDialog,
        customAppTitleTitle = customAppTitleTitle,
        currentCustomAppTitle = currentCustomAppTitle,
        showCustomAppTitleDialog = showCustomAppTitleDialog,
        desktopAppNameTitle = desktopAppNameTitle,
        currentDesktopAppName = currentDesktopAppName,
        showDesktopAppNameDialog = showDesktopAppNameDialog,
        dpiTitle = dpiTitle,
        dpiValue = dpiValue,
        showDpiDialog = showDpiDialog,
    )

    GeneralMaintenanceSection(
        flat = flat,
        highlightKey = highlightKey,
        logTitle = logTitle,
        cleanStorageTitle = cleanStorageTitle,
        cleanStorageSummary = cleanStorageSummary,
        showCleanStorageDialog = showCleanStorageDialog,
    )

    if (showUpdateDialog.value) {
        UpdateDialog(
            onDismiss = { showUpdateDialog.value = false },
            onUpdate = {
                showUpdateDialog.value = false
                UpdateChecker.openUpdateUrl(context)
            }
        )
    }

    if (showResetSuPathDialog.value) {
        ResetSUPathDialog(showResetSuPathDialog)
    }

    if (showCleanStorageDialog.value) {
        CleanStorageDialog(showCleanStorageDialog, R.string.settings_clear_cache)
    }

    if (showSELinuxModeDialog.value) {
        SELinuxModeDialog(
            showDialog = showSELinuxModeDialog,
            currentMode = currentSELinuxMode,
            onModeChanged = onSELinuxModeChange
        )
    }

    if (showAppTitleDialog.value) {
        AppTitleChooseDialog(showAppTitleDialog) { newTitle ->
            currentAppTitle = newTitle
        }
    }

    if (showCustomAppTitleDialog.value) {
        CustomAppTitleDialog(showCustomAppTitleDialog, snackBarHost) { newTitle ->
            currentCustomAppTitle = newTitle
        }
    }

    if (showLauncherIconDialog.value) {
        LauncherIconStyleDialog(showLauncherIconDialog) { newStyle ->
            launcherIconStyle = newStyle
        }
    }

    if (showDesktopAppNameDialog.value) {
        DesktopAppNameChooseDialog(showDesktopAppNameDialog) { newName ->
            currentDesktopAppName = newName
        }
    }

    if (showDpiDialog.value) {
        DpiChooseDialog(showDpiDialog)
    }

    if (showAppListLoadingSchemeDialog.value) {
        AppListLoadingSchemeDialog(showAppListLoadingSchemeDialog) { newScheme ->
            currentScheme = newScheme
        }
    }

    if (showNewAppProfileModeDialog.value) {
        NewAppProfileModeDialog(showNewAppProfileModeDialog, newAppProfileMode) { mode ->
            newAppProfileMode = mode
            prefs.edit { putInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, mode) }
        }
    }
}

