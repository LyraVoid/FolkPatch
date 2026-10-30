package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.NavGraphs
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.navigation.BottomBarDestination
import me.bmax.apatch.util.AppData
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.HomeBottomSpacer
import me.bmax.apatch.ui.theme.tokens.FolkType

@Composable
fun HomeScreenCircle(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    // Check if update notification is blocked (including when jailbreak mode is active)
    val isJailbreak = LocalHomeJailbreakState.current.isActive
    val kpState = if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE && (apApp.isKernelPatchUpdateBlocked() || isJailbreak)) {
        APApplication.State.KERNELPATCH_INSTALLED
    } else {
        kpState
    }

    val apState = if (apState == APApplication.State.ANDROIDPATCH_NEED_UPDATE && apApp.isAndroidPatchUpdateBlocked()) {
        APApplication.State.ANDROIDPATCH_INSTALLED
    } else {
        apState
    }

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FolkDimens.Size16),
        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
    ) {
        if (BackgroundConfig.isCustomBackgroundEnabled) {
            Spacer(Modifier.height(FolkDimens.Size0))
        }
        
        // Status Card
        StatusCardCircle(kpState, apState, navigator, showUninstallDialog)

        // Superuser and Module Cards
        val showCoreCards = kpState != APApplication.State.UNKNOWN_STATE || LocalHomeJailbreakState.current.isActive
        if (showCoreCards) {
            LaunchedEffect(Unit) {
                AppData.DataRefreshManager.ensureCountsLoaded()
            }
            
            val superuserCount by AppData.DataRefreshManager.superuserCount.collectAsStateWithLifecycle()
            val moduleCount by AppData.DataRefreshManager.apmModuleCount.collectAsStateWithLifecycle()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
            ) {
                TonalCard(modifier = Modifier.weight(1f), flat = true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigator.navigate(BottomBarDestination.SuperUser.direction) {
                                    popUpTo(NavGraphs.root) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = FolkDimens.Size20, vertical = FolkDimens.Size16),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size20),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(FolkDimens.Size16))
                        Column {
                            Text(
                                text = stringResource(R.string.superuser),
                                style = FolkType.Summary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(FolkDimens.Size4))
                            Text(
                                text = superuserCount.toString(),
                                style = FolkType.Numeral,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                
                TonalCard(modifier = Modifier.weight(1f), flat = true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigator.navigate(BottomBarDestination.AModule.direction) {
                                    popUpTo(NavGraphs.root) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = FolkDimens.Size20, vertical = FolkDimens.Size16),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size20),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(FolkDimens.Size16))
                        Column {
                            Text(
                                text = stringResource(R.string.module),
                                style = FolkType.Summary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(FolkDimens.Size4))
                            Text(
                                text = moduleCount.toString(),
                                style = FolkType.Numeral,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        
        // System Patch Detection
        if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
             AStatusCardCircle(apState)
        }

        // Info Card
        InfoCardCircle(kpState, apState)

        // Learn More
        val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
        if (!hideApatchCard) {
            LearnMoreCardCircle()
        }
        
        HomeBottomSpacer()
    }
}

