package me.bmax.apatch.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkSettingsScaffold
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.getSELinuxMode
import me.bmax.apatch.util.isGlobalNamespaceEnabled as checkGlobalNamespaceEnabled
import me.bmax.apatch.util.isMagicMountEnabled as checkMagicMountEnabled
import me.bmax.apatch.util.ui.LocalSnackbarHost

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun GeneralSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    var isGlobalNamespaceEnabled by rememberSaveable { mutableStateOf(false) }
    var namespaceLoaded by remember { mutableStateOf(false) }
    var isMagicMountEnabled by rememberSaveable { mutableStateOf(false) }
    var currentSELinuxMode by rememberSaveable { mutableStateOf("Unknown") }

    LaunchedEffect(kPatchReady, aPatchReady) {
        if (kPatchReady && aPatchReady) {
            withContext(Dispatchers.IO) {
                isGlobalNamespaceEnabled = checkGlobalNamespaceEnabled()
                isMagicMountEnabled = checkMagicMountEnabled()
                currentSELinuxMode = getSELinuxMode()
            }
            namespaceLoaded = true
        }
    }

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null

    FolkSettingsScaffold(
        title = stringResource(R.string.settings_category_general),
        onBack = { navigator.popBackStack() },
        snackbarHostState = snackBarHost,
    ) {
        item(key = "general_content") {
            GeneralSettingsContent(
                kPatchReady = kPatchReady,
                aPatchReady = aPatchReady,
                currentSELinuxMode = currentSELinuxMode,
                onSELinuxModeChange = { currentSELinuxMode = it },
                isGlobalNamespaceEnabled = isGlobalNamespaceEnabled,
                namespaceLoaded = namespaceLoaded,
                onGlobalNamespaceChange = { isGlobalNamespaceEnabled = it },
                isMagicMountEnabled = isMagicMountEnabled,
                onMagicMountChange = { isMagicMountEnabled = it },
                snackBarHost = snackBarHost,
                flat = flat,
                navigator = navigator,
                highlightKey = highlightKey,
            )
        }
    }
}
