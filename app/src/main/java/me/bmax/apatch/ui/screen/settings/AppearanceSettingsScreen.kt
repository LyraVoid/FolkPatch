package me.bmax.apatch.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ApiMarketplaceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeStoreScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkSettingsScaffold
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.LocalSnackbarHost

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun AppearanceSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null
    val prefs = APApplication.sharedPreferences

    val themeModeKey = "theme_mode"
    var themeMode by remember { mutableStateOf(prefs.getString(themeModeKey, null)) }

    FolkSettingsScaffold(
        title = stringResource(R.string.settings_category_appearance),
        onBack = { navigator.popBackStack() },
        snackbarHostState = snackBarHost,
    ) {
        item(key = "appearance_content") {
            AppearanceSettingsContent(
                snackBarHost = snackBarHost,
                kPatchReady = kPatchReady,
                onNavigateToThemeStore = { navigator.navigate(ThemeStoreScreenDestination) },
                onNavigateToApiMarketplace = { navigator.navigate(ApiMarketplaceScreenDestination) },
                flat = flat,
                highlightKey = highlightKey,
                themeStoreMode = themeMode,
                onThemeStoreModeChanged = { newMode ->
                    themeMode = newMode
                },
            )
        }
    }
}
