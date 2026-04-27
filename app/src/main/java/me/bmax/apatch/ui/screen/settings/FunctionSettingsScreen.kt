package me.bmax.apatch.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import me.bmax.apatch.util.isUtsSpoofEnabled as checkUtsSpoofEnabled
import me.bmax.apatch.util.setUtsSpoofEnabled
import me.bmax.apatch.util.writeUtsSpoofConfig
import me.bmax.apatch.util.removeUtsSpoofConfig
import me.bmax.apatch.util.isPathHideEnabled as checkPathHideEnabled
import me.bmax.apatch.util.isPathHideUidModeEnabled as checkPathHideUidMode
import me.bmax.apatch.util.isPathHideFilterSystemEnabled as checkPathHideFilterSystem
import me.bmax.apatch.util.setPathHideEnabled
import me.bmax.apatch.util.writePathHidePaths
import me.bmax.apatch.util.readPathHidePaths
import me.bmax.apatch.util.isNetIsolateEnabled as checkNetIsolateEnabled
import me.bmax.apatch.util.readNetIsolateUids
import me.bmax.apatch.util.setNetIsolateEnabled
import me.bmax.apatch.util.writeNetIsolateUids
import me.bmax.apatch.util.writePathHideUids
import me.bmax.apatch.util.setPathHideUidMode
import me.bmax.apatch.util.setPathHideFilterSystem
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.ui.platform.LocalContext
import me.bmax.apatch.util.ui.showToast
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun FunctionSettingsScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    var isHideServiceEnabled by rememberSaveable { mutableStateOf(false) }
    var isKernelSpoofEnabled by rememberSaveable { mutableStateOf(false) }
    var kernelSpoofVersion by rememberSaveable { mutableStateOf("") }
    var kernelSpoofBuildTime by rememberSaveable { mutableStateOf("") }
    var isUmountEnabled by rememberSaveable { mutableStateOf(false) }
    var umountPaths by rememberSaveable { mutableStateOf("") }
    var isNetIsolateEnabled by rememberSaveable { mutableStateOf(false) }
    var niSelectedUids by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    var isPathHideEnabled by rememberSaveable { mutableStateOf(false) }
    var pathHidePaths by rememberSaveable { mutableStateOf("") }
    var isPathHideUidMode by rememberSaveable { mutableStateOf(false) }
    var isPathHideFilterSystem by rememberSaveable { mutableStateOf(false) }
    val showFilterSystemWarningDialog = rememberSaveable { mutableStateOf(false) }
    var selectedUids by rememberSaveable { mutableStateOf(emptySet<Int>()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(kPatchReady, aPatchReady) {
        if (kPatchReady && aPatchReady) {
            withContext(Dispatchers.IO) {
                isHideServiceEnabled = checkHideServiceEnabled()
                val prefs = APApplication.sharedPreferences
                isKernelSpoofEnabled = prefs.getBoolean(APApplication.PREF_UTS_SPOOF_ENABLED, false)
                    && checkUtsSpoofEnabled()
                kernelSpoofVersion = prefs.getString(APApplication.PREF_UTS_SPOOF_RELEASE, "") ?: ""
                kernelSpoofBuildTime = prefs.getString(APApplication.PREF_UTS_SPOOF_VERSION, "") ?: ""
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
                    pathHidePaths = kernelPaths.trimEnd('\n')
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
                    writePathHidePaths(paths)
                    Natives.pathHideClear()
                    if (paths.isNotBlank()) {
                        paths.lines()
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
                    UmountConfigManager.saveConfig(context, UmountConfig(enabled = isUmountEnabled, paths = paths))
                }
        }
    }

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_category_function), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackBarHost) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                FunctionSettingsContent(
                    kPatchReady = kPatchReady,
                    aPatchReady = aPatchReady,
                    isHideServiceEnabled = isHideServiceEnabled,
                    onHideServiceChange = { isHideServiceEnabled = it },
                    isKernelSpoofEnabled = isKernelSpoofEnabled,
                    onKernelSpoofChange = { enabled ->
                        isKernelSpoofEnabled = enabled
                        scope.launch(Dispatchers.IO) {
                            val prefs = APApplication.sharedPreferences
                            prefs.edit().putBoolean(APApplication.PREF_UTS_SPOOF_ENABLED, enabled).apply()
                            if (enabled) {
                                setUtsSpoofEnabled(true)
                                val savedRelease = prefs.getString(APApplication.PREF_UTS_SPOOF_RELEASE, "") ?: ""
                                val savedVersion = prefs.getString(APApplication.PREF_UTS_SPOOF_VERSION, "") ?: ""
                                writeUtsSpoofConfig(savedRelease, savedVersion)
                                Natives.utsSet(savedRelease.ifBlank { null }, savedVersion.ifBlank { null })
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar(context.getString(R.string.kernel_spoof_enabled))
                                }
                            } else {
                                Natives.utsReset()
                                setUtsSpoofEnabled(false)
                                removeUtsSpoofConfig()
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar(context.getString(R.string.kernel_spoof_disabled_restored))
                                }
                            }
                        }
                    },
                    kernelSpoofVersion = kernelSpoofVersion,
                    onKernelSpoofVersionChange = { kernelSpoofVersion = it },
                    kernelSpoofBuildTime = kernelSpoofBuildTime,
                    onKernelSpoofBuildTimeChange = { kernelSpoofBuildTime = it },
                    onKernelSpoofSave = {
                        val currentEnabled = isKernelSpoofEnabled
                        val currentVersion = kernelSpoofVersion
                        val currentBuildTime = kernelSpoofBuildTime
                        scope.launch(Dispatchers.IO) {
                            val prefs = APApplication.sharedPreferences
                            prefs.edit()
                                .putBoolean(APApplication.PREF_UTS_SPOOF_ENABLED, currentEnabled)
                                .putString(APApplication.PREF_UTS_SPOOF_RELEASE, currentVersion)
                                .putString(APApplication.PREF_UTS_SPOOF_VERSION, currentBuildTime)
                                .apply()

                            if (currentEnabled) {
                                setUtsSpoofEnabled(true)
                                writeUtsSpoofConfig(currentVersion, currentBuildTime)
                                val rc = Natives.utsSet(
                                    currentVersion.ifBlank { null },
                                    currentBuildTime.ifBlank { null }
                                )
                                withContext(Dispatchers.Main) {
                                    if (rc < 0) {
                                        snackBarHost.showSnackbar(context.getString(R.string.kernel_spoof_failed, rc))
                                    } else {
                                        snackBarHost.showSnackbar(context.getString(R.string.kernel_spoof_applied))
                                    }
                                }
                            } else {
                                Natives.utsReset()
                                setUtsSpoofEnabled(false)
                                removeUtsSpoofConfig()
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar(context.getString(R.string.kernel_spoof_disabled_restored))
                                }
                            }
                        }
                    },
                    onKernelSpoofRestore = {
                        scope.launch(Dispatchers.IO) {
                            Natives.utsReset()
                            val uname = Os.uname()
                            val realRelease = uname.release
                            val realVersion = uname.version
                            withContext(Dispatchers.Main) {
                                kernelSpoofVersion = realRelease
                                kernelSpoofBuildTime = realVersion
                            }
                            if (isKernelSpoofEnabled) {
                                val prefs = APApplication.sharedPreferences
                                val savedRelease = prefs.getString(APApplication.PREF_UTS_SPOOF_RELEASE, "") ?: ""
                                val savedVersion = prefs.getString(APApplication.PREF_UTS_SPOOF_VERSION, "") ?: ""
                                if (savedRelease.isNotBlank() || savedVersion.isNotBlank()) {
                                    Natives.utsSet(
                                        savedRelease.ifBlank { null },
                                        savedVersion.ifBlank { null }
                                    )
                                }
                            }
                        }
                    },
                    snackBarHost = snackBarHost,
                    isPathHideEnabled = isPathHideEnabled,
                    onPathHideChange = { enabled ->
                        isPathHideEnabled = enabled
                        scope.launch(Dispatchers.IO) {
                            setPathHideEnabled(enabled)
                            val rc = Natives.pathHideEnable(enabled)
                            withContext(Dispatchers.Main) {
                                if (rc < 0) {
                                    snackBarHost.showSnackbar(context.getString(R.string.path_hide_failed, rc.toInt()))
                                } else {
                                    snackBarHost.showSnackbar(
                                        context.getString(if (enabled) R.string.path_hide_enabled else R.string.path_hide_disabled)
                                    )
                                }
                            }
                        }
                    },
                    pathHidePaths = pathHidePaths,
                    onPathHidePathsChange = { pathHidePaths = it },
                    onPathHideSave = {
                        val currentPaths = pathHidePaths
                        scope.launch(Dispatchers.IO) {
                            // Save to config file for persistence
                            writePathHidePaths(currentPaths)
                            // Clear existing kernel paths and re-add
                            Natives.pathHideClear()
                            if (currentPaths.isNotBlank()) {
                                currentPaths.lines()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach { path ->
                                        Natives.pathHideAdd(path)
                                    }
                            }
                            withContext(Dispatchers.Main) {
                                snackBarHost.showSnackbar(context.getString(R.string.path_hide_applied))
                            }
                        }
                    },
                    isPathHideUidMode = isPathHideUidMode,
                    onPathHideUidModeChange = { enabled ->
                        isPathHideUidMode = enabled
                        scope.launch(Dispatchers.IO) {
                            setPathHideUidMode(enabled)
                            Natives.pathHideUidMode(enabled)
                            withContext(Dispatchers.Main) {
                                snackBarHost.showSnackbar(
                                    context.getString(if (enabled) R.string.path_hide_uid_mode_enabled else R.string.path_hide_uid_mode_disabled)
                                )
                            }
                        }
                    },
                    isPathHideFilterSystem = isPathHideFilterSystem,
                    onPathHideFilterSystemChange = { enabled ->
                        if (enabled) {
                            showFilterSystemWarningDialog.value = true
                        } else {
                            isPathHideFilterSystem = false
                            scope.launch(Dispatchers.IO) {
                                setPathHideFilterSystem(false)
                                Natives.pathHideFilterSystem(false)
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar(
                                        context.getString(R.string.path_hide_filter_system_disabled)
                                    )
                                }
                            }
                        }
                    },
                    selectedUids = selectedUids,
                    onUidToggle = { uid ->
                        scope.launch(Dispatchers.IO) {
                            val newSet = if (uid in selectedUids) {
                                Natives.pathHideUidRemove(uid)
                                selectedUids - uid
                            } else {
                                Natives.pathHideUidAdd(uid)
                                selectedUids + uid
                            }
                            writePathHideUids(newSet.joinToString("\n"))
                            withContext(Dispatchers.Main) {
                                selectedUids = newSet
                            }
                        }
                    },
                    onUidRemoveStale = {},
                    isUmountEnabled = isUmountEnabled,
                    onUmountEnabledChange = { enabled ->
                        isUmountEnabled = enabled
                        scope.launch(Dispatchers.IO) {
                            val config = UmountConfig(enabled = enabled, paths = umountPaths)
                            val success = UmountConfigManager.saveConfig(context, config)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    showToast(context, context.getString(R.string.umount_config_save_success))
                                } else {
                                    showToast(context, context.getString(R.string.umount_config_save_failed))
                                }
                            }
                        }
                    },
                    umountPaths = umountPaths,
                    onUmountPathsChange = { umountPaths = it },
                    onUmountSave = {
                        val currentEnabled = isUmountEnabled
                        val currentPaths = umountPaths
                        scope.launch(Dispatchers.IO) {
                            val config = UmountConfig(enabled = currentEnabled, paths = currentPaths)
                            val success = UmountConfigManager.saveConfig(context, config)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    showToast(context, context.getString(R.string.umount_config_save_success))
                                } else {
                                    showToast(context, context.getString(R.string.umount_config_save_failed))
                                }
                            }
                        }
                    },
                    flat = flat,
                    isNetIsolateEnabled = isNetIsolateEnabled,
                    onNetIsolateChange = { enabled ->
                        isNetIsolateEnabled = enabled
                        scope.launch(Dispatchers.IO) {
                            setNetIsolateEnabled(enabled)
                            Natives.netIsolateEnable(enabled)
                            withContext(Dispatchers.Main) {
                                snackBarHost.showSnackbar(
                                    context.getString(if (enabled) R.string.netisolate_enable else R.string.netisolate_disable)
                                )
                            }
                        }
                    },
                    niSelectedUids = niSelectedUids,
                    onNiUidToggle = { uid ->
                        scope.launch(Dispatchers.IO) {
                            val newSet = if (uid in niSelectedUids) {
                                Natives.netIsolateUidRemove(uid)
                                niSelectedUids - uid
                            } else {
                                Natives.netIsolateUidAdd(uid)
                                niSelectedUids + uid
                            }
                            writeNetIsolateUids(newSet.joinToString("\n"))
                            withContext(Dispatchers.Main) {
                                niSelectedUids = newSet
                            }
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { NavigationBarsSpacer() }
        }
    }

    if (showFilterSystemWarningDialog.value) {
        PathHideFilterSystemWarningDialog(
            showDialog = showFilterSystemWarningDialog,
            onConfirm = {
                isPathHideFilterSystem = true
                scope.launch(Dispatchers.IO) {
                    setPathHideFilterSystem(true)
                    Natives.pathHideFilterSystem(true)
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(
                            context.getString(R.string.path_hide_filter_system_enabled)
                        )
                    }
                }
            },
        )
    }
}
