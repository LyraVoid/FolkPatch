package me.bmax.apatch.ui.screen.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import me.bmax.apatch.util.ui.showToast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ramcosta.composedestinations.generated.destinations.LanguagePickerScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.component.UpdateDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.util.*
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.ui.screen.settings.general.*
import java.io.File
import java.util.Locale

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

    val launcherIconTitle = stringResource(id = R.string.settings_alt_icon)
    val launcherIconSummary = stringResource(id = R.string.alt_icon_summary)

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

    val cleanStorageTitle = stringResource(id = R.string.settings_clean_storage)
    val cleanStorageSummary = stringResource(id = R.string.settings_clean_storage_summary)

    val folkXEngineTitle = stringResource(id = R.string.settings_folkx_engine_title)
    val folkXEngineSummary = stringResource(id = R.string.settings_folkx_engine_summary)

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
    val showFolkXAnimationTypeDialog = remember { mutableStateOf(false) }
    val showFolkXAnimationSpeedDialog = remember { mutableStateOf(false) }
    val showAppListLoadingSchemeDialog = remember { mutableStateOf(false) }
    val showNewAppProfileModeDialog = remember { mutableStateOf(false) }
    val showSELinuxModeDialog = remember { mutableStateOf(false) }

    val useAltIcon = remember { mutableStateOf(prefs.getBoolean("use_alt_icon", false)) }
    var autoUpdateCheck by remember { mutableStateOf(prefs.getBoolean("auto_update_check", true)) }
    var blockUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, false)) }
    var blockApUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, false)) }
    var folkXEngineEnabled by remember { mutableStateOf(prefs.getBoolean("folkx_engine_enabled", true)) }
    var currentType by remember { mutableStateOf(prefs.getString("folkx_animation_type", "linear") ?: "linear") }
    var currentSpeed by remember { mutableStateOf(prefs.getFloat("folkx_animation_speed", 1.0f)) }
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

    SplicedColumnGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_language") {
            ExpressiveCard(flat = flat, onClick = { navigator.navigate(LanguagePickerScreenDestination) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = languageTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = languageValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_check_update") {
            ExpressiveCard(flat = flat, onClick = {
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
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Update, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = updateTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item(key = "general_auto_update") {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Autorenew,
            title = autoUpdateTitle,
            description = autoUpdateSummary,
            checked = autoUpdateCheck,
            onCheckedChange = {
                autoUpdateCheck = it
                prefs.edit { putBoolean("auto_update_check", it) }
            }
        )
        }

        item(key = "general_block_kp_update") {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Block,
            title = blockUpdateTitle,
            description = blockUpdateSummary,
            checked = blockUpdateChecked,
            onCheckedChange = {
                blockUpdateChecked = it
                prefs.edit { putBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, it) }
            }
        )
        }

        item(key = "general_block_ap_update") {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Block,
            title = blockApUpdateTitle,
            description = blockApUpdateSummary,
            checked = blockApUpdateChecked,
            onCheckedChange = {
                blockApUpdateChecked = it
                prefs.edit { putBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, it) }
            }
        )
        }

        item(key = "general_folkx_engine") {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.AutoAwesome,
            title = folkXEngineTitle,
            description = folkXEngineSummary,
            checked = folkXEngineEnabled,
            onCheckedChange = {
                folkXEngineEnabled = it
                prefs.edit().putBoolean("folkx_engine_enabled", it).apply()
            }
        )
        }

        item(key = "general_folkx_animation_type", visible = folkXEngineEnabled) {
            val animationTypeLabel = when (currentType) {
                "linear" -> R.string.settings_folkx_animation_linear
                "spatial" -> R.string.settings_folkx_animation_spatial
                "fade" -> R.string.settings_folkx_animation_fade
                "vertical" -> R.string.settings_folkx_animation_vertical
                "diagonal" -> R.string.settings_folkx_animation_diagonal
                else -> R.string.settings_folkx_animation_linear
            }

            ExpressiveCard(flat = flat, onClick = { showFolkXAnimationTypeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_folkx_animation_type),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(animationTypeLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_folkx_animation_speed", visible = folkXEngineEnabled) {
            ExpressiveCard(flat = flat, onClick = { showFolkXAnimationSpeedDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_folkx_animation_speed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${currentSpeed}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_predictive_back", visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.ArrowBack,
                title = predictiveBackTitle,
                description = predictiveBackSummary,
                checked = predictiveBackEnabled,
                onCheckedChange = {
                    predictiveBackEnabled = it
                    prefs.edit { putBoolean("predictive_back_enabled", it) }
                    (context as? Activity)?.recreate()
                }
            )
        }

        item(key = "general_new_app_profile_enabled", visible = kPatchReady) {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.AppRegistration,
                title = newAppProfileEnabledTitle,
                description = newAppProfileEnabledSummary,
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
                }
            )
        }

        item(key = "general_new_app_profile", visible = kPatchReady && newAppProfileEnabled) {
            ExpressiveCard(flat = flat, onClick = { showNewAppProfileModeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.SettingsApplications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = newAppProfileTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentNewAppProfileLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_app_list_scheme", visible = kPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showAppListLoadingSchemeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = appListLoadingSchemeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentSchemeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_selinux_mode", visible = kPatchReady && aPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showSELinuxModeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = selinuxModeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_selinux_current_mode, selinuxModeValue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_global_namespace", visible = kPatchReady && aPatchReady) {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Public,
                title = globalNamespaceTitle,
                description = globalNamespaceSummary,
                checked = isGlobalNamespaceEnabled,
                enabled = namespaceLoaded,
                onCheckedChange = {
                    setGlobalNamespaceEnabled(if (isGlobalNamespaceEnabled) "0" else "1")
                    onGlobalNamespaceChange(it)
                }
            )
        }

        item(key = "general_magic_mount", visible = kPatchReady && aPatchReady) {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.FolderSpecial,
                title = magicMountTitle,
                description = magicMountSummary,
                checked = isMagicMountEnabled,
                onCheckedChange = {
                    setMagicMountEnabled(it)
                    onMagicMountChange(it)
                }
            )
        }

        item(key = "general_alt_icon") {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Android,
            title = launcherIconTitle,
            description = launcherIconSummary,
            checked = useAltIcon.value,
            onCheckedChange = {
                prefs.edit { putBoolean("use_alt_icon", it) }
                LauncherIconUtils.updateLauncherState(context)
                useAltIcon.value = it
            }
        )
        }

        item(key = "general_sucompat", visible = kPatchReady && aPatchReady) {
            var sucompatEnabled by remember { mutableStateOf(prefs.getBoolean("sucompat_enabled", false)) }
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.FeaturedPlayList,
                title = stringResource(id = R.string.settings_sucompat),
                description = stringResource(id = R.string.settings_sucompat_summary),
                checked = sucompatEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        val result = if (enabled) {
                            // Enable: create marker file and register hooks via supercall
                            rootShellForResult("touch ${APApplication.SUCOMPAT_FILE}")
                            Natives.controlFeature("sucompat_extra", true)
                        } else {
                            // Disable: remove marker file and unregister hooks via supercall
                            rootShellForResult("rm -f ${APApplication.SUCOMPAT_FILE}")
                            Natives.controlFeature("sucompat_extra", false)
                        }
                        if (result == 0L) {
                            sucompatEnabled = enabled
                            prefs.edit().putBoolean("sucompat_enabled", enabled).apply()
                        }
                    }
                }
            )
        }
        item(key = "general_selinux_hide", visible = kPatchReady && aPatchReady) {
            val kernelVersion = remember { getKernelVersionCode() }
            val kernelSupported = (kernelVersion ?: 0) >= 419
            val isGki = remember { isGkiKernel() }
            var selinuxHideEnabled by rememberSaveable {
                mutableStateOf(prefs.getBoolean("selinux_hide_enabled", false))
            }
            val showSelinuxHideWarning = remember { mutableStateOf(false) }

            fun applySelinuxHide(enabled: Boolean) {
                scope.launch(Dispatchers.IO) {
                    val command = if (enabled) {
                        "touch ${APApplication.SELINUX_HIDE_FILE}"
                    } else {
                        "rm -f ${APApplication.SELINUX_HIDE_FILE}"
                    }
                    val result = rootShellForResult(command)
                    if (result.isSuccess) {
                        selinuxHideEnabled = enabled
                        prefs.edit().putBoolean("selinux_hide_enabled", enabled).apply()
                    }
                }
            }

            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Security,
                title = stringResource(id = R.string.settings_selinux_hide),
                description = stringResource(id = R.string.settings_selinux_hide_summary),
                checked = selinuxHideEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        // Only tested on 5.10+, and non-GKI carries a bigger risk, so warn first.
                        val below510 = (kernelVersion ?: 0) < 510
                        if (below510 || !isGki) {
                            showSelinuxHideWarning.value = true
                        } else {
                            applySelinuxHide(true)
                        }
                    } else {
                        applySelinuxHide(false)
                    }
                },
                enabled = kernelSupported,
            )

            if (showSelinuxHideWarning.value) {
                SelinuxHideWarningDialog(
                    showDialog = showSelinuxHideWarning,
                    kernelVersion = kernelVersion,
                    isGki = isGki,
                    onConfirm = { applySelinuxHide(true) },
                )
            }
        }
        item(key = "general_reset_su_path", visible = kPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showResetSuPathDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = resetSuPathTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item(key = "general_app_title") {
            ExpressiveCard(flat = flat, onClick = { showAppTitleDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = appTitleTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = appTitleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_custom_app_title", visible = currentAppTitle == "custom") {
            ExpressiveCard(flat = flat, onClick = { showCustomAppTitleDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = customAppTitleTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentCustomAppTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_desktop_app_name") {
            ExpressiveCard(flat = flat, onClick = { showDesktopAppNameDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = desktopAppNameTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentDesktopAppName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_dpi") {
            ExpressiveCard(flat = flat, onClick = { showDpiDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = dpiTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dpiValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "general_send_log") {
            ExpressiveCard(flat = flat, onClick = {
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
                        type = "application/gzip"
                        clipData = android.content.ClipData.newRawUri(null, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.send_log)
                        )
                    )
                }
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = logTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item(key = "general_clean_storage") {
            ExpressiveCard(flat = flat, onClick = { showCleanStorageDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = cleanStorageTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = cleanStorageSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

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
        CleanStorageDialog(showCleanStorageDialog)
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

    if (showDesktopAppNameDialog.value) {
        DesktopAppNameChooseDialog(showDesktopAppNameDialog) { newName ->
            currentDesktopAppName = newName
        }
    }

    if (showDpiDialog.value) {
        DpiChooseDialog(showDpiDialog)
    }

    if (showFolkXAnimationTypeDialog.value) {
        FolkXAnimationTypeDialog(showFolkXAnimationTypeDialog) { newType ->
            currentType = newType
        }
    }

    if (showFolkXAnimationSpeedDialog.value) {
        FolkXAnimationSpeedDialog(showFolkXAnimationSpeedDialog) { newSpeed ->
            currentSpeed = newSpeed
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelinuxHideWarningDialog(
    showDialog: MutableState<Boolean>,
    kernelVersion: Int?,
    isGki: Boolean,
    onConfirm: () -> Unit,
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
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Text(
                    text = stringResource(id = R.string.settings_selinux_hide_warning_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                if ((kernelVersion ?: 0) < 510) {
                    Text(
                        text = stringResource(id = R.string.settings_selinux_hide_warning_below_5_10),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (!isGki) {
                    Text(
                        text = stringResource(id = R.string.settings_selinux_hide_warning_non_gki),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        showDialog.value = false
                        onConfirm()
                    }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
    }
}
