package me.bmax.apatch.ui.screen.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
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

    val appTitleTitle = stringResource(id = R.string.settings_app_title)
    val currentAppTitle = remember { prefs.getString("app_title", "folkpatch") }
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
    val dpiValue = if (currentDpiVal == -1) stringResource(id = R.string.system_default) else "$currentDpiVal DPI"

    val logTitle = stringResource(id = R.string.send_log)

    val folkXEngineTitle = stringResource(id = R.string.settings_folkx_engine_title)
    val folkXEngineSummary = stringResource(id = R.string.settings_folkx_engine_summary)

    val predictiveBackTitle = stringResource(id = R.string.settings_predictive_back)
    val predictiveBackSummary = stringResource(id = R.string.settings_predictive_back_summary)

    val appListLoadingSchemeTitle = stringResource(id = R.string.settings_app_list_loading_scheme)
    val currentScheme = remember { prefs.getString("app_list_loading_scheme", "root_service") }
    val currentSchemeLabel = if (currentScheme == "root_service") stringResource(R.string.app_list_loading_scheme_root_service) else stringResource(R.string.app_list_loading_scheme_package_manager)

    val blockUpdateTitle = stringResource(id = R.string.settings_block_kernelpatch_update)
    val blockUpdateSummary = stringResource(id = R.string.settings_block_kernelpatch_update_summary)

    val blockApUpdateTitle = stringResource(id = R.string.settings_block_androidpatch_update)
    val blockApUpdateSummary = stringResource(id = R.string.settings_block_androidpatch_update_summary)

    val showLanguageDialog = remember { mutableStateOf(false) }
    val showUpdateDialog = remember { mutableStateOf(false) }
    val showResetSuPathDialog = remember { mutableStateOf(false) }
    val showAppTitleDialog = remember { mutableStateOf(false) }
    val showCustomAppTitleDialog = remember { mutableStateOf(false) }
    val showDesktopAppNameDialog = remember { mutableStateOf(false) }
    val showDpiDialog = remember { mutableStateOf(false) }
    val showFolkXAnimationTypeDialog = remember { mutableStateOf(false) }
    val showFolkXAnimationSpeedDialog = remember { mutableStateOf(false) }
    val showAppListLoadingSchemeDialog = remember { mutableStateOf(false) }
    val showSELinuxModeDialog = remember { mutableStateOf(false) }

    var autoUpdateCheck by remember { mutableStateOf(prefs.getBoolean("auto_update_check", true)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        ExpressiveCard(flat = flat, onClick = { showLanguageDialog.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = languageTitle,
                        style = MaterialTheme.typography.titleMedium,
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

        ExpressiveCard(flat = flat, onClick = {
            scope.launch {
                loadingDialog.show()
                val hasUpdate = UpdateChecker.checkUpdate()
                loadingDialog.hide()
                if (hasUpdate) {
                    showUpdateDialog.value = true
                } else {
                    Toast.makeText(context, R.string.update_latest, Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = updateTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        ToggleSettingCard(
            flat = flat,
            title = autoUpdateTitle,
            description = autoUpdateSummary,
            checked = autoUpdateCheck,
            onCheckedChange = {
                autoUpdateCheck = it
                prefs.edit { putBoolean("auto_update_check", it) }
            }
        )

        var blockUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, false)) }
        ToggleSettingCard(
            flat = flat,
            title = blockUpdateTitle,
            description = blockUpdateSummary,
            checked = blockUpdateChecked,
            onCheckedChange = {
                blockUpdateChecked = it
                prefs.edit { putBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, it) }
            }
        )

        var blockApUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, false)) }
        ToggleSettingCard(
            flat = flat,
            title = blockApUpdateTitle,
            description = blockApUpdateSummary,
            checked = blockApUpdateChecked,
            onCheckedChange = {
                blockApUpdateChecked = it
                prefs.edit { putBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, it) }
            }
        )

        var folkXEngineEnabled by remember { mutableStateOf(prefs.getBoolean("folkx_engine_enabled", true)) }
        ToggleSettingCard(
            flat = flat,
            title = folkXEngineTitle,
            description = folkXEngineSummary,
            checked = folkXEngineEnabled,
            onCheckedChange = {
                folkXEngineEnabled = it
                prefs.edit().putBoolean("folkx_engine_enabled", it).apply()
            }
        )

        if (folkXEngineEnabled) {
            val currentType = remember { prefs.getString("folkx_animation_type", "linear") }
            val currentSpeed = remember { prefs.getFloat("folkx_animation_speed", 1.0f) }

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
                    Column {
                        Text(
                            text = stringResource(R.string.settings_folkx_animation_type),
                            style = MaterialTheme.typography.titleMedium,
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

            ExpressiveCard(flat = flat, onClick = { showFolkXAnimationSpeedDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_folkx_animation_speed),
                            style = MaterialTheme.typography.titleMedium,
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var predictiveBackEnabled by remember { mutableStateOf(prefs.getBoolean("predictive_back_enabled", true)) }
            ToggleSettingCard(
            flat = flat,
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

        if (kPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showAppListLoadingSchemeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = appListLoadingSchemeTitle,
                            style = MaterialTheme.typography.titleMedium,
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

        if (kPatchReady && aPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showSELinuxModeDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selinuxModeTitle,
                            style = MaterialTheme.typography.titleMedium,
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

            ToggleSettingCard(
            flat = flat,
                title = globalNamespaceTitle,
                description = globalNamespaceSummary,
                checked = isGlobalNamespaceEnabled,
                onCheckedChange = {
                    setGlobalNamespaceEnabled(if (isGlobalNamespaceEnabled) "0" else "1")
                    onGlobalNamespaceChange(it)
                }
            )
        }

        if (kPatchReady) {
            ExpressiveCard(flat = flat, onClick = { showResetSuPathDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resetSuPathTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        ExpressiveCard(flat = flat, onClick = { showAppTitleDialog.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = appTitleTitle,
                        style = MaterialTheme.typography.titleMedium,
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

        if (currentAppTitle == "custom") {
            ExpressiveCard(flat = flat, onClick = { showCustomAppTitleDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = customAppTitleTitle,
                            style = MaterialTheme.typography.titleMedium,
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

        ExpressiveCard(flat = flat, onClick = { showDesktopAppNameDialog.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = desktopAppNameTitle,
                        style = MaterialTheme.typography.titleMedium,
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

        ExpressiveCard(flat = flat, onClick = { showDpiDialog.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dpiTitle,
                        style = MaterialTheme.typography.titleMedium,
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
                Text(
                    text = logTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    LanguageDialog(showLanguageDialog)

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
        AppTitleChooseDialog(showAppTitleDialog)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(showLanguageDialog: MutableState<Boolean>) {

    val languages = stringArrayResource(id = R.array.languages)
    val languagesValues = stringArrayResource(id = R.array.languages_values)

    if (showLanguageDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showLanguageDialog.value = false }
        ) {
            Surface(
                modifier = Modifier
                    .width(150.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                LazyColumn {
                    itemsIndexed(languages) { index, item ->
                        ListItem(
                            headlineContent = { Text(item) },
                            modifier = Modifier.clickable {
                                showLanguageDialog.value = false
                                if (index == 0) {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.getEmptyLocaleList()
                                    )
                                } else {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(
                                            languagesValues[index]
                                        )
                                    )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpiChooseDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val activity = context as? Activity

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
                items(DPIUtils.presets) { preset ->
                    ListItem(
                        headlineContent = { Text(text = preset.name) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            DPIUtils.setDpi(context, preset.value)
                            activity?.recreate()
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
fun AppTitleChooseDialog(showDialog: MutableState<Boolean>) {
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
                        Toast.makeText(
                            context,
                            if (success) R.string.success else R.string.failure,
                            Toast.LENGTH_SHORT
                        ).show()
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
