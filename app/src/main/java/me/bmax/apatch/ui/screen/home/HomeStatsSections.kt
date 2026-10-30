package me.bmax.apatch.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkType
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

@Composable
internal fun StatsStatusSection(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showUninstallDialog: MutableState<Boolean>,
    countsAvailable: Boolean,
    superuserCount: Int,
    moduleCount: Int,
    onOpenSuperUser: () -> Unit,
    onOpenModule: () -> Unit,
    grid: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)) {
        if (!grid) {
            StatusCardCircle(kpState, apState, navigator, showUninstallDialog)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            ) {
                StatsGridStatusCard(
                    kpState = kpState,
                    apState = apState,
                    navigator = navigator,
                    showUninstallDialog = showUninstallDialog,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
                ) {
                    HomeCountCard(
                        icon = Icons.Outlined.Person,
                        label = stringResource(R.string.superuser),
                        count = superuserCount,
                        onClick = onOpenSuperUser,
                        modifier = Modifier.weight(1f),
                        available = countsAvailable,
                    )
                    HomeCountCard(
                        icon = Icons.Outlined.Extension,
                        label = stringResource(R.string.module),
                        count = moduleCount,
                        onClick = onOpenModule,
                        modifier = Modifier.weight(1f),
                        available = countsAvailable,
                    )
                }
            }
        }
        if (apState != APApplication.State.UNKNOWN_STATE &&
            apState != APApplication.State.ANDROIDPATCH_INSTALLED
        ) {
            AStatusCardCircle(apState)
        }
    }
}

@Composable
private fun StatsGridStatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showUninstallDialog: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val jailbreakState = LocalHomeJailbreakState.current
    val jailbreak = jailbreakState.isActive
    val working = kpState == APApplication.State.KERNELPATCH_INSTALLED
    val update = kpState == APApplication.State.KERNELPATCH_NEED_UPDATE ||
        kpState == APApplication.State.KERNELPATCH_NEED_REBOOT
    val wallpaperMode = LocalFolkWallpaperTokens.current != null
    val container = when {
        wallpaperMode -> LocalFolkWallpaperTokens.current?.group?.fill ?: MaterialTheme.colorScheme.surface
        jailbreak -> MaterialTheme.colorScheme.tertiaryContainer
        working -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val content = if (wallpaperMode) MaterialTheme.colorScheme.onSurface else contentColorFor(container)
    val title = when {
        jailbreak -> stringResource(R.string.settings_jailbreak_mode)
        working -> stringResource(R.string.home_working) +
            if (BackgroundConfig.isListWorkingCardModeHidden) "😋" else ""
        update -> stringResource(R.string.home_kp_need_update)
        else -> stringResource(R.string.home_not_installed)
    }
    val summary = when {
        jailbreak -> stringResource(R.string.settings_jailbreak_mode_summary)
        working -> {
            val full = apState == APApplication.State.ANDROIDPATCH_INSTALLED
            BackgroundConfig.getCustomBadgeText() ?: stringResource(
                if (full) R.string.home_info_mode_full else R.string.home_info_mode_half
            )
        }
        else -> stringResource(R.string.home_click_to_install)
    }
    val icon = when {
        jailbreak -> Icons.Outlined.LockOpen
        working -> Icons.Outlined.CheckCircle
        update -> Icons.Outlined.SystemUpdate
        else -> Icons.Outlined.Warning
    }

    TonalCard(
        modifier = modifier,
        containerColor = container,
        contentColor = content,
        flat = true,
        onClick = {
            when {
                jailbreak -> jailbreakState.performPrimaryAction()
                working && apState == APApplication.State.ANDROIDPATCH_INSTALLED -> {
                    showUninstallDialog.value = true
                }
                !working -> navigator.navigate(InstallModeSelectScreenDestination)
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(FolkDimens.Size16),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
            ) {
                Text(text = title, style = FolkType.Title)
                Text(
                    text = summary,
                    style = FolkType.Caption,
                    color = content.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.align(Alignment.TopEnd).size(FolkDimens.Size48),
                tint = content,
            )
        }
    }
}
