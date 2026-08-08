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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ApiMarketplaceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeStoreScreenDestination
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null
    val prefs = APApplication.sharedPreferences

    val themeModeKey = "theme_mode"
    var themeMode by remember { mutableStateOf(prefs.getString(themeModeKey, null)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_category_appearance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackBarHost) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
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
            item { Spacer(Modifier.height(8.dp)) }
            item { NavigationBarsSpacer() }
        }
    }
}
