package me.bmax.apatch.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.system.Os
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.UmountConfig
import me.bmax.apatch.ui.component.UmountConfigManager
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.isHideServiceEnabled as checkHideServiceEnabled
import me.bmax.apatch.util.isRealKernelPatchInstalled
import me.bmax.apatch.util.isUtsSpoofEnabled as checkUtsSpoofEnabled
import me.bmax.apatch.util.setUtsSpoofEnabled
import me.bmax.apatch.util.writeUtsSpoofConfig
import me.bmax.apatch.util.isPathHideEnabled as checkPathHideEnabled
import me.bmax.apatch.util.isPathHideUidModeEnabled as checkPathHideUidMode
import me.bmax.apatch.util.isPathHideFilterSystemEnabled as checkPathHideFilterSystem
import me.bmax.apatch.util.writePathHidePaths
import me.bmax.apatch.util.readPathHidePaths
import me.bmax.apatch.util.normalizePathHidePaths
import me.bmax.apatch.util.isNetIsolateEnabled as checkNetIsolateEnabled
import me.bmax.apatch.util.readNetIsolateUids
import me.bmax.apatch.util.writePathHideUids
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import me.bmax.apatch.ui.component.folk.FolkSettingsScaffold
import androidx.compose.ui.platform.LocalContext
import me.bmax.apatch.util.ShizukuServiceManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun FunctionSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    val settings = rememberFunctionSettingsState()
    var isHideServiceEnabled by settings.isHideServiceEnabled
    var isKernelSpoofEnabled by settings.isKernelSpoofEnabled
    var kernelSpoofVersion by settings.kernelSpoofVersion
    var kernelSpoofBuildTime by settings.kernelSpoofBuildTime
    var isUmountEnabled by settings.isUmountEnabled
    var umountPaths by settings.umountPaths
    var isNetIsolateEnabled by settings.isNetIsolateEnabled
    var niSelectedUids by settings.niSelectedUids
    var isPathHideEnabled by settings.isPathHideEnabled
    var pathHidePaths by settings.pathHidePaths
    var isPathHideUidMode by settings.isPathHideUidMode
    var isPathHideFilterSystem by settings.isPathHideFilterSystem
    val showFilterSystemWarningDialog = settings.showFilterSystemWarningDialog
    var selectedUids by settings.selectedUids
    var jailbreakEnabled by settings.jailbreakEnabled
    var showJailbreakSoftRebootDialog by settings.showJailbreakSoftRebootDialog

    // Shizuku 服务开关状态
    var isShizukuEnabled by settings.isShizukuEnabled
    var isShizukuRunning by settings.isShizukuRunning

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(kPatchReady, aPatchReady) {
        if (kPatchReady && aPatchReady) {
            withContext(Dispatchers.IO) {
                isHideServiceEnabled = checkHideServiceEnabled()
                val prefs = APApplication.sharedPreferences
                isKernelSpoofEnabled = prefs.getBoolean(APApplication.PREF_UTS_SPOOF_ENABLED, false)
                    && checkUtsSpoofEnabled()
                var savedRelease = prefs.getString(APApplication.PREF_UTS_SPOOF_RELEASE, "") ?: ""
                var savedBuildTime = prefs.getString(APApplication.PREF_UTS_SPOOF_VERSION, "") ?: ""
                if (savedRelease.isBlank() && savedBuildTime.isBlank()) {
                    // First use: seed the fields with the real values so the
                    // toggle never applies a silent no-op spoof.
                    val uname = Os.uname()
                    savedRelease = uname.release
                    savedBuildTime = uname.version
                }
                kernelSpoofVersion = savedRelease
                kernelSpoofBuildTime = savedBuildTime
                val umountConfig = UmountConfigManager.loadConfig(context)
                isUmountEnabled = umountConfig.enabled
                umountPaths = umountConfig.paths
                // Load netisolate state
                isNetIsolateEnabled = checkNetIsolateEnabled()
                val niUidSource = Natives.netIsolateUidList().ifBlank {
                    readNetIsolateUids()
                }
                niSelectedUids = niUidSource.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.toIntOrNull() }
                    .toSet()
                // Load pathhide state from kernel + config file
                isPathHideEnabled = checkPathHideEnabled()
                // Try to get paths from kernel first, fall back to config file
                val kernelPaths = Natives.pathHideList()
                if (kernelPaths.isNotBlank()) {
                    pathHidePaths = normalizePathHidePaths(kernelPaths)
                } else {
                    pathHidePaths = readPathHidePaths()
                }
                // Load UID mode state
                isPathHideUidMode = checkPathHideUidMode()
                isPathHideFilterSystem = checkPathHideFilterSystem()
                val pm = context.packageManager
                val uidSource = Natives.pathHideUidList().ifBlank {
                    me.bmax.apatch.util.readPathHideUids()
                }
                if (uidSource.isNotBlank()) {
                    val loaded = uidSource.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.toIntOrNull() }
                        .toMutableSet()
                    // Auto-cleanup: remove UIDs for uninstalled apps
                    val stale = loaded.filter { uid ->
                        uid > 0 && pm.getPackagesForUid(uid) == null
                    }
                    if (stale.isNotEmpty()) {
                        stale.forEach { loaded.remove(it) }
                        // Sync cleanup to kernel + file
                        Natives.pathHideUidClear()
                        loaded.forEach { Natives.pathHideUidAdd(it) }
                        writePathHideUids(loaded.joinToString("\n"))
                    }
                    selectedUids = loaded.toSet()
                }
            }
        }

        launch {
            snapshotFlow { kernelSpoofVersion to kernelSpoofBuildTime }
                .drop(1)
                .debounce(1000L)
                .collect { (version, buildTime) ->
                    val prefs = APApplication.sharedPreferences
                    prefs.edit()
                        .putString(APApplication.PREF_UTS_SPOOF_RELEASE, version)
                        .putString(APApplication.PREF_UTS_SPOOF_VERSION, buildTime)
                        .apply()
                    if (isKernelSpoofEnabled) {
                        setUtsSpoofEnabled(true)
                        writeUtsSpoofConfig(version, buildTime)
                        Natives.utsSet(version.ifBlank { null }, buildTime.ifBlank { null })
                    }
                }
        }

        launch {
            snapshotFlow { pathHidePaths }
                .drop(1)
                .debounce(1000L)
                .collect { paths ->
                    val normalizedPaths = normalizePathHidePaths(paths)
                    writePathHidePaths(normalizedPaths)
                    Natives.pathHideClear()
                    if (normalizedPaths.isNotBlank()) {
                        normalizedPaths.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { path -> Natives.pathHideAdd(path) }
                    }
                }
        }

        launch {
            snapshotFlow { umountPaths }
                .drop(1)
                .debounce(1000L)
                .collect { paths ->
                    withContext(Dispatchers.IO) {
                        UmountConfigManager.saveConfig(context, UmountConfig(enabled = isUmountEnabled, paths = paths))
                    }
                }
        }
    }

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null

    val actions = remember(settings, context, scope, snackBarHost) {
        FunctionSettingsActions(context, scope, snackBarHost, settings)
    }
    val serviceActions = remember(settings, context, scope, snackBarHost, navigator) {
        FunctionSettingsServiceActions(context, scope, snackBarHost, navigator, settings)
    }

    // 初始化 Shizuku 状态
    LaunchedEffect(kPatchReady, aPatchReady) {
        if (kPatchReady && aPatchReady) {
            withContext(Dispatchers.IO) {
                isShizukuRunning = ShizukuServiceManager.isServerRunning()
            }
        }
    }

    // 周期轮询 Shizuku Binder 状态；进程存在但 Binder 未就绪时不应显示为运行中。
    LaunchedEffect(kPatchReady, aPatchReady) {
        if (!(kPatchReady && aPatchReady)) return@LaunchedEffect
        while (true) {
            delay(8000)
            withContext(Dispatchers.IO) {
                val running = ShizukuServiceManager.isServerRunning()
                withContext(Dispatchers.Main) {
                    isShizukuRunning = running
                }
            }
        }
    }

    FolkSettingsScaffold(
        title = stringResource(R.string.settings_category_function),
        onBack = { navigator.popBackStack() },
        snackbarHostState = snackBarHost,
    ) {
            item {
                FunctionSettingsContent(
                    kPatchReady = kPatchReady,
                    aPatchReady = aPatchReady,
                    jailbreakEnabled = jailbreakEnabled,
                    jailbreakAvailable = !isRealKernelPatchInstalled(),
                    onJailbreakChange = serviceActions::onJailbreakChange,
                    isHideServiceEnabled = isHideServiceEnabled,
                    onHideServiceChange = actions::onHideServiceChange,
                    isKernelSpoofEnabled = isKernelSpoofEnabled,
                    onKernelSpoofChange = actions::onKernelSpoofChange,
                    kernelSpoofVersion = kernelSpoofVersion,
                    onKernelSpoofVersionChange = actions::onKernelSpoofVersionChange,
                    kernelSpoofBuildTime = kernelSpoofBuildTime,
                    onKernelSpoofBuildTimeChange = actions::onKernelSpoofBuildTimeChange,
                    onKernelSpoofSave = actions::onKernelSpoofSave,
                    onKernelSpoofRestore = actions::onKernelSpoofRestore,
                    snackBarHost = snackBarHost,
                    isPathHideEnabled = isPathHideEnabled,
                    onPathHideChange = actions::onPathHideChange,
                    pathHidePaths = pathHidePaths,
                    onPathHidePathsChange = actions::onPathHidePathsChange,
                    onPathHideSave = actions::onPathHideSave,
                    isPathHideUidMode = isPathHideUidMode,
                    onPathHideUidModeChange = actions::onPathHideUidModeChange,
                    isPathHideFilterSystem = isPathHideFilterSystem,
                    onPathHideFilterSystemChange = actions::onPathHideFilterSystemChange,
                    selectedUids = selectedUids,
                    onUidToggle = actions::onUidToggle,
                    onUidRemoveStale = {},
                    isUmountEnabled = isUmountEnabled,
                    onUmountEnabledChange = actions::onUmountEnabledChange,
                    umountPaths = umountPaths,
                    onUmountPathsChange = actions::onUmountPathsChange,
                    onUmountSave = actions::onUmountSave,
                    flat = flat,
                    highlightKey = highlightKey,
                    isNetIsolateEnabled = isNetIsolateEnabled,
                    onNetIsolateChange = actions::onNetIsolateChange,
                    niSelectedUids = niSelectedUids,
                    onNiUidToggle = actions::onNiUidToggle,
                    isShizukuEnabled = isShizukuEnabled,
                    isShizukuRunning = isShizukuRunning,
                    onShizukuToggle = serviceActions::onShizukuToggle,
                    onShizukuManage = serviceActions::onShizukuManage,
                )
            }
    }

    if (showFilterSystemWarningDialog.value) {
        PathHideFilterSystemWarningDialog(
            showDialog = showFilterSystemWarningDialog,
            onConfirm = actions::onConfirmFilterSystem,
        )
    }

    if (showJailbreakSoftRebootDialog) {
        AlertDialog(
            onDismissRequest = serviceActions::onJailbreakSoftRebootDismiss,
            title = { Text(stringResource(R.string.settings_jailbreak_restart_framework)) },
            text = { Text(stringResource(R.string.settings_jailbreak_restart_framework_message)) },
            confirmButton = {
                TextButton(
                    onClick = serviceActions::onJailbreakSoftRebootConfirm,
                ) {
                    Text(stringResource(R.string.settings_jailbreak_restart_framework))
                }
            },
            dismissButton = {
                TextButton(onClick = serviceActions::onJailbreakSoftRebootDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
