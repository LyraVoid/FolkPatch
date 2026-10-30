package me.bmax.apatch.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun BackupSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = APApplication.sharedPreferences
    var autoBackupModule by rememberSaveable { mutableStateOf(prefs.getBoolean("auto_backup_module", false)) }

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null

    FolkSettingsScaffold(
        title = stringResource(R.string.settings_category_backup),
        onBack = { navigator.popBackStack() },
        snackbarHostState = snackBarHost,
    ) {
        item(key = "backup_content") {
            BackupSettingsContent(
                autoBackupModule = autoBackupModule,
                onAutoBackupModuleChange = { autoBackupModule = it },
                flat = flat,
                highlightKey = highlightKey,
            )
        }
    }
}
