package me.bmax.apatch.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkSettingsScaffold
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.LocalSnackbarHost

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun SecuritySettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null

    FolkSettingsScaffold(
        title = stringResource(R.string.settings_category_security),
        onBack = { navigator.popBackStack() },
        snackbarHostState = snackBarHost,
    ) {
        item(key = "security_content") {
            SecuritySettingsContent(
                snackBarHost = snackBarHost,
                kPatchReady = kPatchReady,
                flat = flat,
                highlightKey = highlightKey,
            )
        }
    }
}
