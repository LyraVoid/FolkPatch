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
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.ui.platform.LocalContext
import me.bmax.apatch.util.ui.showToast

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
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
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp).nestedScroll(scrollBehavior.nestedScrollConnection),
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
                                    snackBarHost.showSnackbar("Kernel spoof enabled")
                                }
                            } else {
                                Natives.utsReset()
                                setUtsSpoofEnabled(false)
                                removeUtsSpoofConfig()
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar("Kernel spoof disabled and restored")
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
                                        snackBarHost.showSnackbar("Kernel spoof failed: $rc")
                                    } else {
                                        snackBarHost.showSnackbar("Kernel spoof applied")
                                    }
                                }
                            } else {
                                Natives.utsReset()
                                setUtsSpoofEnabled(false)
                                removeUtsSpoofConfig()
                                withContext(Dispatchers.Main) {
                                    snackBarHost.showSnackbar("Kernel spoof disabled and restored")
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
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { NavigationBarsSpacer() }
        }
    }
}
