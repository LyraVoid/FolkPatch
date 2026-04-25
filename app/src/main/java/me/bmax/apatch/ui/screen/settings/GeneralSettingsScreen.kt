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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.getSELinuxMode
import me.bmax.apatch.util.isGlobalNamespaceEnabled as checkGlobalNamespaceEnabled
import me.bmax.apatch.util.isMagicMountEnabled as checkMagicMountEnabled
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    var isGlobalNamespaceEnabled by rememberSaveable { mutableStateOf(false) }
    var isMagicMountEnabled by rememberSaveable { mutableStateOf(false) }
    var currentSELinuxMode by rememberSaveable { mutableStateOf("Unknown") }

    LaunchedEffect(kPatchReady, aPatchReady) {
        if (kPatchReady && aPatchReady) {
            withContext(Dispatchers.IO) {
                isGlobalNamespaceEnabled = checkGlobalNamespaceEnabled()
                isMagicMountEnabled = checkMagicMountEnabled()
                currentSELinuxMode = getSELinuxMode()
            }
        }
    }

    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_category_general), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
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
                GeneralSettingsContent(
                    kPatchReady = kPatchReady,
                    aPatchReady = aPatchReady,
                    currentSELinuxMode = currentSELinuxMode,
                    onSELinuxModeChange = { currentSELinuxMode = it },
                    isGlobalNamespaceEnabled = isGlobalNamespaceEnabled,
                    onGlobalNamespaceChange = { isGlobalNamespaceEnabled = it },
                    isMagicMountEnabled = isMagicMountEnabled,
                    onMagicMountChange = { isMagicMountEnabled = it },
                    snackBarHost = snackBarHost,
                    flat = flat,
                    navigator = navigator,
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { NavigationBarsSpacer() }
        }
    }
}
