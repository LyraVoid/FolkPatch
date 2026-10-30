package me.bmax.apatch.ui.screen.superuser

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import me.bmax.apatch.ui.component.SegmentedChevron
import me.bmax.apatch.ui.component.folk.navOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.PkgConfig
import me.bmax.apatch.util.SuAuditLog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppItemM3E(
    app: SuperUserViewModel.AppInfo,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: (() -> Unit)? = null,
) {
    val config = app.config
    val rootGranted = config.allow != 0
    val excludeApp = config.exclude == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navOrigin(cornerRadius = FolkDimens.Size20)
            .background(
                if (isSelected && selectionMode) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                else FolkColors.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.packageInfo)
                .crossfade(true)
                .build(),
            contentDescription = app.label,
            modifier = Modifier
                .size(FolkDimens.Size48)
                .clip(CircleShape),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = FolkDimens.Size16),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(modifier = Modifier.padding(top = FolkDimens.Size4)) {
                if (rootGranted) {
                    LabelText(label = "ROOT", containerColor = MaterialTheme.colorScheme.primaryContainer)
                }
                if (excludeApp) {
                    LabelText(
                        label = stringResource(id = R.string.su_pkg_excluded_label),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
        }

        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection?.invoke() },
            )
        } else {
            SegmentedChevron()
        }
    }
}

// ── Legacy App Item ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppItemLegacy(
    app: SuperUserViewModel.AppInfo,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val config = app.config
    var showEditProfile by remember { mutableStateOf(false) }
    var rootGranted by remember { mutableStateOf(config.allow != 0) }
    var excludeApp by remember { mutableIntStateOf(config.exclude) }

    ListItem(
        modifier = Modifier
            .background(
                if (isSelected && selectionMode)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                else FolkColors.Transparent
            )
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelection?.invoke()
                    } else {
                        // Tapping the row only toggles the profile editor;
                        // granting or revoking root goes through the Switch
                        // alone so a stray tap can never strip an app's root
                        // access.
                        showEditProfile = !showEditProfile
                    }
                },
                onLongClick = {
                    if (!selectionMode) {
                        onLongPress?.invoke()
                    }
                }
            ),
        colors = ListItemDefaults.colors(containerColor = FolkColors.Transparent),
        headlineContent = { Text(app.label) },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(app.packageInfo)
                    .crossfade(true).build(),
                contentDescription = app.label,
                modifier = Modifier
                    .padding(FolkDimens.Size4)
                    .width(FolkDimens.Size48)
                    .height(FolkDimens.Size48)
            )
        },
        supportingContent = {
            Column {
                Text(app.packageName)
                FlowRow {
                    if (excludeApp == 1) {
                        LabelText(
                            label = stringResource(id = R.string.su_pkg_excluded_label),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        )
                    }
                    if (rootGranted) {
                        LabelText(label = config.profile.uid.toString())
                        LabelText(label = config.profile.toUid.toString())
                        LabelText(
                            label = when {
                                config.profile.scontext.isNotEmpty() -> config.profile.scontext
                                else -> stringResource(id = R.string.su_selinux_via_hook)
                            }
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection?.invoke() },
                )
            } else {
                ExpressiveSwitch(checked = rootGranted, onCheckedChange = {
                    rootGranted = !rootGranted
                    if (rootGranted) {
                        excludeApp = 0
                        config.allow = 1
                        config.exclude = 0
                        config.profile.scontext = APApplication.MAGISK_SCONTEXT
                    } else {
                        config.allow = 0
                    }
                    config.profile.uid = app.uid
                    PkgConfig.changeConfig(config)
                    if (config.allow == 1) {
                        Natives.grantSu(app.uid, 0, config.profile.scontext)
                        Natives.setUidExclude(app.uid, 0)
                        SuAuditLog.logGrant(app.packageName, app.uid)
                    } else {
                        Natives.revokeSu(app.uid)
                        SuAuditLog.logRevoke(app.packageName, app.uid)
                    }
                })
            }
        },
    )

    AnimatedVisibility(
        visible = showEditProfile && !rootGranted,
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedSwitchItem(
            icon = Icons.Outlined.Security,
            title = stringResource(id = R.string.su_pkg_excluded_setting_title),
            summary = stringResource(id = R.string.su_pkg_excluded_setting_summary),
            checked = excludeApp == 1,
            onCheckedChange = {
                if (it) {
                    excludeApp = 1
                    config.allow = 0
                    config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                    Natives.revokeSu(app.uid)
                    SuAuditLog.logExclude(app.packageName, app.uid)
                } else {
                    excludeApp = 0
                    SuAuditLog.logRevoke(app.packageName, app.uid)
                }
                config.exclude = excludeApp
                config.profile.uid = app.uid
                PkgConfig.changeConfig(config)
                Natives.setUidExclude(app.uid, excludeApp)
            },
        )
    }
}

// ── Label Text Badge ──────────────────────────────────────────────────────

