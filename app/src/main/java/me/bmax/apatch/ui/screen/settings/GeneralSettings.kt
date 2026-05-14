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
import java.util.Locale

@Composable
fun GeneralSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    currentSELinuxMode: String,
    onSELinuxModeChange: (String) -> Unit,
    isGlobalNamespaceEnabled: Boolean,
    onGlobalNamespaceChange: (Boolean) -> Unit,
    isMagicMountEnabled: Boolean,
    onMagicMountChange: (Boolean) -> Unit,
    snackBarHost: SnackbarHostState,
    flat: Boolean = false,
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val languageTitle = stringResource(id = R.string.settings_app_language)
    val languageValue = AppCompatDelegate.getApplicationLocales()[0]?.displayLanguage?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    } ?: stringResource(id = R.string.system_default)

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
    val currentCustomAppTitle = remember { prefs.getString("custom_app_title", "FolkPatch") }

    val desktopAppNameTitle = stringResource(id = R.string.desktop_app_name)
    val currentDesktopAppName = remember { prefs.getString("desktop_app_name", "FolkPatch") }

    val dpiTitle = stringResource(id = R.string.settings_app_dpi)
    val currentDpiVal = DPIUtils.currentDpi
    val dpiValue = if (currentDpiVal == DPIUtils.DEFAULT_DPI) stringResource(id = R.string.system_default) else "${DPIUtils.getDpiFriendlyName(currentDpiVal)} ($currentDpiVal DPI)"

    val logTitle = stringResource(id = R.string.send_log)

    val folkXEngineTitle = stringResource(id = R.string.settings_folkx_engine_title)
    val folkXEngineSummary = stringResource(id = R.string.settings_folkx_engine_summary)

    val predictiveBackTitle = stringResource(id = R.string.settings_predictive_back)
    val predictiveBackSummary = stringResource(id = R.string.settings_predictive_back_summary)

    val appListLoadingSchemeTitle = stringResource(id = R.string.settings_app_list_loading_scheme)
    val currentScheme = remember { prefs.getString("app_list_loading_scheme", "root_service") }
    val currentSchemeLabel = if (currentScheme == "root_service") stringResource(R.string.app_list_loading_scheme_root_service) else stringResource(R.string.app_list_loading_scheme_package_manager)
    val newAppProfileTitle = stringResource(id = R.string.settings_new_app_profile_mode)

    val blockUpdateTitle = stringResource(id = R.string.settings_block_kernelpatch_update)
    val blockUpdateSummary = stringResource(id = R.string.settings_block_kernelpatch_update_summary)

    val blockApUpdateTitle = stringResource(id = R.string.settings_block_androidpatch_update)
    val blockApUpdateSummary = stringResource(id = R.string.settings_block_androidpatch_update_summary)

    val showUpdateDialog = remember { mutableStateOf(false) }
    val showResetSuPathDialog = remember { mutableStateOf(false) }
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
    val currentType = remember { prefs.getString("folkx_animation_type", "linear") }
    val currentSpeed = remember { prefs.getFloat("folkx_animation_speed", 1.0f) }
    var predictiveBackEnabled by remember { mutableStateOf(prefs.getBoolean("predictive_back_enabled", true)) }
    var newAppProfileMode by remember {
        mutableIntStateOf(loadNewAppProfileMode(prefs))
    }
    val currentNewAppProfileLabel = when (newAppProfileMode) {
        1 -> stringResource(R.string.settings_new_app_profile_root)
        2 -> stringResource(R.string.settings_new_app_profile_exclude)
        else -> stringResource(R.string.settings_new_app_profile_normal)
    }

    SplicedColumnGroup(flat = flat) {

        item {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = languageValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
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

        item {
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

        item {
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

        item {
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

        item(visible = folkXEngineEnabled) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(animationTypeLabel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(visible = folkXEngineEnabled) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${currentSpeed}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

        item(visible = kPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showNewAppProfileModeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = newAppProfileTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentNewAppProfileLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(visible = kPatchReady) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentSchemeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(visible = kPatchReady && aPatchReady) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_selinux_current_mode, selinuxModeValue),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(visible = kPatchReady && aPatchReady) {
            ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Public,
                title = globalNamespaceTitle,
                description = globalNamespaceSummary,
                checked = isGlobalNamespaceEnabled,
                onCheckedChange = {
                    setGlobalNamespaceEnabled(if (isGlobalNamespaceEnabled) "0" else "1")
                    onGlobalNamespaceChange(it)
                }
            )
        }

        item(visible = kPatchReady && aPatchReady) {
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

        item {
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

        item(visible = kPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showResetSuPathDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = resetSuPathTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = appTitleLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(visible = currentAppTitle == "custom") {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentCustomAppTitle ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentDesktopAppName.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dpiValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
        CustomAppTitleDialog(showCustomAppTitleDialog, snackBarHost)
    }

    if (showDesktopAppNameDialog.value) {
        DesktopAppNameChooseDialog(showDesktopAppNameDialog)
    }

    if (showDpiDialog.value) {
        DpiChooseDialog(showDpiDialog)
    }

    if (showFolkXAnimationTypeDialog.value) {
        FolkXAnimationTypeDialog(showFolkXAnimationTypeDialog)
    }

    if (showFolkXAnimationSpeedDialog.value) {
        FolkXAnimationSpeedDialog(showFolkXAnimationSpeedDialog)
    }

    if (showAppListLoadingSchemeDialog.value) {
        AppListLoadingSchemeDialog(showAppListLoadingSchemeDialog)
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
fun NewAppProfileModeDialog(
    showDialog: MutableState<Boolean>,
    initialMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val currentMode = remember(initialMode) { mutableIntStateOf(initialMode) }
    val options = listOf(
        0 to R.string.settings_new_app_profile_normal,
        1 to R.string.settings_new_app_profile_root,
        2 to R.string.settings_new_app_profile_exclude,
    )

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
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_new_app_profile_mode),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        options.forEach { (mode, labelId) ->
                            ListItem(
                                headlineContent = { Text(stringResource(labelId)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentMode.intValue == mode,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val result = Natives.setNewAppProfileMode(mode)
                                    if (result == 0L) {
                                        currentMode.intValue = mode
                                        onModeChanged(mode)
                                        showDialog.value = false
                                    } else {
                                        showToast(context, "New app profile update failed: $result")
                                    }
                                }
                            )
                        }
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

private fun loadNewAppProfileMode(prefs: SharedPreferences): Int {
    val fallback = runCatching {
        prefs.getInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0)
    }.getOrDefault(0)
    val nativeMode = runCatching {
        Natives.getNewAppProfileMode()
    }.getOrDefault(fallback)
    if (nativeMode != fallback) {
        prefs.edit { putInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, nativeMode) }
    }
    return nativeMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpiChooseDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val activity = context as? Activity

    val savedDpi = DPIUtils.currentDpi
    var tempDpi by remember { mutableIntStateOf(if (savedDpi == DPIUtils.DEFAULT_DPI) DPIUtils.systemDpi else savedDpi) }
    var isSystemDefault by remember { mutableStateOf(savedDpi == DPIUtils.DEFAULT_DPI) }

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
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_app_dpi),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(16.dp))

                // System default toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSystemDefault) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            isSystemDefault = !isSystemDefault
                            if (isSystemDefault) {
                                tempDpi = DPIUtils.systemDpi
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.system_default),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSystemDefault)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSystemDefault) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                AnimatedVisibility(visible = !isSystemDefault) {
                    Column {
                        Spacer(Modifier.height(16.dp))

                        // Slider
                        val sliderValue by animateFloatAsState(
                            targetValue = tempDpi.toFloat(),
                            label = "DpiSlider",
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                tempDpi = newValue.toInt()
                            },
                            valueRange = DPIUtils.DPI_MIN.toFloat()..DPIUtils.DPI_MAX.toFloat(),
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        // Current value display
                        Text(
                            text = "${DPIUtils.getDpiFriendlyName(tempDpi)} ($tempDpi DPI)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        // Preset pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DPIUtils.presets.forEach { preset ->
                                val isSelected = tempDpi == preset.value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { tempDpi = preset.value }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Apply button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalDpi = if (isSystemDefault) DPIUtils.DEFAULT_DPI else tempDpi
                            showDialog.value = false
                            DPIUtils.setDpi(context, finalDpi)
                            activity?.recreate()
                        },
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.dpi_apply_settings))
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
fun SELinuxModeDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(currentMode) }

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
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_selinux_mode),
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
                            headlineContent = { Text(stringResource(R.string.settings_selinux_mode_enforcing)) },
                            supportingContent = {
                                Text(
                                    text = stringResource(R.string.settings_selinux_mode_enforcing_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedMode == "Enforcing",
                                    onClick = { selectedMode = "Enforcing" }
                                )
                            },
                            modifier = Modifier.clickable { selectedMode = "Enforcing" }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_selinux_mode_permissive)) },
                            supportingContent = {
                                Text(
                                    text = stringResource(R.string.settings_selinux_mode_permissive_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedMode == "Permissive",
                                    onClick = { selectedMode = "Permissive" }
                                )
                            },
                            modifier = Modifier.clickable { selectedMode = "Permissive" }
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

                    Button(
                        onClick = {
                            val success = setSELinuxMode(selectedMode == "Enforcing")
                            if (success) {
                                onModeChanged(selectedMode)
                            }
                            showDialog.value = false
                        },
                        enabled = selectedMode != currentMode
                    ) {
                        Text(stringResource(id = android.R.string.ok))
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
fun AppTitleChooseDialog(showDialog: MutableState<Boolean>, onTitleChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    val currentTitle = remember { prefs.getString("app_title", "folkpatch") }
    val titles = listOf(
        "custom" to stringResource(R.string.app_title_custom),
        "fpatch" to stringResource(R.string.app_title_fpatch),
        "apatch_folk" to stringResource(R.string.app_title_apatch_folk),
        "apatchx" to stringResource(R.string.app_title_apatchx),
        "apatch" to stringResource(R.string.app_title_apatch),
        "folkpatch" to stringResource(R.string.app_title_folkpatch),
        "kernelpatch" to stringResource(R.string.app_title_kernelpatch),
        "kernelsu" to stringResource(R.string.app_title_kernelsu),
        "supersu" to stringResource(R.string.app_title_supersu),
        "folksu" to stringResource(R.string.app_title_fpatch),
        "superuser" to stringResource(R.string.app_title_superuser),
        "superpatch" to stringResource(R.string.app_title_superpatch),
        "magicpatch" to stringResource(R.string.app_title_magicpatch)
    )

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
                items(titles.size) { index ->
                    val (key, displayName) = titles[index]
                    ListItem(
                        headlineContent = { Text(text = displayName) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("app_title", key) }
                            onTitleChanged(key)
                        },
                        trailingContent = {
                            if (currentTitle == key) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppTitleDialog(showDialog: MutableState<Boolean>, snackBarHost: SnackbarHostState) {
    val prefs = APApplication.sharedPreferences
    var customTitle by remember {
        mutableStateOf(prefs.getString("custom_app_title", "FolkPatch") ?: "FolkPatch")
    }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight()
                .padding(24.dp),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.custom_app_title_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    placeholder = { Text(stringResource(R.string.custom_app_title_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        val trimmed = customTitle.trim()
                        if (trimmed.isEmpty()) {
                            showDialog.value = false
                            return@TextButton
                        }
                        prefs.edit { putString("custom_app_title", trimmed) }
                        showDialog.value = false
                    }) {
                        Text(stringResource(R.string.custom_app_title_dialog_confirm))
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
fun DesktopAppNameChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val currentName = remember { prefs.getString("desktop_app_name", "FolkPatch") }
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
                item {
                    ListItem(
                        headlineContent = { Text(text = "FolkPatch") },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit {
                                putString("desktop_app_name", "FolkPatch")
                            }
                            LauncherIconUtils.applySaved(context)
                        },
                        trailingContent = {
                            if (currentName == "FolkPatch" || currentName == null) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(text = "FPatch") },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit {
                                putString("desktop_app_name", "FPatch")
                            }
                            LauncherIconUtils.applySaved(context)
                        },
                        trailingContent = {
                            if (currentName == "FPatch") {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkXAnimationTypeDialog(showDialog: MutableState<Boolean>) {
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
                    text = stringResource(R.string.settings_folkx_animation_type),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentType = remember { prefs.getString("folkx_animation_type", "linear") }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        listOf("linear", "spatial", "fade", "vertical", "diagonal").forEach { type ->
                            val labelId = when (type) {
                                "linear" -> R.string.settings_folkx_animation_linear
                                "spatial" -> R.string.settings_folkx_animation_spatial
                                "fade" -> R.string.settings_folkx_animation_fade
                                "vertical" -> R.string.settings_folkx_animation_vertical
                                "diagonal" -> R.string.settings_folkx_animation_diagonal
                                else -> R.string.settings_folkx_animation_linear
                            }
                            ListItem(
                                headlineContent = { Text(stringResource(labelId)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentType == type,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    prefs.edit().putString("folkx_animation_type", type).apply()
                                    showDialog.value = false
                                }
                            )
                        }
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
fun AppListLoadingSchemeDialog(showDialog: MutableState<Boolean>) {
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
                    text = stringResource(R.string.settings_app_list_loading_scheme),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentScheme = remember { prefs.getString("app_list_loading_scheme", "root_service") }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        val schemes = listOf(
                            "root_service" to R.string.app_list_loading_scheme_root_service,
                            "package_manager" to R.string.app_list_loading_scheme_package_manager
                        )

                        schemes.forEach { (scheme, labelId) ->
                            ListItem(
                                headlineContent = { Text(stringResource(labelId)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentScheme == scheme,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    prefs.edit { putString("app_list_loading_scheme", scheme) }
                                    showDialog.value = false
                                }
                            )
                        }
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
fun ResetSUPathDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    var suPath by remember { mutableStateOf(me.bmax.apatch.Natives.suPath()) }
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
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.setting_reset_su_path),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 12.dp))
                        .align(Alignment.Start)
                ) {
                    OutlinedTextField(
                        value = suPath,
                        onValueChange = {
                            suPath = it
                        },
                        label = { Text(stringResource(id = R.string.setting_reset_su_new_path)) },
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {

                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(enabled = suPath.startsWith("/") && suPath.trim().length > 1, onClick = {
                        showDialog.value = false
                        val success = me.bmax.apatch.Natives.resetSuPath(suPath)
                        showToast(context, if (success) R.string.success else R.string.failure)
                        rootShellForResult("echo $suPath > ${APApplication.SU_PATH_FILE}")
                    }) {
                        Text(stringResource(id = android.R.string.ok))
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
fun FolkXAnimationSpeedDialog(showDialog: MutableState<Boolean>) {
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
                    text = stringResource(R.string.settings_folkx_animation_speed),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentSpeed = remember { prefs.getFloat("folkx_animation_speed", 1.0f) }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        val speeds = listOf(
                            0.5f to "0.5x",
                            0.75f to "0.75x",
                            1.0f to "1.0x",
                            1.25f to "1.25x",
                            1.5f to "1.5x",
                            2.0f to "2.0x"
                        )

                        speeds.forEach { (speed, label) ->
                            ListItem(
                                headlineContent = { Text(label) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentSpeed == speed,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    prefs.edit().putFloat("folkx_animation_speed", speed).apply()
                                    showDialog.value = false
                                }
                            )
                        }
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
