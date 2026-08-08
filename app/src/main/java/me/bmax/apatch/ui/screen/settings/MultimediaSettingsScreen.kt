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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.ui.NavigationBarsSpacer

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val snackBarHost = LocalSnackbarHost.current
    val flat = BackgroundConfig.isCustomBackgroundEnabled || BackgroundConfig.settingsBackgroundUri != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_category_multimedia), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
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
                MultimediaSettingsContent(
                    snackBarHost = snackBarHost,
                    flat = flat,
                    highlightKey = highlightKey,
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { NavigationBarsSpacer() }
        }
    }
}
